package com.jinlin.mysqlandredis.mysql.transaction;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 【面试题 11】一条 UPDATE 是不是原子性的？为什么？
 *
 * 核心考点深度剖析：
 * 【核心答案】：单条 UPDATE 语句是一定具有原子性的！(要么全部行修改成功，要么全部行回滚为初始状态，绝不会出现部分行成功部分行失败)。
 *
 * 为什么单条 UPDATE 是原子性的？
 * 1. 隐式微型事务 (Implicit Transaction)：
 *    在 MySQL 默认的 `autocommit = 1` 模式下，每一条单独的 SQL 语句在执行开始时，InnoDB 都会隐式开启一个微型事务，并在语句执行完毕后自动提交。
 *
 * 2. Undo Log 与语句级自动回滚 (Statement Rollback)：
 *    当一条 UPDATE 语句批量扫描并更新多行记录时，InnoDB 在物理修改每一行数据页之前，必须先将该行的旧值镜像严格写入 Undo Log。
 *    若在更新到某一行时发生任何异常(如唯一键冲突 Duplicate entry、字段长度溢出、外键约束受阻、死锁冲突)：
 *    InnoDB 会立即触发【语句级回滚 (Statement Rollback)】，利用本次语句生成的 Undo Log，
 *    把当前语句之前已经成功修改的所有行【原路反向全部恢复】！
 *
 * 3. 崩溃恢复保证 (Crash Recovery)：
 *    若在 UPDATE 语句执行中途服务器突发断电宕机，由于没有完成 Commit，
 *    MySQL 重启进入崩溃恢复流程时，会根据 Undo Log 将未完成的单语句事务全部回滚清零。
 */
public class Tx11_SingleUpdateAtomicityDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 11】单条批量 UPDATE 原子性与 Statement Rollback 机制实机检验");
        System.out.println("====================================================================");

        // 1. 初始化唯一键校验表数据
        DbConnectionHelper.executeSqlScript(
                "DELETE FROM tx_user_unique;" +
                "INSERT INTO tx_user_unique (id, user_code, nick_name, score) VALUES " +
                "(1, 'CODE_A', 'Alice', 100), " +
                "(2, 'CODE_B', 'Bob', 200), " +
                "(3, 'CODE_C', 'Charlie', 300);"
        );
        DbConnectionHelper.printQueryResults("【初始状态】tx_user_unique 表初始数据",
                "SELECT id, user_code, nick_name, score FROM tx_user_unique ORDER BY id;"
        );

        // 2. 执行一条试图更新所有行，但中途必定引发唯一键冲突的单条 UPDATE 语句
        System.out.println("\n>>> 执行单条批量 UPDATE: 试图将所有行 user_code 统一改为 'DUPLICATE_CODE'，score 加 1000...");
        System.out.println("   [预期行为]: 更新第 1 行成功；更新第 2 行时触发 uk_user_code 唯一键冲突报错；整条语句触发 Statement Rollback！");

        boolean exceptionCaught = false;
        try (Connection conn = DbConnectionHelper.getConnection()) {
            // 注意：保持默认的 autocommit = true，测试单独一条 SQL 的原子性
            String faultyUpdateSql = "UPDATE tx_user_unique SET user_code = 'DUPLICATE_CODE', score = score + 1000 WHERE id IN (1, 2, 3);";
            try (PreparedStatement ps = conn.prepareStatement(faultyUpdateSql)) {
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            exceptionCaught = true;
            System.err.println("   [捕获预期 SQL 异常]: " + e.getMessage());
        }

        // 3. 验证执行失败后，第 1 行是否被回滚复原
        System.out.println("\n>>> 核验单条 UPDATE 失败后的数据状态 (重点观察 id=1 是否被撤销恢复):");
        DbConnectionHelper.printQueryResults("【失败后数据核查】若 id=1 的 user_code 依然为 'CODE_A' 且 score=100，即证明单条 UPDATE 具备绝对原子性！",
                "SELECT id, user_code, nick_name, score FROM tx_user_unique ORDER BY id;"
        );

        if (exceptionCaught) {
            System.out.println("--> 【实测结论】：即使是一条更新千万行的单条 UPDATE 语句，中途任何错误都会通过 Undo Log 整体回滚，具备强原子性！");
        }
    }
}

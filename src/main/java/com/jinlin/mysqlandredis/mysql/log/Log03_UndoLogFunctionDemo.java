package com.jinlin.mysqlandredis.mysql.log;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 【面试题 03】Undo Log 日志的作用是什么？
 *
 * 核心考点深度剖析：
 * Undo Log (回滚日志) 是 InnoDB 引擎特有的逻辑日志，其在事务架构中发挥着两大不可或缺的核心作用：
 *
 * 作用一：保障事务的【原子性 (Atomicity)】与异常回滚
 * 1. 逻辑逆向操作记录：
 *    - 当执行 INSERT 时，Undo Log 记录一条反向的 DELETE (记录主键)；
 *    - 当执行 DELETE 时，Undo Log 记录被删除整行记录的完整反向 INSERT 语句；
 *    - 当执行 UPDATE 时，Undo Log 记录被更新字段修改前的【历史旧值镜像】。
 * 2. 故障自动回滚：
 *    - 当事务主动执行 ROLLBACK、系统检测到死锁被作为牺牲者、或者 SQL 抛出异常触发语句级回滚 (Statement Rollback) 时，
 *      InnoDB 依据 Undo Log 逆向执行抵消修改，保证“要么全成功，要么全失败回滚”。
 *
 * 作用二：支撑【MVCC 多版本并发控制】与无锁快照读
 * 1. 版本链串联：
 *    - 聚簇索引记录的隐藏列 `DB_ROLL_PTR` (回滚指针) 严格指向该记录最近一次写入 Undo Log 的历史旧版本，
 *      多次修改便形成了一条从【最新版本 -> 最早历史快照】的单向 Undo 版本链。
 * 2. 读写互不阻塞：
 *    - 在 RC 和 RR 隔离级别下，普通 SELECT 语句结合 ReadView 遍历 Undo Log 版本链，
 *      直接读取符合可见性规则的历史快照版本，实现“读不加锁，写不阻塞读”的超高并发。
 *
 * 作用三：Undo Log 的物理存储与异步清理 (Purge)
 * - Undo Log 保存在专门的独立 Undo Tablespace 表空间中 (如 `undo_001`, `undo_002`)；
 * - 当事务已提交，且其产生的 Undo Log 版本不再被任何活跃事务的 ReadView 引用时，
 *   后台的 `Purge` 线程会自动将其物理回收并释放空间。
 */
public class Log03_UndoLogFunctionDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 03】Undo Log 核心双重使命实机剖析 (原子回滚 + MVCC 版本链)");
        System.out.println("====================================================================");

        // 1. 初始化金库余额为 1,000,000 元
        DbConnectionHelper.executeSqlScript(
                "UPDATE log_account_wal SET balance = 1000000.00 WHERE id = 1;"
        );

        // 2. 实测演示: 利用 Undo Log 实现事务原子性逆向反洗回滚
        System.out.println(">>> 演示事务异常回滚过程: 借款扣减 300,000 元，模拟业务异常触发 ROLLBACK...");
        try (Connection conn = DbConnectionHelper.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE log_account_wal SET balance = balance - 300000.00, last_trx_desc = '准备扣减30万' WHERE id = 1;")) {
                ps.executeUpdate();
            }

            System.out.println("   [已写入 Undo Log] InnoDB 已在 Undo Log 中记录了扣减前的旧值 1000000.00 元！");
            System.out.println("   [模拟异常发生] 业务层抛出异常，调用 conn.rollback()...");

            conn.rollback(); // 利用 Undo Log 恢复数据
            System.out.println("   [回滚执行完毕] Undo Log 成功将数据还原！");
        } catch (SQLException e) {
            System.err.println("SQL 异常: " + e.getMessage());
        }

        // 3. 验证回滚结果
        DbConnectionHelper.printQueryResults("【回滚后数据核查】验证 Undo Log 保障数据完好无损",
                "SELECT id, account_name, balance, last_trx_desc FROM log_account_wal WHERE id = 1;"
        );

        // 实机核验 Undo Tablespace 与 Purge 线程状态
        DbConnectionHelper.printQueryResults("当前 MySQL 实例 Undo Log 表空间与 Purge 线程参数",
                "SHOW VARIABLES WHERE Variable_name IN ('innodb_undo_tablespaces', 'innodb_purge_threads', 'innodb_max_undo_log_size');"
        );
    }
}

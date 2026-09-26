package com.jinlin.mysqlandredis.mysql.log;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 【面试题 08】UPDATE 语句的具体执行过程是怎样的？
 *
 * 核心考点深度剖析 (全链路底层架构打通)：
 * 执行一条简单的 `UPDATE log_account_wal SET balance = balance + 500 WHERE id = 1;`
 * 在 MySQL 内部经历以下经典的 8 大全链路关键步骤：
 *
 * 步骤 ① 【连接与鉴权 (Connector)】：
 * 客户端与 MySQL 连接器建立 TCP 连接，完成用户认证、密码匹配与库表权限核验。
 *
 * 步骤 ② 【词法与语法分析 (Parser)】：
 * 分析器识别 SQL 关键词与表名字段，构建语法树，验证 SQL 语法是否合法。
 *
 * 步骤 ③ 【执行计划优化 (Optimizer)】：
 * 优化器分析可用索引，选择最佳成本访问路径(如通过主键 `id` 走聚簇索引点查)，生成最优执行计划。
 *
 * 步骤 ④ 【执行器调用引擎接口 (Executor -> Storage Engine)】：
 * 执行器调用 InnoDB 引擎接口，请求读取 `id = 1` 这一行记录。
 *
 * 步骤 ⑤ 【缓存命中检查与磁盘加载 (Buffer Pool)】：
 * InnoDB 查看内存 Buffer Pool 中是否已有包含该记录的 16KB 数据页：
 * - 若在内存中：直接读取内存数据；
 * - 若不在内存中：从物理磁盘 `.ibd` 文件中将该数据页加载到 Buffer Pool。
 *
 * 步骤 ⑥ 【写 Undo Log 与内存物理更新 (脏页产生)】：
 * - InnoDB 先将该行修改前的历史旧值 (如 balance=1000000) 写入 Undo Log Buffer (用于回滚和 MVCC)；
 * - 随后在 Buffer Pool 内存页中修改 balance 字段为新值，此时该数据页变为【脏页 (Dirty Page)】；
 * - 同时在 Redo Log Buffer 中追加记录本次物理页层面的二进制修改。
 *
 * 步骤 ⑦ 【两阶段提交保证一致性 (2PC)】：
 * - 阶段 1: 将 Redo Log 刷入磁盘，并将内部事务标记为 `prepare` 状态；
 * - 阶段 2: 执行器将该事务的 Event 写入 Server 层的 Binlog 文件并调用 `fsync` 刷盘；
 * - 阶段 3: 执行器调用引擎接口，将 Redo Log 状态置为 `commit`，向客户端返回更新成功影响 1 行！
 *
 * 步骤 ⑧ 【异步后台刷脏 (Page Cleaner Thread)】：
 * 后台 Page Cleaner 线程在系统空闲或检查点推进时，异步将 Buffer Pool 中的物理脏页写回磁盘 `.ibd` 文件。
 */
public class Log08_UpdateExecutionLifecycleDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 08】UPDATE 语句全链路 8 步执行生命周期实机剖析");
        System.out.println("====================================================================");

        // 1. 实操执行一条真正的 UPDATE 语句
        System.out.println(">>> 客户端发起 UPDATE 事务请求: 给金库 A (id=1) 充值 500,000 元...");
        try (Connection conn = DbConnectionHelper.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE log_account_wal SET balance = balance + 500000.00, last_trx_desc = '2PC正常提交' WHERE id = 1;")) {
                int affectedRows = ps.executeUpdate();
                System.out.printf("   [更新成功] 影响行数 = %d 行 (经历了 Undo/Redo Prepare/Binlog/Commit 全流程)！%n", affectedRows);
            }

            conn.commit();
        } catch (SQLException e) {
            System.err.println("UPDATE 异常: " + e.getMessage());
        }

        // 2. 打印最新状态
        DbConnectionHelper.printQueryResults("【UPDATE 提交后数据状态】",
                "SELECT id, account_name, balance, last_trx_desc FROM log_account_wal WHERE id = 1;"
        );

        // 3. 打印全链路流程总结
        System.out.println(">>> 全链路 8 步流转精炼总结：");
        System.out.println("   [连接器] -> [分析器] -> [优化器] -> [执行器] -> [Buffer Pool 读写] -> [记 Undo] -> [改内存脏页并记 Redo Prepare] -> [写 Binlog] -> [Redo Commit] -> [异步落盘]");
    }
}

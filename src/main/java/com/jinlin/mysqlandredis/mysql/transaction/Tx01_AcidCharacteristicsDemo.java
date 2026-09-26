package com.jinlin.mysqlandredis.mysql.transaction;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 【面试题 01】事务的特性是什么？如何实现的？
 *
 * 核心考点剖析：
 * 1. 事务四大核心特性 (ACID)：
 *    - A (Atomicity, 原子性)：事务是不可分割的最小工作单元，事务中的操作要么全部发生，要么全部不发生。
 *      【底层实现】：依赖 InnoDB 的 Undo Log (回滚日志)。每次做 DML 操作时，InnoDB 都会记录反向操作(INSERT -> DELETE, DELETE -> INSERT, UPDATE -> 旧值镜像)。
 *                 当事务发生 ROLLBACK、死锁牺牲或系统异常时，利用 Undo Log 将数据反向恢复。
 *    - C (Consistency, 一致性)：事务执行前后，数据库从一个合法状态转换到另一个合法状态，满足所有完整性约束与业务守恒规则(如转账总金额不变)。
 *      【底层实现】：一致性是事务追求的【终极目标】。它由 A(原子性)、I(隔离性)、D(持久性)共同作为底层基石，再配合应用层业务逻辑约束最终达成。
 *    - I (Isolation, 隔离性)：多个并发事务同时操作同一数据时，各事务之间互不干扰、相互隔离。
 *      【底层实现】：依赖【锁机制】(行级排他锁/共享锁、间隙锁解决写写冲突) + 【MVCC 多版本并发控制】(解决读写冲突，读不加锁)。
 *    - D (Durability, 持久性)：事务一旦提交，其所做的物理数据修改将永久保存在数据库中，即使操作系统崩溃或服务器断电也不会丢失。
 *      【底层实现】：依赖 InnoDB 的 Redo Log (重做日志) 与 WAL (Write-Ahead Logging，先写日志后写磁盘) 机制。
 *                 事务提交前只需确保 Redo Log 顺序刷盘(由 innodb_flush_log_at_trx_commit 控制)，崩溃重启时通过 Redo Log 前滚重做恢复未落盘的脏页。
 */
public class Tx01_AcidCharacteristicsDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 01】事务的特性是什么？如何实现的？(ACID 全生命周期实测)");
        System.out.println("====================================================================");

        // 1. 初始化张三(id=1)和李四(id=2)的初始资金为 1000 元
        DbConnectionHelper.executeSqlScript(
                "UPDATE tx_account SET balance = 1000.00 WHERE id IN (1, 2);"
        );
        printAccountBalances("【初始状态】张三和李四余额");

        // 2. 验证 Atomicity (原子性) 与 Undo Log 机制：转账中途异常触发 ROLLBACK
        System.out.println("\n>>> [实测 1] 演示原子性 (Atomicity): 张三转账 500 元给李四，中途模拟异常回滚...");
        try (Connection conn = DbConnectionHelper.getConnection()) {
            conn.setAutoCommit(false); // 开启事务

            // 步骤一：张三账户扣款 500 元 (写入 Undo Log 旧值 1000.00)
            try (PreparedStatement ps = conn.prepareStatement("UPDATE tx_account SET balance = balance - 500 WHERE id = 1;")) {
                ps.executeUpdate();
            }

            // 模拟发生异常 (如系统停电、网络中断或业务校验失败)
            boolean errorOccurred = true;
            if (errorOccurred) {
                System.out.println("   [模拟事故] 转账业务发生异常，触发 conn.rollback() 回滚！");
                conn.rollback(); // 利用 Undo Log 反向恢复张三扣减的金额
            } else {
                conn.commit();
            }
        } catch (SQLException e) {
            System.err.println("SQL 执行失败: " + e.getMessage());
        }

        printAccountBalances("【回滚后验证】张三和李四余额 (验证原子性: 张三扣除的 500 元因回滚完全复原)");

        // 3. 验证 Durability (持久性) 与 Redo Log 机制：转账正常 COMMIT
        System.out.println("\n>>> [实测 2] 演示持久性 (Durability): 张三成功转账 200 元给李四并 COMMIT...");
        try (Connection conn = DbConnectionHelper.getConnection()) {
            conn.setAutoCommit(false); // 开启事务

            // 张三扣款 200
            try (PreparedStatement ps1 = conn.prepareStatement("UPDATE tx_account SET balance = balance - 200 WHERE id = 1;")) {
                ps1.executeUpdate();
            }
            // 李四入账 200
            try (PreparedStatement ps2 = conn.prepareStatement("UPDATE tx_account SET balance = balance + 200 WHERE id = 2;")) {
                ps2.executeUpdate();
            }

            conn.commit(); // 提交事务：Redo Log 刷盘并置为 Commit 状态
            System.out.println("   [事务提交] conn.commit() 执行完毕，Redo Log 落盘保证持久性！");
        } catch (SQLException e) {
            System.err.println("SQL 执行失败: " + e.getMessage());
        }

        printAccountBalances("【提交后验证】张三和李四余额 (总金额守恒保持 2000，验证一致性与持久性)");
    }

    private static void printAccountBalances(String stage) {
        DbConnectionHelper.printQueryResults(stage,
                "SELECT id, user_name, balance FROM tx_account WHERE id IN (1, 2) ORDER BY id;"
        );
    }
}

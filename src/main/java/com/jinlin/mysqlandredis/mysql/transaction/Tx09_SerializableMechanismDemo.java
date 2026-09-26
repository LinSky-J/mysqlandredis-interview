package com.jinlin.mysqlandredis.mysql.transaction;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 【面试题 09】串行化隔离级别是通过什么实现的？
 *
 * 核心考点剖析：
 * 1. 串行化 (Serializable) 的底层实现原理：
 *    - 【核心机制】：读写全加锁，普通读自动隐式加共享锁 (S 锁)，MVCC 快照读机制退化失效！
 *    - 具体行为：
 *      ① 在该级别下，InnoDB 会将所有普通的 `SELECT` 查询语句隐式转换为 `SELECT ... LOCK IN SHARE MODE` (加共享锁/临键锁)。
 *      ② 所有写操作 (`INSERT` / `UPDATE` / `DELETE`) 自动加排他锁 (X 锁)。
 *      ③ 根据锁兼容矩阵：
 *         - S 锁与 S 锁兼容 (读读可并发)；
 *         - S 锁与 X 锁互斥 (读会阻塞写，写会阻塞读)；
 *         - X 锁与 X 锁互斥 (写写互斥)。
 *    - 结果：通过锁的互斥性，使得对相同数据的读写操作严格排队串行化执行，彻底杜绝脏读、不可重复读和幻读。
 *
 * 2. 代价与缺陷：
 *    - 并发吞吐量暴跌，读操作严重阻塞写操作，极易频繁爆发死锁 (Deadlock) 和锁等待超时 (Lock wait timeout)。
 *    - 生产系统除特殊严苛审计/计费对账外，极少采用该隔离级别。
 */
public class Tx09_SerializableMechanismDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 09】串行化 (Serializable) 隔离级别底层加锁实现机制实测");
        System.out.println("====================================================================");

        // 初始化测试数据
        DbConnectionHelper.executeSqlScript("UPDATE tx_account SET balance = 1000.00 WHERE id = 1;");

        CountDownLatch readSharedLockAcquired = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(2);

        // 线程 1: 事务 A 在 SERIALIZABLE 级别下仅执行普通 SELECT 查询
        new Thread(() -> {
            try (Connection connA = DbConnectionHelper.getConnection()) {
                connA.setAutoCommit(false);

                // 设置隔离级别为 SERIALIZABLE
                try (java.sql.Statement stmt = connA.createStatement()) {
                    stmt.execute("SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;");
                }

                System.out.println(">>> [事务 A (Serializable)] 开启事务，执行【普通 SELECT】查询 (此时 InnoDB 隐式自动加 S 共享锁)...");
                try (PreparedStatement ps = connA.prepareStatement("SELECT balance FROM tx_account WHERE id = 1;")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            System.out.println("   [事务 A 读到数据] 余额 = " + rs.getDouble("balance") + "，S 共享锁已挂载！");
                        }
                    }
                }

                readSharedLockAcquired.countDown(); // 通知线程 B 尝试更新

                // 事务 A 持有 S 锁 1.5 秒
                Thread.sleep(1500);

                connA.commit();
                System.out.println(">>> [事务 A] 提交事务 COMMIT，释放 S 共享锁！");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                finishLatch.countDown();
            }
        }, "TxA-SerializableReader").start();

        // 线程 2: 事务 B 尝试执行 UPDATE (需要获取 X 排他锁)
        new Thread(() -> {
            try (Connection connB = DbConnectionHelper.getConnection()) {
                connB.setAutoCommit(false);
                readSharedLockAcquired.await(3, TimeUnit.SECONDS);

                System.out.println(">>> [事务 B] 尝试执行 UPDATE tx_account SET balance = 2000 WHERE id = 1 (请求 X 锁)...");
                long startTime = System.currentTimeMillis();

                try (PreparedStatement ps = connB.prepareStatement(
                        "UPDATE tx_account SET balance = 2000.00 WHERE id = 1;")) {
                    ps.executeUpdate();
                }

                long blockedTime = System.currentTimeMillis() - startTime;
                System.out.printf("   [事务 B 更新成功] 事务 B 被事务 A 的 S 共享锁阻塞了 %d ms，验证了普通读自动加 S 锁阻塞写！%n",
                        blockedTime);

                connB.commit();
            } catch (Exception e) {
                System.err.println("事务 B 异常: " + e.getMessage());
            } finally {
                finishLatch.countDown();
            }
        }, "TxB-Writer").start();

        try {
            finishLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        DbConnectionHelper.printQueryResults("【最终状态核验】验证串行化执行完毕",
                "SELECT id, user_name, balance FROM tx_account WHERE id = 1;"
        );
    }
}

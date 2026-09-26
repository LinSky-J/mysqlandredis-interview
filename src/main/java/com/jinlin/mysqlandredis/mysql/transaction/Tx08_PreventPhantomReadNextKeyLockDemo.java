package com.jinlin.mysqlandredis.mysql.transaction;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 【面试题 08】MySQL 设置了可重复读隔离级别后，怎么保证不发生幻读？
 *
 * 核心考点剖析：
 * MySQL InnoDB 并非只靠单一技术，而是采用【快照读 MVCC + 当前读 Next-Key Lock】双管齐下的组合拳：
 *
 * 1. 应对快照读 (普通 SELECT)：依靠【MVCC ReadView】
 *    - 机制：事务在第一次执行 SELECT 时创建 ReadView，后续查询全程复用该一致性视图。
 *    - 效果：即使其他事务 INSERT 了新记录并提交，由于事务 ID 比较规则，新记录对快照读不可见。
 *
 * 2. 应对当前读 (SELECT FOR UPDATE / LOCK IN SHARE MODE / UPDATE / DELETE)：依靠【Next-Key Lock 临键锁】
 *    - 机制：Next-Key Lock 是【记录锁 (Record Lock) + 间隙锁 (Gap Lock)】的组合体。
 *    - 效果：当执行范围当前读(如 `SELECT * FROM tx_account WHERE id >= 20 FOR UPDATE;`)时，
 *           InnoDB 不仅给已有记录加 X 行锁，还会将区间 `[20, +∞)` 全部打上间隙锁 (Gap Lock)！
 *    - 关键阻断点：若其他并发事务此时试图在被锁定的间隙内执行 `INSERT INTO ... VALUES (30, ...)`，
 *                必须获取插入意向锁 (Insert Intention Lock)，而插入意向锁与间隙锁天然互斥！
 *                其他事务的 INSERT 将被【强制挂起阻塞】，直到本事务 COMMIT 释放锁！
 *    - 结论：彻底从物理层杜绝了并发新增数据的可能性，彻底消除了幻读！
 */
public class Tx08_PreventPhantomReadNextKeyLockDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 08】Next-Key Lock 临键锁如何物理杜绝当前读幻读实战测试");
        System.out.println("====================================================================");

        // 清理测试环境：保证当前无 id >= 30 的数据
        DbConnectionHelper.executeSqlScript("DELETE FROM tx_account WHERE id >= 30;");

        CountDownLatch lockAcquiredLatch = new CountDownLatch(1);
        CountDownLatch insertAttemptLatch = new CountDownLatch(1);

        // 线程 1: 事务 B 执行范围当前读加锁 (持有 id >= 20 的 Next-Key Lock / 间隙锁)
        Thread threadB = new Thread(() -> {
            try (Connection connB = DbConnectionHelper.getConnection()) {
                connB.setAutoCommit(false);
                System.out.println(">>> [事务 B] 开启事务，执行范围当前读 SELECT * FROM tx_account WHERE id >= 20 FOR UPDATE;");
                try (PreparedStatement ps = connB.prepareStatement(
                        "SELECT id, user_name FROM tx_account WHERE id >= 20 FOR UPDATE;")) {
                    ps.executeQuery();
                }
                System.out.println("   [事务 B 临键锁已挂载] 区间 [20, +∞) 已被 Next-Key Lock 锁定，间隙锁已生效！");
                lockAcquiredLatch.countDown(); // 通知事务 A 尝试插入

                // 持有锁 1.5 秒
                Thread.sleep(1500);

                connB.commit();
                System.out.println(">>> [事务 B] 提交事务 COMMIT，释放 Next-Key Lock 间隙锁！");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "TxB-NextKeyLock");

        // 线程 2: 事务 A 尝试在锁定的间隙内执行 INSERT (id=35)
        Thread threadA = new Thread(() -> {
            try (Connection connA = DbConnectionHelper.getConnection()) {
                connA.setAutoCommit(false);
                lockAcquiredLatch.await(3, TimeUnit.SECONDS);

                System.out.println(">>> [事务 A] 尝试在被锁定的间隙内插入新记录 (id=35, balance=999)...");
                long startTime = System.currentTimeMillis();

                try (PreparedStatement ps = connA.prepareStatement(
                        "INSERT INTO tx_account (id, user_name, balance, version) VALUES (35, '间隙插入测试', 999.00, 0);")) {
                    ps.executeUpdate();
                }
                long blockTime = System.currentTimeMillis() - startTime;
                System.out.printf("   [事务 A 插入成功] 事务 A 之前被 Next-Key Lock 间隙锁阻塞了 %d ms，直到事务 B 提交后才完成插入！%n",
                        blockTime);

                connA.commit();
            } catch (Exception e) {
                System.err.println("事务 A 插入被阻塞并抛出: " + e.getMessage());
            } finally {
                insertAttemptLatch.countDown();
            }
        }, "TxA-Insert");

        threadB.start();
        threadA.start();

        try {
            insertAttemptLatch.await(5, TimeUnit.SECONDS);
            threadB.join();
            threadA.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 清理插入的测试数据
        DbConnectionHelper.executeSqlScript("DELETE FROM tx_account WHERE id >= 30;");

        System.out.println("\n>>> 【核心结论归纳】：");
        System.out.println("1. 快照读：通过 MVCC 的 ReadView 保证读取历史版本，屏蔽外部 INSERT。");
        System.out.println("2. 当前读：通过 Next-Key Lock 锁定记录及其间隙，物理阻塞外部 INSERT，彻底消灭幻读！");
    }
}

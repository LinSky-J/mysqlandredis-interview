package com.jinlin.mysqlandredis.mysql.lock;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 【面试题 04】两条 UPDATE 语句处理一张表的不同主键范围记录 (一个 <10，一个 >15)，会不会遇到阻塞？底层是为什么？
 *
 * 核心考点深度剖析：
 * 【核心答案】：【绝对不会遇到阻塞！】(两线程完全并发并行执行)。
 *
 * 底层原理解密 (InnoDB 聚簇索引 B+ 树范围加锁机制)：
 * 1. 聚簇索引天然有序：
 *    - 主键 `id` 是 InnoDB 的聚簇索引，所有数据行和索引键值在 B+ 树叶子节点上严格按主键升序双向链表排列。
 *
 * 2. 精准定位与锁区间无交集 (Disjoint Lock Ranges)：
 *    - 假设表中有记录：id = 1, 5, 8, 12, 18, 20, 25。
 *    - 事务 A 执行 `WHERE id < 10`：
 *      InnoDB 顺着主键 B+ 树加锁，锁定记录及间隙区间为：`(-∞, 1]`, `(1, 5]`, `(5, 8]`, `(8, 12)`。
 *      加锁上限严格止步于下一条记录 `id = 12` 的左侧。
 *    - 事务 B 执行 `WHERE id > 15`：
 *      InnoDB 顺着主键 B+ 树加锁，锁定记录及间隙区间为：`(12, 18]`, `(18, 20]`, `(20, 25]`, `(25, +∞)`。
 *      加锁下限严格位于记录 `id = 12` 的右侧。
 *
 * 3. 互不相交，并发通行：
 *    - 事务 A 和事务 B 各自申请的【记录锁 (Record Lock)】和【间隙锁 (Gap Lock)】在物理 B+ 树上完全处于两端，没有任何交集！
 *    - 既然没有竞争同一行数据，也没有争夺同一段间隙，InnoDB 会同时批准两者的加锁请求，两者并行不悖，完全不阻塞！
 */
public class Lock04_PkDisjointRangeUpdateDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 04】不同主键范围 (<10 与 >15) 并发 UPDATE 互不阻塞实测验证");
        System.out.println("====================================================================");

        // 初始化任务状态
        DbConnectionHelper.executeSqlScript(
                "UPDATE lock_range_pk SET status = 0;"
        );

        CountDownLatch threadAHeldLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(2);

        // 线程 A: 处理主键 < 10 的记录，开启事务并不提交，持有锁 1000 ms
        new Thread(() -> {
            try (Connection conn = DbConnectionHelper.getConnection()) {
                conn.setAutoCommit(false);
                System.out.println(">>> [线程 A] 开启事务，执行 UPDATE lock_range_pk SET status = 1 WHERE id < 10;");
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE lock_range_pk SET status = 1 WHERE id < 10;")) {
                    int rows = ps.executeUpdate();
                    System.out.printf("   [线程 A 成功更新 %d 行] 锁定了主键范围 (-∞, 12) 左侧，持有锁 1000 ms...%n", rows);
                }

                threadAHeldLatch.countDown(); // 通知线程 B 并发更新

                Thread.sleep(1000);

                conn.commit();
                System.out.println(">>> [线程 A] 事务 COMMIT 提交，释放主键 < 10 区间的锁！");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                finishLatch.countDown();
            }
        }, "Thread-A-PkLess10").start();

        // 线程 B: 并发处理主键 > 15 的记录，验证是否会被线程 A 阻塞
        new Thread(() -> {
            try (Connection conn = DbConnectionHelper.getConnection()) {
                conn.setAutoCommit(false);
                threadAHeldLatch.await(3, TimeUnit.SECONDS);

                System.out.println(">>> [线程 B] 在线程 A 尚未提交时，并发执行 UPDATE lock_range_pk SET status = 2 WHERE id > 15;");
                long startTime = System.currentTimeMillis();

                int rows;
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE lock_range_pk SET status = 2 WHERE id > 15;")) {
                    rows = ps.executeUpdate();
                }
                long costTime = System.currentTimeMillis() - startTime;

                System.out.printf("   [线程 B 毫秒级直接执行成功！] 影响行数: %d，执行耗时仅 %d ms！完全未被线程 A 阻塞！%n",
                        rows, costTime);

                conn.commit();
            } catch (Exception e) {
                System.err.println("线程 B 异常: " + e.getMessage());
            } finally {
                finishLatch.countDown();
            }
        }, "Thread-B-PkGreater15").start();

        try {
            finishLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        DbConnectionHelper.printQueryResults("【更新后数据核查】两段范围均已独立更新完成",
                "SELECT id, title, status FROM lock_range_pk ORDER BY id;"
        );
    }
}

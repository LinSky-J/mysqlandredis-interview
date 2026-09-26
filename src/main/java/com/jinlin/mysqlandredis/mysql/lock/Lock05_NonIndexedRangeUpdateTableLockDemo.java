package com.jinlin.mysqlandredis.mysql.lock;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 【面试题 05】如果两个范围不是主键或索引？还会阻塞吗？
 *
 * 核心考点深度剖析 (高频慢 SQL 故障与线上全表锁死根源)：
 * 【核心答案】：【一定会严重阻塞！】
 *
 * 为什么业务范围看似不重叠，无索引却会导致互斥阻塞？
 * 1. InnoDB 行锁的物理本质：
 *    - InnoDB 的行级锁不是挂在物理数据本身上的，而是挂在【B+ 树索引项】上的！
 *
 * 2. 无索引导致被迫全表扫描 (Table Scan, type=ALL)：
 *    - 当执行 `UPDATE ... WHERE age < 10` 时，由于 `age` 列没有任何索引，优化器无法走索引范围查找，只能命令引擎进行全表聚簇索引扫描！
 *
 * 3. 行锁与间隙锁退化为“锁全表”：
 *    - 在扫描整张表的过程中，InnoDB 必须对扫描到的【每一行聚簇索引记录】都强制打上 X 排他锁；
 *    - 在默认 REPEATABLE READ (可重复读) 隔离级别下，为了防止其他事务并发插入满足条件的行，InnoDB 还会把整张表所有的记录间隙全部打上【间隙锁 (Gap Lock)】！
 *    - 结果：整张表从头到尾、每一行记录、每一处缝隙全部被锁死，在实际表现上完全退化成了【全表锁死】！
 *
 * 4. 线程 B 扫描第一行即刻撞锁被挂起：
 *    - 另一个事务执行 `UPDATE ... WHERE age > 15` 时，同样因为没有索引而必须从表头开始全表扫描；
 *    - 结果第一条记录就碰到了事务 A 持有的 X 排他锁，瞬间被挂起阻塞，无法继续执行！
 */
public class Lock05_NonIndexedRangeUpdateTableLockDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 05】无索引范围更新 (<10 与 >15) 退化为全表锁死与严重阻塞实测");
        System.out.println("====================================================================");

        // 1. 初始化无索引表分数
        DbConnectionHelper.executeSqlScript(
                "UPDATE lock_range_non_index SET score = 100;"
        );

        // 2. 查看执行计划: 验证无索引时走的是全表扫描 type=ALL
        System.out.println(">>> 观察无索引列查询的执行计划 EXPLAIN (验证 type=ALL 全表扫描):");
        DbConnectionHelper.printQueryResults("EXPLAIN UPDATE 执行计划",
                "EXPLAIN SELECT id, age, user_name FROM lock_range_non_index WHERE age < 10;"
        );

        CountDownLatch threadAHeldLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(2);

        // 线程 A: 执行 WHERE age < 10 (age 无索引，导致全表所有行和间隙全部被锁)
        new Thread(() -> {
            try (Connection conn = DbConnectionHelper.getConnection()) {
                conn.setAutoCommit(false);
                System.out.println(">>> [线程 A] 开启事务，执行 UPDATE lock_range_non_index SET score = 200 WHERE age < 10;");
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE lock_range_non_index SET score = 200 WHERE age < 10;")) {
                    int rows = ps.executeUpdate();
                    System.out.printf("   [线程 A 成功更新 %d 行] 由于 age 无索引走全表扫描，全表所有记录和间隙已被锁死！持有锁 1200 ms...%n", rows);
                }

                threadAHeldLatch.countDown(); // 通知线程 B 并发更新

                Thread.sleep(1200);

                conn.commit();
                System.out.println(">>> [线程 A] 事务 COMMIT 提交，释放全表行锁与间隙锁！");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                finishLatch.countDown();
            }
        }, "Thread-A-NoIndexLess10").start();

        // 线程 B: 执行看似不相关的 WHERE age > 15，实测被全表锁死阻塞！
        new Thread(() -> {
            try (Connection conn = DbConnectionHelper.getConnection()) {
                conn.setAutoCommit(false);
                threadAHeldLatch.await(3, TimeUnit.SECONDS);

                System.out.println(">>> [线程 B] 尝试执行 UPDATE lock_range_non_index SET score = 300 WHERE age > 15;");
                long startBlockTime = System.currentTimeMillis();

                int rows;
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE lock_range_non_index SET score = 300 WHERE age > 15;")) {
                    rows = ps.executeUpdate();
                }

                long blockDuration = System.currentTimeMillis() - startBlockTime;
                System.out.printf("   [线程 B 终于完成更新！] 影响行数: %d，被整整阻塞了 %d ms (验证无索引导致全表锁死互斥)！%n",
                        rows, blockDuration);

                conn.commit();
            } catch (Exception e) {
                System.err.println("线程 B 异常: " + e.getMessage());
            } finally {
                finishLatch.countDown();
            }
        }, "Thread-B-NoIndexGreater15").start();

        try {
            finishLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        DbConnectionHelper.printQueryResults("【最终数据确认】两批更新最终状态",
                "SELECT id, age, user_name, score FROM lock_range_non_index ORDER BY id;"
        );

        System.out.println("\n>>> 【生产架构血泪教训总结】：");
        System.out.println("1. 严禁对没有索引的列执行 UPDATE / DELETE！否则会把整张表从头到尾全部锁死，并发吞吐归零；");
        System.out.println("2. 即使是在 RC 隔离级别下，虽然没有间隙锁，但全表扫描仍会在每一行上先加行锁，不匹配才释放，仍然会产生剧烈的行锁争抢；");
        System.out.println("3. 生产发布 DML 脚本前，必须在测试环境 EXPLAIN 确认 `key` 走到了索引，防止锁全表事故！");
    }
}

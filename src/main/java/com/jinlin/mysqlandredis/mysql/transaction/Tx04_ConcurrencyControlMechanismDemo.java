package com.jinlin.mysqlandredis.mysql.transaction;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 【面试题 04】MySQL 是怎么解决并发问题的？
 *
 * 核心考点剖析：
 * MySQL InnoDB 解决并发事务冲突依赖三大协同架构支柱：
 *
 * 1. 支柱一：MVCC (多版本并发控制) —— 解决【读-写并发冲突】
 *    - 核心价值：实现“读不加锁，写不阻塞读”。
 *    - 在 RC 和 RR 隔离级别下，普通 SELECT 语句属于【快照读 (Consistent Read)】，
 *      读取的是 Undo Log 链条中的历史快照版本，与写事务持有的行锁互不干扰，大幅提升读吞吐量。
 *
 * 2. 支柱二：锁机制 (Locking) —— 解决【写-写并发冲突】与【当前读强一致性】
 *    - 行级锁粒度：
 *      ① Record Lock (记录锁)：精准锁定单条索引记录，防止其他事务 UPDATE/DELETE。
 *      ② Gap Lock (间隙锁)：锁定索引记录之间的开区间，防止其他事务并发 INSERT 插入。
 *      ③ Next-Key Lock (临键锁)：Record Lock + Gap Lock 的组合(左开右闭区间)，InnoDB 在 RR 下的默认加锁算法，从物理底层防止幻读。
 *    - 意向锁 (IS / IX)：表级意向锁，用于事务在加表锁时快速获知表内是否有被锁定的行，避免遍历整表。
 *
 * 3. 支柱三：事务隔离级别 (Isolation Level) —— 策略编排
 *    - 组合 MVCC 与不同的锁范围策略(RU / RC / RR / Serializable)，
 *      让架构师根据业务场景在【吞吐性能】与【一致性保障】之间做出最佳权衡。
 */
public class Tx04_ConcurrencyControlMechanismDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 04】MySQL 并发解决机制实测: MVCC(读写不冲突) + 锁(写写互斥)");
        System.out.println("====================================================================");

        // 初始化商品库存
        DbConnectionHelper.executeSqlScript(
                "UPDATE tx_product_stock SET stock_count = 10 WHERE product_id = 101;"
        );

        CountDownLatch writeLockHeldLatch = new CountDownLatch(1);
        CountDownLatch readFinishedLatch = new CountDownLatch(1);
        CountDownLatch finishAllLatch = new CountDownLatch(2);

        // 线程 1: 写事务 (开启事务，更新库存并不提交，持有 product_id=101 的 X 行锁)
        new Thread(() -> {
            try (Connection conn = DbConnectionHelper.getConnection()) {
                conn.setAutoCommit(false);
                System.out.println(">>> [写线程 1] 开启事务，执行 UPDATE 扣减库存为 5，持有行级排他锁(X锁)...");
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE tx_product_stock SET stock_count = 5 WHERE product_id = 101;")) {
                    ps.executeUpdate();
                }

                writeLockHeldLatch.countDown(); // 通知读线程和第二写线程开始测试

                // 等待读线程完成 MVCC 测试
                readFinishedLatch.await(3, TimeUnit.SECONDS);

                Thread.sleep(1000); // 维持锁持有状态
                conn.commit();
                System.out.println(">>> [写线程 1] 事务最终 COMMIT 提交，释放行锁！");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                finishAllLatch.countDown();
            }
        }, "Writer-1").start();

        // 线程 2: 读线程 (验证 MVCC: 写事务持有锁时，普通 SELECT 快照读无锁畅通无阻，读到历史值 10)
        new Thread(() -> {
            try (Connection conn = DbConnectionHelper.getConnection()) {
                writeLockHeldLatch.await(3, TimeUnit.SECONDS);
                System.out.println(">>> [读线程 2] 在写线程 1 持有 X 锁的情况下执行普通 SELECT 查询...");

                long startTime = System.currentTimeMillis();
                int currentStock = -1;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT stock_count FROM tx_product_stock WHERE product_id = 101;")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            currentStock = rs.getInt("stock_count");
                        }
                    }
                }
                long costTime = System.currentTimeMillis() - startTime;
                System.out.printf("   [MVCC 生效] 读线程耗时仅 %d ms (完全未被写锁阻塞)！读到快照库存为: %d (验证读不加锁，写不阻塞读)%n",
                        costTime, currentStock);

                readFinishedLatch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                finishAllLatch.countDown();
            }
        }, "Reader-2").start();

        try {
            finishAllLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        DbConnectionHelper.printQueryResults("【最终数据确认】商品库存最终状态",
                "SELECT product_id, product_name, stock_count FROM tx_product_stock WHERE product_id = 101;"
        );
    }
}

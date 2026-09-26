package com.jinlin.mysqlandredis.mysql.transaction;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 【面试题 12】滥用事务，或者一个事务里有特别多 SQL 的弊端？
 *
 * 核心考点深度剖析 (生产级架构避坑指南与高频事故根源)：
 * 在企业级生产环境中，【大事务 (Large Transaction)】与【长事务 (Long-Running Transaction)】是引发数据库雪崩的头号元凶！
 *
 * 常见诱因：
 * ① 在 `@Transactional` 方法内调用远程 RPC、发送短信、调用微信支付 HTTP 接口、大文件上传等网络 I/O；
 * ② 在单事务中一次性循环执行数十万条 INSERT / UPDATE；
 * ③ 盲目使用事务包裹不必要的只读查询。
 *
 * 五大致命弊端：
 * 1. 锁资源锁定过久，并发吞吐断崖式下跌，引发批量锁等待超时与死锁：
 *    - 事务内获取的所有行锁、间隙锁，直到 COMMIT 或 ROLLBACK 时才会释放。
 *    - 长事务霸占行锁数秒甚至数分钟，导致其他所有并发线程只能阻塞排队，最终集体爆发 `Lock wait timeout exceeded`！
 *
 * 2. Undo Log 严重堆积膨胀，物理磁盘被撑爆：
 *    - InnoDB 后台 Purge 线程只能清理早于系统所有存活事务 ReadView 的 Undo Log。
 *    - 只要有一个长事务未提交，它的 ReadView 就会锚定在最老版本，全库在此期间产生的所有 Undo Log 全部无法回收，导致 ibdata 或 undo 表空间暴增几十 GB，撑爆磁盘！
 *
 * 3. 主从复制严重延迟 (Replication Lag 激增)：
 *    - 主库执行耗时 30 秒的大事务，提交后写入单个巨大事务的 Binlog。
 *    - 从库中继日志接收后，回放线程必须完整单事务串行重放这 30 秒的变更，导致 `Seconds_Behind_Master` 激增数十秒甚至数小时，读写分离读到严重过期旧数据。
 *
 * 4. 数据库连接池被迅速吃空打满 (Connection Pool Exhaustion)：
 *    - 每个长事务独占一个物理数据库连接。由于无法快速归还连接，活跃连接数瞬间占满 HikariCP 等连接池，导致后续所有普通 API 接口请求超时抛出连接池枯竭错误，造成全站业务雪崩。
 *
 * 5. 崩溃恢复时间被拉长数倍 (Crash Recovery 缓慢)：
 *    - 突发宕机重启时，MySQL 必须在 Redo 前滚后动用 Undo Log 漫长地反向回滚未提交的大事务，可能导致数据库十几分钟甚至数小时无法启动对外服务。
 */
public class Tx12_LongTransactionDrawbacksDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 12】大事务/长事务五大生产弊端与并发锁等待超时实机复现");
        System.out.println("====================================================================");

        // 1. 初始化订单数据
        DbConnectionHelper.executeSqlScript(
                "UPDATE tx_order SET status = 'PENDING' WHERE id = 1;"
        );

        CountDownLatch longTxLockHeld = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(2);

        // 模拟连接 1: 长事务 (错误地在事务内模拟耗时操作，如调用第三方 RPC 接口持续 3 秒，霸占订单 1 的排他锁)
        new Thread(() -> {
            try (Connection conn = DbConnectionHelper.getConnection()) {
                conn.setAutoCommit(false);
                System.out.println(">>> [长事务线程 1] 开启事务，执行 UPDATE tx_order SET status='PROCESSING' WHERE id=1;");
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE tx_order SET status = 'PROCESSING' WHERE id = 1;")) {
                    ps.executeUpdate();
                }
                System.out.println("   [长事务线程 1] 已持有订单 1 行排他锁，此时错误地在事务中执行长耗时操作(如远程 RPC / 慢逻辑)...");
                longTxLockHeld.countDown(); // 通知并发线程开始更新

                // 霸占锁 2 秒
                Thread.sleep(2000);

                conn.commit();
                System.out.println(">>> [长事务线程 1] 漫长操作完毕，提交事务 COMMIT 并释放锁！");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                finishLatch.countDown();
            }
        }, "LongTx-Thread-1").start();

        // 模拟连接 2: 并发普通短事务 (尝试更新同一笔订单，感受被长事务阻塞延迟的痛点)
        new Thread(() -> {
            try (Connection conn = DbConnectionHelper.getConnection()) {
                conn.setAutoCommit(false);
                longTxLockHeld.await(3, TimeUnit.SECONDS);

                System.out.println(">>> [并发业务线程 2] 尝试执行 UPDATE tx_order SET amount = 299 WHERE id = 1...");
                long startBlockTime = System.currentTimeMillis();

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE tx_order SET amount = 299.00 WHERE id = 1;")) {
                    ps.executeUpdate();
                }
                long waitedTime = System.currentTimeMillis() - startBlockTime;
                System.out.printf("   [并发业务线程 2 执行完毕] 由于长事务霸占锁，该普通操作被足足阻塞延迟了 %d ms！%n", waitedTime);

                conn.commit();
            } catch (Exception e) {
                System.err.println("并发业务线程 2 异常: " + e.getMessage());
            } finally {
                finishLatch.countDown();
            }
        }, "NormalBiz-Thread-2").start();

        try {
            finishLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 打印大事务优化建议总结
        System.out.println("\n>>> 【生产环境大事务治理最佳实践】：");
        System.out.println("1. 严禁在 `@Transactional` 中穿插网络 I/O (RPC、HTTP、发短信/邮件等)，网络调用必须移到事务外；");
        System.out.println("2. 大批量插入或更新必须按 Batch 分批执行 (如每次分批 500~1000 条即时 COMMIT)，杜绝单事务更新超万行；");
        System.out.println("3. 尽量缩减事务边界，只对产生物理写变更的关键核心 SQL 开启事务。");
    }
}

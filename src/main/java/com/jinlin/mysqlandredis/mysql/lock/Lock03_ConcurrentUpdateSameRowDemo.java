package com.jinlin.mysqlandredis.mysql.lock;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 【面试题 03】MySQL 两个线程的 UPDATE 语句同时处理一条数据，会不会有阻塞？
 *
 * 核心考点深度剖析：
 * 【核心答案】：【一定会发生阻塞！】
 *
 * 底层执行机制全链路剖析：
 * 1. 自动加行级排他锁 (X Lock)：
 *    - 当线程 1 执行 `UPDATE ... WHERE id = 1;` 时，InnoDB 引擎会在物理修改前，自动对 `id = 1` 这条聚簇索引记录打上【行级排他锁 (X 锁)】。
 *
 * 2. 锁兼容性冲突与互斥阻塞：
 *    - 线程 2 随后执行修改同一行记录的 UPDATE 语句，由于 UPDATE 属于当前读写操作，必须先向 InnoDB 申请获得该行的 X 排他锁。
 *    - 根据数据库锁冲突矩阵：【X 锁与 X 锁互斥冲突】！
 *    - 线程 2 的加锁请求被拒绝，InnoDB 内部将线程 2 挂起，放入该行记录的锁等待队列 (Lock Wait Queue)，线程 2 陷入【阻塞 (Blocked)】状态！
 *
 * 3. 阻塞等待的两种终局：
 *    ① 正常走向：线程 1 执行 COMMIT 或 ROLLBACK 提交/回滚事务，行锁被释放。
 *       InnoDB 调度唤醒等待队列中的线程 2，线程 2 拿到 X 锁后继续执行 UPDATE。
 *    ② 超时报错：若线程 1 长时间不提交，超过了 MySQL 的 `innodb_lock_wait_timeout` (默认 50 秒)，
 *       线程 2 将被强制中断，抛出高频经典异常：`ERROR 1205 (HY000): Lock wait timeout exceeded; try restarting transaction`！
 */
public class Lock03_ConcurrentUpdateSameRowDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 03】两线程并发 UPDATE 同一数据行的 X 排他锁互斥与阻塞实测");
        System.out.println("====================================================================");

        // 初始化商品库存
        DbConnectionHelper.executeSqlScript(
                "UPDATE lock_item_stock SET stock = 100 WHERE id = 1;"
        );

        CountDownLatch thread1HoldingLock = new CountDownLatch(1);
        CountDownLatch testFinished = new CountDownLatch(2);

        // 线程 1: 开启事务，执行 UPDATE id=1，持有 X 行锁 1.2 秒
        new Thread(() -> {
            try (Connection conn = DbConnectionHelper.getConnection()) {
                conn.setAutoCommit(false);
                System.out.println(">>> [线程 1] 开启事务，执行 UPDATE lock_item_stock SET stock = 90 WHERE id = 1;");
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE lock_item_stock SET stock = 90 WHERE id = 1;")) {
                    ps.executeUpdate();
                }
                System.out.println("   [线程 1] 成功获取 id=1 的行排他锁 (X 锁)，故意持有锁 1200 ms 暂不提交...");
                thread1HoldingLock.countDown(); // 通知线程 2 启动争抢

                Thread.sleep(1200);

                conn.commit();
                System.out.println(">>> [线程 1] 事务 COMMIT 提交，释放 id=1 的 X 行锁！");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                testFinished.countDown();
            }
        }, "Thread-1-LockOwner").start();

        // 线程 2: 尝试并发更新同一行 id=1，实测阻塞等待
        new Thread(() -> {
            try (Connection conn = DbConnectionHelper.getConnection()) {
                conn.setAutoCommit(false);
                thread1HoldingLock.await(3, TimeUnit.SECONDS);

                System.out.println(">>> [线程 2] 尝试执行同一行的更新: UPDATE lock_item_stock SET stock = 80 WHERE id = 1;");
                long startBlockTime = System.currentTimeMillis();

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE lock_item_stock SET stock = 80 WHERE id = 1;")) {
                    ps.executeUpdate();
                }

                long blockDuration = System.currentTimeMillis() - startBlockTime;
                System.out.printf("   [线程 2 终于执行成功] 线程 2 被线程 1 的 X 排他锁足足阻塞挂起了 %d ms (等待线程 1 提交释放锁)！%n",
                        blockDuration);

                conn.commit();
            } catch (Exception e) {
                System.err.println("线程 2 出现锁异常: " + e.getMessage());
            } finally {
                testFinished.countDown();
            }
        }, "Thread-2-BlockedWaiter").start();

        try {
            testFinished.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        DbConnectionHelper.printQueryResults("【最终数据确认】验证最终库存经过两次更新后的状态",
                "SELECT id, item_name, stock FROM lock_item_stock WHERE id = 1;"
        );
    }
}

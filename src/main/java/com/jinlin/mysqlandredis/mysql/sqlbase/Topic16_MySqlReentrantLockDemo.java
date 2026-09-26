package com.jinlin.mysqlandredis.mysql.sqlbase;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 问题 16: 如何用 MySQL 实现一个可重入的锁？
 * 
 * 实战测试演示类：
 * 1. 测试单线程多次重入与逐层释放。
 * 2. 测试多线程并发互斥竞争与超时保护。
 */
public class Topic16_MySqlReentrantLockDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 16】基于 MySQL 的可重入分布式锁实战测试 (单线程重入 + 多线程互斥)");
        System.out.println("====================================================================");

        // 1. 初始化锁表
        Topic16_MySqlReentrantLock.initLockTable();

        String testLockName = "interview_resource_lock";

        // 2. 测试一: 单线程多次重入测试
        System.out.println(">>> [测试 1] 单线程多层重入验证:");
        Topic16_MySqlReentrantLock lock = new Topic16_MySqlReentrantLock(testLockName, 10000); // 10秒超时
        try {
            System.out.println("1. 第一次获取锁...");
            boolean firstAcquire = lock.tryLock(1, TimeUnit.SECONDS);
            System.out.println("   第一次加锁结果: " + firstAcquire);
            DbConnectionHelper.printQueryResults("第一次加锁后的表状态 (reentrant_count 应为 1)", 
                    "SELECT lock_name, lock_owner, reentrant_count, expire_time FROM mysql_reentrant_lock;");

            System.out.println("2. 同一实例/线程第二次重入获取同一把锁...");
            boolean secondAcquire = lock.tryLock(1, TimeUnit.SECONDS);
            System.out.println("   第二次重入结果: " + secondAcquire);
            DbConnectionHelper.printQueryResults("第二次重入后的表状态 (reentrant_count 应递增为 2)", 
                    "SELECT lock_name, lock_owner, reentrant_count, expire_time FROM mysql_reentrant_lock;");

            System.out.println("3. 释放第一层重入锁...");
            lock.unlock();
            DbConnectionHelper.printQueryResults("释放一层重入锁后的表状态 (reentrant_count 应递减回 1)", 
                    "SELECT lock_name, lock_owner, reentrant_count, expire_time FROM mysql_reentrant_lock;");

            System.out.println("4. 释放最后一层锁...");
            lock.unlock();
            DbConnectionHelper.printQueryResults("彻底释放锁后的表状态 (记录已被 DELETE 删除)", 
                    "SELECT lock_name, lock_owner, reentrant_count, expire_time FROM mysql_reentrant_lock;");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 3. 测试二: 多线程并发互斥测试
        System.out.println(">>> [测试 2] 多线程并发竞争与互斥排他性测试:");
        CountDownLatch latch = new CountDownLatch(2);

        Thread threadA = new Thread(() -> {
            Topic16_MySqlReentrantLock lockA = new Topic16_MySqlReentrantLock(testLockName, 5000);
            try {
                System.out.println("[线程 A] 正在尝试获取锁...");
                if (lockA.tryLock(2, TimeUnit.SECONDS)) {
                    System.out.println("[线程 A] 成功获取锁！正在执行关键业务 (耗时 1.5s)...");
                    Thread.sleep(1500);
                    lockA.unlock();
                    System.out.println("[线程 A] 业务执行完毕并成功释放锁！");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        }, "Thread-A");

        Thread threadB = new Thread(() -> {
            try {
                // 确保线程 A 先抢到锁
                Thread.sleep(200);
                Topic16_MySqlReentrantLock lockB = new Topic16_MySqlReentrantLock(testLockName, 5000);
                System.out.println("[线程 B] 尝试获取同一把锁 (等待超时时间设为 500ms，预期获取失败)...");
                boolean acquired = lockB.tryLock(500, TimeUnit.MILLISECONDS);
                System.out.println("[线程 B] 第一次抢锁结果 (预期 false): " + acquired);

                System.out.println("[线程 B] 再次尝试获取锁 (等待超时时间设为 3000ms，等待线程 A 释放)...");
                boolean acquiredRetry = lockB.tryLock(3000, TimeUnit.MILLISECONDS);
                System.out.println("[线程 B] 第二次抢锁结果 (预期 true): " + acquiredRetry);
                if (acquiredRetry) {
                    lockB.unlock();
                    System.out.println("[线程 B] 释放锁！");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        }, "Thread-B");

        threadA.start();
        threadB.start();

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("====================================================================");
        System.out.println("【面试总结】MySQL 实现可重入锁要点：");
        System.out.println("1. 表结构必须具备: lock_name (唯一主键), lock_owner (持有者标识), reentrant_count (重入计数), expire_time (绝对失效时间);");
        System.out.println("2. 加锁: 首次 INSERT，重入 UPDATE count + 1，超时 CAS 抢占;");
        System.out.println("3. 释放: 校验所有者，count > 1 则 UPDATE count - 1，count == 1 则 DELETE;");
        System.out.println("4. 相比 Redis/Redisson 锁，MySQL 锁天然持久化、适合无独立 Redis 中间件的小型系统，但高并发下吞吐不如 Redis。");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}


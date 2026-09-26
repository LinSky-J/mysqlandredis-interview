package com.jinlin.mysqlandredis.mysql.topic16;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 问题 16: 基于 MySQL 实现的可重入分布式锁
 * 
 * 核心机制：
 * 1. 互斥性：利用唯一主键 PRIMARY KEY (lock_name) 保证并发竞争时仅单线程插入成功。
 * 2. 可重入性：记录 lock_owner 和 reentrant_count。同线程重入时递增计数器。
 * 3. 释放安全性：只有 lock_owner 匹配才能扣减计数，减至 0 时执行 DELETE。
 * 4. 防死锁容灾：维护 expire_time 毫秒时间戳。若进程崩溃未释放，超时后允许其他线程 CAS 抢占。
 */
public class MySqlReentrantLock {

    private final String lockName;
    private final String lockOwner;
    private final long lockTtlMs;

    public MySqlReentrantLock(String lockName, long lockTtlMs) {
        this.lockName = lockName;
        this.lockTtlMs = lockTtlMs;
        // 生成具有唯一标识的 owner: 实例 UUID + 线程 ID
        this.lockOwner = UUID.randomUUID().toString() + "-" + Thread.currentThread().getId();
    }

    /**
     * 初始化锁数据表
     */
    public static void initLockTable() {
        String dropTable = "DROP TABLE IF EXISTS mysql_reentrant_lock;";
        String createTable = "CREATE TABLE mysql_reentrant_lock ("
                + "  lock_name VARCHAR(64) NOT NULL PRIMARY KEY,"
                + "  lock_owner VARCHAR(100) NOT NULL,"
                + "  reentrant_count INT NOT NULL DEFAULT 1,"
                + "  expire_time BIGINT NOT NULL,"
                + "  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
        DbConnectionHelper.executeSqlScript(dropTable, createTable);
    }

    /**
     * 阻塞加锁
     */
    public void lock() {
        try {
            if (!tryLock(Long.MAX_VALUE, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("加锁失败");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("加锁过程被中断", e);
        }
    }

    /**
     * 尝试在指定时间内加锁 (带重试与自旋)
     */
    public boolean tryLock(long waitTime, TimeUnit unit) throws InterruptedException {
        long endWaitTime = System.currentTimeMillis() + unit.toMillis(waitTime);

        while (System.currentTimeMillis() <= endWaitTime) {
            long now = System.currentTimeMillis();
            long newExpire = now + lockTtlMs;

            // 1. 尝试首次插入获取锁
            String insertSql = "INSERT INTO mysql_reentrant_lock (lock_name, lock_owner, reentrant_count, expire_time) "
                    + "VALUES (?, ?, 1, ?);";
            try (Connection conn = DbConnectionHelper.getConnection();
                 PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, lockName);
                ps.setString(2, lockOwner);
                ps.setLong(3, newExpire);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    return true; // 首次加锁成功
                }
            } catch (SQLException ex) {
                // 主键冲突，进入锁已存在逻辑
            }

            // 2. 检查锁是否为当前持有者 (可重入逻辑) 或 已经超时 (死锁恢复)
            String selectSql = "SELECT lock_owner, reentrant_count, expire_time FROM mysql_reentrant_lock WHERE lock_name = ?;";
            try (Connection conn = DbConnectionHelper.getConnection();
                 PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, lockName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String currentOwner = rs.getString("lock_owner");
                        long expireTime = rs.getLong("expire_time");

                        // 2.1 重入逻辑：当前所有者就是自己
                        if (lockOwner.equals(currentOwner)) {
                            String reentrantSql = "UPDATE mysql_reentrant_lock "
                                    + "SET reentrant_count = reentrant_count + 1, expire_time = ? "
                                    + "WHERE lock_name = ? AND lock_owner = ?;";
                            try (PreparedStatement reentrantPs = conn.prepareStatement(reentrantSql)) {
                                reentrantPs.setLong(1, newExpire);
                                reentrantPs.setString(2, lockName);
                                reentrantPs.setString(3, lockOwner);
                                if (reentrantPs.executeUpdate() > 0) {
                                    return true; // 重入加锁成功
                                }
                            }
                        }

                        // 2.2 超时容灾抢占：如果锁已过期，利用 CAS 原语抢占
                        if (now > expireTime) {
                            String preemptSql = "UPDATE mysql_reentrant_lock "
                                    + "SET lock_owner = ?, reentrant_count = 1, expire_time = ? "
                                    + "WHERE lock_name = ? AND expire_time = ?;";
                            try (PreparedStatement preemptPs = conn.prepareStatement(preemptSql)) {
                                preemptPs.setString(1, lockOwner);
                                preemptPs.setLong(2, newExpire);
                                preemptPs.setString(3, lockName);
                                preemptPs.setLong(4, expireTime);
                                if (preemptPs.executeUpdate() > 0) {
                                    return true; // 超时抢占成功
                                }
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("查询或重入锁失败: " + e.getMessage());
            }

            // 自旋休眠 50ms 重试
            Thread.sleep(50);
        }

        return false; // 超时未获取到锁
    }

    /**
     * 释放锁 (支持重入逐层释放)
     */
    public void unlock() {
        String selectSql = "SELECT lock_owner, reentrant_count FROM mysql_reentrant_lock WHERE lock_name = ?;";
        try (Connection conn = DbConnectionHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setString(1, lockName);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalMonitorStateException("试图释放不存在的锁: " + lockName);
                }
                String currentOwner = rs.getString("lock_owner");
                int count = rs.getInt("reentrant_count");

                if (!lockOwner.equals(currentOwner)) {
                    throw new IllegalMonitorStateException("当前线程不是锁的持有者，无权释放！");
                }

                if (count > 1) {
                    // 重入层数递减
                    String decrementSql = "UPDATE mysql_reentrant_lock "
                            + "SET reentrant_count = reentrant_count - 1 "
                            + "WHERE lock_name = ? AND lock_owner = ?;";
                    try (PreparedStatement decPs = conn.prepareStatement(decrementSql)) {
                        decPs.setString(1, lockName);
                        decPs.setString(2, lockOwner);
                        decPs.executeUpdate();
                    }
                } else {
                    // count == 1，完全释放锁
                    String deleteSql = "DELETE FROM mysql_reentrant_lock WHERE lock_name = ? AND lock_owner = ?;";
                    try (PreparedStatement delPs = conn.prepareStatement(deleteSql)) {
                        delPs.setString(1, lockName);
                        delPs.setString(2, lockOwner);
                        delPs.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("释放锁出现 SQL 异常", e);
        }
    }

    public String getLockName() {
        return lockName;
    }

    public String getLockOwner() {
        return lockOwner;
    }
}

-- ====================================================================
-- 问题 16: 如何用 MySQL 实现一个可重入的锁？
-- ====================================================================
-- 可重入锁核心要求:
-- 1. 互斥性: 同一时刻只能有一个线程/持有者成功加锁。
-- 2. 可重入性: 同一持有者再次加锁时，不发生死锁，重入计数器 (count) 累加。
-- 3. 释放安全性: 谁加锁必须由谁释放；释放时 count 逐级递减，归零时彻底删除锁记录。
-- 4. 防死锁容灾: 设置超时失效时间 (expire_time)，避免持有者宕机导致死锁。
-- ====================================================================

DROP TABLE IF EXISTS mysql_reentrant_lock;
CREATE TABLE mysql_reentrant_lock (
    lock_name VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '分布式锁名称(业务资源唯一标示)',
    lock_owner VARCHAR(64) NOT NULL COMMENT '锁持有者(例如: UUID+ThreadId)',
    reentrant_count INT NOT NULL DEFAULT 1 COMMENT '重入计数器',
    expire_time BIGINT NOT NULL COMMENT '锁过期绝对毫秒时间戳',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MySQL 可重入分布式锁表';

-- --------------------------------------------------------------------
-- 模拟底层核心 SQL 流程:
-- --------------------------------------------------------------------

-- 1. 初次加锁 (尝试插入):
-- INSERT INTO mysql_reentrant_lock (lock_name, lock_owner, reentrant_count, expire_time)
-- VALUES ('order_pay_lock', 'thread-001', 1, 1790400000000);

-- 2. 同一线程重入 (更新计数 + 续期):
-- UPDATE mysql_reentrant_lock 
-- SET reentrant_count = reentrant_count + 1, expire_time = 1790400005000 
-- WHERE lock_name = 'order_pay_lock' AND lock_owner = 'thread-001';

-- 3. 释放锁第 1 次 (如果 count > 1 则递减):
-- UPDATE mysql_reentrant_lock 
-- SET reentrant_count = reentrant_count - 1 
-- WHERE lock_name = 'order_pay_lock' AND lock_owner = 'thread-001';

-- 4. 彻底释放锁 (当 count == 1 时删除记录):
-- DELETE FROM mysql_reentrant_lock 
-- WHERE lock_name = 'order_pay_lock' AND lock_owner = 'thread-001';

-- 5. 补充方案: MySQL 内置命名锁 (轻量级测试可用):
-- SELECT GET_LOCK('my_named_lock', 10);   -- 获取命名锁，超时等待 10s
-- SELECT RELEASE_LOCK('my_named_lock');   -- 释放命名锁

-- ====================================================================
-- 问题 03: MySQL 为什么 InnoDB 是默认引擎？
-- ====================================================================
-- 历史背景:
--   MySQL 5.1 之前默认是 MyISAM，从 MySQL 5.5 开始，InnoDB 成为默认引擎。
--   MySQL 8.0 更是将系统字典表从 MyISAM 全部重构为 InnoDB。
-- 
-- 核心决定性技术优势:
-- 1. ACID 事务支持: 商业/金融/电商业务不可妥协的基石。
-- 2. 行级锁 (Row-Level Locking): 替代 MyISAM 表锁，并发写能力提升万倍。
-- 3. Crash-Safe (崩盘安全): Redo Log + 两阶段提交，断电自动恢复，无需人工修复。
-- 4. MVCC 多版本并发控制: 读写互不阻塞，非锁定快照读。
-- 5. Buffer Pool 内存缓冲池: 统一缓存数据与索引，自适应哈希索引 (AHI) 加速。
-- 6. 聚簇索引 (Clustered Index): 主键与数据同存叶子节点，主键点查 1 次 I/O 命中。
-- ====================================================================

-- 1. 查看当前 MySQL 数据库实例的默认引擎参数
SHOW VARIABLES LIKE 'default_storage_engine';

-- 2. 演示核心优势一: 事务支持与回滚 (ROLLBACK) —— InnoDB 独有
DROP TABLE IF EXISTS interview_why_innodb;
CREATE TABLE interview_why_innodb (
    id INT PRIMARY KEY AUTO_INCREMENT,
    account_name VARCHAR(50) NOT NULL,
    balance DECIMAL(10, 2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_why_innodb (account_name, balance) VALUES ('张三', 1000.00);

-- 开启事务，扣减金额后发生异常回滚
START TRANSACTION;
UPDATE interview_why_innodb SET balance = balance - 500 WHERE id = 1;
-- 模拟业务发生异常，执行回滚
ROLLBACK;

-- 验证回滚结果: 余额依然是 1000.00，完美保障原子性！
SELECT * FROM interview_why_innodb WHERE id = 1;

-- 3. 演示核心优势二: 行级并发锁 (Record Lock)
-- 事务 A 锁定 id=1，事务 B 依然可以并发更新 id=2 (如果是 MyISAM，整表将被锁死互斥！)
INSERT INTO interview_why_innodb (account_name, balance) VALUES ('李四', 2000.00);

-- 查看当前 InnoDB 存储引擎的状态信息 (包含 Buffer Pool, 锁信息, 事务日志状态)
SHOW ENGINE INNODB STATUS;

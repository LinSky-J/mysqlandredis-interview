-- ====================================================================
-- 问题 02: 讲一讲 MySQL 的引擎吧，你有什么了解？
-- ====================================================================
-- 1. 架构特性:
--    MySQL 采用【插件式存储引擎 (Pluggable Storage Engine)】架构。
--    存储引擎作用在【表级别】，一个数据库中的不同数据表可以根据业务需要使用不同的引擎。
-- 
-- 2. 常见引擎横向对比:
--    - InnoDB:  事务支持、行级锁、MVCC、外键、聚簇索引、Crash-safe (综合能力最强，默认引擎)。
--    - MyISAM:  不支持事务、表锁、不支持外键、非聚簇索引、Count(*) 极快、只读或日志分析。
--    - MEMORY:  纯内存存储、断电丢失、支持 Hash 索引与 BTree 索引、表锁、不支持 TEXT/BLOB。
--    - ARCHIVE: 仅支持 INSERT 与 SELECT、高度压缩、无索引或仅自增索引、海量日志归档。
--    - CSV:     以纯文本逗号分隔文件存储，便于直接导入导出。
-- ====================================================================

-- 1. 查看当前 MySQL 数据库实例支持的所有存储引擎列表及默认引擎
SHOW ENGINES;

-- 2. 创建不同引擎的表进行实测
-- (1) 创建 MEMORY 内存表
DROP TABLE IF EXISTS interview_engine_memory;
CREATE TABLE interview_engine_memory (
    id INT PRIMARY KEY,
    key_name VARCHAR(50) NOT NULL,
    val VARCHAR(100) NOT NULL
) ENGINE=MEMORY DEFAULT CHARSET=utf8mb4 COMMENT='纯内存快速缓存表';

-- (2) 创建 MyISAM 表
DROP TABLE IF EXISTS interview_engine_myisam;
CREATE TABLE interview_engine_myisam (
    id INT PRIMARY KEY,
    log_title VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT='非事务日志表';

-- (3) 创建 InnoDB 事务表
DROP TABLE IF EXISTS interview_engine_innodb;
CREATE TABLE interview_engine_innodb (
    id INT PRIMARY KEY,
    account_no VARCHAR(50) NOT NULL,
    balance DECIMAL(12, 2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事务与行锁核心业务表';

-- 插入测试数据
INSERT INTO interview_engine_memory VALUES (1, 'SESSION_TOKEN', 'abc123xyz');
INSERT INTO interview_engine_myisam VALUES (1, '系统开机日志', NOW());
INSERT INTO interview_engine_innodb VALUES (1, '622202000000001', 99999.00);

-- 查看各表的引擎元数据信息
SELECT 
    TABLE_NAME, 
    ENGINE, 
    ROW_FORMAT, 
    TABLE_ROWS, 
    DATA_LENGTH, 
    INDEX_LENGTH
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'interview_db' 
  AND TABLE_NAME IN ('interview_engine_memory', 'interview_engine_myisam', 'interview_engine_innodb');

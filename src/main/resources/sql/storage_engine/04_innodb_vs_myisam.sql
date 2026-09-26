-- ====================================================================
-- 问题 04: 说一下 MySQL 的 InnoDB 与 MyISAM 的区别？
-- ====================================================================
-- 核心区别全景对比：
-- 1. 事务: InnoDB 支持 ACID 事务; MyISAM 不支持。
-- 2. 锁级别: InnoDB 支持行级锁与间隙锁; MyISAM 仅支持全表锁。
-- 3. 崩溃恢复: InnoDB 依靠 Redo Log 保证 Crash-Safe; MyISAM 易损毁需 myisamchk。
-- 4. 索引方式: InnoDB 为聚簇索引 (主键+数据同在 B+Tree 叶子节点); MyISAM 为非聚簇索引 (叶子存指针)。
-- 5. COUNT(*) 机制: MyISAM 有内置计数器 O(1); InnoDB 受 MVCC 影响需逐行统计 O(N)。
-- 6. 存储文件: InnoDB 默认 .ibd 单文件; MyISAM 分为 .MYD (数据) 和 .MYI (索引)。
-- ====================================================================

-- 1. 准备对比表
DROP TABLE IF EXISTS interview_cmp_innodb;
DROP TABLE IF EXISTS interview_cmp_myisam;

CREATE TABLE interview_cmp_innodb (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    score INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_cmp_myisam (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    score INT NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4;

-- 插入基准测试数据
INSERT INTO interview_cmp_innodb (username, score) VALUES ('Alice', 95), ('Bob', 88), ('Charlie', 91);
INSERT INTO interview_cmp_myisam (username, score) VALUES ('Alice', 95), ('Bob', 88), ('Charlie', 91);

-- 2. 验证 COUNT(*) 统计与执行计划
-- MyISAM 在 EXPLAIN 中直接显示 "Select tables optimized away"，代表无需查表直接从元数据计数器读取！
EXPLAIN SELECT COUNT(*) FROM interview_cmp_myisam;

-- InnoDB 则必须扫描主键或二级索引来满足 MVCC 可见性！
EXPLAIN SELECT COUNT(*) FROM interview_cmp_innodb;

-- 3. 查询两表在 information_schema 中的存储引擎属性与差异
SELECT 
    TABLE_NAME, 
    ENGINE, 
    TABLE_ROWS, 
    AVG_ROW_LENGTH, 
    DATA_LENGTH, 
    INDEX_LENGTH 
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'interview_db' 
  AND TABLE_NAME IN ('interview_cmp_innodb', 'interview_cmp_myisam');

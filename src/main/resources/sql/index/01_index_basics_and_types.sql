-- ====================================================================
-- 问题 01: 索引是什么？有什么好处？索引分类与哈希索引场景深度解析
-- ====================================================================
-- 1. 索引是什么？
--    索引是数据库管理系统中用于【帮助快速查找和排序数据的数据结构】(相当于图书的目录索引)。
--    核心好处:
--    - 大幅降低磁盘 I/O 成本 (从全表扫描 O(N) 降低至 B+ 树索引寻道 O(log N))；
--    - 通过索引的有序性降低 CPU 排序与分组开销 (避免 Filesort)；
--    - 加速表连接 (JOIN) 探测效率。
--    核心代价:
--    - 额外占用物理磁盘空间与内存缓冲池 (Buffer Pool)；
--    - 降低写操作性能 (增删改需要同步维护 B+ 树分裂与重平衡)。
-- 
-- 2. 索引的分类维度:
--    (1) 物理存储分类: 聚簇索引 (Clustered Index) vs 非聚簇索引 (Secondary Index / 二级索引)。
--    (2) 逻辑特性分类: 主键索引 (PRIMARY)、唯一索引 (UNIQUE)、普通单列索引 (INDEX)、
--                     联合复合索引 (COMPOSITE)、全文索引 (FULLTEXT)。
--    (3) 数据结构分类: B+Tree 索引 (默认)、Hash 索引 (MEMORY 引擎支持)、R-Tree (空间地理)。
-- 
-- 3. 哈希索引 (Hash Index) 使用场景与局限性:
--    - 优势: 精确等值点查 (=, IN) 极快，时间复杂度 O(1)。
--    - 致命局限:
--      ① 无法进行范围查询 (<, >, BETWEEN)，因为哈希计算后无序；
--      ② 无法用于 ORDER BY 排序；
--      ③ 无法使用前缀匹配或最左匹配；
--      ④ 存在哈希冲突时，链表遍历导致性能退化。
--    - InnoDB 自适应哈希索引 (AHI):
--      InnoDB 自动监控 B+ 树热点数据页，透明在内存构建哈希表，无需人工维护。
-- ====================================================================

-- 准备演示表: 分别展示各种逻辑索引的创建
DROP TABLE IF EXISTS interview_idx_basics;
CREATE TABLE interview_idx_basics (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键聚簇索引',
    user_code VARCHAR(32) NOT NULL COMMENT '唯一业务编码',
    nickname VARCHAR(50) NOT NULL COMMENT '普通单列索引',
    age INT NOT NULL,
    dept_id INT NOT NULL,
    bio TEXT COMMENT '全文检索字段',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (id),                         -- 1. 主键索引 (唯一且非空，默认聚簇)
    UNIQUE KEY uk_user_code (user_code),      -- 2. 唯一索引 (保证唯一性)
    INDEX idx_nickname (nickname),            -- 3. 普通单列索引
    INDEX idx_dept_age (dept_id, age),        -- 4. 联合复合索引
    FULLTEXT KEY ft_bio (bio)                 -- 5. 全文索引
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入演示基准数据
INSERT INTO interview_idx_basics (user_code, nickname, age, dept_id, bio) VALUES
('U1001', '张三', 25, 10, '资深 Java 后端架构师，精通 MySQL 调优与底层原理'),
('U1002', '李四', 30, 10, '分布式系统专家，熟悉高并发缓存与分库分表'),
('U1003', '王五', 28, 20, '前端技术专家，擅长前端工程化与性能优化');

-- 演示普通索引等值检索的执行计划 (type = ref)
EXPLAIN SELECT * FROM interview_idx_basics WHERE nickname = '张三';

-- 演示主键索引等值检索的执行计划 (type = const，效率最高)
EXPLAIN SELECT * FROM interview_idx_basics WHERE id = 1;

-- 演示 MEMORY 引擎中的显式 HASH 索引
DROP TABLE IF EXISTS interview_idx_hash_memory;
CREATE TABLE interview_idx_hash_memory (
    session_id VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (session_id) USING HASH
) ENGINE=MEMORY DEFAULT CHARSET=utf8mb4 COMMENT='哈希索引内存表';

INSERT INTO interview_idx_hash_memory VALUES ('sess_token_abc123', 1001);

-- 查看哈希索引点查的执行计划
EXPLAIN SELECT * FROM interview_idx_hash_memory WHERE session_id = 'sess_token_abc123';

-- 查看 InnoDB 自适应哈希索引 (AHI) 的当前启用状态 (默认 ON)
SHOW VARIABLES LIKE 'innodb_adaptive_hash_index';

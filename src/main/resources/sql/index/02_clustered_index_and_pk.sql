-- ====================================================================
-- 问题 02: 聚簇索引、主键选型 (自增 ID vs UUID)、性别索引深度解密
-- ====================================================================
-- 1. 聚簇索引 vs 非聚簇索引:
--    - 聚簇索引: 数据和主键索引一体化存放于 B+ 树叶子节点中。每表仅一个。
--    - 非聚簇索引 (二级索引): 叶子节点存放的是主键值，查询其他字段需要【回表】。
-- 
-- 2. 聚簇索引数据更新时存储是否变化？
--    - 更新非主键字段: 原地更新 (In-place) 或空间不足时页分裂。
--    - 更新主键字段: 物理上先 DELETE 旧记录，再在目标位置重新 INSERT 新整行，
--                   且全表所有二级索引对应的主键值必须全部同步更新！代价极其巨大，严禁更新主键！
-- 
-- 3. MySQL 主键一定是聚簇索引吗？
--    - 有主键 -> 主键即聚簇索引；
--    - 无主键 -> 首个 NOT NULL UNIQUE 列作为聚簇索引；
--    - 均无   -> 隐式创建 6 字节全局自增 DB_ROW_ID 作为聚簇索引。
-- 
-- 4. 自增 ID 为什么比 UUID 快？UUID 在 B+ 树有序吗？
--    - UUID 随机无序: 导致在 B+ 树中产生海量随机插入、频繁触发【页分裂 (Page Split)】和磁盘碎片。
--    - 自增 ID 严格单调递增: 永远在 B+ 树末尾顺序追加，页填充率高，零随机分裂，寻道快。
--    - 占用空间差异: BIGINT 仅 8 字节，UUID 占 36 字节。二级索引全部膨胀 4 倍，挤占 Buffer Pool。
-- 
-- 5. 性别字段能不能建索引？
--    - 语法允许，但工程中坚决不建！
--    - 原因: 基数 (Cardinality) 极低，选择性仅 50%，走索引回表成本远大于全表扫描，优化器会自动放弃使用！
-- ====================================================================

-- 1. 创建自增主键表 (顺序写入最佳实践)
DROP TABLE IF EXISTS interview_pk_autoincrement;
CREATE TABLE interview_pk_autoincrement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_sn VARCHAR(64) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    gender TINYINT NOT NULL COMMENT '性别: 1男 2女 (基数极低)',
    INDEX idx_gender (gender) -- 演示低区分度索引
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 创建 UUID 主键表 (随机插入对比)
DROP TABLE IF EXISTS interview_pk_uuid;
CREATE TABLE interview_pk_uuid (
    id VARCHAR(36) PRIMARY KEY COMMENT 'UUID 随机主键',
    order_sn VARCHAR(64) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入自增测试数据 (批量递增顺序写)
INSERT INTO interview_pk_autoincrement (order_sn, amount, gender) VALUES
('ORD_AUTO_001', 199.00, 1),
('ORD_AUTO_002', 299.00, 2),
('ORD_AUTO_003', 399.00, 1),
('ORD_AUTO_004', 499.00, 2);

-- 插入 UUID 测试数据 (UUID() 产生 36 字节随机字符串)
INSERT INTO interview_pk_uuid (id, order_sn, amount) VALUES
(UUID(), 'ORD_UUID_001', 199.00),
(UUID(), 'ORD_UUID_002', 299.00),
(UUID(), 'ORD_UUID_003', 399.00);

-- 3. 验证低基数性别索引为什么会被优化器废弃
-- 查看执行计划: 当过滤 50% 左右数据时，优化器即使有 idx_gender 索引也会选择 type=ALL 全表扫描！
EXPLAIN SELECT * FROM interview_pk_autoincrement WHERE gender = 1;

-- 4. 演示聚簇索引主键被 UPDATE 时的深层变化
UPDATE interview_pk_autoincrement SET id = 999 WHERE id = 1;

-- 查询验证主键更新结果
SELECT id, order_sn, amount FROM interview_pk_autoincrement;

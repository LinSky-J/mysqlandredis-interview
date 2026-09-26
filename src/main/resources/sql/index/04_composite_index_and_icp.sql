-- ====================================================================
-- 问题 04: 联合索引原理、最左前缀、范围查询失效与索引下推 (ICP)
-- ====================================================================
-- 1. 联合索引物理原理:
--    - 联合索引 (a, b, c) 在 B+ 树上按 a 排序，a 相等时按 b 排序，b 相等时按 c 排序。
--    - 没有最左前导列 a，后面的 b 和 c 是完全无序的，因此必须遵循【最左前缀匹配原则】。
-- 
-- 2. 经典高频面试题剖析:
--    (1) where b > xxx and a = x 会生效吗？
--        -> 会生效！优化器自动重排条件顺序为 a = x and b > xxx，命中前两列！
--    (2) where a = 2 and c = 1 会用到索引吗？
--        -> 会用到索引！但仅用列 a 定位查找，列 c 走 MySQL 5.6+ 索引下推 (ICP) 引擎层过滤。
--    (3) where A = xxx and C < xxx 怎么走？
--        -> 列 A 精确定位范围，列 C 走索引下推 (Using index condition)，大幅降低回表开销。
--    (4) 一个列既是单列索引又是联合索引前缀，单查走哪一个？
--        -> 优化器基于成本 (CBO) 选择更小更紧凑的索引树 (通常走单列索引，但单列属于冗余索引)。
-- ====================================================================

DROP TABLE IF EXISTS interview_composite_idx;
CREATE TABLE interview_composite_idx (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    a INT NOT NULL,
    b INT NOT NULL,
    c INT NOT NULL,
    extra_info VARCHAR(100) NOT NULL,
    
    INDEX idx_single_a (a),          -- 单列索引 a
    INDEX idx_composite_abc (a, b, c) -- 联合索引 (a, b, c)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入测试数据
INSERT INTO interview_composite_idx (a, b, c, extra_info) VALUES
(1, 10, 100, 'Info-1'),
(1, 20, 200, 'Info-2'),
(2, 10, 100, 'Info-3'),
(2, 30, 300, 'Info-4'),
(3, 40, 400, 'Info-5');

-- 场景 1: where b > 10 and a = 1 (验证优化器自动重排顺序，成功命中联合索引前缀)
EXPLAIN SELECT * FROM interview_composite_idx WHERE b > 10 AND a = 1;

-- 场景 2: where a = 2 and c = 100 (验证列 a 命中查找，列 c 触发 Using index condition 索引下推)
EXPLAIN SELECT * FROM interview_composite_idx WHERE a = 2 AND c = 100;

-- 场景 3: where a = 1 and c < 300 (验证 A=xxx and C < xxx 索引下推过滤)
EXPLAIN SELECT * FROM interview_composite_idx WHERE a = 1 AND c < 300;

-- 场景 4: 单独查列 a: 观察单列索引与联合索引共存时的优化器选择
EXPLAIN SELECT a FROM interview_composite_idx WHERE a = 2;

-- ====================================================================
-- 问题 05: 索引失效 8 大场景、回表查询与覆盖索引深度实机剖析
-- ====================================================================
-- 1. 什么是回表查询？
--    二级索引叶子节点仅包含【索引列 + 主键值】。如果查询需要获取其他列数据，
--    必须拿着主键 ID 回到聚簇索引树上再查一次整行，产生随机 I/O，称为【回表】。
-- 
-- 2. 什么是覆盖索引 (Covering Index)？
--    查询所需的全部列 (SELECT, WHERE) 均已包含在二级索引中，无需回表！
--    EXPLAIN 的 Extra 列呈现【Using index】，性能最高。
-- 
-- 3. 常见 8 大索引失效场景:
--    (1) 左模糊匹配: LIKE '%abc' (LIKE 'abc%' 可以走索引)
--    (2) 索引列加函数或计算: WHERE SUBSTRING(phone, 1, 3) = '138' 或 WHERE age + 1 = 20
--    (3) 隐式类型转换: VARCHAR 字段输入数值 (隐式调用 CAST 函数)
--    (4) 违背最左前缀: 联合索引 (a, b) 查询跳过 a 直接搜 b
--    (5) OR 连接无索引列: WHERE a = 1 OR extra_info = 'xxx'
--    (6) 范围查询右侧列失效: 联合索引 (a, b) 中 WHERE a > 10 AND b = 5 (b 无法二分定位)
--    (7) 不等于 (!= / <>) 导致索引放弃
--    (8) 数据倾斜/全表成本更低: 检索结果超过全表约 20%~30% 时，优化器主动放弃走索引回表。
-- ====================================================================

DROP TABLE IF EXISTS interview_invalidation_demo;
CREATE TABLE interview_invalidation_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    age INT NOT NULL,
    address VARCHAR(200) NOT NULL,
    
    INDEX idx_user_age (user_name, age),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入多条样本数据
INSERT INTO interview_invalidation_demo (user_name, phone, age, address) VALUES
('zhangsan', '13800000001', 25, '北京市海淀区中关村南大街1号'),
('lisi',     '13800000002', 30, '上海市浦东新区陆家嘴环路88号'),
('wangwu',   '13900000003', 28, '深圳市南山区科技园南区5栋'),
('zhaoliu',  '13900000004', 35, '广州市天河区天河路100号');

-- --------------------------------------------------------------------
-- 1. 回表查询 vs 覆盖索引
-- --------------------------------------------------------------------
-- (A) 发生回表: 查 address 列，该列未在 idx_user_age 中，必须回表
EXPLAIN SELECT user_name, age, address FROM interview_invalidation_demo WHERE user_name = 'zhangsan';

-- (B) 覆盖索引: 仅查 user_name, age, id，全部被 idx_user_age 覆盖，Extra 显示 Using index (零回表！)
EXPLAIN SELECT id, user_name, age FROM interview_invalidation_demo WHERE user_name = 'zhangsan';

-- --------------------------------------------------------------------
-- 2. 索引失效典型案例实机验证
-- --------------------------------------------------------------------
-- (失效 1) 索引列使用函数: 对 user_name 包裹 UPPER() -> type=ALL
EXPLAIN SELECT * FROM interview_invalidation_demo WHERE UPPER(user_name) = 'ZHANGSAN';

-- (失效 2) 左模糊匹配: LIKE '%san' -> type=ALL (对比 LIKE 'zhang%' 可以走索引 range)
EXPLAIN SELECT * FROM interview_invalidation_demo WHERE user_name LIKE '%san';
EXPLAIN SELECT * FROM interview_invalidation_demo WHERE user_name LIKE 'zhang%';

-- (失效 3) 隐式类型转换: phone 为 VARCHAR，传入整型数字 -> type=ALL 索引彻底失效！
EXPLAIN SELECT * FROM interview_invalidation_demo WHERE phone = 13800000001;
-- 正确写法 (传入字符串): 走索引 ref
EXPLAIN SELECT * FROM interview_invalidation_demo WHERE phone = '13800000001';

-- (失效 4) OR 条件未走索引列: address 无索引 -> type=ALL 全表扫描
EXPLAIN SELECT * FROM interview_invalidation_demo WHERE phone = '13800000001' OR address = '北京';

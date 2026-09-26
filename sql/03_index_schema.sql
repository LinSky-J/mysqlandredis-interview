-- ====================================================================
-- 03_index_schema.sql
-- MySQL 索引底层核心原理专题表结构定义与初始数据
-- 
-- 包含索引分类、哈希索引限制、聚簇 vs 非聚簇索引、自增 ID 与 UUID 写性能、
-- B+ 树底层分裂与双向链表、联合索引最左匹配与索引下推 (ICP)、
-- 8 大索引失效场景与前缀索引区分度优化实战。
-- ====================================================================

USE interview_db;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- --------------------------------------------------------------------
-- 【面试题 01: 索引是什么？好处？索引分类？哈希索引使用场景？】
-- 核心考点：
-- 1. 索引本质：排好序能快速查找的排他数据结构（MySQL 主流为 B+ 树）；
-- 2. 索引分类：主键索引（PRIMARY）、唯一索引（UNIQUE）、普通二级索引（INDEX）、
--    复合联合索引（COMPOSITE）、全文索引（FULLTEXT）；
-- 3. 哈希索引（HASH）：仅 MEMORY 引擎和 InnoDB 内部自适应哈希（AHI）使用，O(1) 精确匹配极快；
--    但完全不支持范围查找、排序和前缀匹配。
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS interview_idx_basics;
DROP TABLE IF EXISTS interview_idx_hash_memory;
DROP TABLE IF EXISTS interview_idx_user;
DROP TABLE IF EXISTS interview_hash_user;

CREATE TABLE interview_idx_basics (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键聚簇索引',
    user_no VARCHAR(32) NOT NULL COMMENT '用户工号',
    card_id VARCHAR(18) NOT NULL COMMENT '身份证唯一索引',
    age INT NOT NULL COMMENT '年龄普通索引',
    dept_id INT NOT NULL COMMENT '部门ID',
    bio TEXT COMMENT '个人简介全文检索',
    UNIQUE KEY uk_card (card_id) COMMENT '唯一索引',
    INDEX idx_age (age) COMMENT '普通单列索引',
    INDEX idx_age_dept (age, dept_id) COMMENT '复合联合索引',
    FULLTEXT KEY ft_bio (bio) COMMENT '全文索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题01: 索引分类全景】集合主键、唯一、单列、联合与全文索引对照表';

CREATE TABLE interview_idx_hash_memory (
    id INT NOT NULL PRIMARY KEY COMMENT '主键',
    session_token VARCHAR(64) NOT NULL COMMENT '哈希索引查找键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    KEY idx_hash_token (session_token) USING HASH COMMENT '显式指定使用 HASH 索引'
) ENGINE=MEMORY DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题01: 哈希索引限制】Memory引擎哈希索引表，演示等值超快但范围与排序全表扫描';

INSERT INTO interview_idx_basics (user_no, card_id, age, dept_id, bio) VALUES
('NO_001', '110101199001011234', 28, 10, '资深 Java 后端架构师，深入理解 B+ 树底层原理'),
('NO_002', '110101199102022345', 32, 10, 'MySQL 性能调优专家，熟悉索引下推与覆盖索引'),
('NO_003', '110101199203033456', 25, 20, '大数据研发工程师，精通实时计算与数仓架构');

INSERT INTO interview_idx_hash_memory VALUES (1, 'token_xyz_888', 1001);

CREATE TABLE interview_idx_user (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '聚簇索引主键',
    id_card VARCHAR(18) NOT NULL COMMENT '唯一索引列',
    user_name VARCHAR(50) NOT NULL COMMENT '普通单列索引',
    age INT NOT NULL,
    dept_id INT NOT NULL,
    email VARCHAR(100),
    description TEXT,
    UNIQUE KEY uk_id_card (id_card),
    INDEX idx_user_name (user_name),
    INDEX idx_age_dept (age, dept_id),
    FULLTEXT KEY ft_desc (description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='【面试题01】用户索引对照表';

CREATE TABLE interview_hash_user (
    id INT NOT NULL,
    token VARCHAR(64) NOT NULL,
    user_name VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_hash_token (token) USING HASH
) ENGINE=MEMORY DEFAULT CHARSET=utf8mb4 COMMENT='【面试题01】哈希索引验证表';

INSERT INTO interview_idx_user (id_card, user_name, age, dept_id, email, description) VALUES
('110101199001011234', '张三', 28, 10, 'zhangsan@example.com', 'Java 高级工程师，精通 MySQL 调优与索引设计'),
('110101199102022345', '李四', 32, 10, 'lisi@example.com', '分布式架构师，专注高可用高并发系统设计'),
('110101199203033456', '王五', 25, 20, 'wangwu@example.com', '前端全栈开发工程师'),
('110101199304044567', '赵六', 30, 20, 'zhaoliu@example.com', '大数据开发工程师');

INSERT INTO interview_hash_user VALUES
(1, 'token_aaa_111', '张三'),
(2, 'token_bbb_222', '李四'),
(3, 'token_ccc_333', '王五');

-- --------------------------------------------------------------------
-- 【面试题 02: 聚簇索引 vs 非聚簇索引？主键是聚簇索引吗？为什么主键推荐自增 ID 而非 UUID？】
-- 核心考点：
-- 1. 聚簇索引（Clustered）：数据与主键紧密绑定存储在 B+ 树叶子节点中，一张表仅有一个；
-- 2. 非聚簇索引（Secondary）：叶子节点只保存索引列自身 + 主键值，检索非索引字段必须“回表”；
-- 3. 自增 ID 为什么更快：单调连续递增，永远顺序追加当前页末尾，无页分裂；
-- 4. UUID 为什么慢：无序随机写入引发大量 B+ 树页分裂（Page Split）与磁盘碎片。
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS interview_pk_autoincrement;
DROP TABLE IF EXISTS interview_pk_uuid;

CREATE TABLE interview_pk_autoincrement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键 (单调连续递增，零页分裂顺序追加)',
    user_code VARCHAR(32) NOT NULL COMMENT '业务编码',
    score INT NOT NULL COMMENT '考核分数',
    gender TINYINT NOT NULL DEFAULT 1 COMMENT '性别: 1-男, 2-女 (低基数区分度演示)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_score (score) COMMENT '普通二级索引 (叶子节点只存储 score + id)',
    INDEX idx_gender (gender) COMMENT '低区分度索引 (演示优化器放弃索引走全表扫描)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题02: 聚簇索引与自增主键】自增ID主键表，演示顺序IO高效写入与回表机制';

CREATE TABLE interview_pk_uuid (
    id VARCHAR(36) PRIMARY KEY COMMENT 'UUID 无序随机主键 (易导致频繁 B+ 树页分裂与空间膨胀)',
    user_code VARCHAR(32) NOT NULL COMMENT '业务编码',
    score INT NOT NULL COMMENT '考核分数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_score (score) COMMENT '二级索引 (由于主键为 36 字节长 UUID，二级索引体积急剧膨胀)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题02: UUID主键劣势】UUID主键表，演示无序插入页分裂与索引膨胀开销';

INSERT INTO interview_pk_autoincrement (user_code, score) VALUES
('USER_001', 95), ('USER_002', 88), ('USER_003', 92);

INSERT INTO interview_pk_uuid (id, user_code, score) VALUES
(UUID(), 'USER_001', 95), (UUID(), 'USER_002', 88), (UUID(), 'USER_003', 92);

-- --------------------------------------------------------------------
-- 【面试题 03: B+ 树底层原理、叶子节点检索与双向链表反向扫描】
-- 核心考点：
-- 1. B+ 树特性：非叶子节点仅存索引键与指针，单页扇出极大，3~4 层即可支撑千万级数据；
-- 2. 页内查找：通过页目录（Page Directory）二分检索精确定位记录槽（Slot）；
-- 3. 双向链表好处：叶子节点双向指针相连，支持高效范围扫描与 ORDER BY DESC 反向逆序扫描。
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS interview_bplus_tree;

CREATE TABLE interview_bplus_tree (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '聚簇索引主键',
    user_name VARCHAR(50) NOT NULL COMMENT '用户姓名',
    age INT NOT NULL COMMENT '年龄 (二级索引列)',
    balance DECIMAL(12,2) NOT NULL COMMENT '账户余额',
    INDEX idx_age (age) COMMENT 'B+ 树二级索引，叶子节点保存 age + id'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题03: B+树原理与双向链表】演示非叶子节点二分定位、叶子节点双向链表正反向范围扫描';

INSERT INTO interview_bplus_tree (user_name, age, balance) VALUES
('用户A', 20, 1000.00),
('用户B', 22, 1500.00),
('用户C', 25, 2300.00),
('用户D', 28, 3100.00),
('用户E', 30, 4200.00),
('用户F', 35, 5500.00),
('用户G', 40, 6800.00);

-- --------------------------------------------------------------------
-- 【面试题 04: 复合联合索引、最左前缀匹配与索引下推 (ICP)】
-- 核心考点：
-- 1. 最左匹配原则：复合索引 `(a, b, c)` 按 a 有序、a 相同按 b 有序、b 相同按 c 有序构建；
-- 2. 索引下推（ICP）：MySQL 5.6+ 核心优化，将索引列的过滤条件下推到存储引擎层，极大减少回表次数。
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS interview_composite_idx;

CREATE TABLE interview_composite_idx (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    a INT NOT NULL COMMENT '联合索引最左第一列',
    b INT NOT NULL COMMENT '联合索引中间第二列',
    c INT NOT NULL COMMENT '联合索引末尾第三列',
    d VARCHAR(50) NOT NULL COMMENT '载荷字段',
    INDEX idx_composite_abc (a, b, c) COMMENT '复合联合索引 (a, b, c)',
    INDEX idx_single_a (a) COMMENT '单列索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题04: 联合索引与ICP】演示最左匹配、范围截断与 Extra: Using index condition 索引下推';

INSERT INTO interview_composite_idx (a, b, c, d) VALUES
(1, 10, 100, 'Payload Data 1-10-100'),
(1, 10, 200, 'Payload Data 1-10-200'),
(1, 20, 150, 'Payload Data 1-20-150'),
(1, 30, 300, 'Payload Data 1-30-300'),
(2, 10, 100, 'Payload Data 2-10-100'),
(2, 20, 200, 'Payload Data 2-20-200'),
(3, 10, 100, 'Payload Data 3-10-100');

-- --------------------------------------------------------------------
-- 【面试题 05: 8 大索引失效场景与覆盖索引 (Covering Index)】
-- 核心考点：
-- 1. 索引失效典型场景：违背最左前缀、列函数与运算、隐式类型转换、前导模糊、OR 无索引列；
-- 2. 覆盖索引（Covering Index）：查询字段全部存在于二级索引中，Extra 显式呈现 Using index，彻底消除回表。
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS interview_invalidation;

CREATE TABLE interview_invalidation (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键聚簇索引',
    user_code VARCHAR(32) NOT NULL COMMENT '编码 (单列索引，演示函数UPPER()与隐式转换失效)',
    phone_num VARCHAR(20) NOT NULL COMMENT '手机号 (演示字符串未加单引号导致隐式类型转换失效)',
    age INT NOT NULL COMMENT '年龄 (与user_code构成覆盖索引)',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    extra_info VARCHAR(100) COMMENT '非索引列 (用于对比覆盖索引 vs 回表查询代价，以及 OR 连接失效)',
    INDEX idx_user_code (user_code) COMMENT '单列索引',
    INDEX idx_phone (phone_num) COMMENT '手机号索引',
    INDEX idx_created (created_at) COMMENT '时间索引',
    INDEX idx_covering (user_code, age) COMMENT '覆盖索引: 包含 user_code 和 age，实现 Using index 零回表'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题05: 索引失效与覆盖索引】实机验证8大索引失效场景与覆盖索引消除回表';

INSERT INTO interview_invalidation (user_code, phone_num, age, created_at, extra_info) VALUES
('USR1001', '13800000001', 25, '2026-09-01 10:00:00', '北京海淀'),
('USR1002', '13800000002', 28, '2026-09-02 11:30:00', '上海浦东'),
('USR1003', '13900000003', 30, '2026-09-03 14:20:00', '深圳南山'),
('USR1004', '13700000004', 35, '2026-09-04 16:45:00', '广州天河'),
('USR1005', '13600000005', 22, '2026-09-05 09:10:00', '杭州西湖');

-- --------------------------------------------------------------------
-- 【面试题 06: 前缀索引（Prefix Index）与长文本长字段索引优化】
-- 核心考点：
-- 1. 前缀索引原理：长字符串截取前缀字符建索引 `KEY (col(prefix_len))`，以小博大大幅节省内存；
-- 2. 前缀区分度计算公式：`COUNT(DISTINCT LEFT(col, len)) / COUNT(*)`；
-- 3. 前缀索引局限性：无法利用覆盖索引（必须回表），无法用于 ORDER BY 排序。
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS interview_orders;

CREATE TABLE interview_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    order_no VARCHAR(32) NOT NULL COMMENT '订单流水号',
    buyer_email VARCHAR(100) NOT NULL COMMENT '买家邮箱 (长字符串，演示前缀索引长度与区分度计算)',
    order_status TINYINT NOT NULL DEFAULT 1 COMMENT '订单状态: 1-已归档(99%), 0-待处理积压(0.1%)，演示极端数据倾斜走联合索引',
    total_price DECIMAL(10,2) NOT NULL COMMENT '订单总额',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    INDEX idx_email_prefix (buyer_email(8)) COMMENT '前缀索引: 截取前 8 字符建立 B+ 树索引，以小博大节省内存',
    INDEX idx_status_created (order_status, created_at) COMMENT '倾斜状态联合索引: 解决低区分度字段在极端倾斜下的高效检索'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题06: 前缀索引与倾斜字段优化】演示前缀索引区分度计算与以小博大节省内存设计';

INSERT INTO interview_orders (order_no, buyer_email, order_status, total_price) VALUES
('ORD2026090001', 'zhangsan_dev@company.com', 1, 299.00),
('ORD2026090002', 'zhangsan_test@company.com', 1, 199.00),
('ORD2026090003', 'lisi_engineer@company.com', 1, 899.00),
('ORD2026090004', 'wangwu_architect@company.com', 1, 499.00),
('ORD2026090005', 'zhaoliu_pending@company.com', 0, 999.00),
('ORD2026090006', 'sunqi_pending@company.com', 0, 1599.00);

SET FOREIGN_KEY_CHECKS = 1;
-- 03_index_schema.sql 初始化完毕！

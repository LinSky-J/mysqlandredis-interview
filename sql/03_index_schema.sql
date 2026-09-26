-- ====================================================================
-- 03_index_schema.sql
-- MySQL 索引模块数据表结构定义与初始数据
-- 由 DataGrip 统一执行创建，供索引 Java 演示类使用
-- ====================================================================

USE interview_db;

-- ----------------------------------------------------
-- Topic 01: 索引分类与 Memory 哈希索引演示表
-- ----------------------------------------------------
DROP TABLE IF EXISTS interview_idx_user;
DROP TABLE IF EXISTS interview_hash_user;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_hash_user (
    id INT NOT NULL,
    token VARCHAR(64) NOT NULL,
    user_name VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_hash_token (token) USING HASH
) ENGINE=MEMORY DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_idx_user (id_card, user_name, age, dept_id, email, description) VALUES
('110101199001011234', '张三', 28, 10, 'zhangsan@example.com', 'Java 高级工程师，精通 MySQL 调优与索引设计'),
('110101199102022345', '李四', 32, 10, 'lisi@example.com', '分布式架构师，专注高可用高并发系统设计'),
('110101199203033456', '王五', 25, 20, 'wangwu@example.com', '前端全栈开发工程师'),
('110101199304044567', '赵六', 30, 20, 'zhaoliu@example.com', '大数据开发工程师');

INSERT INTO interview_hash_user VALUES
(1, 'token_aaa_111', '张三'),
(2, 'token_bbb_222', '李四'),
(3, 'token_ccc_333', '王五');

-- ----------------------------------------------------
-- Topic 02: 聚簇索引与主键选择 (自增 ID vs 随机 UUID)
-- ----------------------------------------------------
DROP TABLE IF EXISTS interview_pk_autoincrement;
DROP TABLE IF EXISTS interview_pk_uuid;

CREATE TABLE interview_pk_autoincrement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_code VARCHAR(32) NOT NULL,
    score INT NOT NULL,
    gender TINYINT NOT NULL DEFAULT 1 COMMENT '性别: 1-男, 2-女',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_score (score),
    INDEX idx_gender (gender)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_pk_uuid (
    id VARCHAR(36) PRIMARY KEY,
    user_code VARCHAR(32) NOT NULL,
    score INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_score (score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_pk_autoincrement (user_code, score) VALUES
('USER_001', 95), ('USER_002', 88), ('USER_003', 92);

INSERT INTO interview_pk_uuid (id, user_code, score) VALUES
(UUID(), 'USER_001', 95), (UUID(), 'USER_002', 88), (UUID(), 'USER_003', 92);

-- ----------------------------------------------------
-- Topic 03: B+ 树底层原理、叶子节点检索与双向链表反向扫描
-- ----------------------------------------------------
DROP TABLE IF EXISTS interview_bplus_tree;

CREATE TABLE interview_bplus_tree (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(50) NOT NULL,
    age INT NOT NULL,
    balance DECIMAL(12,2) NOT NULL,
    INDEX idx_age (age)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_bplus_tree (user_name, age, balance) VALUES
('用户A', 20, 1000.00),
('用户B', 22, 1500.00),
('用户C', 25, 2300.00),
('用户D', 28, 3100.00),
('用户E', 30, 4200.00),
('用户F', 35, 5500.00),
('用户G', 40, 6800.00);

-- ----------------------------------------------------
-- Topic 04: 联合索引、最左匹配原则与索引下推 (ICP)
-- ----------------------------------------------------
DROP TABLE IF EXISTS interview_composite_idx;

CREATE TABLE interview_composite_idx (
    id INT AUTO_INCREMENT PRIMARY KEY,
    a INT NOT NULL,
    b INT NOT NULL,
    c INT NOT NULL,
    d VARCHAR(50) NOT NULL,
    INDEX idx_composite_abc (a, b, c),
    INDEX idx_single_a (a)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_composite_idx (a, b, c, d) VALUES
(1, 10, 100, 'Payload Data 1-10-100'),
(1, 10, 200, 'Payload Data 1-10-200'),
(1, 20, 150, 'Payload Data 1-20-150'),
(1, 30, 300, 'Payload Data 1-30-300'),
(2, 10, 100, 'Payload Data 2-10-100'),
(2, 20, 200, 'Payload Data 2-20-200'),
(3, 10, 100, 'Payload Data 3-10-100');

-- ----------------------------------------------------
-- Topic 05: 索引失效场景、回表查询与覆盖索引对比表
-- ----------------------------------------------------
DROP TABLE IF EXISTS interview_invalidation;

CREATE TABLE interview_invalidation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_code VARCHAR(32) NOT NULL,
    phone_num VARCHAR(20) NOT NULL,
    age INT NOT NULL,
    created_at DATETIME NOT NULL,
    extra_info VARCHAR(100),
    INDEX idx_user_code (user_code),
    INDEX idx_phone (phone_num),
    INDEX idx_created (created_at),
    INDEX idx_covering (user_code, age)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_invalidation (user_code, phone_num, age, created_at, extra_info) VALUES
('USR1001', '13800000001', 25, '2026-09-01 10:00:00', '北京海淀'),
('USR1002', '13800000002', 28, '2026-09-02 11:30:00', '上海浦东'),
('USR1003', '13900000003', 30, '2026-09-03 14:20:00', '深圳南山'),
('USR1004', '13700000004', 35, '2026-09-04 16:45:00', '广州天河'),
('USR1005', '13600000005', 22, '2026-09-05 09:10:00', '杭州西湖');

-- ----------------------------------------------------
-- Topic 06: 前缀索引、状态值区分度与三星索引优化
-- ----------------------------------------------------
DROP TABLE IF EXISTS interview_orders;

CREATE TABLE interview_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL,
    buyer_email VARCHAR(100) NOT NULL,
    order_status TINYINT NOT NULL DEFAULT 1 COMMENT '1:已归档(99%), 0:待处理积压(0.1%)',
    total_price DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email_prefix (buyer_email(8)),
    INDEX idx_status_created (order_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_orders (order_no, buyer_email, order_status, total_price) VALUES
('ORD2026090001', 'zhangsan_dev@company.com', 1, 299.00),
('ORD2026090002', 'zhangsan_test@company.com', 1, 199.00),
('ORD2026090003', 'lisi_engineer@company.com', 1, 899.00),
('ORD2026090004', 'wangwu_architect@company.com', 1, 499.00),
('ORD2026090005', 'zhaoliu_pending@company.com', 0, 999.00),
('ORD2026090006', 'sunqi_pending@company.com', 0, 1599.00);

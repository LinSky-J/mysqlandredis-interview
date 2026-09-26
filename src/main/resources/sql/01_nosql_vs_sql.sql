-- ====================================================================
-- 问题 01: NOSQL 和 SQL 的区别？
-- ====================================================================
-- 本 SQL 文件演示关系型数据库 (SQL) 的结构化存储、外键关联、ACID 强一致性，
-- 并对比 NoSQL (以 Redis / MongoDB 为代表) 的使用场景与设计哲学。
-- ====================================================================

-- 1. SQL (关系型数据库，如 MySQL):
-- 特点:
--   - 预定义严格 Schema (模式规范，列类型严格)
--   - 遵循 ACID 特性 (原子性、一致性、隔离性、持久性)，保障金融级强一致
--   - 二维表结构，支持复杂的 JOIN 连表查询、子查询、聚合统计
--   - 纵向扩展为主 (Scale-up)，分布式分库分表有较高运维和侵入成本

-- 创建演示表: 用户表 (强类型、结构化)
DROP TABLE IF EXISTS interview_sql_user;
CREATE TABLE interview_sql_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '唯一用户名',
    email VARCHAR(100) NOT NULL COMMENT '邮箱',
    balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '账户余额，需严格事务保护',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关系型结构化用户表';

-- 创建演示表: 订单表 (外键/强关系关联)
DROP TABLE IF EXISTS interview_sql_order;
CREATE TABLE interview_sql_order (
    order_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    amount DECIMAL(10, 2) NOT NULL COMMENT '订单金额',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关系型订单表';

-- 插入测试数据
INSERT INTO interview_sql_user (username, email, balance) VALUES
('zhangsan', 'zhangsan@example.com', 1000.00),
('lisi', 'lisi@example.com', 2500.50);

INSERT INTO interview_sql_order (user_id, amount, status) VALUES
(1, 299.00, 'PAID'),
(1, 499.00, 'PAID'),
(2, 88.00, 'PENDING');

-- SQL 典型的关联分析查询 (NoSQL 难以原生高效处理的复杂连表与事务聚合)
SELECT 
    u.id AS user_id,
    u.username,
    u.balance,
    COUNT(o.order_id) AS total_orders,
    COALESCE(SUM(o.amount), 0.00) AS total_spent
FROM interview_sql_user u
LEFT JOIN interview_sql_order o ON u.id = o.user_id AND o.status = 'PAID'
GROUP BY u.id, u.username, u.balance;

-- ====================================================================
-- 2. NoSQL (非关系型数据库，如 Redis, MongoDB, Cassandra 等) 对比总结:
-- --------------------------------------------------------------------
-- (1) 数据模型:
--     SQL:   二维表格、强类型、预先定义字段约束。
--     NoSQL: 键值(Redis)、文档(MongoDB, JSON/BSON)、列族(HBase)、图(Neo4j)。灵活无模式 (Schema-free)。
-- (2) 一致性与事务:
--     SQL:   遵循 ACID 原则，重视事务与数据的强一致性。
--     NoSQL: 遵循 CAP 定理与 BASE 理论 (基本可用、软状态、最终一致性)，高可用与水平扩展优先。
-- (3) 扩展方式:
--     SQL:   以纵向扩展 (Scale-up) 提升单机硬件性能为主；海量数据需分库分表，跨分片事务与聚合极难。
--     NoSQL: 原生支持横向分片分布式扩展 (Scale-out)，加机器即可线性提升吞吐。
-- (4) 查询能力:
--     SQL:   标准 SQL，多表 JOIN、GROUP BY、HAVING、窗口函数、触发器、存储过程。
--     NoSQL: API 或专有查询语法，不支持或弱支持跨集合 JOIN，鼓励反范式冗余嵌套。
-- (5) 现代系统最佳架构实践:
--     "SQL + NoSQL 协同体系"：
--     - 核心业务、财务订单、强一致账户 -> MySQL (Source of Truth)
--     - 高并发读、热点数据、会话/限流/排行榜 -> Redis (缓存/加速)
--     - 动态属性、日志追踪、海量半结构化数据 -> MongoDB / Elasticsearch
-- ====================================================================

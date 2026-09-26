-- ===================================================================================
-- 07_tuning_schema.sql: MySQL 性能调优、EXPLAIN 执行计划分析与慢查询优化实测数据表
-- 说明：该文件由 DataGrip / 数据库管理工具执行，Java 代码中不包含任何 DDL 语句。
-- 覆盖面试题：
-- 1. EXPLAIN 核心作用与关键字段 (type, possible_keys, key, key_len, rows, Extra)
-- 2. 怎么查看是否走索引 (key, type, key_len 分析准则)
-- 3. 怎么查看表的索引 (SHOW INDEX 结构与 Cardinality 区分度解析)
-- 4. 单表慢查询排查优化 7 大解决方案 (从索引、SQL改写、延迟关联到架构缓存)
-- 5. 优化器选错索引的 5 种干预手段 (FORCE INDEX / IGNORE INDEX / ANALYZE TABLE)
-- ===================================================================================

USE `interview_db`;

-- 1. 员工档案表 (用于 EXPLAIN 各类 type、查看索引、索引干预测试)
DROP TABLE IF EXISTS `tuning_employee`;
CREATE TABLE `tuning_employee` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '员工主键ID (聚簇索引，用于 const/eq_ref 验证)',
    `emp_code` VARCHAR(32) NOT NULL COMMENT '员工工号 (唯一索引，验证 const 点查)',
    `name` VARCHAR(64) NOT NULL COMMENT '员工姓名',
    `dept_id` INT NOT NULL COMMENT '所属部门ID (普通二级索引，验证 ref 等值查找)',
    `age` INT NOT NULL COMMENT '年龄 (联合索引前缀列)',
    `salary` DECIMAL(10,2) NOT NULL COMMENT '薪水 (联合索引后续列)',
    `phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入职创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_emp_code` (`emp_code`),
    INDEX `idx_dept_id` (`dept_id`),
    INDEX `idx_age_salary` (`age`, `salary`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='【面试题：EXPLAIN/索引判定/索引干预】员工档案表，用于演示 EXPLAIN 字段、索引命中与 FORCE INDEX';

-- 初始化员工测试数据
INSERT INTO `tuning_employee` (`emp_code`, `name`, `dept_id`, `age`, `salary`, `phone`) VALUES
('E001', '张三', 10, 25, 8000.00, '13800000001'),
('E002', '李四', 10, 28, 12000.00, '13800000002'),
('E003', '王五', 20, 32, 18000.00, '13800000003'),
('E004', '赵六', 20, 24, 6500.00, '13800000004'),
('E005', '孙七', 30, 35, 25000.00, '13800000005'),
('E006', '周八', 30, 29, 15000.00, '13800000006');

-- 2. 调优测试海量订单表 (用于演示深度分页延迟关联与慢查询优化)
DROP TABLE IF EXISTS `tuning_large_orders`;
CREATE TABLE `tuning_large_orders` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单主键ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
    `customer_id` INT NOT NULL COMMENT '客户ID',
    `total_amount` DECIMAL(12,2) NOT NULL COMMENT '订单金额',
    `order_status` TINYINT NOT NULL DEFAULT 1 COMMENT '订单状态: 1-已支付, 0-未支付',
    `remark` VARCHAR(500) NULL COMMENT '订单长备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    INDEX `idx_cust_created` (`customer_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='【面试题：慢查询治理】海量订单表，演示深度分页延迟关联、索引覆盖与慢 SQL 优化';

INSERT INTO `tuning_large_orders` (`order_no`, `customer_id`, `total_amount`, `order_status`, `remark`) VALUES
('ORD-2026-0001', 101, 299.00, 1, '商品已发货'),
('ORD-2026-0002', 101, 599.00, 1, '送货上门'),
('ORD-2026-0003', 102, 1299.00, 0, '待付款中'),
('ORD-2026-0004', 103, 89.00, 1, '优惠券抵扣');

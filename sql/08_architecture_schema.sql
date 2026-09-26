-- ==================================================================================
-- MySQL 架构高频考点实战表结构与模拟数据集
-- 包含内容：
-- 1. 主从复制与心跳监控模拟表 (arch_replication_heartbeat)
-- 2. 垂直分表设计模拟：订单基础信息表 (arch_order_base) 与 订单大字段扩展表 (arch_order_ext)
-- 3. 水平分表设计模拟：分表 0 (arch_order_h0) 与 分表 1 (arch_order_h1)
-- ==================================================================================

USE `interview_db`;

-- ----------------------------------------------------------------------------------
-- 1. 主从复制与心跳同步模拟表
-- 作用：模拟主库心跳上报、Binlog 生成序列、从库回放时间戳比对以计算 Seconds_Behind_Master
-- ----------------------------------------------------------------------------------
DROP TABLE IF EXISTS `arch_replication_heartbeat`;
CREATE TABLE `arch_replication_heartbeat` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `master_node` VARCHAR(64) NOT NULL DEFAULT 'master-01' COMMENT '主库节点名称',
    `heartbeat_time` DATETIME(3) NOT NULL COMMENT '主库心跳上报精确时间(毫秒级)',
    `trx_seq` BIGINT NOT NULL COMMENT '事务流水递增序号',
    `payload` VARCHAR(255) DEFAULT 'heartbeat_ping' COMMENT '心跳附带业务负载',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='架构篇-主从复制心跳与延迟探测表';

-- ----------------------------------------------------------------------------------
-- 2. 垂直分表示例：
-- 原表字段过多、包含 TEXT/VARCHAR 大文本时，单行数据过长导致 16KB 页容纳行数极少。
-- 垂直拆分为：核心高频字段表 (arch_order_base) + 低频大字段表 (arch_order_ext)
-- ----------------------------------------------------------------------------------
DROP TABLE IF EXISTS `arch_order_ext`;
DROP TABLE IF EXISTS `arch_order_base`;

CREATE TABLE `arch_order_base` (
    `order_id` BIGINT NOT NULL COMMENT '全局业务订单ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID(高频查询/索引字段)',
    `order_sn` VARCHAR(64) NOT NULL COMMENT '唯一订单编号',
    `total_amount` DECIMAL(12, 2) NOT NULL COMMENT '订单总金额',
    `order_status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态: 0-待支付, 1-已支付, 2-已发货, 3-已完成',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    PRIMARY KEY (`order_id`),
    KEY `idx_user_status` (`user_id`, `order_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='架构篇-垂直分表基础表(紧凑高频核心数据)';

CREATE TABLE `arch_order_ext` (
    `order_id` BIGINT NOT NULL COMMENT '关联基础表订单ID',
    `shipping_address` VARCHAR(500) NOT NULL COMMENT '详细收货地址',
    `user_remarks` VARCHAR(1000) DEFAULT NULL COMMENT '用户下单备注留言',
    `snapshot_json` TEXT COMMENT '下单时商品快照JSON(大文本字段，极占Page空间)',
    `invoice_info` VARCHAR(500) DEFAULT NULL COMMENT '发票开具信息',
    PRIMARY KEY (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='架构篇-垂直分表扩展表(低频大字段分离)';

-- ----------------------------------------------------------------------------------
-- 3. 水平分表示例：
-- 相同结构的表根据 sharding_key (user_id % 2) 路由到不同物理子表
-- ----------------------------------------------------------------------------------
DROP TABLE IF EXISTS `arch_order_h0`;
CREATE TABLE `arch_order_h0` (
    `order_id` BIGINT NOT NULL COMMENT '分布式雪花算法订单ID',
    `user_id` BIGINT NOT NULL COMMENT '分片键 Sharding Key (user_id % 2 == 0)',
    `order_sn` VARCHAR(64) NOT NULL COMMENT '订单流水号',
    `amount` DECIMAL(12, 2) NOT NULL COMMENT '支付金额',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`order_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='架构篇-水平分表分片0 (偶数用户)';

DROP TABLE IF EXISTS `arch_order_h1`;
CREATE TABLE `arch_order_h1` (
    `order_id` BIGINT NOT NULL COMMENT '分布式雪花算法订单ID',
    `user_id` BIGINT NOT NULL COMMENT '分片键 Sharding Key (user_id % 2 == 1)',
    `order_sn` VARCHAR(64) NOT NULL COMMENT '订单流水号',
    `amount` DECIMAL(12, 2) NOT NULL COMMENT '支付金额',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`order_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='架构篇-水平分表分片1 (奇数用户)';

-- 初始化部分测试数据
INSERT INTO `arch_replication_heartbeat` (`master_node`, `heartbeat_time`, `trx_seq`, `payload`) VALUES
('node-master-01', NOW(3), 10001, 'initial_heartbeat_sync');

INSERT INTO `arch_order_base` (`order_id`, `user_id`, `order_sn`, `total_amount`, `order_status`) VALUES
(1001, 2001, 'SN202609260001', 199.00, 1),
(1002, 2002, 'SN202609260002', 499.50, 0);

INSERT INTO `arch_order_ext` (`order_id`, `shipping_address`, `user_remarks`, `snapshot_json`) VALUES
(1001, '北京市海淀区中关村南大街1号', '请工作日送达', '{"items":[{"sku":888,"name":"机械键盘","price":199.00}]}'),
(1002, '上海市浦东新区陆家嘴环路1000号', '放置门卫处', '{"items":[{"sku":999,"name":"人体工学椅","price":499.50}]}');

INSERT INTO `arch_order_h0` (`order_id`, `user_id`, `order_sn`, `amount`) VALUES
(5001, 1000, 'SN_H0_001', 88.00);

INSERT INTO `arch_order_h1` (`order_id`, `user_id`, `order_sn`, `amount`) VALUES
(5002, 1001, 'SN_H1_001', 99.00);

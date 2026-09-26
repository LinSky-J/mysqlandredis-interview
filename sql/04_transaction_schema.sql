-- ===================================================================================
-- 04_transaction_schema.sql: MySQL 事务核心机制、隔离级别与并发控制数据表定义
-- 说明：该文件由 DataGrip / 数据库管理工具单独执行，Java 代码中不包含任何 DDL 语句。
-- 覆盖面试题：
-- 1. 事务特性 ACID 与底层实现 (Undo Log / Redo Log)
-- 2. 并发问题：脏读、不可重复读、幻读、丢失更新
-- 3. 脏读不适场景与电商/金融转账资损案例
-- 4. 并发解决方案：MVCC + 锁机制
-- 5. 隔离级别定义与 MySQL 默认 RR
-- 6. 可重复读下已提交数据可见性 (快照读 vs 当前读)
-- 7. 幻读复现全过程 (幽灵记录)
-- 8. Next-Key Lock 临键锁消除幻读
-- 9. 串行化 Serializable 读写互斥实现
-- 10. MVCC 隐藏列、版本链与 ReadView 核心算法
-- 11. 单条 UPDATE 原子性与 Statement Rollback
-- 12. 长事务/大事务的 5 大弊端与线上事故防范
-- ===================================================================================

USE `interview_db`;

-- 1. 账户资金表 (用于演示 ACID、脏读、不可重复读、当前读与快照读、幻读)
DROP TABLE IF EXISTS `tx_account`;
CREATE TABLE `tx_account` (
    `id` INT NOT NULL COMMENT '账户主键ID (聚簇索引，用于行锁与间隙锁加锁基准)',
    `user_name` VARCHAR(64) NOT NULL COMMENT '用户姓名',
    `balance` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额 (金融场景，严禁脏读与非原子更新)',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号 (解决业务层第二类丢失更新)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '账户创建时间戳',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='【面试题：ACID/隔离级别/幻读】账户资金表，用于实测并发事务下的读写冲突、MVCC 快照读与 Next-Key Lock 间隙锁';

-- 初始化测试账户数据
INSERT INTO `tx_account` (`id`, `user_name`, `balance`, `version`) VALUES
(1, '张三', 1000.00, 0),
(2, '李四', 1000.00, 0),
(10, 'VIP-赵六', 5000.00, 0),
(20, 'VIP-钱七', 8000.00, 0);

-- 2. 交易订单表 (用于演示长事务、锁等待超时、大事务 Undo 膨胀与主从延迟)
DROP TABLE IF EXISTS `tx_order`;
CREATE TABLE `tx_order` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '订单流水主键ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '全局唯一订单业务编号',
    `user_id` INT NOT NULL COMMENT '下单用户ID',
    `amount` DECIMAL(12,2) NOT NULL COMMENT '订单总金额',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '订单状态: PENDING-待支付, PAID-已支付, CANCELLED-已取消',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='【面试题：大事务弊端】交易订单表，用于演示长事务持有行级排他锁导致的 Lock Wait Timeout 及连接池占满问题';

INSERT INTO `tx_order` (`order_no`, `user_id`, `amount`, `status`) VALUES
('ORD-20260901-0001', 1, 199.00, 'PAID'),
('ORD-20260901-0002', 2, 299.00, 'PENDING');

-- 3. 商品库存表 (用于演示超卖并发、当前读加锁 SELECT FOR UPDATE、避免第二类丢失更新)
DROP TABLE IF EXISTS `tx_product_stock`;
CREATE TABLE `tx_product_stock` (
    `product_id` INT NOT NULL COMMENT '商品主键ID (行锁锁定粒度)',
    `product_name` VARCHAR(64) NOT NULL COMMENT '商品名称',
    `stock_count` INT NOT NULL DEFAULT 0 COMMENT '可用库存数量 (高并发扣减争抢核心字段)',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='【面试题：并发更新与丢失更新】商品库存表，演示无锁并发更新丢失与 SELECT FOR UPDATE 悲观排他锁方案';

INSERT INTO `tx_product_stock` (`product_id`, `product_name`, `stock_count`) VALUES
(101, 'iPhone 17 Pro Max', 10),
(102, 'MacBook Pro M4', 5);

-- 4. 用户信息唯一约束校验表 (用于演示单条 UPDATE 语句原子性与内部语句级自动回滚 Statement Rollback)
DROP TABLE IF EXISTS `tx_user_unique`;
CREATE TABLE `tx_user_unique` (
    `id` INT NOT NULL COMMENT '用户主键ID',
    `user_code` VARCHAR(32) NOT NULL COMMENT '用户唯一编码 (唯一键约束，用于制造局部冲突验证整体原子回滚)',
    `nick_name` VARCHAR(64) NOT NULL COMMENT '用户昵称',
    `score` INT NOT NULL DEFAULT 0 COMMENT '用户积分',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_code` (`user_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='【面试题：单条 UPDATE 原子性】用户信息表，通过唯一键约束触发单条批量 UPDATE 中途失败，实测整句全部回滚的原子性';

INSERT INTO `tx_user_unique` (`id`, `user_code`, `nick_name`, `score`) VALUES
(1, 'CODE_A', 'Alice', 100),
(2, 'CODE_B', 'Bob', 200),
(3, 'CODE_C', 'Charlie', 300);

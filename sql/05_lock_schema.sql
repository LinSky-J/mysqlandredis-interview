-- ===================================================================================
-- 05_lock_schema.sql: MySQL 锁机制核心考点、表锁与行锁、主键与非索引范围并发锁实测数据表
-- 说明：该文件由 DataGrip / 数据库管理工具执行，Java 代码中不包含任何 DDL 语句。
-- 覆盖面试题：
-- 1. MySQL 锁分类全景 (全局锁/表锁/行锁/共享锁/排他锁/意向锁/MDL锁/临键锁)
-- 2. 表锁与行锁的作用、适用场景与性能对比
-- 3. 两个线程同时 UPDATE 同一条记录的阻塞与 X 锁互斥实测
-- 4. 两个线程 UPDATE 不同的主键范围 (<10 与 >15) 无阻塞底层原理实测
-- 5. 两个线程 UPDATE 无索引字段的范围 (<10 与 >15) 全表锁退化与严重阻塞实测
-- ===================================================================================

USE `interview_db`;

-- 1. 商品库存锁竞争表 (用于演示同记录并发 UPDATE 行排他锁互斥与阻塞)
DROP TABLE IF EXISTS `lock_item_stock`;
CREATE TABLE `lock_item_stock` (
    `id` INT NOT NULL COMMENT '商品主键ID (聚簇索引，行锁锚定点)',
    `item_name` VARCHAR(64) NOT NULL COMMENT '商品名称',
    `stock` INT NOT NULL DEFAULT 0 COMMENT '可用库存量 (两个线程并发争抢同一行的核心资源)',
    `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '商品价格',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间戳',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='【面试题02/03: 行排他锁与阻塞】商品库存表，演示两线程并发 UPDATE 同一行时的 X 锁互斥与锁等待超时';

INSERT INTO `lock_item_stock` (`id`, `item_name`, `stock`, `price`) VALUES
(1, 'RTX 5090 显卡', 100, 15999.00),
(2, 'Core Ultra 9 处理器', 50, 4299.00);

-- 2. 主键范围锁实测表 (用于演示主键 B+ 树索引下 <10 与 >15 范围更新互不阻塞)
DROP TABLE IF EXISTS `lock_range_pk`;
CREATE TABLE `lock_range_pk` (
    `id` INT NOT NULL COMMENT '主键ID (聚簇索引，有序 B+ 树排列，提供精确 Next-Key Lock 范围)',
    `title` VARCHAR(64) NOT NULL COMMENT '任务标题',
    `status` INT NOT NULL DEFAULT 0 COMMENT '任务状态: 0-未完成, 1-已完成',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='【面试题04: 主键范围更新无阻塞】主键范围更新实测表，演示 <10 与 >15 间隙锁区间无交集时不发生阻塞';

INSERT INTO `lock_range_pk` (`id`, `title`, `status`) VALUES
(1, 'Task-1 (属于 <10 区间)', 0),
(5, 'Task-5 (属于 <10 区间)', 0),
(8, 'Task-8 (属于 <10 区间)', 0),
(12, 'Task-12 (中间隔离区 10~15)', 0),
(18, 'Task-18 (属于 >15 区间)', 0),
(20, 'Task-20 (属于 >15 区间)', 0),
(25, 'Task-25 (属于 >15 区间)', 0);

-- 3. 无索引范围锁实测表 (用于演示无索引条件导致全表扫描、全表加锁与严重阻塞)
DROP TABLE IF EXISTS `lock_range_non_index`;
CREATE TABLE `lock_range_non_index` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '自增主键ID',
    `age` INT NOT NULL COMMENT '年龄 (故意不建立任何索引！用于实测 WHERE age < 10 导致全表加锁)',
    `user_name` VARCHAR(64) NOT NULL COMMENT '用户姓名',
    `score` INT NOT NULL DEFAULT 0 COMMENT '积分数值',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='【面试题05: 无索引范围更新锁全表】无索引表，实测 WHERE age < 10 与 age > 15 发生全表扫描与行锁全表退化引发严重阻塞';

INSERT INTO `lock_range_non_index` (`id`, `age`, `user_name`, `score`) VALUES
(1, 5, 'Alice (age=5 <10)', 100),
(2, 8, 'Bob (age=8 <10)', 100),
(3, 12, 'Charlie (age=12 中间隔离)', 100),
(4, 18, 'David (age=18 >15)', 100),
(5, 22, 'Eva (age=22 >15)', 100);

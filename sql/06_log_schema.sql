-- ===================================================================================
-- 06_log_schema.sql: MySQL 日志架构 (Binlog / Redo Log / Undo Log / 2PC / 双写缓冲) 数据表
-- 说明：该文件由 DataGrip / 数据库管理工具执行，Java 代码中不包含任何 DDL 语句。
-- 覆盖面试题：
-- 1. MySQL 7 大日志文件分类 (Binlog / Redo / Undo / Error / Slow / General / Relay)
-- 2. Binlog 三种格式与逻辑日志机制
-- 3. Undo Log 原子性与 MVCC 版本链
-- 4. Undo Log 与 Redo Log 协同互补
-- 5. Redo Log WAL 机制与刷盘策略 (innodb_flush_log_at_trx_commit)
-- 6. 为什么 Binlog 无法替代 Redo Log (Crash-Safe 崩溃恢复)
-- 7. Binlog 与 Redo Log 两阶段提交 (2PC)
-- 8. UPDATE 语句全链路生命周期 (Server 层与引擎层交互)
-- 9. 双 1 配置与数据零丢失保障 (innodb_flush_log_at_trx_commit=1 + sync_binlog=1)
-- 10. Redo Log 内存 (Redo Log Buffer) 与磁盘物理文件架构
-- 11. 顺序 I/O vs 随机 I/O: 为什么写 Redo Log 而不是直接写 B+ 树
-- 12. Doublewrite Buffer (两次写) 解决页断裂 (Partial Page Write)
-- ===================================================================================

USE `interview_db`;

-- 1. WAL 与两阶段提交账户实测表
DROP TABLE IF EXISTS `log_account_wal`;
CREATE TABLE `log_account_wal` (
    `id` INT NOT NULL COMMENT '账户主键ID (聚簇索引，定位 Buffer Pool 数据页)',
    `account_name` VARCHAR(64) NOT NULL COMMENT '账户名称',
    `balance` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额 (UPDATE 修改字段，触发 Undo 旧值与 Redo 增量物理日志)',
    `last_trx_desc` VARCHAR(255) NULL COMMENT '最近一次事务所处 2PC 阶段描述',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='【面试题：日志全链路/2PC】WAL 与两阶段提交演示表，实测 UPDATE 语句生命周期与日志协同';

INSERT INTO `log_account_wal` (`id`, `account_name`, `balance`, `last_trx_desc`) VALUES
(1, '企业对公金库A', 1000000.00, '初始准备阶段'),
(2, '供应商往来账户B', 500000.00, '初始准备阶段');

-- 2. 顺序写 vs 随机写性能实测表
DROP TABLE IF EXISTS `log_wal_performance`;
CREATE TABLE `log_wal_performance` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `payload` VARCHAR(255) NOT NULL COMMENT '业务有效载荷 (模拟写入产生 Redo 顺序记录)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='【面试题：为什么写 Redo Log】性能对照表，演示 WAL 顺序追加写相较于直接离散刷脏页的巨大性能优势';

INSERT INTO `log_wal_performance` (`payload`) VALUES
('WAL Bench Initial Payload 1'),
('WAL Bench Initial Payload 2');

-- ====================================================================
-- 02_storage_engine_schema.sql
-- MySQL 存储引擎专题数据表结构定义与初始数据
-- 
-- 包含一条 SQL 请求执行生命周期、WAL 与两阶段提交 (2PC)、
-- 四大存储引擎（InnoDB/MyISAM/Memory/Archive）全景特性对比、
-- 为什么 InnoDB 成为默认引擎，以及底层数据文件与表空间巡检。
-- ====================================================================

USE interview_db;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- --------------------------------------------------------------------
-- 【面试题 01: 执行一条 SQL 请求的过程与两阶段提交 (2PC)】
-- 核心考点：
-- 1. 架构分层：Server 层（连接器 -> 分析器 -> 优化器 -> 执行器） + 存储引擎层（Buffer Pool 与磁盘交互）；
-- 2. 更新操作 WAL 机制：写 Undo Log -> 写 Buffer Pool -> 写 Redo Log(prepare) -> 写 Binlog -> Redo Log(commit)。
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS interview_engine_order;
DROP TABLE IF EXISTS interview_user_2pc;

CREATE TABLE interview_user_2pc (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '账户主键ID',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    balance DECIMAL(10,2) NOT NULL COMMENT '账户资金 (用于验证事务原子扣减与2PC两阶段提交)',
    version INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题01: SQL执行流程】用户资金表，演示 Redo Log 与 Binlog 两阶段提交与崩溃恢复';

INSERT INTO interview_user_2pc (id, name, balance, version) VALUES
(1, '张三', 1000.00, 1),
(2, '李四', 500.00, 1);

CREATE TABLE interview_engine_order (
    order_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单主键ID',
    user_id BIGINT NOT NULL COMMENT '下单用户ID',
    amount DECIMAL(10, 2) NOT NULL COMMENT '支付金额',
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED' COMMENT '订单状态',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    INDEX idx_user_status (user_id, status) COMMENT '联合索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题01: SQL执行流程】订单表，演示优化器 CBO 成本估算与执行器下发存储引擎过程';

INSERT INTO interview_engine_order (user_id, amount, status) VALUES 
(1001, 299.00, 'CREATED');

-- --------------------------------------------------------------------
-- 【面试题 02: 讲一讲 MySQL 的引擎吧，你有什么了解？】
-- 核心考点：
-- 1. InnoDB：支持事务（ACID）、行级锁、外键、MVCC、Crash-Safe，是 OLTP 绝对主流；
-- 2. MyISAM：不支持事务、仅支持表级锁，无崩溃安全保护，只读全文搜索历史遗留；
-- 3. MEMORY：全内存存储，使用哈希索引，极度快速，但重启即丢全部数据；
-- 4. ARCHIVE：只支持 INSERT 和 SELECT，采用 zlib 超高压缩，适合海量审计归档日志。
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS interview_engine_innodb;
DROP TABLE IF EXISTS interview_engine_myisam;
DROP TABLE IF EXISTS interview_engine_memory;
DROP TABLE IF EXISTS interview_engine_archive;

CREATE TABLE interview_engine_innodb (
    id INT PRIMARY KEY COMMENT '主键ID',
    account_no VARCHAR(50) NOT NULL COMMENT '银行卡账号',
    balance DECIMAL(12, 2) NOT NULL COMMENT '资金余额'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题02: 存储引擎对照】InnoDB引擎：支持事务ACID、行级锁、MVCC与崩溃自愈';

CREATE TABLE interview_engine_myisam (
    id INT PRIMARY KEY COMMENT '主键ID',
    log_title VARCHAR(100) NOT NULL COMMENT '日志标题',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间'
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题02: 存储引擎对照】MyISAM引擎：无事务、全表锁、只读高检索效率';

CREATE TABLE interview_engine_memory (
    id INT PRIMARY KEY COMMENT '主键ID',
    key_name VARCHAR(50) NOT NULL COMMENT '缓存键',
    val VARCHAR(100) NOT NULL COMMENT '缓存值'
) ENGINE=MEMORY DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题02: 存储引擎对照】MEMORY引擎：纯内存驻留、哈希索引支持、重启即丢';

CREATE TABLE interview_engine_archive (
    id INT COMMENT '自增流水ID',
    action_log VARCHAR(100) COMMENT '操作行为日志',
    created_at DATETIME COMMENT '时间'
) ENGINE=ARCHIVE DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题02: 存储引擎对照】ARCHIVE引擎：超高压缩比、仅追加无修改、历史审计归档专用';

INSERT INTO interview_engine_innodb VALUES (1, '622202000000001', 99999.00);
INSERT INTO interview_engine_myisam VALUES (1, '系统启动日志', NOW());
INSERT INTO interview_engine_memory VALUES (1, 'SESSION_TOKEN', 'abc123xyz');
INSERT INTO interview_engine_archive VALUES (1, 'User Login Event', NOW());

-- --------------------------------------------------------------------
-- 【面试题 03: 为什么 MySQL 把 InnoDB 作为默认存储引擎？】
-- 核心考点：
-- 1. 写并发：MyISAM 全表锁读写互斥，InnoDB 细粒度行锁并发能力强；
-- 2. 数据可靠性：ACID 事务保障与 Redo/Undo 两阶段提交 Crash-Safe 崩溃安全保护；
-- 3. 聚簇索引架构：主键直接挂整行数据，减少非聚簇索引回表开销。
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS interview_why_innodb;
DROP TABLE IF EXISTS interview_default_innodb;

CREATE TABLE interview_default_innodb (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键ID',
    account_no VARCHAR(32) NOT NULL UNIQUE COMMENT '账户唯一编号',
    money DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '金额',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题03: 默认InnoDB原因】演示高并发扣减、行级锁互不阻塞与原子提交特性';

CREATE TABLE interview_why_innodb (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    account_name VARCHAR(50) NOT NULL COMMENT '开户人姓名',
    balance DECIMAL(10, 2) NOT NULL COMMENT '账户余额'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题03: 默认InnoDB原因】InnoDB行锁与聚簇索引特性验证表';

INSERT INTO interview_default_innodb (account_no, money) VALUES
('ACC_001', 5000.00),
('ACC_002', 3000.00);

INSERT INTO interview_why_innodb (account_name, balance) VALUES 
('张三', 1000.00), 
('李四', 2000.00);

-- --------------------------------------------------------------------
-- 【面试题 04: MySQL 的 InnoDB 与 MyISAM 的区别？】
-- 核心考点：
-- 1. 事务支持：InnoDB 支持 COMMIT/ROLLBACK；MyISAM 完全不支持，回滚直接失效；
-- 2. 锁粒度：InnoDB 行锁与 Next-Key Lock；MyISAM 仅支持全表锁；
-- 3. COUNT(*) 效率：MyISAM 元数据计数器秒出；InnoDB 因 MVCC 必须扫描索引逐行计数；
-- 4. 物理文件构成：InnoDB 为 `.ibd`；MyISAM 分为 `.MYD`（数据）与 `.MYI`（索引）。
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS interview_cmp_innodb;
DROP TABLE IF EXISTS interview_cmp_myisam;
DROP TABLE IF EXISTS interview_compare_innodb;
DROP TABLE IF EXISTS interview_compare_myisam;

CREATE TABLE interview_cmp_innodb (
    id INT PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    score INT NOT NULL COMMENT '分数'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题04: 引擎差异对照】InnoDB对比表：验证事务回滚有效性与COUNT(*)执行计划';

CREATE TABLE interview_cmp_myisam (
    id INT PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    score INT NOT NULL COMMENT '分数'
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题04: 引擎差异对照】MyISAM对比表：验证事务回滚失效(脏写依然落盘)与全表锁限制';

INSERT INTO interview_cmp_innodb VALUES (1, 'Alice', 95), (2, 'Bob', 88), (3, 'Charlie', 91);
INSERT INTO interview_cmp_myisam VALUES (1, 'Alice', 95), (2, 'Bob', 88), (3, 'Charlie', 91);

CREATE TABLE interview_compare_innodb (
    id INT PRIMARY KEY,
    name VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='【面试题04】事务对比InnoDB表';

CREATE TABLE interview_compare_myisam (
    id INT PRIMARY KEY,
    name VARCHAR(50)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT='【面试题04】事务对比MyISAM表';

INSERT INTO interview_compare_innodb VALUES (1, 'A'), (2, 'B');
INSERT INTO interview_compare_myisam VALUES (1, 'A'), (2, 'B');

-- --------------------------------------------------------------------
-- 【面试题 05: 数据管理里，数据文件大体分成哪几种数据文件？】
-- 核心考点：
-- 1. 表空间数据文件：独立表空间 `.ibd` 与共享系统表空间 `ibdata1`；
-- 2. 重做日志文件（Redo Log）：`ib_logfile0/1`，WAL 崩溃恢复核心；
-- 3. 回滚日志文件（Undo Log）：`undo_001/002`，记录修改前镜像；
-- 4. 二进制归档日志（Binlog）：`binlog.000001`，主从复制与 PITR 跨天增量恢复；
-- 5. 元数据定义与控制文件：`.sdi`（MySQL 8.0 数据字典）与 `my.ini`。
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS interview_file_innodb;
DROP TABLE IF EXISTS interview_file_myisam;

CREATE TABLE interview_file_innodb (
    id INT PRIMARY KEY COMMENT '主键ID',
    file_info VARCHAR(100) NOT NULL COMMENT '文件描述'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题05: 物理文件架构】演示物理磁盘上 .ibd 独立表空间文件的生成';

CREATE TABLE interview_file_myisam (
    id INT PRIMARY KEY COMMENT '主键ID',
    file_info VARCHAR(100) NOT NULL COMMENT '文件描述'
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题05: 物理文件架构】演示物理磁盘上 .MYD(数据) 与 .MYI(索引) 文件的分立存储';

INSERT INTO interview_file_innodb VALUES (1, '独立表空间 ibd 格式验证记录');
INSERT INTO interview_file_myisam VALUES (1, 'MyISAM MYD/MYI 格式验证记录');

SET FOREIGN_KEY_CHECKS = 1;
-- 02_storage_engine_schema.sql 初始化完毕！

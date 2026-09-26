-- ====================================================================
-- 02_storage_engine_schema.sql
-- 存储引擎模块数据表结构定义与初始数据
-- 由 DataGrip 统一执行创建，供存储引擎 Java 演示类使用
-- ====================================================================

USE interview_db;

-- ----------------------------------------------------
-- Topic 01: SQL 执行两阶段提交 (2PC) 演示表
-- ----------------------------------------------------
DROP TABLE IF EXISTS interview_engine_order;
DROP TABLE IF EXISTS interview_user_2pc;

CREATE TABLE interview_user_2pc (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    balance DECIMAL(10,2) NOT NULL,
    version INT NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_user_2pc (id, name, balance, version) VALUES
(1, '张三', 1000.00, 1),
(2, '李四', 500.00, 1);

CREATE TABLE interview_engine_order (
    order_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_engine_order (user_id, amount, status) VALUES 
(1001, 299.00, 'CREATED');

-- ----------------------------------------------------
-- Topic 02: MySQL 多存储引擎全景对照表
-- ----------------------------------------------------
DROP TABLE IF EXISTS interview_engine_innodb;
DROP TABLE IF EXISTS interview_engine_myisam;
DROP TABLE IF EXISTS interview_engine_memory;
DROP TABLE IF EXISTS interview_engine_archive;

CREATE TABLE interview_engine_innodb (
    id INT PRIMARY KEY,
    account_no VARCHAR(50) NOT NULL,
    balance DECIMAL(12, 2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='InnoDB支持事务与外键';

CREATE TABLE interview_engine_myisam (
    id INT PRIMARY KEY,
    log_title VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT='MyISAM非事务全文分析引擎';

CREATE TABLE interview_engine_memory (
    id INT PRIMARY KEY,
    key_name VARCHAR(50) NOT NULL,
    val VARCHAR(100) NOT NULL
) ENGINE=MEMORY DEFAULT CHARSET=utf8mb4 COMMENT='Memory内存临时高速引擎';

CREATE TABLE interview_engine_archive (
    id INT,
    action_log VARCHAR(100),
    created_at DATETIME
) ENGINE=ARCHIVE DEFAULT CHARSET=utf8mb4 COMMENT='Archive高压缩审计日志引擎';

INSERT INTO interview_engine_innodb VALUES (1, '622202000000001', 99999.00);
INSERT INTO interview_engine_myisam VALUES (1, '系统启动日志', NOW());
INSERT INTO interview_engine_memory VALUES (1, 'SESSION_TOKEN', 'abc123xyz');
INSERT INTO interview_engine_archive VALUES (1, 'User Login Event', NOW());

-- ----------------------------------------------------
-- Topic 03: 为什么 InnoDB 成为默认存储引擎验证表
-- ----------------------------------------------------
DROP TABLE IF EXISTS interview_why_innodb;
DROP TABLE IF EXISTS interview_default_innodb;

CREATE TABLE interview_default_innodb (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account_no VARCHAR(32) NOT NULL UNIQUE,
    money DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_why_innodb (
    id INT PRIMARY KEY AUTO_INCREMENT,
    account_name VARCHAR(50) NOT NULL,
    balance DECIMAL(10, 2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_default_innodb (account_no, money) VALUES
('ACC_001', 5000.00),
('ACC_002', 3000.00);

INSERT INTO interview_why_innodb (account_name, balance) VALUES 
('张三', 1000.00), 
('李四', 2000.00);

-- ----------------------------------------------------
-- Topic 04: InnoDB vs MyISAM 深度对比表
-- ----------------------------------------------------
DROP TABLE IF EXISTS interview_cmp_innodb;
DROP TABLE IF EXISTS interview_cmp_myisam;
DROP TABLE IF EXISTS interview_compare_innodb;
DROP TABLE IF EXISTS interview_compare_myisam;

CREATE TABLE interview_cmp_innodb (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    score INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_cmp_myisam (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    score INT NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_cmp_innodb (username, score) VALUES ('Alice', 95), ('Bob', 88), ('Charlie', 91);
INSERT INTO interview_cmp_myisam (username, score) VALUES ('Alice', 95), ('Bob', 88), ('Charlie', 91);

CREATE TABLE interview_compare_innodb (
    id INT PRIMARY KEY,
    val VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_compare_myisam (
    id INT PRIMARY KEY,
    val VARCHAR(50)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_compare_innodb VALUES (1, 'InnoDB Row 1'), (2, 'InnoDB Row 2');
INSERT INTO interview_compare_myisam VALUES (1, 'MyISAM Row 1'), (2, 'MyISAM Row 2');

-- ----------------------------------------------------
-- Topic 05: 数据文件架构全景演示表
-- ----------------------------------------------------
DROP TABLE IF EXISTS interview_file_innodb;
DROP TABLE IF EXISTS interview_file_myisam;

CREATE TABLE interview_file_innodb (
    id INT AUTO_INCREMENT PRIMARY KEY,
    detail VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_file_myisam (
    id INT AUTO_INCREMENT PRIMARY KEY,
    detail VARCHAR(100)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_file_innodb (detail) VALUES ('InnoDB Page Architecture Demo');
INSERT INTO interview_file_myisam (detail) VALUES ('MyISAM Three File Architecture Demo');

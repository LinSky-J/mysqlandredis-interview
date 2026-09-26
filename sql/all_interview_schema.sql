-- ====================================================================
-- all_interview_schema.sql
-- 全量数据库与表结构一键初始化总脚本 (含全部面试高频考点详注)
-- 
-- 推荐直接在 DataGrip 中打开并一键执行 (Ctrl + Enter)
-- 包含：00_init_database.sql, 01_mysql_basics_schema.sql, 
--       02_storage_engine_schema.sql, 03_index_schema.sql
-- ====================================================================

CREATE DATABASE IF NOT EXISTS interview_db 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE interview_db;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;



-- ====================================================================
-- 00_init_database.sql
-- 数据库初始化脚本
-- 由 DataGrip 统一执行，创建面试演示数据库 interview_db
-- ====================================================================

CREATE DATABASE IF NOT EXISTS interview_db 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE interview_db;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


-- ====================================================================
-- 01_mysql_basics_schema.sql
-- MySQL 核心基础模块 16 讲表结构定义与初始数据
-- 
-- 本脚本包含三大范式、多表关联、防重插入、数据类型容量、IP 高效存储、
-- 外键约束机制、IN/EXISTS 陷阱、SQL 执行顺序及经典面试 SQL 实战题。
-- 每个表与关键字段均附带精准的面试考点 COMMENT 注解，可直接在 DataGrip 中查阅。
-- ====================================================================

USE interview_db;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- --------------------------------------------------------------------
-- 【面试题 01: SQL 与 NoSQL 的区别与选型】
-- 核心考点：
-- 1. ACID vs BASE：SQL 提供高可靠的事务保障与强模式（Schema）检查；
-- 2. NoSQL 适合高并发、弱结构化与横向弹性伸缩（Sharding）；
-- 3. 本组表演示结构化实体及其严格的外键与唯一性约束。
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS interview_sql_order;
DROP TABLE IF EXISTS interview_sql_user;

CREATE TABLE interview_sql_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户主键ID (自增聚簇索引)',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名 (唯一约束，强一致性校验)',
    email VARCHAR(100) NOT NULL COMMENT '用户电子邮箱',
    balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '账户余额 (高精度DECIMAL防浮点精度失真)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '注册创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题01: SQL与NoSQL】关系型强结构化用户主表，演示ACID与强约束机制';

CREATE TABLE interview_sql_order (
    order_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单主键ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID (外键逻辑关联)',
    amount DECIMAL(10, 2) NOT NULL COMMENT '订单总金额',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '订单状态: PENDING/PAID/CANCELLED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    INDEX idx_user_id (user_id) COMMENT '买家用户ID索引，加速用户订单多表JOIN与检索'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题01: SQL与NoSQL】订单从表，演示多表范式化存储与二级索引关联';

INSERT INTO interview_sql_user (username, email, balance) VALUES 
('zhangsan', 'zhangsan@example.com', 1000.00),
('lisi', 'lisi@example.com', 2500.50);

INSERT INTO interview_sql_order (user_id, amount, status) VALUES 
(1, 299.00, 'PAID'),
(1, 499.00, 'PAID'),
(2, 88.00, 'PENDING');

-- --------------------------------------------------------------------
-- 【面试题 02: 数据库三大范式 (1NF, 2NF, 3NF) 及反范式化】
-- 核心考点：
-- 1. 1NF：字段不可再分（原子性）。将 address 拆为 province/city/detail_address；
-- 2. 2NF：消除非主键列对主键的部分依赖（满足 1NF 且每一非主属性完全依赖复合主键）；
-- 3. 3NF：消除非主键列对主键的传递依赖（满足 2NF 且非主属性不传递依赖主键）；
-- 4. 商业权衡：适度反范式（如冗余字段减少昂贵的分布式多表 JOIN）。
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS interview_1nf_user;
DROP TABLE IF EXISTS interview_1nf_student;
DROP TABLE IF EXISTS interview_2nf_order_item;
DROP TABLE IF EXISTS interview_2nf_product;
DROP TABLE IF EXISTS interview_3nf_employee;
DROP TABLE IF EXISTS interview_3nf_department;
DROP TABLE IF EXISTS interview_3nf_emp;
DROP TABLE IF EXISTS interview_3nf_dept;

CREATE TABLE interview_1nf_user (
    user_id BIGINT PRIMARY KEY COMMENT '用户ID',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    province VARCHAR(50) NOT NULL COMMENT '1NF拆分: 省份',
    city VARCHAR(50) NOT NULL COMMENT '1NF拆分: 城市',
    detail_address VARCHAR(100) NOT NULL COMMENT '1NF拆分: 详细门牌地址'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题02: 1NF原子性】演示第一范式：字段原子化拆分，杜绝单一列存储复杂非结构文本';

CREATE TABLE interview_1nf_student (
    student_id INT PRIMARY KEY COMMENT '学号',
    student_name VARCHAR(50) NOT NULL COMMENT '学生姓名',
    province VARCHAR(50) NOT NULL COMMENT '省份',
    city VARCHAR(50) NOT NULL COMMENT '城市',
    detail_address VARCHAR(100) NOT NULL COMMENT '门牌地址'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题02: 1NF验证】学生信息原子表';

CREATE TABLE interview_2nf_product (
    product_id BIGINT PRIMARY KEY COMMENT '商品唯一ID',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    unit_price DECIMAL(10, 2) NOT NULL COMMENT '商品单价'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题02: 2NF完全依赖】商品主表，剥离对订单复合主键的部分依赖';

CREATE TABLE interview_2nf_order_item (
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    quantity INT NOT NULL COMMENT '购买数量 (完全依赖复合主键 order_id + product_id)',
    PRIMARY KEY (order_id, product_id) COMMENT '复合主键'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题02: 2NF完全依赖】订单明细表，消除复合主键的部分依赖';

CREATE TABLE interview_3nf_department (
    dept_id BIGINT PRIMARY KEY COMMENT '部门主键ID',
    dept_name VARCHAR(50) NOT NULL COMMENT '部门名称',
    location VARCHAR(100) NOT NULL COMMENT '办公地点'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题02: 3NF消除传递依赖】部门独立实体表，避免员工表存储部门属性';

CREATE TABLE interview_3nf_employee (
    emp_id BIGINT PRIMARY KEY COMMENT '员工主键ID',
    emp_name VARCHAR(50) NOT NULL COMMENT '员工姓名',
    dept_id BIGINT NOT NULL COMMENT '所属部门ID (通过逻辑外键消除 emp_id -> dept_id -> location 传递依赖)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题02: 3NF消除传递依赖】员工表，仅保留部门外键，彻底消除传递依赖';

CREATE TABLE interview_3nf_dept (
    dept_id INT PRIMARY KEY,
    dept_name VARCHAR(50) NOT NULL,
    dept_location VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='【面试题02】3NF部门表对照';

CREATE TABLE interview_3nf_emp (
    emp_id INT PRIMARY KEY,
    emp_name VARCHAR(50) NOT NULL,
    dept_id INT NOT NULL,
    FOREIGN KEY (dept_id) REFERENCES interview_3nf_dept(dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='【面试题02】3NF员工表对照';

INSERT INTO interview_1nf_user VALUES 
(1, '张三', '北京市', '海淀区', '中关村南大街1号'),
(2, '李四', '上海市', '浦东新区', '陆家嘴环路88号');
INSERT INTO interview_1nf_student VALUES (101, '张三', '北京市', '海淀区', '中关村南大街1号');
INSERT INTO interview_2nf_product VALUES (101, '机械键盘', 299.00), (102, '无线鼠标', 99.00);
INSERT INTO interview_2nf_order_item VALUES (2026001, 101, 1), (2026001, 102, 2);
INSERT INTO interview_3nf_department VALUES (10, '研发中心', '科技大厦A座'), (20, '市场运营', '科技大厦B座');
INSERT INTO interview_3nf_employee VALUES (1001, '王工', 10), (1002, '赵经理', 20);
INSERT INTO interview_3nf_dept VALUES (1, '研发部', '创新大厦A座8F'), (2, '运营部', '创新大厦B座5F');
INSERT INTO interview_3nf_emp VALUES (1, '李工', 1), (2, '王运营', 2);

-- --------------------------------------------------------------------
-- 【面试题 03: MySQL 多表 JOIN 区别与底层执行原理】
-- 核心考点：
-- 1. INNER JOIN：仅返回两表匹配交集；
-- 2. LEFT JOIN：返回左表全部数据，右表未匹配填充 NULL；
-- 3. 小表驱动大表：NLJ（嵌套循环连接）算法中，小表作为 Outer 表能显著减少大表扫描次数。
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS interview_join_employee;
DROP TABLE IF EXISTS interview_join_department;
DROP TABLE IF EXISTS interview_emp;
DROP TABLE IF EXISTS interview_dept;

CREATE TABLE interview_join_department (
    dept_id BIGINT PRIMARY KEY COMMENT '部门ID',
    dept_name VARCHAR(50) NOT NULL COMMENT '部门名称'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题03: 连表查询】部门表，演示 INNER/LEFT JOIN 中无员工部门的 NULL 填充';

CREATE TABLE interview_join_employee (
    emp_id BIGINT PRIMARY KEY COMMENT '员工主键ID',
    emp_name VARCHAR(50) NOT NULL COMMENT '员工姓名',
    dept_id BIGINT NULL COMMENT '部门ID (可为NULL，模拟未分配部门的新员工)',
    salary DECIMAL(10, 2) NOT NULL COMMENT '基本薪资'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题03: 连表查询】员工表，演示部门ID为空时内连接丢失与外连接保留差异';

INSERT INTO interview_join_department VALUES (10, '基础架构部'), (20, '业务中台部'), (30, '前沿创新实验室');
INSERT INTO interview_join_employee VALUES 
(1, '张三', 10, 18000.00), 
(2, '李四', 20, 15000.00), 
(3, '王五', NULL, 12000.00);

CREATE TABLE interview_dept (
    dept_id INT PRIMARY KEY COMMENT '部门ID',
    dept_name VARCHAR(50) NOT NULL COMMENT '部门名称'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='【面试题03/10】部门对照表';

CREATE TABLE interview_emp (
    emp_id INT PRIMARY KEY COMMENT '员工ID',
    emp_name VARCHAR(50) NOT NULL COMMENT '员工姓名',
    dept_id INT NULL COMMENT '所属部门',
    salary DECIMAL(10,2) COMMENT '薪资',
    INDEX idx_dept (dept_id) COMMENT '部门索引，用于加速 IN/EXISTS 子查询匹配'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='【面试题03/10】员工对照表';

INSERT INTO interview_dept (dept_id, dept_name) VALUES 
(1, '技术部'), (2, '运营部'), (3, '空置部门(无员工)');
INSERT INTO interview_emp (emp_id, emp_name, dept_id, salary) VALUES 
(101, '张工', 1, 15000), 
(102, '李工', 1, 18000), 
(103, '赵运营', 2, 12000), 
(104, '实习无部门人员', NULL, 6000);

-- --------------------------------------------------------------------
-- 【面试题 04: MySQL 避免重复插入数据的三种经典方案】
-- 核心考点：
-- 1. 业务唯一键 (UNIQUE KEY) 是防止脏写和并发穿透的唯一可靠底线；
-- 2. INSERT IGNORE：存在则忽略不报错，返回 Affected Rows = 0；
-- 3. REPLACE INTO：存在则底层先 DELETE 触发级联与自增变更，再 INSERT；
-- 4. ON DUPLICATE KEY UPDATE：存在则就地执行 UPDATE 累加，性能最优且无删行副作用。
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS interview_duplicate_user;
DROP TABLE IF EXISTS interview_avoid_dup;

CREATE TABLE interview_duplicate_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    id_card VARCHAR(18) NOT NULL UNIQUE COMMENT '身份证号 (全局唯一约束，并发防重核心阻断点)',
    username VARCHAR(50) NOT NULL COMMENT '用户姓名',
    score INT NOT NULL DEFAULT 0 COMMENT '用户积分 (用于验证 ON DUPLICATE KEY UPDATE 累加)',
    login_count INT NOT NULL DEFAULT 1 COMMENT '登录总次数 (幂等插入更新计数器)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题04: 避免重复插入】演示 INSERT IGNORE、REPLACE INTO、DUPLICATE UPDATE 机制差异';

INSERT INTO interview_duplicate_user (id_card, username, score, login_count) 
VALUES ('110101199003072345', '张三', 80, 1);

CREATE TABLE interview_avoid_dup (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_card VARCHAR(18) NOT NULL UNIQUE COMMENT '唯一索引列',
    user_name VARCHAR(50) NOT NULL,
    visit_count INT DEFAULT 1 COMMENT '访问次数累加'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='【面试题04】防重插入对照表';

INSERT INTO interview_avoid_dup (id_card, user_name, visit_count)
VALUES ('110101199001011234', '张三', 1);

-- --------------------------------------------------------------------
-- 【面试题 05: CHAR 与 VARCHAR 底层机制与空间性能对比】
-- 核心考点：
-- 1. CHAR(N)：固定分配 N 个字符空间，写入不足自动补空格，检索时截断尾部空格，适合定长字符串；
-- 2. VARCHAR(N)：变长存储，外加 1~2 字节存储真实长度前缀，节约磁盘与 Buffer Pool 内存；
-- 3. 频繁变长更新易产生 B+ 树页内碎片与行迁移。
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS interview_char_varchar;

CREATE TABLE interview_char_varchar (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '测试主键ID',
    fixed_code CHAR(5) NOT NULL COMMENT '定长 CHAR(5)，固定分配5字符空间，尾部空格在读取时自动剥离',
    var_code VARCHAR(5) NOT NULL COMMENT '变长 VARCHAR(5)，按需动态分配+1~2字节长度前缀，保留尾部空格'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题05: CHAR与VARCHAR区别】定长 vs 变长存储、尾随空格处理与字符数定义实机对照表';

INSERT INTO interview_char_varchar (fixed_code, var_code) VALUES 
('AB', 'AB'), 
('HELLO', 'HELLO');

-- --------------------------------------------------------------------
-- 【面试题 06: INT(1) 与 INT(10) 深度区别与 ZEROFILL】
-- 核心考点：
-- 1. 存储容量绝对相同：无论括号内是 1 还是 10，物理存储都恒为 4 字节（-2147483648 ~ 2147483647）；
-- 2. 括号数字代表【显示宽度】（Display Width），仅在搭配 ZEROFILL 关键字时自动补零展示；
-- 3. MySQL 8.0.17+ 已正式废弃整数显示宽度规范。
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS interview_int_display;
DROP TABLE IF EXISTS interview_int_width;

CREATE TABLE interview_int_display (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    num_tiny_width INT(1) NOT NULL COMMENT 'INT(1): 存储 4 字节，能存 2147483647',
    num_normal INT NOT NULL COMMENT '标准 INT: 存储 4 字节，数值范围一致',
    num_zerofill INT(5) ZEROFILL NOT NULL COMMENT 'INT(5) ZEROFILL: 显示宽度仅在搭配 ZEROFILL 前导补零时生效'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题06: INT显示宽度】验证 INT(1) 与 INT(10) 物理存储容量完全相同，括号仅代表显示宽度';

INSERT INTO interview_int_display (num_tiny_width, num_normal, num_zerofill) VALUES 
(5, 5, 5), 
(12345, 12345, 12345);

CREATE TABLE interview_int_width (
    id INT AUTO_INCREMENT PRIMARY KEY,
    val_int1 INT(1) NOT NULL,
    val_int10 INT(10) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='【面试题06】宽度对照备用表';

-- --------------------------------------------------------------------
-- 【面试题 07: TEXT 数据类型机制与行溢出存储 (Off-page)】
-- 核心考点：
-- 1. TEXT 包含 TINYTEXT(255B), TEXT(64KB), MEDIUMTEXT(16MB), LONGTEXT(4GB)；
-- 2. TEXT 列不能设置 DEFAULT 默认值，排序操作只能走内存临时表或 filesort 磁盘排序；
-- 3. 行溢出：InnoDB 每页 16KB，单行数据过大时，超过阈值将移至溢出页；
-- 4. 阿里规范：极力建议大 TEXT 字段垂直拆分到扩展子表中，防止 Buffer Pool 缓存被非索引大字段污染。
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS interview_text_storage;
DROP TABLE IF EXISTS interview_text;

CREATE TABLE interview_text_storage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tiny_text TINYTEXT COMMENT 'TINYTEXT: 最大 255 字节',
    regular_text TEXT COMMENT 'TEXT: 最大 64KB，单行超页阈值触发行溢出 (Off-page) 存储',
    medium_text MEDIUMTEXT COMMENT 'MEDIUMTEXT: 最大 16MB',
    long_text LONGTEXT COMMENT 'LONGTEXT: 最大 4GB，受 max_allowed_packet 限制'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题07: TEXT数据类型机制】大文本四层容量边界、行溢出存储与临时表排序性能损耗实测表';

INSERT INTO interview_text_storage (tiny_text, regular_text, medium_text, long_text) VALUES 
('短文本(最大255字节)', '普通文本(最大64KB)', '中型文本(最大16MB)', '超长文本(最大4GB)');

CREATE TABLE interview_text (
    id INT AUTO_INCREMENT PRIMARY KEY,
    short_desc VARCHAR(255),
    full_content TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='【面试题07】TEXT对照备用表';

-- --------------------------------------------------------------------
-- 【面试题 08: IP 地址高效存储：VARCHAR(15) vs INT UNSIGNED】
-- 核心考点：
-- 1. 传统 VARCHAR(15) 占用 7~15 字节，字符串按字典序比对无法高效进行网段掩码过滤；
-- 2. 推荐 INT UNSIGNED：固定仅占 4 字节，节省 75% 磁盘与索引缓存；
-- 3. 配合内置函数 INET_ATON()（字符串转数字）和 INET_NTOA()（数字转字符串）；
-- 4. 整数数字天然支持 BETWEEN 范围查询与网段计算。
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS interview_ip_storage;

CREATE TABLE interview_ip_storage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    ip_string VARCHAR(15) NOT NULL COMMENT '传统点分十进制字符串: 占用 7~15 字节，无法走网段范围索引',
    ip_numeric INT UNSIGNED NOT NULL COMMENT '高效无符号整型: 恒定占用 4 字节，配合 INET_ATON/INET_NTOA 完美支持网段范围查找',
    INDEX idx_ip_numeric (ip_numeric) COMMENT '整型 IP 索引，支持高效范围查找'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题08: IP地址存储选型】验证 INT UNSIGNED 节省 75% 空间与网段掩码范围检索的高效性';

INSERT INTO interview_ip_storage (ip_string, ip_numeric) VALUES
('192.168.1.1', INET_ATON('192.168.1.1')),
('10.0.0.254',  INET_ATON('10.0.0.254')),
('127.0.0.1',   INET_ATON('127.0.0.1'));

-- --------------------------------------------------------------------
-- 【面试题 09: 物理外键约束、级联删除与阿里禁止外键规范】
-- 核心考点：
-- 1. 外键（FOREIGN KEY）用于保证主子表引用的强一致性，支持 CASCADE/SET NULL/RESTRICT；
-- 2. 为什么阿里规范【强制】禁止物理外键：
--    - 插入子表必须对父表对应行加 S 锁（共享锁），极易导致大促高并发死锁；
--    - DML 写入 TPS 暴降 30%~50%，每次操作均触发隐式联检开销；
--    - 分库分表微服务场景下彻底失效；
-- 3. 生产实践：一律使用“逻辑外键”在应用层 Service 事务内保证一致性。
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS interview_fk_student;
DROP TABLE IF EXISTS interview_fk_college;

CREATE TABLE interview_fk_college (
    college_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '学院主键ID (父表)',
    college_name VARCHAR(50) NOT NULL COMMENT '学院名称'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题09: 外键约束】父表学院表，演示被子表外键引用及级联删除';

CREATE TABLE interview_fk_student (
    student_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '学生主键ID (子表)',
    student_name VARCHAR(50) NOT NULL COMMENT '学生姓名',
    college_id INT NOT NULL COMMENT '所属学院ID (物理外键引用父表主键)',
    CONSTRAINT fk_student_college FOREIGN KEY (college_id) REFERENCES interview_fk_college(college_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题09: 级联删除】子表学生表，带 ON DELETE CASCADE 级联删除约束';

INSERT INTO interview_fk_college (college_id, college_name) VALUES (1, '计算机学院'), (2, '软件学院');
INSERT INTO interview_fk_student (student_id, student_name, college_id) VALUES (1, '张三', 1), (2, '李四', 1), (3, '王五', 2);

-- --------------------------------------------------------------------
-- 【面试题 12: SQL 查询逻辑执行顺序 10 步法】
-- 核心考点：
-- 1. 书写顺序 vs 逻辑执行顺序差异：
--    FROM -> ON -> JOIN -> WHERE -> GROUP BY -> HAVING -> SELECT -> DISTINCT -> ORDER BY -> LIMIT；
-- 2. 为什么 WHERE 不能用 SELECT 别名？因为 WHERE 在 SELECT 投影之前执行，别名尚未诞生；
-- 3. 为什么 ORDER BY 可以用别名？因为 ORDER BY 在 SELECT 之后执行，别名已经生效；
-- 4. 为什么 WHERE 不能写聚合函数 SUM()？因为 WHERE 发生于 GROUP BY 之前，尚未完成聚合运算。
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS interview_exec_order_sales;
DROP TABLE IF EXISTS interview_query_order;

CREATE TABLE interview_exec_order_sales (
    sale_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '销售记录ID',
    dept_name VARCHAR(50) NOT NULL COMMENT '销售大区/部门 (用于 GROUP BY)',
    salesperson VARCHAR(50) NOT NULL COMMENT '业务员姓名',
    amount DECIMAL(10, 2) NOT NULL COMMENT '单笔成单金额 (用于 SUM(amount) 聚合与 HAVING 过滤)',
    status VARCHAR(20) NOT NULL COMMENT '订单状态 (用于 WHERE status=PAID 过滤)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题12: SQL执行顺序】销售明细表，全面验证 FROM->WHERE->GROUP BY->HAVING->SELECT->ORDER BY 流程';

INSERT INTO interview_exec_order_sales (dept_name, salesperson, amount, status) VALUES 
('华东区', '张伟', 1200.00, 'PAID'), 
('华东区', '张伟', 800.00,  'PAID'), 
('华东区', '李芳', 500.00,  'REFUNDED'), 
('华北区', '王刚', 3000.00, 'PAID'), 
('华北区', '王刚', 2500.00, 'PAID'), 
('华南区', '赵强', 400.00,  'PAID');

CREATE TABLE interview_query_order (
    id INT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(20) NOT NULL,
    product_name VARCHAR(50) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    status INT NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='【面试题12】执行顺序备选测试表';

-- --------------------------------------------------------------------
-- 【面试题 13: 经典 SQL 题：求不存在 01 课程但存在 02 课程的学生成绩】
-- 核心考点：
-- 1. 存在性与排除性子查询的复合使用；
-- 2. 解法 1 (EXISTS / NOT EXISTS)：走 (student_id, course_id) 联合索引，短路求值，性能最优；
-- 3. 解法 2 (IN / NOT IN)：直观易懂，但须注意 NOT IN 遇 NULL 陷阱；
-- 4. 解法 3 (GROUP BY + HAVING 条件聚合)：行转列技巧。
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS interview_course_score;
DROP TABLE IF EXISTS interview_student;

CREATE TABLE interview_student (
    student_id VARCHAR(20) PRIMARY KEY COMMENT '学号 (如: s01, s02)',
    student_name VARCHAR(50) NOT NULL COMMENT '学生姓名'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题13: 选课过滤】学生基础表，支持字母编号学号';

CREATE TABLE interview_course_score (
    student_id VARCHAR(20) NOT NULL COMMENT '学号',
    course_id VARCHAR(20) NOT NULL COMMENT '课程号 (如: 01, 02, 03)',
    score DECIMAL(5, 2) NOT NULL COMMENT '考分',
    PRIMARY KEY (student_id, course_id) COMMENT '复合主键：同一学生同门课唯一',
    INDEX idx_course (course_id) COMMENT '课程号索引，加速单科查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题13: 选课过滤】学生选课成绩表，演示 EXISTS/NOT EXISTS 联合主键过滤';

INSERT INTO interview_student (student_id, student_name) VALUES 
('s01', '张三 (选了01, 02)'), 
('s02', '李四 (选了02, 03 -> 目标命中)'), 
('s03', '王五 (选了01, 03)'), 
('s04', '赵六 (仅选02 -> 目标命中)'), 
('s05', '孙七 (仅选03)');

INSERT INTO interview_course_score (student_id, course_id, score) VALUES 
('s01', '01', 88.0), 
('s01', '02', 90.0), 
('s02', '02', 85.5), 
('s02', '03', 92.0), 
('s03', '01', 76.0), 
('s03', '03', 81.0), 
('s04', '02', 95.0), 
('s05', '03', 89.0);

-- --------------------------------------------------------------------
-- 【面试题 14: 经典 SQL 题：查询总分排名在 5-10 名的学生 ID 与总分】
-- 核心考点：
-- 1. 传统分页 LIMIT 偏移法：`ORDER BY total_score DESC LIMIT 4, 6`（跳过前 4 名，获取随后 6 条）；
-- 2. MySQL 8.0+ 窗口函数法：`DENSE_RANK() OVER (ORDER BY total_score DESC)`，优雅处理并列同分。
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS student_score;
DROP TABLE IF EXISTS interview_student_score;

CREATE TABLE student_score (
    stu_id INT NOT NULL COMMENT '学生ID',
    subject_id INT NOT NULL COMMENT '科目ID',
    score DECIMAL(5, 2) NOT NULL COMMENT '科目分数',
    PRIMARY KEY (stu_id, subject_id) COMMENT '学生与科目复合主键'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题14: 成绩排名前5-10】演示 LIMIT 4, 6 偏移与窗口函数 DENSE_RANK() 综合查询';

INSERT INTO student_score (stu_id, subject_id, score) VALUES 
(101, 1, 98), (101, 2, 99), 
(102, 1, 95), (102, 2, 96), 
(103, 1, 92), (103, 2, 94), 
(104, 1, 90), (104, 2, 91), 
(105, 1, 88), (105, 2, 89), 
(106, 1, 85), (106, 2, 86), 
(107, 1, 82), (107, 2, 83), 
(108, 1, 80), (108, 2, 80), 
(109, 1, 78), (109, 2, 77), 
(110, 1, 75), (110, 2, 74), 
(111, 1, 70), (111, 2, 70), 
(112, 1, 60), (112, 2, 65);

CREATE TABLE interview_student_score (
    id INT AUTO_INCREMENT PRIMARY KEY,
    stu_id INT NOT NULL,
    subject_id VARCHAR(20) NOT NULL,
    score DECIMAL(5,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='【面试题14】成绩备份表';

-- --------------------------------------------------------------------
-- 【面试题 15: 经典 SQL 题：查指定班级下“所有学生”的选课情况】
-- 核心考点：
-- 1. 为什么必须用 LEFT JOIN？若使用 INNER JOIN，未选课的学生会被内连接直接过滤丢失！
-- 2. 报表展示：GROUP_CONCAT 合并课程名称，COUNT(co.course_id) 准确计算门数；
-- 3. 得分陷阱：COUNT(co.course_id) 绝不能写成 COUNT(*)。
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS interview_student_course;
DROP TABLE IF EXISTS interview_course;
DROP TABLE IF EXISTS interview_cls_student;
DROP TABLE IF EXISTS interview_class;

CREATE TABLE interview_class (
    class_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '班级ID',
    class_name VARCHAR(50) NOT NULL COMMENT '班级名称'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题15: 班级学生选课全覆盖】班级实体表';

CREATE TABLE interview_cls_student (
    student_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '学生ID',
    student_name VARCHAR(50) NOT NULL COMMENT '学生姓名',
    class_id INT NOT NULL COMMENT '所属班级ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题15: 班级学生选课全覆盖】学生主表，演示左连接基表完整保留';

CREATE TABLE interview_course (
    course_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '课程ID',
    course_name VARCHAR(50) NOT NULL COMMENT '课程名称'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题15: 班级学生选课全覆盖】课程基础字典表';

CREATE TABLE interview_student_course (
    student_id INT NOT NULL COMMENT '学生ID',
    course_id INT NOT NULL COMMENT '课程ID',
    PRIMARY KEY (student_id, course_id) COMMENT '选课记录复合主键'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题15: 班级学生选课全覆盖】学生选课关系表';

INSERT INTO interview_class (class_id, class_name) VALUES (1, '高三(1)班'), (2, '高三(2)班');
INSERT INTO interview_cls_student (student_id, student_name, class_id) VALUES 
(1, '张三', 1), (2, '李四', 1), (3, '王五 (未选任何课程)', 1), (4, '赵六 (2班学生)', 2);
INSERT INTO interview_course (course_id, course_name) VALUES (101, '高等数学'), (102, '大学物理'), (103, '大学英语');
INSERT INTO interview_student_course (student_id, course_id) VALUES (1, 101), (1, 102), (2, 103);

-- --------------------------------------------------------------------
-- 【面试题 16: 基于 MySQL 唯一索引实现的高可用可重入分布式锁】
-- 核心考点：
-- 1. 互斥性：`PRIMARY KEY (lock_name)` 保证高并发抢锁时仅单个实例/线程 INSERT 成功；
-- 2. 可重入性：维护 `lock_owner` 实例与线程唯一标识，持有者重入递增 `reentrant_count`；
-- 3. 释放安全性：只有 owner 匹配才能扣减计数，计数归零时执行 DELETE；
-- 4. 防死锁容灾：维护 `expire_time` 毫秒时间戳，崩溃宕机后超时允许 CAS 重新抢占。
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS mysql_reentrant_lock;
DROP TABLE IF EXISTS interview_distributed_lock;

CREATE TABLE mysql_reentrant_lock (
    lock_name VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '分布式锁资源名 (主键唯一约束，互斥竞争核心基石)',
    lock_owner VARCHAR(100) NOT NULL COMMENT '锁持有者全局唯一标识 (UUID + ThreadId，防误解锁)',
    reentrant_count INT NOT NULL DEFAULT 1 COMMENT '重入计数器 (同线程重入+1，释放-1，归零删除)',
    expire_time BIGINT NOT NULL COMMENT '锁超时绝对时间戳(毫秒) (防止服务宕机产生死锁，支持主动CAS抢占)',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新心跳时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 
COMMENT='【面试题16: MySQL可重入分布式锁】利用主键互斥性、owner与reentrant_count实现的分布式锁表';

CREATE TABLE interview_distributed_lock (
    lock_key VARCHAR(64) NOT NULL PRIMARY KEY,
    owner_id VARCHAR(128) NOT NULL,
    reentrant_count INT NOT NULL DEFAULT 1,
    expire_at DATETIME NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='【面试题16】分布式锁对照备用表';

SET FOREIGN_KEY_CHECKS = 1;
-- 01_mysql_basics_schema.sql 初始化完毕！


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



SET FOREIGN_KEY_CHECKS = 1;
-- 全量数据表创建、中文考点注释与初始种子数据加载完毕！

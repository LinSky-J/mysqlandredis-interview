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

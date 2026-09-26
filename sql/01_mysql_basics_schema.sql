-- ====================================================================
-- 01_mysql_basics_schema.sql
-- MySQL 基础模块数据表结构定义与初始数据
-- 由 DataGrip 统一执行创建，供 Java 面试题演示类调用
-- ====================================================================

USE interview_db;

-- ----------------------------------------------------
-- Topic 01: SQL vs NoSQL 实体与外键关联表
-- ----------------------------------------------------
DROP TABLE IF EXISTS interview_sql_order;
DROP TABLE IF EXISTS interview_sql_user;
DROP TABLE IF EXISTS interview_orders;
DROP TABLE IF EXISTS interview_users;

CREATE TABLE interview_sql_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL,
    balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_sql_order (
    order_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_sql_user (username, email, balance) VALUES 
('zhangsan', 'zhangsan@example.com', 1000.00),
('lisi', 'lisi@example.com', 2500.50);

INSERT INTO interview_sql_order (user_id, amount, status) VALUES 
(1, 299.00, 'PAID'),
(1, 499.00, 'PAID'),
(2, 88.00, 'PENDING');

CREATE TABLE interview_users (
    user_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '用户主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户姓名',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱（强一致性约束）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '订单ID',
    user_id INT NOT NULL COMMENT '所属用户ID',
    total_amount DECIMAL(10, 2) NOT NULL COMMENT '订单金额',
    FOREIGN KEY (user_id) REFERENCES interview_users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_users (username, email) VALUES 
('Alice', 'alice@example.com'),
('Bob', 'bob@example.com');

INSERT INTO interview_orders (user_id, total_amount) VALUES 
(1, 99.90),
(1, 199.50),
(2, 50.00);

-- ----------------------------------------------------
-- Topic 02: 数据库三大范式演示表 (1NF, 2NF, 3NF)
-- ----------------------------------------------------
DROP TABLE IF EXISTS interview_1nf_user;
DROP TABLE IF EXISTS interview_1nf_student;
DROP TABLE IF EXISTS interview_2nf_order_item;
DROP TABLE IF EXISTS interview_2nf_product;
DROP TABLE IF EXISTS interview_3nf_employee;
DROP TABLE IF EXISTS interview_3nf_department;
DROP TABLE IF EXISTS interview_3nf_emp;
DROP TABLE IF EXISTS interview_3nf_dept;

CREATE TABLE interview_1nf_user (
    user_id BIGINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    province VARCHAR(50) NOT NULL,
    city VARCHAR(50) NOT NULL,
    detail_address VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_1nf_student (
    student_id INT PRIMARY KEY,
    student_name VARCHAR(50) NOT NULL,
    province VARCHAR(50) NOT NULL,
    city VARCHAR(50) NOT NULL,
    detail_address VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_2nf_product (
    product_id BIGINT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_2nf_order_item (
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (order_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_3nf_department (
    dept_id BIGINT PRIMARY KEY,
    dept_name VARCHAR(50) NOT NULL,
    location VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_3nf_employee (
    emp_id BIGINT PRIMARY KEY,
    emp_name VARCHAR(50) NOT NULL,
    dept_id BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_3nf_dept (
    dept_id INT PRIMARY KEY,
    dept_name VARCHAR(50) NOT NULL,
    dept_location VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_3nf_emp (
    emp_id INT PRIMARY KEY,
    emp_name VARCHAR(50) NOT NULL,
    dept_id INT NOT NULL,
    FOREIGN KEY (dept_id) REFERENCES interview_3nf_dept(dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

-- ----------------------------------------------------
-- Topic 03: 连表查询
-- ----------------------------------------------------
DROP TABLE IF EXISTS interview_join_employee;
DROP TABLE IF EXISTS interview_join_department;
DROP TABLE IF EXISTS interview_emp;
DROP TABLE IF EXISTS interview_dept;

CREATE TABLE interview_join_department (
    dept_id BIGINT PRIMARY KEY,
    dept_name VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_join_employee (
    emp_id BIGINT PRIMARY KEY,
    emp_name VARCHAR(50) NOT NULL,
    dept_id BIGINT NULL,
    salary DECIMAL(10, 2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_join_department VALUES (10, '基础架构部'), (20, '业务中台部'), (30, '前沿创新实验室');
INSERT INTO interview_join_employee VALUES (1, '张三', 10, 18000.00), (2, '李四', 20, 15000.00), (3, '王五', NULL, 12000.00);

CREATE TABLE interview_dept (
    dept_id INT PRIMARY KEY,
    dept_name VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_emp (
    emp_id INT PRIMARY KEY,
    emp_name VARCHAR(50) NOT NULL,
    dept_id INT,
    salary DECIMAL(10,2)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_dept (dept_id, dept_name) VALUES (10, '研发部'), (20, '市场部'), (30, '财务部'), (40, '无人部门');
INSERT INTO interview_emp (emp_id, emp_name, dept_id, salary) VALUES (1, '张三', 10, 15000), (2, '李四', 10, 18000), (3, '王五', 20, 12000), (4, '赵六', NULL, 9000);

-- ----------------------------------------------------
-- Topic 04: 避免重复插入数据
-- ----------------------------------------------------
DROP TABLE IF EXISTS interview_duplicate_user;
DROP TABLE IF EXISTS interview_avoid_dup;

CREATE TABLE interview_duplicate_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    id_card VARCHAR(18) NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL,
    score INT NOT NULL DEFAULT 0,
    login_count INT NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_duplicate_user (id_card, username, score, login_count) 
VALUES ('110101199003072345', '张三', 80, 1);

CREATE TABLE interview_avoid_dup (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_card VARCHAR(18) NOT NULL UNIQUE,
    user_name VARCHAR(50) NOT NULL,
    score INT NOT NULL DEFAULT 0,
    update_count INT NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_avoid_dup (id_card, user_name, score, update_count) 
VALUES ('110101199003072345', '张三', 80, 1);

-- ----------------------------------------------------
-- Topic 05: CHAR vs VARCHAR
-- ----------------------------------------------------
DROP TABLE IF EXISTS interview_char_varchar;

CREATE TABLE interview_char_varchar (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fixed_code CHAR(5) NOT NULL,
    var_code VARCHAR(5) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_char_varchar (fixed_code, var_code) VALUES ('AB', 'AB'), ('HELLO', 'HELLO');

-- ----------------------------------------------------
-- Topic 06: INT(1) vs INT(10)
-- ----------------------------------------------------
DROP TABLE IF EXISTS interview_int_display;
DROP TABLE IF EXISTS interview_int_width;

CREATE TABLE interview_int_display (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    num_tiny_width INT(1) NOT NULL,
    num_normal INT NOT NULL,
    num_zerofill INT(5) ZEROFILL NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_int_display (num_tiny_width, num_normal, num_zerofill) VALUES (5, 5, 5), (12345, 12345, 12345);

CREATE TABLE interview_int_width (
    id INT AUTO_INCREMENT PRIMARY KEY,
    num_default INT,
    num_tiny_width INT(1),
    num_zerofill INT(4) ZEROFILL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_int_width (num_default, num_tiny_width, num_zerofill) VALUES (7, 7, 7), (123456, 123456, 123456);

-- ----------------------------------------------------
-- Topic 07: TEXT 数据类型
-- ----------------------------------------------------
DROP TABLE IF EXISTS interview_text_storage;
DROP TABLE IF EXISTS interview_text;

CREATE TABLE interview_text_storage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tiny_text TINYTEXT,
    regular_text TEXT,
    medium_text MEDIUMTEXT,
    long_text LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_text_storage (tiny_text, regular_text, medium_text, long_text) 
VALUES ('短文本(最大255字节)', '普通文本(最大64KB)', '中型文本(最大16MB)', '超长文本(最大4GB)');

CREATE TABLE interview_text (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100),
    short_desc TINYTEXT,
    content TEXT,
    big_content MEDIUMTEXT,
    huge_content LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_text (title, short_desc, content) VALUES
('MySQL TEXT 类型解析', '概要描述', 'TEXT 字段超过 768 字节时会采用行溢出页 (Off-page) 存储。');

-- ----------------------------------------------------
-- Topic 08: IP 地址存储
-- ----------------------------------------------------
DROP TABLE IF EXISTS interview_ip_storage;

CREATE TABLE interview_ip_storage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ip_string VARCHAR(15) NOT NULL,
    ip_numeric INT UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_ip_storage (ip_string, ip_numeric) VALUES
('192.168.1.1', INET_ATON('192.168.1.1')),
('10.0.0.254', INET_ATON('10.0.0.254')),
('127.0.0.1', INET_ATON('127.0.0.1'));

-- ----------------------------------------------------
-- Topic 09: 外键约束
-- ----------------------------------------------------
DROP TABLE IF EXISTS interview_fk_student;
DROP TABLE IF EXISTS interview_fk_college;

CREATE TABLE interview_fk_college (
    college_id INT PRIMARY KEY AUTO_INCREMENT,
    college_name VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_fk_student (
    student_id INT PRIMARY KEY AUTO_INCREMENT,
    student_name VARCHAR(50) NOT NULL,
    college_id INT NOT NULL,
    CONSTRAINT fk_student_college FOREIGN KEY (college_id) REFERENCES interview_fk_college(college_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_fk_college (college_id, college_name) VALUES (1, '计算机学院'), (2, '软件学院');
INSERT INTO interview_fk_student (student_id, student_name, college_id) VALUES (1, '张三', 1), (2, '李四', 1), (3, '王五', 2);

-- ----------------------------------------------------
-- Topic 12: SQL 执行顺序演示表
-- ----------------------------------------------------
DROP TABLE IF EXISTS interview_exec_order_sales;
DROP TABLE IF EXISTS interview_query_order;

CREATE TABLE interview_exec_order_sales (
    sale_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dept_name VARCHAR(50) NOT NULL,
    salesperson VARCHAR(50) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_exec_order_sales (dept_name, salesperson, amount, status) VALUES
('华北区', '张三', 5000.00, 'PAID'),
('华北区', '李四', 8000.00, 'PAID'),
('华北区', '王五', 2000.00, 'CANCELLED'),
('华东区', '赵六', 12000.00, 'PAID'),
('华东区', '孙七', 3000.00, 'PAID'),
('华南区', '周八', 1000.00, 'PAID');

CREATE TABLE interview_query_order (
    id INT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(20) NOT NULL,
    product_name VARCHAR(50) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    status INT NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_query_order (category, product_name, price, status) VALUES
('数码', '手机', 4999.00, 1), ('数码', '平板', 3299.00, 1), ('数码', '耳机', 899.00, 1),
('数码', '下架手表', 1299.00, 0), ('图书', 'Java核心技术', 129.00, 1), ('图书', 'MySQL技术内幕', 99.00, 1),
('图书', 'Redis设计与实现', 79.00, 1), ('服装', 'T恤', 99.00, 1);

-- ----------------------------------------------------
-- Topic 13: 选课过滤 SQL 题
-- ----------------------------------------------------
-- Topic 13: 不存在01课程但存在02课程的学生成绩
-- ----------------------------------------------------
DROP TABLE IF EXISTS interview_course_score;
DROP TABLE IF EXISTS interview_student;

CREATE TABLE interview_student (
    student_id VARCHAR(20) PRIMARY KEY,
    student_name VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_course_score (
    student_id VARCHAR(20) NOT NULL,
    course_id VARCHAR(20) NOT NULL,
    score DECIMAL(5, 2) NOT NULL,
    PRIMARY KEY (student_id, course_id),
    INDEX idx_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

-- ----------------------------------------------------
-- Topic 14: 成绩排名前 5-10
-- ----------------------------------------------------
DROP TABLE IF EXISTS student_score;
DROP TABLE IF EXISTS interview_student_score;

CREATE TABLE student_score (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stu_id BIGINT NOT NULL,
    subject_id VARCHAR(32) NOT NULL,
    score DECIMAL(5, 2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO student_score (stu_id, subject_id, score) VALUES
(1, '语文', 95), (1, '数学', 98),
(2, '语文', 92), (2, '数学', 96),
(3, '语文', 90), (3, '数学', 94),
(4, '语文', 88), (4, '数学', 92),
(5, '语文', 85), (5, '数学', 90),
(6, '语文', 82), (6, '数学', 88),
(7, '语文', 80), (7, '数学', 85),
(8, '语文', 78), (8, '数学', 82),
(9, '语文', 75), (9, '数学', 80),
(10, '语文', 72), (10, '数学', 78),
(11, '语文', 70), (11, '数学', 75),
(12, '语文', 68), (12, '数学', 72);

CREATE TABLE interview_student_score (
    id INT AUTO_INCREMENT PRIMARY KEY,
    stu_id INT NOT NULL,
    subject_id VARCHAR(20) NOT NULL,
    score DECIMAL(5,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_student_score (stu_id, subject_id, score) VALUES
(101, '语文', 95), (101, '数学', 98), (101, '英语', 92),
(102, '语文', 90), (102, '数学', 96), (102, '英语', 91),
(103, '语文', 88), (103, '数学', 92), (103, '英语', 90),
(104, '语文', 85), (104, '数学', 90), (104, '英语', 88),
(105, '语文', 82), (105, '数学', 88), (105, '英语', 85),
(106, '语文', 80), (106, '数学', 85), (106, '英语', 82),
(107, '语文', 78), (107, '数学', 82), (107, '英语', 80),
(108, '语文', 75), (108, '数学', 80), (108, '英语', 78),
(109, '语文', 72), (109, '数学', 78), (109, '英语', 75),
(110, '语文', 70), (110, '数学', 75), (110, '英语', 72);

-- ----------------------------------------------------
-- Topic 15: 班级学生选课全覆盖
-- ----------------------------------------------------
DROP TABLE IF EXISTS interview_student_course;
DROP TABLE IF EXISTS interview_course;
DROP TABLE IF EXISTS interview_cls_student;
DROP TABLE IF EXISTS interview_class;

CREATE TABLE interview_class (
    class_id INT PRIMARY KEY AUTO_INCREMENT,
    class_name VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_cls_student (
    student_id INT PRIMARY KEY AUTO_INCREMENT,
    student_name VARCHAR(50) NOT NULL,
    class_id INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_course (
    course_id INT PRIMARY KEY AUTO_INCREMENT,
    course_name VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_student_course (
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    PRIMARY KEY (student_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO interview_class (class_id, class_name) VALUES (1, '高三(1)班'), (2, '高三(2)班');
INSERT INTO interview_cls_student (student_id, student_name, class_id) VALUES 
(1, '张三', 1), (2, '李四', 1), (3, '王五 (未选任何课程)', 1), (4, '赵六 (2班学生)', 2);
INSERT INTO interview_course (course_id, course_name) VALUES (101, '高等数学'), (102, '大学物理'), (103, '大学英语');
INSERT INTO interview_student_course (student_id, course_id) VALUES (1, 101), (1, 102), (2, 103);

-- ----------------------------------------------------
-- Topic 16: MySQL 分布式锁表
-- ----------------------------------------------------
DROP TABLE IF EXISTS mysql_reentrant_lock;
DROP TABLE IF EXISTS interview_distributed_lock;

CREATE TABLE mysql_reentrant_lock (
    lock_name VARCHAR(64) NOT NULL PRIMARY KEY,
    lock_owner VARCHAR(100) NOT NULL,
    reentrant_count INT NOT NULL DEFAULT 1,
    expire_time BIGINT NOT NULL,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_distributed_lock (
    lock_key VARCHAR(64) NOT NULL PRIMARY KEY,
    owner_id VARCHAR(128) NOT NULL,
    reentrant_count INT NOT NULL DEFAULT 1,
    expire_at DATETIME NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

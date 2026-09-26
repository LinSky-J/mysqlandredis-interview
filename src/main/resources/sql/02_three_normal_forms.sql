-- ====================================================================
-- 问题 02: 数据库三大范式是什么？
-- ====================================================================
-- 规范化理论旨在消除数据冗余、避免插入/更新/删除异常。
-- ====================================================================

-- --------------------------------------------------------------------
-- 1. 第一范式 (1NF, First Normal Form):
-- 定义: 字段具有原子性，表中的每一列都是不可分割的最小数据单元。
-- --------------------------------------------------------------------
-- 违背 1NF 的典型反例 (联系方式在一个字段内逗号分隔，地址未拆分):
-- CREATE TABLE bad_user (
--     id INT PRIMARY KEY,
--     contacts VARCHAR(100), -- 存了 "13800138000, 010-88888888, test@qq.com" (非原子，难以按手机号索引查询)
--     address VARCHAR(200)   -- 存了 "北京市海淀区中关村南大街1号" (难以按省市区聚合统计)
-- );

-- 满足 1NF 的表设计:
DROP TABLE IF EXISTS interview_1nf_user;
CREATE TABLE interview_1nf_user (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    mobile VARCHAR(20) NOT NULL COMMENT '独立手机号',
    email VARCHAR(100) COMMENT '独立邮箱',
    province VARCHAR(50) NOT NULL COMMENT '省份',
    city VARCHAR(50) NOT NULL COMMENT '城市',
    district VARCHAR(50) NOT NULL COMMENT '区县',
    detail_address VARCHAR(200) NOT NULL COMMENT '详细门牌地址'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='满足第一范式: 列原子化不可再分';

-- --------------------------------------------------------------------
-- 2. 第二范式 (2NF, Second Normal Form):
-- 定义: 在 1NF 基础上，消除非主属性对主键的【部分函数依赖】。
--      如果表的主键是复合主键 (由多个列组成)，那么所有非主键列必须完全依赖于【整个复合主键】，
--      而不能只依赖复合主键中的某一部分列。如果表是单列主键，则天然满足 2NF。
-- --------------------------------------------------------------------
-- 违背 2NF 的典型反例 (复合主键 (order_id, product_id)):
-- CREATE TABLE bad_order_item (
--     order_id BIGINT,
--     product_id BIGINT,
--     quantity INT NOT NULL,
--     product_name VARCHAR(100),  -- 仅依赖 product_id，不依赖 order_id！(部分依赖)
--     product_price DECIMAL(10,2),-- 仅依赖 product_id！(部分依赖)
--     PRIMARY KEY (order_id, product_id)
-- );

-- 满足 2NF 的规范化拆分 (拆成商品表 + 订单明细表):
DROP TABLE IF EXISTS interview_2nf_product;
CREATE TABLE interview_2nf_product (
    product_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID (完全主键)',
    product_name VARCHAR(100) NOT NULL,
    product_price DECIMAL(10, 2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表: 独立存储商品固有属性';

DROP TABLE IF EXISTS interview_2nf_order_item;
CREATE TABLE interview_2nf_order_item (
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    quantity INT NOT NULL COMMENT '购买数量 (完全依赖于 order_id + product_id 两个维度)',
    PRIMARY KEY (order_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表: 满足 2NF，无部分依赖';

-- --------------------------------------------------------------------
-- 3. 第三范式 (3NF, Third Normal Form):
-- 定义: 在 2NF 基础上，消除非主属性对主键的【传递函数依赖】。
--      即任何非主属性列不能依赖于其它非主属性列 (A -> B, B -> C, 则 A -> C 为传递依赖)。
-- --------------------------------------------------------------------
-- 违背 3NF 的典型反例:
-- CREATE TABLE bad_employee (
--     emp_id BIGINT PRIMARY KEY,
--     emp_name VARCHAR(50),
--     dept_id BIGINT,
--     dept_name VARCHAR(50),      -- emp_id -> dept_id -> dept_name (传递依赖)
--     dept_manager VARCHAR(50)    -- emp_id -> dept_id -> dept_manager (传递依赖)
-- );

-- 满足 3NF 的规范化拆分 (将部门固有属性抽离为独立的部门表):
DROP TABLE IF EXISTS interview_3nf_department;
CREATE TABLE interview_3nf_department (
    dept_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '部门ID',
    dept_name VARCHAR(50) NOT NULL COMMENT '部门名称',
    dept_manager VARCHAR(50) NOT NULL COMMENT '部门负责人'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表: 满足 3NF 独立维护';

DROP TABLE IF EXISTS interview_3nf_employee;
CREATE TABLE interview_3nf_employee (
    emp_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '员工ID',
    emp_name VARCHAR(50) NOT NULL COMMENT '员工姓名',
    dept_id BIGINT NOT NULL COMMENT '部门外键 (消除传递依赖)',
    INDEX idx_dept (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表: 消除传递依赖，仅存外键引用';

-- --------------------------------------------------------------------
-- 4. 工业界权衡: 反范式化设计 (Denormalization)
-- 为什么互联网大厂常在订单表故意冗余 product_name, user_nickname?
-- - 读多写少、追求极限 QPS
-- - 避免高并发时庞大的 JOIN 联表开销
-- - 业务快照需求 (历史订单记录发生时的商品名称和价格，不随后续商品改名而改变)
-- --------------------------------------------------------------------

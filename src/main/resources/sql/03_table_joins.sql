-- ====================================================================
-- 问题 03: MySQL 怎么连表查询？
-- ====================================================================
-- 涵盖:
-- 1. INNER JOIN (内连接)
-- 2. LEFT JOIN (左外连接)
-- 3. RIGHT JOIN (右外连接)
-- 4. FULL OUTER JOIN (全外连接在 MySQL 中的 UNION 模拟方案)
-- 5. CROSS JOIN (交叉连接/笛卡尔积)
-- 6. Self JOIN (自连接)
-- 7. 底层 Join 算法 (INLJ, BNLJ, Hash Join) 与“小表驱动大表”优化
-- ====================================================================

-- 准备演示表: 员工表 (部分员工暂无部门)
DROP TABLE IF EXISTS interview_join_employee;
CREATE TABLE interview_join_employee (
    emp_id INT PRIMARY KEY AUTO_INCREMENT,
    emp_name VARCHAR(50) NOT NULL,
    dept_id INT NULL,
    manager_id INT NULL COMMENT '直接上级员工ID (用于自连接演示)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 准备演示表: 部门表 (部分部门暂无员工)
DROP TABLE IF EXISTS interview_join_department;
CREATE TABLE interview_join_department (
    dept_id INT PRIMARY KEY AUTO_INCREMENT,
    dept_name VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入演示数据
INSERT INTO interview_join_department (dept_id, dept_name) VALUES
(10, '研发中心'),
(20, '产品设计部'),
(30, '市场营销部'),
(40, '战略预研部(暂无员工)');

INSERT INTO interview_join_employee (emp_id, emp_name, dept_id, manager_id) VALUES
(1, '张总监', 10, NULL),
(2, '李架构师', 10, 1),
(3, '王前端', 10, 2),
(4, '赵产品经理', 20, 1),
(5, '孙实习生(未分配部门)', NULL, 2);

-- --------------------------------------------------------------------
-- 1. INNER JOIN (内连接): 仅返回两表均满足匹配条件的交集记录
-- --------------------------------------------------------------------
SELECT 
    e.emp_id, e.emp_name, d.dept_name
FROM interview_join_employee e
INNER JOIN interview_join_department d ON e.dept_id = d.dept_id;

-- --------------------------------------------------------------------
-- 2. LEFT JOIN (左外连接): 以左表为基础，左表全部保留，右表无匹配补 NULL
-- --------------------------------------------------------------------
SELECT 
    e.emp_id, e.emp_name, COALESCE(d.dept_name, '【未分配部门】') AS dept_name
FROM interview_join_employee e
LEFT JOIN interview_join_department d ON e.dept_id = d.dept_id;

-- --------------------------------------------------------------------
-- 3. RIGHT JOIN (右外连接): 以右表为基础，右表全部保留，左表无匹配补 NULL
-- --------------------------------------------------------------------
SELECT 
    COALESCE(e.emp_name, '【该部门尚无在职员工】') AS emp_name,
    d.dept_id,
    d.dept_name
FROM interview_join_employee e
RIGHT JOIN interview_join_department d ON e.dept_id = d.dept_id;

-- --------------------------------------------------------------------
-- 4. FULL OUTER JOIN (全外连接): MySQL 不原生支持，采用 LEFT JOIN UNION RIGHT JOIN 模拟
-- --------------------------------------------------------------------
SELECT e.emp_id, e.emp_name, d.dept_name
FROM interview_join_employee e
LEFT JOIN interview_join_department d ON e.dept_id = d.dept_id
UNION
SELECT e.emp_id, e.emp_name, d.dept_name
FROM interview_join_employee e
RIGHT JOIN interview_join_department d ON e.dept_id = d.dept_id;

-- --------------------------------------------------------------------
-- 5. Self JOIN (自连接): 同一张表与自身做连接 (例如查询员工的直属领导姓名)
-- --------------------------------------------------------------------
SELECT 
    e.emp_id AS 员工编号,
    e.emp_name AS 员工姓名,
    COALESCE(m.emp_name, '【公司顶级领导/无上级】') AS 直属上级姓名
FROM interview_join_employee e
LEFT JOIN interview_join_employee m ON e.manager_id = m.emp_id;

-- --------------------------------------------------------------------
-- 6. CROSS JOIN (交叉连接): 返回笛卡尔积 (M * N 行)
-- --------------------------------------------------------------------
SELECT e.emp_name, d.dept_name
FROM interview_join_employee e
CROSS JOIN interview_join_department d;

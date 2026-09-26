-- ====================================================================
-- 问题 10: MySQL 的关键字 IN 和 EXISTS
-- ====================================================================
-- 1. 区别与执行机制:
--    - IN:     先执行子查询构建临时散列表/物化表，再遍历外表匹配。适用于【外表大，子表小】。
--    - EXISTS: 先遍历外表，再将外表行代入子查询判断布尔真假。适用于【外表小，子表大】。
-- 2. 致命陷阱: NOT IN 与 NULL 值的神坑 (三值逻辑)
--    - 当子查询包含 NULL 时，NOT IN 返回空集 (0行)！
--    - NOT EXISTS 则不受 NULL 干扰，行为始终符合业务预期。
-- ====================================================================

DROP TABLE IF EXISTS interview_dept;
DROP TABLE IF EXISTS interview_emp;

CREATE TABLE interview_dept (
    dept_id INT PRIMARY KEY,
    dept_name VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_emp (
    emp_id INT PRIMARY KEY,
    emp_name VARCHAR(50) NOT NULL,
    dept_id INT NULL,
    INDEX idx_dept (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入数据 (注意: 部门表有 1, 2, 3，员工表中包含 dept_id=NULL 的临时工)
INSERT INTO interview_dept (dept_id, dept_name) VALUES 
(1, '技术部'), (2, '运营部'), (3, '空置部门(无员工)');

INSERT INTO interview_emp (emp_id, emp_name, dept_id) VALUES 
(101, '张工', 1), 
(102, '李工', 1), 
(103, '赵运营', 2), 
(104, '实习无部门人员', NULL);

-- --------------------------------------------------------------------
-- 1. IN 查询: 查出有员工的部门
-- --------------------------------------------------------------------
SELECT * FROM interview_dept d
WHERE d.dept_id IN (SELECT e.dept_id FROM interview_emp e);

-- --------------------------------------------------------------------
-- 2. EXISTS 查询: 查出有员工的部门 (与上述结果相同，但执行方式不同)
-- --------------------------------------------------------------------
SELECT * FROM interview_dept d
WHERE EXISTS (SELECT 1 FROM interview_emp e WHERE e.dept_id = d.dept_id);

-- --------------------------------------------------------------------
-- 3. 核心避坑演练: 查询【没有员工】的空置部门
-- --------------------------------------------------------------------
-- (A) 使用 NOT EXISTS: 能正确查出 dept_id = 3 的空置部门
SELECT * FROM interview_dept d
WHERE NOT EXISTS (SELECT 1 FROM interview_emp e WHERE e.dept_id = d.dept_id);

-- (B) 使用 NOT IN: 因为员工表中有 dept_id 为 NULL 的行，导致结果意外变成空集！
-- 原理: d.dept_id NOT IN (1, 2, NULL) => d.dept_id!=1 AND d.dept_id!=2 AND d.dept_id!=NULL
-- 任何值 != NULL 的结果是 UNKNOWN，导致整行过滤为 FALSE！
SELECT * FROM interview_dept d
WHERE d.dept_id NOT IN (SELECT e.dept_id FROM interview_emp e);

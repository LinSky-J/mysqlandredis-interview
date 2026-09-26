-- ====================================================================
-- 问题 12: SQL 查询语句的执行顺序是怎么样的？
-- ====================================================================
-- 1. 书写顺序 (Syntactic Order):
--    SELECT -> FROM -> JOIN -> ON -> WHERE -> GROUP BY -> HAVING -> ORDER BY -> LIMIT
-- 
-- 2. 逻辑执行顺序 (Logical Query Processing Order):
--    (1) FROM       : 加载源表，多表连接产生笛卡尔积
--    (2) ON         : 根据连接条件过滤行
--    (3) JOIN       : (如果是外连接) 将保留表的未匹配行补 NULL 添回
--    (4) WHERE      : 执行单行级别条件过滤 (此时 SELECT 别名与聚合函数均不可用！)
--    (5) GROUP BY   : 按照指定列进行分组
--    (6) HAVING     : 对分组聚合后的结果进行筛选 (可使用聚合函数)
--    (7) SELECT     : 提取投影列，执行表达式计算并赋予别名
--    (8) DISTINCT   : 消除重复行
--    (9) ORDER BY   : 对结果集排序 (此时 SELECT 别名已生效，可以使用别名)
--    (10) LIMIT     : 截取指定行数并返回客户端
-- ====================================================================

-- 准备演示表: 员工销售业绩表
DROP TABLE IF EXISTS interview_exec_order_sales;
CREATE TABLE interview_exec_order_sales (
    id INT PRIMARY KEY AUTO_INCREMENT,
    dept_name VARCHAR(50) NOT NULL,
    salesperson VARCHAR(50) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入测试数据
INSERT INTO interview_exec_order_sales (dept_name, salesperson, amount, status) VALUES 
('华东区', '张伟', 1200.00, 'PAID'),
('华东区', '张伟', 800.00,  'PAID'),
('华东区', '李芳', 500.00,  'REFUNDED'), -- 退款状态，将被 WHERE 过滤
('华北区', '王刚', 3000.00, 'PAID'),
('华北区', '王刚', 2500.00, 'PAID'),
('华南区', '赵强', 400.00,  'PAID');    -- 总金额 400 < 1000，将被 HAVING 过滤

-- 演示一条包含所有核心关键字的查询语句，体会逻辑执行顺序:
-- 需求: 统计每个部门有效订单(PAID)的总销售额，只保留总额大于 1000 的部门，按总销售额降序排列，取前 1 名
SELECT 
    dept_name, 
    SUM(amount) AS total_sales -- (7) SELECT: 命名别名 total_sales
FROM interview_exec_order_sales -- (1) FROM: 读取数据源
WHERE status = 'PAID'          -- (4) WHERE: 过滤掉非 PAID 行 (注意: 此处不能使用别名 total_sales)
GROUP BY dept_name             -- (5) GROUP BY: 按部门聚合分组
HAVING SUM(amount) >= 1000.00  -- (6) HAVING: 过滤聚合总额大于等于 1000 的分组
ORDER BY total_sales DESC      -- (9) ORDER BY: 排序时可以使用 (7) 定义的别名 total_sales
LIMIT 1;                       -- (10) LIMIT: 截取第一名输出

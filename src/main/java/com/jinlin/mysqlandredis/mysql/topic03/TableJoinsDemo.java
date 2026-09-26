package com.jinlin.mysqlandredis.mysql.topic03;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 03: MySQL 怎么连表查询？
 * 
 * 核心技术要点：
 * 1. 连表语法：INNER JOIN, LEFT JOIN, RIGHT JOIN, CROSS JOIN, 自连接 (Self Join)。
 * 2. 全外连接：MySQL 原生不支持 FULL OUTER JOIN，通过 LEFT JOIN UNION RIGHT JOIN 实现。
 * 3. 底层算法：
 *    - Index Nested-Loop Join (INLJ): 走被驱动表索引，时间复杂度 O(N * logM)。
 *    - Block Nested-Loop Join (BNLJ): 驱动表加载到 join_buffer，MySQL 8.0 之前用于无索引连表。
 *    - Hash Join: MySQL 8.0+ 默认启用替代 BNLJ，在驱动表上构建内存哈希表探测。
 * 4. 优化法则：“小表驱动大表” (过滤后结果集较小的表作为驱动表)。
 */
public class TableJoinsDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 03】MySQL 连表查询类型与底层 Join 算法实机演示");
        System.out.println("====================================================================");

        // 1. 初始化连表演示表
        String dropEmp = "DROP TABLE IF EXISTS interview_join_employee;";
        String createEmp = "CREATE TABLE interview_join_employee ("
                + "  emp_id INT PRIMARY KEY AUTO_INCREMENT,"
                + "  emp_name VARCHAR(50) NOT NULL,"
                + "  dept_id INT NULL,"
                + "  manager_id INT NULL"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String dropDept = "DROP TABLE IF EXISTS interview_join_department;";
        String createDept = "CREATE TABLE interview_join_department ("
                + "  dept_id INT PRIMARY KEY AUTO_INCREMENT,"
                + "  dept_name VARCHAR(50) NOT NULL"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String insertDept = "INSERT INTO interview_join_department (dept_id, dept_name) VALUES "
                + "(10, '研发中心'), (20, '产品设计部'), (30, '市场营销部'), (40, '战略预研部(暂无员工)');";

        String insertEmp = "INSERT INTO interview_join_employee (emp_id, emp_name, dept_id, manager_id) VALUES "
                + "(1, '张总监', 10, NULL), "
                + "(2, '李架构师', 10, 1), "
                + "(3, '王前端', 10, 2), "
                + "(4, '赵产品经理', 20, 1), "
                + "(5, '孙实习生(未分配部门)', NULL, 2);";

        DbConnectionHelper.executeSqlScript(dropEmp, dropDept, createDept, createEmp, insertDept, insertEmp);

        // 2. 执行 INNER JOIN
        String innerJoinSql = "SELECT e.emp_id, e.emp_name, d.dept_name "
                + "FROM interview_join_employee e "
                + "INNER JOIN interview_join_department d ON e.dept_id = d.dept_id;";
        DbConnectionHelper.printQueryResults("1. INNER JOIN (内连接：仅两表均匹配的记录)", innerJoinSql);

        // 3. 执行 LEFT JOIN
        String leftJoinSql = "SELECT e.emp_id, e.emp_name, COALESCE(d.dept_name, '【未分配部门】') AS dept_name "
                + "FROM interview_join_employee e "
                + "LEFT JOIN interview_join_department d ON e.dept_id = d.dept_id;";
        DbConnectionHelper.printQueryResults("2. LEFT JOIN (左外连接：以员工为主表，展示所有员工)", leftJoinSql);

        // 4. 执行 RIGHT JOIN
        String rightJoinSql = "SELECT COALESCE(e.emp_name, '【该部门暂无员工】') AS emp_name, d.dept_id, d.dept_name "
                + "FROM interview_join_employee e "
                + "RIGHT JOIN interview_join_department d ON e.dept_id = d.dept_id;";
        DbConnectionHelper.printQueryResults("3. RIGHT JOIN (右外连接：以部门为主表，展示所有部门)", rightJoinSql);

        // 5. 模拟 FULL OUTER JOIN (UNION 去重合并)
        String fullOuterJoinSql = "SELECT e.emp_id, e.emp_name, d.dept_name "
                + "FROM interview_join_employee e "
                + "LEFT JOIN interview_join_department d ON e.dept_id = d.dept_id "
                + "UNION "
                + "SELECT e.emp_id, e.emp_name, d.dept_name "
                + "FROM interview_join_employee e "
                + "RIGHT JOIN interview_join_department d ON e.dept_id = d.dept_id;";
        DbConnectionHelper.printQueryResults("4. 模拟 FULL OUTER JOIN (全外连接: LEFT JOIN UNION RIGHT JOIN)", fullOuterJoinSql);

        // 6. 执行 Self JOIN 自连接
        String selfJoinSql = "SELECT e.emp_id AS 员工编号, e.emp_name AS 员工姓名, "
                + "COALESCE(m.emp_name, '【无直接上级】') AS 直属上级姓名 "
                + "FROM interview_join_employee e "
                + "LEFT JOIN interview_join_employee m ON e.manager_id = m.emp_id;";
        DbConnectionHelper.printQueryResults("5. Self JOIN (自连接：查询员工与其直属上级)", selfJoinSql);

        printJoinPrinciples();
    }

    private static void printJoinPrinciples() {
        System.out.println("---------------- Join 底层性能与优化原则 ----------------");
        System.out.println("1. 小表驱动大表: 让过滤后数据行数较少的小表作为驱动表，减少外层循环次数。");
        System.out.println("2. ON 条件必须走索引: 被驱动表连接字段若建有索引，触发 Index Nested-Loop (INLJ)，查询极快。");
        System.out.println("3. Hash Join (MySQL 8.0+): 若无索引，MySQL 8 自动启用 Hash Join 代替 BNLJ，显著提高多表连接效率。");
        System.out.println("4. 阿里开发规范: 超过三张表禁止 JOIN；分布式分库分表下禁止跨库跨分片连表，应在应用层组装数据。");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

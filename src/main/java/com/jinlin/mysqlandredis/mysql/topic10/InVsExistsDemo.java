package com.jinlin.mysqlandredis.mysql.topic10;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 10: MySQL 的关键字 IN 和 EXIST
 * 
 * 核心考点：
 * 1. 机制区别：
 *    - IN: 先执行子查询建立物化表，再遍历外表匹配。适合【外表大、子表小】。
 *    - EXISTS: 先遍历外表，将外表当前行代入子查询判断是否命中索引。适合【外表小、子表大】。
 * 2. 致命神坑 (三值逻辑与 NULL 陷阱)：
 *    - 当子查询结果包含 NULL 时，NOT IN 会因为 `col != NULL` 永远判定为 UNKNOWN，导致整条查询返回 0 行！
 *    - NOT EXISTS 评估子查询的行存在性，不受字段是否为 NULL 影响，逻辑永远正确。
 */
public class InVsExistsDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 10】IN 与 EXISTS 执行机制与 NULL 陷阱深度实机验证");
        System.out.println("====================================================================");

        // 1. 初始化演示表与数据
        String dropEmp = "DROP TABLE IF EXISTS interview_emp;";
        String dropDept = "DROP TABLE IF EXISTS interview_dept;";

        String createDept = "CREATE TABLE interview_dept ("
                + "  dept_id INT PRIMARY KEY,"
                + "  dept_name VARCHAR(50) NOT NULL"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String createEmp = "CREATE TABLE interview_emp ("
                + "  emp_id INT PRIMARY KEY,"
                + "  emp_name VARCHAR(50) NOT NULL,"
                + "  dept_id INT NULL,"
                + "  INDEX idx_dept (dept_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String insertDept = "INSERT INTO interview_dept (dept_id, dept_name) VALUES "
                + "(1, '技术部'), (2, '运营部'), (3, '空置部门(无员工)');";

        // 注意: 包含 dept_id=NULL 的实习员工
        String insertEmp = "INSERT INTO interview_emp (emp_id, emp_name, dept_id) VALUES "
                + "(101, '张工', 1), "
                + "(102, '李工', 1), "
                + "(103, '赵运营', 2), "
                + "(104, '实习无部门人员', NULL);";

        DbConnectionHelper.executeSqlScript(dropEmp, dropDept, createDept, createEmp, insertDept, insertEmp);

        // 2. IN 查询演示
        String inSql = "SELECT * FROM interview_dept d WHERE d.dept_id IN (SELECT e.dept_id FROM interview_emp e);";
        DbConnectionHelper.printQueryResults("1. IN 查询: 查出有员工的部门 (外大内小时推荐)", inSql);

        // 3. EXISTS 查询演示
        String existsSql = "SELECT * FROM interview_dept d WHERE EXISTS (SELECT 1 FROM interview_emp e WHERE e.dept_id = d.dept_id);";
        DbConnectionHelper.printQueryResults("2. EXISTS 查询: 查出有员工的部门 (外小内大走索引时推荐)", existsSql);

        // 4. NOT EXISTS 演示 (正确查出部门 3)
        String notExistsSql = "SELECT * FROM interview_dept d WHERE NOT EXISTS (SELECT 1 FROM interview_emp e WHERE e.dept_id = d.dept_id);";
        DbConnectionHelper.printQueryResults("3. NOT EXISTS: 查出没有员工的空置部门 (正确查出 dept_id=3)", notExistsSql);

        // 5. NOT IN 演示 (踩坑演示: 因子查询含 NULL，返回 0 行)
        String notInSql = "SELECT * FROM interview_dept d WHERE d.dept_id NOT IN (SELECT e.dept_id FROM interview_emp e);";
        DbConnectionHelper.printQueryResults("4. NOT IN 踩坑: 因子查询包含 NULL，意外返回 0 条记录！", notInSql);

        printRuleAdvice();
    }

    private static void printRuleAdvice() {
        System.out.println("---------------- 为什么 NOT IN 遇到 NULL 会变成空集？ ----------------");
        System.out.println("SQL 三值逻辑解析: ");
        System.out.println("d.dept_id NOT IN (1, 2, NULL)");
        System.out.println("= (d.dept_id <> 1) AND (d.dept_id <> 2) AND (d.dept_id <> NULL)");
        System.out.println("由于在 SQL 中任何值与 NULL 做比较结果都为 UNKNOWN (既非 TRUE 也非 FALSE)，");
        System.out.println("AND 链条中只要有一个 UNKNOWN，整条表达式结果永远不能为 TRUE，因此整表被全部过滤！");
        System.out.println("【核心法则】在生产环境反向排除查询时，务必使用【NOT EXISTS】或添加【WHERE col IS NOT NULL】过滤！");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

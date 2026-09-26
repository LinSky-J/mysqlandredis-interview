package com.jinlin.mysqlandredis.mysql.topic12;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 12: SQL 查询语句的执行顺序是怎么样的？
 * 
 * 核心考点：
 * 1. 书写顺序 vs 逻辑执行顺序的差异。
 * 2. 逻辑执行顺序 10 步法：
 *    FROM -> ON -> JOIN -> WHERE -> GROUP BY -> HAVING -> SELECT -> DISTINCT -> ORDER BY -> LIMIT
 * 3. 经典高频追问：
 *    - 为什么 WHERE 不能用 SELECT 别名？(WHERE 执行在 SELECT 之前)
 *    - 为什么 ORDER BY 可以用 SELECT 别名？(ORDER BY 执行在 SELECT 之后)
 *    - 为什么 WHERE 不能直接使用 SUM/COUNT 等聚合函数？(WHERE 先于 GROUP BY 过滤行，此时聚合尚未发生)
 */
public class SqlExecutionOrderExplanation {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 12】SQL 查询生命周期与底层逻辑执行顺序深度解析");
        System.out.println("====================================================================");

        // 1. 初始化销售演示数据
        String dropTable = "DROP TABLE IF EXISTS interview_exec_order_sales;";
        String createTable = "CREATE TABLE interview_exec_order_sales ("
                + "  id INT PRIMARY KEY AUTO_INCREMENT,"
                + "  dept_name VARCHAR(50) NOT NULL,"
                + "  salesperson VARCHAR(50) NOT NULL,"
                + "  amount DECIMAL(10, 2) NOT NULL,"
                + "  status VARCHAR(20) NOT NULL"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String insertData = "INSERT INTO interview_exec_order_sales (dept_name, salesperson, amount, status) VALUES "
                + "('华东区', '张伟', 1200.00, 'PAID'), "
                + "('华东区', '张伟', 800.00,  'PAID'), "
                + "('华东区', '李芳', 500.00,  'REFUNDED'), "
                + "('华北区', '王刚', 3000.00, 'PAID'), "
                + "('华北区', '王刚', 2500.00, 'PAID'), "
                + "('华南区', '赵强', 400.00,  'PAID');";

        DbConnectionHelper.executeSqlScript(dropTable, createTable, insertData);

        // 2. 构造综合 SQL，涵盖 FROM, WHERE, GROUP BY, HAVING, SELECT, ORDER BY, LIMIT
        String complexSql = "SELECT "
                + "  dept_name, "
                + "  SUM(amount) AS total_sales "
                + "FROM interview_exec_order_sales "
                + "WHERE status = 'PAID' "
                + "GROUP BY dept_name "
                + "HAVING SUM(amount) >= 1000.00 "
                + "ORDER BY total_sales DESC "
                + "LIMIT 2;";

        DbConnectionHelper.printQueryResults("执行综合查询验证逻辑执行顺序", complexSql);

        printStepByStepExplanation();
    }

    private static void printStepByStepExplanation() {
        System.out.println("---------------- SQL 逻辑执行顺序 10 步法拆解 ----------------");
        System.out.println("① FROM:        加载源表 interview_exec_order_sales，进入计算管线；");
        System.out.println("② ON:          多表关联时，应用连接筛选条件；");
        System.out.println("③ JOIN:        (如 LEFT JOIN) 将主表未匹配的行补充 NULL 添加；");
        System.out.println("④ WHERE:       对行级记录进行过滤 (剔除 status='REFUNDED' 的订单)；");
        System.out.println("⑤ GROUP BY:    根据 dept_name 对剩余有效数据进行分组；");
        System.out.println("⑥ HAVING:      对分组聚合结果进行筛选 (剔除总额 < 1000 的华南区)；");
        System.out.println("⑦ SELECT:      投影指定字段，计算 SUM(amount) 并赋予别名 total_sales；");
        System.out.println("⑧ DISTINCT:    消除重复行 (本例未用)；");
        System.out.println("⑨ ORDER BY:    按别名 total_sales 降序排列 (因为 SELECT 已执行，别名已诞生)；");
        System.out.println("⑩ LIMIT:       最后截取前 2 条记录输出！");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

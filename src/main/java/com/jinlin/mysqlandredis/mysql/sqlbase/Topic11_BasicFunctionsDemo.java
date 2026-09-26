package com.jinlin.mysqlandredis.mysql.sqlbase;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 11: MySQL 中的一些基本函数，你知道哪些？
 * 
 * 知识体系分类：
 * 1. 字符串函数：CONCAT, CONCAT_WS, SUBSTRING, CHAR_LENGTH, LENGTH, LOWER, UPPER, REPLACE, TRIM
 * 2. 数值函数：ROUND, CEIL, FLOOR, ABS, MOD, RAND
 * 3. 日期时间函数：NOW, CURDATE, DATE_ADD, DATE_SUB, DATEDIFF, TIMESTAMPDIFF, DATE_FORMAT
 * 4. 流程与判空函数：IF, IFNULL, COALESCE, CASE WHEN ... THEN ... ELSE END
 * 5. 聚合函数：COUNT, SUM, AVG, MAX, MIN, GROUP_CONCAT
 * 6. 窗口函数 (MySQL 8.0+)：ROW_NUMBER, RANK, DENSE_RANK
 */
public class Topic11_BasicFunctionsDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 11】MySQL 常用核心函数分类实机演示与实战应用");
        System.out.println("====================================================================");

        // 1. 字符串函数
        String strSql = "SELECT "
                + "  CONCAT('MySQL', '-', '8.0') AS str_concat, "
                + "  CONCAT_WS(';', 'Java', 'Python', 'Go') AS str_concat_ws, "
                + "  SUBSTRING('Hello World', 1, 5) AS str_sub, "
                + "  CHAR_LENGTH('中国人') AS char_len, "
                + "  LENGTH('中国人') AS byte_len_utf8mb4, "
                + "  REPLACE('abc_test', 'test', 'demo') AS str_replace;";
        DbConnectionHelper.printQueryResults("1. 常用字符串处理函数", strSql);

        // 2. 数值函数
        String mathSql = "SELECT "
                + "  ROUND(3.1415926, 2) AS round_val, "
                + "  CEIL(4.1) AS ceil_val, "
                + "  FLOOR(4.9) AS floor_val, "
                + "  ABS(-100) AS abs_val, "
                + "  MOD(10, 3) AS mod_val;";
        DbConnectionHelper.printQueryResults("2. 常用数值与数学计算函数", mathSql);

        // 3. 日期时间函数
        String dateSql = "SELECT "
                + "  NOW() AS current_datetime, "
                + "  DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s') AS formatted_date, "
                + "  DATE_ADD(NOW(), INTERVAL 7 DAY) AS plus_7_days, "
                + "  DATEDIFF(CURRENT_DATE, '2026-01-01') AS diff_days_since_new_year, "
                + "  TIMESTAMPDIFF(HOUR, '2026-09-25 08:00:00', '2026-09-26 12:00:00') AS diff_hours;";
        DbConnectionHelper.printQueryResults("3. 常用日期与时间计算函数", dateSql);

        // 4. 流程与判空函数
        String flowSql = "SELECT "
                + "  IF(10 > 5, '大于', '小于等于') AS if_result, "
                + "  IFNULL(NULL, '默认值') AS ifnull_result, "
                + "  COALESCE(NULL, NULL, '第一非空值', '备用') AS coalesce_result, "
                + "  CASE "
                + "    WHEN 85 >= 90 THEN '优秀' "
                + "    WHEN 85 >= 80 THEN '良好' "
                + "    ELSE '一般' "
                + "  END AS case_result;";
        DbConnectionHelper.printQueryResults("4. 流程控制与空值容错函数", flowSql);

        printSummaryNotice();
    }

    private static void printSummaryNotice() {
        System.out.println("---------------- 生产环境函数索引失效告警 ----------------");
        System.out.println("【关键避坑】绝不能在 WHERE 查询条件的【索引列上使用函数】！");
        System.out.println("反例: WHERE DATE_FORMAT(create_time, '%Y-%m-%d') = '2026-09-26' -> 导致 create_time 索引彻底失效，引发全表扫描！");
        System.out.println("正例: WHERE create_time >= '2026-09-26 00:00:00' AND create_time < '2026-09-27 00:00:00' -> 完美走索引范围扫描。");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}


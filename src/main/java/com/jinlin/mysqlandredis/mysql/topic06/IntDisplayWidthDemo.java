package com.jinlin.mysqlandredis.mysql.topic06;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 06: int(1) 和 int(10) 在 MySQL 中有什么不同？
 * 
 * 核心考点：
 * 1. 存储大小：完全一样，均为 4 字节 (32-bit)。
 * 2. 数值范围：完全一样，[-2147483648 ~ 2147483647] (有符号) 或 [0 ~ 4294967295] (无符号)。
 * 3. 括号中的 M：仅表示【显示宽度 (Display Width)】，只有配合 ZEROFILL 时才会在数值不足 M 位时高位补 0。
 * 4. 演进趋势：MySQL 8.0.17 起已弃用整型显示宽度语法，建议统一声明为 INT。
 */
public class IntDisplayWidthDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 06】int(1) vs int(10) 存储与显示宽度实机验证");
        System.out.println("====================================================================");

        // 1. 初始化表
        String dropTable = "DROP TABLE IF EXISTS interview_int_display;";
        String createTable = "CREATE TABLE interview_int_display ("
                + "  id INT PRIMARY KEY AUTO_INCREMENT,"
                + "  col_int1 INT(1) COMMENT 'int(1)',"
                + "  col_int10 INT(10) COMMENT 'int(10)',"
                + "  col_zerofill_5 INT(5) ZEROFILL COMMENT '带 ZEROFILL 宽度的 5 位'"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        // 插入两组测试数据:
        // 第 1 组: 存入 32 位整型上限 2147483647 到 int(1) 中
        // 第 2 组: 存入 小数字 18 到 int(5) ZEROFILL 中 (观察前导补零)
        String insertData = "INSERT INTO interview_int_display (col_int1, col_int10, col_zerofill_5) VALUES "
                + "(2147483647, 2147483647, 18), "
                + "(7, 7, 7);";

        DbConnectionHelper.executeSqlScript(dropTable, createTable, insertData);

        // 2. 查询并打印
        String verifySql = "SELECT id, col_int1, col_int10, col_zerofill_5 FROM interview_int_display;";
        DbConnectionHelper.printQueryResults("int(1) vs int(10) 存取结果验证", verifySql);

        printInsight();
    }

    private static void printInsight() {
        System.out.println("---------------- 深度总结与面试答题技巧 ----------------");
        System.out.println("1. 辟谣误区: 很多初学者误以为 int(1) 只能存 0~9 的一位数。事实证明它能存 2147483647！");
        System.out.println("2. 为什么设计显示宽度？早年终端界面排版时，DBA 希望数值对齐，配合 ZEROFILL 填充 0。");
        System.out.println("3. 如何真正控制存储空间？");
        System.out.println("   - 存 0/1 状态或枚举: 用 TINYINT (1字节, 范围 -128~127 或 0~255)；");
        System.out.println("   - 存 2~3 万的行数: 用 SMALLINT (2字节, -32768~32767)；");
        System.out.println("   - 存 800 万级: 用 MEDIUMINT (3字节, -8388608~8388607)；");
        System.out.println("   - 存 20 亿级: 用 INT (4字节)；");
        System.out.println("   - 存海量雪花 ID / 交易流水: 用 BIGINT (8字节)。");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

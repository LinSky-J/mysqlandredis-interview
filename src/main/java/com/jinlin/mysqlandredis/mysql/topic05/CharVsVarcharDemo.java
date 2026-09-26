package com.jinlin.mysqlandredis.mysql.topic05;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 05: CHAR 和 VARCHAR 有什么区别？varchar 后面代表字节还是字符？
 * 
 * 核心考点：
 * 1. 定长 vs 变长：CHAR 固定分配空间，右侧补空格；VARCHAR 动态按需分配 + 1~2 字节长度前缀。
 * 2. 尾随空格处理：CHAR 在读取时会自动剔除末尾空格；VARCHAR 保留末尾空格。
 * 3. 字符还是字节？自 MySQL 4.1 开始，VARCHAR(N) 中的 N 明确表示【字符数 (Characters)】，非字节数。
 *    在 utf8mb4 下，1 个汉字占用 3~4 字节，VARCHAR(10) 最多可容纳 10 个汉字 (占用 30~40 字节)。
 */
public class CharVsVarcharDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 05】CHAR vs VARCHAR 存储细节与字符/字节实机验证");
        System.out.println("====================================================================");

        // 1. 初始化表
        String dropTable = "DROP TABLE IF EXISTS interview_char_varchar;";
        String createTable = "CREATE TABLE interview_char_varchar ("
                + "  id INT PRIMARY KEY AUTO_INCREMENT,"
                + "  c_fixed CHAR(10) COMMENT '定长 10 字符',"
                + "  v_variable VARCHAR(10) COMMENT '变长 10 字符'"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        // 插入两组测试数据:
        // 第 1 组: 带尾随空格的英文 'test  ' (4字母 + 2空格 = 6字符)
        // 第 2 组: 4 个中文字符 '中国加油'
        String insertData = "INSERT INTO interview_char_varchar (c_fixed, v_variable) VALUES "
                + "('test  ', 'test  '), "
                + "('中国加油', '中国加油');";

        DbConnectionHelper.executeSqlScript(dropTable, createTable, insertData);

        // 2. 查询比较 CHAR_LENGTH(字符数) 与 LENGTH(字节数)
        String verifySql = "SELECT "
                + "  id, "
                + "  c_fixed, "
                + "  CHAR_LENGTH(c_fixed) AS char_len_c, "
                + "  LENGTH(c_fixed) AS byte_len_c, "
                + "  v_variable, "
                + "  CHAR_LENGTH(v_variable) AS char_len_v, "
                + "  LENGTH(v_variable) AS byte_len_v "
                + "FROM interview_char_varchar;";

        DbConnectionHelper.printQueryResults("CHAR vs VARCHAR 长度与空格行为实测", verifySql);

        printDetailedAnalysis();
    }

    private static void printDetailedAnalysis() {
        System.out.println("---------------- 深度解析与业务选型 ----------------");
        System.out.println("1. 为什么 'test  ' 在 CHAR(10) 中 char_len 为 4，而在 VARCHAR(10) 中为 6？");
        System.out.println("   - CHAR 存入时用空格补齐到 10，但在 SELECT 查询时，MySQL 会自动剥离末尾空格；");
        System.out.println("   - VARCHAR 则完整保留末尾空格。");
        System.out.println("2. 为什么 '中国加油' 的 byte_len 是 12？");
        System.out.println("   - 数据库字符集为 utf8mb4，常见中文字符占用 3 字节，4 * 3 = 12 字节；");
        System.out.println("   - 彻底证实 VARCHAR(10) 限制的是 10 个字符，而非 10 个字节！");
        System.out.println("3. 业务场景选型：");
        System.out.println("   - 选 CHAR: 长度绝对固定且频繁读取的字段，如 UUID、MD5 (32字符)、定长哈希、性别 (1字符)、手机号 (11字符)；寻址更快且无碎片。");
        System.out.println("   - 选 VARCHAR: 长度不确定的普通文本，如 用户名、邮箱、简介；能节省大量磁盘与内存缓冲池空间。");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

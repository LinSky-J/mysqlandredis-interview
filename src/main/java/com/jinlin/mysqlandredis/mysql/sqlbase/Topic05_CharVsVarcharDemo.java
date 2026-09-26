package com.jinlin.mysqlandredis.mysql.sqlbase;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 05: CHAR 和 VARCHAR 有什么区别？varchar 后面代表字节还是字符？
 * 
 * 说明：数据表结构及初始化数据已移至 /sql/01_mysql_basics_schema.sql 统一由 DataGrip 预先创建维护，
 *       本 Java 类专注 CHAR 与 VARCHAR 在字符/字节长度及尾随空格处理上的实机验证。
 * 
 * 核心考点：
 * 1. 定长 vs 变长：CHAR 固定分配空间，右侧补空格；VARCHAR 动态按需分配 + 1~2 字节长度前缀。
 * 2. 尾随空格处理：CHAR 在读取时会自动剔除末尾空格；VARCHAR 保留末尾空格。
 * 3. 字符还是字节？自 MySQL 4.1 开始，VARCHAR(N) 中的 N 明确表示【字符数 (Characters)】，非字节数。
 *    在 utf8mb4 下，1 个汉字占用 3~4 字节，VARCHAR(10) 最多可容纳 10 个汉字 (占用 30~40 字节)。
 */
public class Topic05_CharVsVarcharDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 05】CHAR vs VARCHAR 存储细节与字符/字节实机验证");
        System.out.println("====================================================================");

        // 查询比较 CHAR_LENGTH(字符数) 与 LENGTH(字节数)
        String verifySql = "SELECT "
                + "  id, "
                + "  fixed_code, "
                + "  CHAR_LENGTH(fixed_code) AS char_len_c, "
                + "  LENGTH(fixed_code) AS byte_len_c, "
                + "  var_code, "
                + "  CHAR_LENGTH(var_code) AS char_len_v, "
                + "  LENGTH(var_code) AS byte_len_v "
                + "FROM interview_char_varchar;";

        DbConnectionHelper.printQueryResults("CHAR vs VARCHAR 长度与空格行为实测", verifySql);

        printDetailedAnalysis();
    }

    private static void printDetailedAnalysis() {
        System.out.println("---------------- 深度解析与业务选型 ----------------");
        System.out.println("1. 为什么 CHAR 自动剔除末尾空格？");
        System.out.println("   - CHAR 存入时用空格补齐到预定宽度，但在 SELECT 查询时，MySQL 会自动剥离末尾空格；");
        System.out.println("   - VARCHAR 则完整保留末尾空格。");
        System.out.println("2. 字符数 vs 字节数验证：");
        System.out.println("   - 数据库字符集为 utf8mb4，常见中文字符占用 3 字节；");
        System.out.println("   - 彻底证实 VARCHAR(N) 限制的是 N 个字符，而非 N 个字节！");
        System.out.println("3. 业务场景选型：");
        System.out.println("   - 选 CHAR: 长度绝对固定且频繁读取的字段，如 UUID、MD5 (32字符)、定长哈希、手机号 (11字符)；寻址更快且无碎片。");
        System.out.println("   - 选 VARCHAR: 长度不确定的普通文本，如 用户名、邮箱、简介；能节省大量磁盘与内存缓冲池空间。");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}


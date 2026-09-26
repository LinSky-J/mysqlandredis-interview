package com.jinlin.mysqlandredis.mysql.topic07;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 07: Text 数据类型可以无限大吗？
 * 
 * 核心考点：
 * 1. 容量限制：绝对不能无限大！分为 TINYTEXT(255B)、TEXT(64KB)、MEDIUMTEXT(16MB)、LONGTEXT(4GB)。
 * 2. 网络传输限制：受参数 max_allowed_packet 强约束，超过直接抛出 PacketTooBigException。
 * 3. 底层存储机制：InnoDB 默认页大小为 16KB，超过阈值会触发行溢出 (Off-page storage)，大字段分散在溢出页链表中。
 * 4. 查询性能黑洞：TEXT 字段无法被 MEMORY 存储引擎在内存临时表中高效排序，强制退化为磁盘临时表 (Disk Temp Table)。
 */
public class TextDataTypeDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 07】MySQL Text 数据类型容量边界与底层溢出机制实机演示");
        System.out.println("====================================================================");

        // 1. 查询当前真实 MySQL 实例的 max_allowed_packet 参数限制
        DbConnectionHelper.printQueryResults("当前数据库网络包限制 (max_allowed_packet)", 
                "SHOW VARIABLES LIKE 'max_allowed_packet';");

        // 2. 初始化演示表
        String dropTable = "DROP TABLE IF EXISTS interview_text_storage;";
        String createTable = "CREATE TABLE interview_text_storage ("
                + "  id INT PRIMARY KEY AUTO_INCREMENT,"
                + "  short_notes TINYTEXT,"
                + "  content TEXT,"
                + "  article MEDIUMTEXT,"
                + "  large_payload LONGTEXT"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String insertData = "INSERT INTO interview_text_storage (short_notes, content, article, large_payload) VALUES ("
                + "'简要提示说明', "
                + "REPEAT('TEXT内容-64KB上限-', 30), "
                + "REPEAT('MEDIUMTEXT文章-16MB上限-', 80), "
                + "REPEAT('LONGTEXT超长文本-4GB理论上限-', 200)"
                + ");";

        DbConnectionHelper.executeSqlScript(dropTable, createTable, insertData);

        // 3. 查询各字段实际字符长度
        String queryLengthSql = "SELECT "
                + "  id, "
                + "  CHAR_LENGTH(short_notes) AS len_tiny, "
                + "  CHAR_LENGTH(content) AS len_text, "
                + "  CHAR_LENGTH(article) AS len_medium, "
                + "  CHAR_LENGTH(large_payload) AS len_long "
                + "FROM interview_text_storage;";

        DbConnectionHelper.printQueryResults("各 TEXT 类型存储长度检测", queryLengthSql);

        printArchitectureAdvice();
    }

    private static void printArchitectureAdvice() {
        System.out.println("---------------- 生产架构实践指南 ----------------");
        System.out.println("1. 为什么禁止在业务大表中滥用 TEXT/BLOB？");
        System.out.println("   - SELECT * 时会带出巨量数据，打爆网络带宽并挤出 Buffer Pool 核心缓存；");
        System.out.println("   - ORDER BY / GROUP BY 临时表不能用内存引擎，直接退化为磁盘 I/O，QPS 暴跌。");
        System.out.println("2. 正确架构解法：");
        System.out.println("   - 【表垂直拆分】: 将主业务表(包含核心字段)与大文本扩展表(如 user_ext_info)拆开；");
        System.out.println("   - 【对象存储下沉】: 大文本、长文章富文本、图片视频等直接存储到 OSS / MinIO / S3，数据库只存对应的访问 URL。");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

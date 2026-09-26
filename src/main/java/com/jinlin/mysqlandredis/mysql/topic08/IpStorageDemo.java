package com.jinlin.mysqlandredis.mysql.topic08;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 08: IP 地址如何在数据库里存储？
 * 
 * 核心考点：
 * 1. 为什么不用 VARCHAR(15)？
 *    - 占用空间大 (变长头+字符串高达 16 字节)；
 *    - 字符串排序会导致网段范围查询彻底失真 (例如 '192.168.1.9' > '192.168.1.10')。
 * 2. 为什么推荐 INT UNSIGNED？
 *    - IPv4 本质是 32 位无符号整数，正好占 4 个字节，节省 75% 存储空间与索引内存；
 *    - B+ 树范围查询与网段掩码过滤性能极高。
 * 3. 互转工具：
 *    - MySQL: INET_ATON(ipStr) / INET_NTOA(ipNum)；
 *    - Java: 基于位移运算相互转换。
 */
public class IpStorageDemo {

    /**
     * Java 算法：点分十进制 IPv4 转 32位无符号 long
     */
    public static long ipToLong(String ipAddress) {
        String[] parts = ipAddress.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            long p = Long.parseLong(parts[i]);
            result |= (p << ((3 - i) * 8));
        }
        return result;
    }

    /**
     * Java 算法：32位无符号 long 还原为点分十进制字符串
     */
    public static String longToIp(long ip) {
        return ((ip >> 24) & 0xFF) + "." +
               ((ip >> 16) & 0xFF) + "." +
               ((ip >> 8) & 0xFF) + "." +
               (ip & 0xFF);
    }

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 08】IP 地址的高效存储方案 (INT UNSIGNED) 与网段检索实机演示");
        System.out.println("====================================================================");

        // 1. 初始化表
        String dropTable = "DROP TABLE IF EXISTS interview_ip_storage;";
        String createTable = "CREATE TABLE interview_ip_storage ("
                + "  id INT PRIMARY KEY AUTO_INCREMENT,"
                + "  user_name VARCHAR(50) NOT NULL,"
                + "  ip_varchar VARCHAR(15) NOT NULL,"
                + "  ip_int INT UNSIGNED NOT NULL,"
                + "  INDEX idx_ip_int (ip_int)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String insertData = "INSERT INTO interview_ip_storage (user_name, ip_varchar, ip_int) VALUES "
                + "('用户A', '192.168.1.5',   INET_ATON('192.168.1.5')), "
                + "('用户B', '192.168.1.10',  INET_ATON('192.168.1.10')), "
                + "('用户C', '192.168.1.100', INET_ATON('192.168.1.100')), "
                + "('用户D', '10.0.0.1',      INET_ATON('10.0.0.1'));";

        DbConnectionHelper.executeSqlScript(dropTable, createTable, insertData);

        // 2. 查询并还原 IP
        String selectSql = "SELECT "
                + "  id, "
                + "  user_name, "
                + "  ip_varchar, "
                + "  ip_int, "
                + "  INET_NTOA(ip_int) AS ip_restored "
                + "FROM interview_ip_storage;";

        DbConnectionHelper.printQueryResults("IP 存储与还原结果对比", selectSql);

        // 3. 执行 IP 范围查询 (例如网段 192.168.1.1 ~ 192.168.1.20)
        String rangeQuery = "SELECT "
                + "  id, "
                + "  user_name, "
                + "  INET_NTOA(ip_int) AS ip_matched, "
                + "  ip_int "
                + "FROM interview_ip_storage "
                + "WHERE ip_int BETWEEN INET_ATON('192.168.1.1') AND INET_ATON('192.168.1.20');";

        DbConnectionHelper.printQueryResults("网段范围精确查询 (192.168.1.1 ~ 192.168.1.20)", rangeQuery);

        // 4. 验证 Java 原生转换与 MySQL 转换结果一致
        String testIp = "192.168.1.10";
        long converted = ipToLong(testIp);
        String restored = longToIp(converted);
        System.out.printf("Java 位移转换验证: IP [%s] -> 整型 [%d] -> 还原 [%s]\n", testIp, converted, restored);
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

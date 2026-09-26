package com.jinlin.mysqlandredis.mysql.sqlbase;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 08: IP 地址如何在数据库里存储？
 * 
 * 说明：数据表结构及初始化数据已移至 /sql/01_mysql_basics_schema.sql 统一由 DataGrip 预先创建维护，
 *       本 Java 类专注 INT UNSIGNED 与 VARCHAR(15) 存储对比、网段范围检索与位移算法讲解。
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
public class Topic08_IpStorageDemo {

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

        // 1. 查询并还原 IP
        String selectSql = "SELECT "
                + "  id, "
                + "  ip_string, "
                + "  ip_numeric, "
                + "  INET_NTOA(ip_numeric) AS ip_restored "
                + "FROM interview_ip_storage;";

        DbConnectionHelper.printQueryResults("IP 存储与还原结果对比", selectSql);

        // 2. 执行 IP 范围查询 (例如网段 192.168.1.1 ~ 192.168.1.20)
        String rangeQuery = "SELECT "
                + "  id, "
                + "  ip_string, "
                + "  INET_NTOA(ip_numeric) AS ip_matched, "
                + "  ip_numeric "
                + "FROM interview_ip_storage "
                + "WHERE ip_numeric BETWEEN INET_ATON('192.168.1.1') AND INET_ATON('192.168.1.20');";

        DbConnectionHelper.printQueryResults("网段范围精确查询 (192.168.1.1 ~ 192.168.1.20)", rangeQuery);

        // 3. 验证 Java 原生转换与 MySQL 转换结果一致
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


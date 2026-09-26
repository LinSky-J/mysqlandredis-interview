package com.jinlin.mysqlandredis.mysql.storage;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 02: 讲一讲 MySQL 的引擎吧，你有什么了解？
 * 
 * 核心考点：
 * 1. 插件式存储引擎：引擎作用在表级，支持可插拔扩展。
 * 2. 主流引擎横向对比：
 *    - InnoDB: 事务型、行级锁、MVCC、外键、聚簇索引、Crash-Safe。核心 OLTP 业务。
 *    - MyISAM: 非事务、表锁、不支持外键、非聚簇索引、Count(*) 单独存储。归档报表、只读分析。
 *    - MEMORY: 纯内存、哈希/B树索引、极速、断电丢数据、表锁、不支持 TEXT/BLOB。临时计算、热点字典。
 *    - ARCHIVE: 仅支持 INSERT+SELECT、超高 zlib 压缩比。海量日志审计。
 *    - CSV: 逗号分隔文本存储，方便异构系统数据流转。
 */
public class MysqlEnginesOverviewDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 02】MySQL 常见存储引擎全景剖析与多引擎建表实机演示");
        System.out.println("====================================================================");

        // 1. 查询当前真实 MySQL 实例支持的全部引擎
        DbConnectionHelper.printQueryResults("当前 MySQL 实例支持的全部存储引擎列表 (SHOW ENGINES)", "SHOW ENGINES;");

        // 2. 初始化创建三种具有代表性的引擎表 (MEMORY, MyISAM, InnoDB)
        String dropMemory = "DROP TABLE IF EXISTS interview_engine_memory;";
        String createMemory = "CREATE TABLE interview_engine_memory ("
                + "  id INT PRIMARY KEY,"
                + "  key_name VARCHAR(50) NOT NULL,"
                + "  val VARCHAR(100) NOT NULL"
                + ") ENGINE=MEMORY DEFAULT CHARSET=utf8mb4;";

        String dropMyisam = "DROP TABLE IF EXISTS interview_engine_myisam;";
        String createMyisam = "CREATE TABLE interview_engine_myisam ("
                + "  id INT PRIMARY KEY,"
                + "  log_title VARCHAR(100) NOT NULL,"
                + "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ") ENGINE=MyISAM DEFAULT CHARSET=utf8mb4;";

        String dropInnodb = "DROP TABLE IF EXISTS interview_engine_innodb;";
        String createInnodb = "CREATE TABLE interview_engine_innodb ("
                + "  id INT PRIMARY KEY,"
                + "  account_no VARCHAR(50) NOT NULL,"
                + "  balance DECIMAL(12, 2) NOT NULL"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String insertMemory = "INSERT INTO interview_engine_memory VALUES (1, 'SESSION_TOKEN', 'abc123xyz');";
        String insertMyisam = "INSERT INTO interview_engine_myisam VALUES (1, '系统启动日志', NOW());";
        String insertInnodb = "INSERT INTO interview_engine_innodb VALUES (1, '622202000000001', 99999.00);";

        DbConnectionHelper.executeSqlScript(
                dropMemory, createMemory, insertMemory,
                dropMyisam, createMyisam, insertMyisam,
                dropInnodb, createInnodb, insertInnodb
        );

        // 3. 从 information_schema 查询这三张表在底层元数据中的差异
        String queryMetaSql = "SELECT "
                + "  TABLE_NAME, "
                + "  ENGINE, "
                + "  ROW_FORMAT, "
                + "  TABLE_ROWS, "
                + "  DATA_LENGTH, "
                + "  INDEX_LENGTH "
                + "FROM information_schema.TABLES "
                + "WHERE TABLE_SCHEMA = 'interview_db' "
                + "  AND TABLE_NAME IN ('interview_engine_memory', 'interview_engine_myisam', 'interview_engine_innodb');";

        DbConnectionHelper.printQueryResults("三类引擎元数据存储与行格式对比", queryMetaSql);

        printEnginesComparisonGuide();
    }

    private static void printEnginesComparisonGuide() {
        System.out.println("---------------- 存储引擎技术选型全景矩阵 ----------------");
        System.out.println("1. InnoDB: 互联网绝对主力，支持 ACID 事务、MVCC 高并发非锁定读、行锁高吞吐。");
        System.out.println("2. MyISAM: 早年广泛使用，但因缺少事务与崩溃容易损毁 (Crash-Unsafe)，除极少只读历史数据外已逐步淘汰。");
        System.out.println("3. MEMORY: 适合作为短周期快速中间临时表，但在分布式架构下通常已被 Redis 替代。");
        System.out.println("4. ARCHIVE: 压缩率高达 75%，对于需要保存 3~5 年以上的合规审计、操作日志极具存储成本优势。");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

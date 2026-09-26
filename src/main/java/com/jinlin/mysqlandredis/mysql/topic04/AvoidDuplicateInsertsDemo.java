package com.jinlin.mysqlandredis.mysql.topic04;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 04: MySQL 如何避免重复插入数据？
 * 
 * 核心方案与机制对比：
 * 1. 唯一索引 (UNIQUE KEY)：底层约束防线，任何方案的前提。
 * 2. INSERT IGNORE INTO：冲突则跳过，无异常，受影响行数 0。
 * 3. REPLACE INTO：冲突则先 DELETE 再 INSERT，注意主键自增 ID 会变化，且易引发外键级联灾难。
 * 4. INSERT INTO ... ON DUPLICATE KEY UPDATE：冲突则转为 UPDATE，保留原 ID，企业开发首选。
 * 5. 条件插入：INSERT INTO ... SELECT ... WHERE NOT EXISTS (...)。
 */
public class AvoidDuplicateInsertsDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 04】MySQL 避免重复插入的 4 种方案实机演示与对比");
        System.out.println("====================================================================");

        // 1. 初始化表
        String dropTable = "DROP TABLE IF EXISTS interview_duplicate_user;";
        String createTable = "CREATE TABLE interview_duplicate_user ("
                + "  id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "  phone VARCHAR(20) NOT NULL,"
                + "  nickname VARCHAR(50) NOT NULL,"
                + "  login_count INT NOT NULL DEFAULT 1,"
                + "  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "  UNIQUE KEY uk_phone (phone)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        // 插入初始数据: id=1, phone='13900001111', nickname='张三', count=1
        String initData = "INSERT INTO interview_duplicate_user (phone, nickname, login_count) "
                + "VALUES ('13900001111', '张三-初始', 1);";

        DbConnectionHelper.executeSqlScript(dropTable, createTable, initData);
        DbConnectionHelper.printQueryResults("初始数据状态", "SELECT id, phone, nickname, login_count FROM interview_duplicate_user;");

        // 2. 方案一：INSERT IGNORE
        String sqlIgnore = "INSERT IGNORE INTO interview_duplicate_user (phone, nickname, login_count) "
                + "VALUES ('13900001111', '张三-尝试Ignore插入', 99);";
        DbConnectionHelper.executeSqlScript(sqlIgnore);
        DbConnectionHelper.printQueryResults("方案 1: INSERT IGNORE (遇到冲突直接忽略，数据保持不变)", 
                "SELECT id, phone, nickname, login_count FROM interview_duplicate_user;");

        // 3. 方案二：REPLACE INTO
        String sqlReplace = "REPLACE INTO interview_duplicate_user (phone, nickname, login_count) "
                + "VALUES ('13900001111', '张三-已被Replace', 2);";
        DbConnectionHelper.executeSqlScript(sqlReplace);
        DbConnectionHelper.printQueryResults("方案 2: REPLACE INTO (先 DELETE 再 INSERT，注意主键 ID 变为 2！)", 
                "SELECT id, phone, nickname, login_count FROM interview_duplicate_user;");

        // 4. 方案三：ON DUPLICATE KEY UPDATE (生产推荐)
        String sqlOnDuplicate = "INSERT INTO interview_duplicate_user (phone, nickname, login_count) "
                + "VALUES ('13900001111', '张三-优雅更新', 1) "
                + "ON DUPLICATE KEY UPDATE "
                + "  nickname = VALUES(nickname), "
                + "  login_count = login_count + 1;";
        DbConnectionHelper.executeSqlScript(sqlOnDuplicate);
        DbConnectionHelper.printQueryResults("方案 3: ON DUPLICATE KEY UPDATE (保留主键 ID=2，原子更新 nickname 与 count)", 
                "SELECT id, phone, nickname, login_count FROM interview_duplicate_user;");

        printComparisonSummary();
    }

    private static void printComparisonSummary() {
        System.out.println("---------------- 生产环境选用建议 ----------------");
        System.out.println("1. 为什么尽量不用 REPLACE INTO？");
        System.out.println("   - 自增 ID 会递增耗尽；");
        System.out.println("   - 触发 DELETE + INSERT 两次索引写操作，引起额外的页分裂与 Binlog 变动；");
        System.out.println("   - 若表上建有外键约束，会无辜触发级联删除。");
        System.out.println("2. 最佳实践：");
        System.out.println("   - 数据幂等入库：优先选用【INSERT ... ON DUPLICATE KEY UPDATE】；");
        System.out.println("   - 并发防重：高并发入口必须在应用层使用分布式锁 (Redis/Redisson) + 数据库唯一索引兜底。");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

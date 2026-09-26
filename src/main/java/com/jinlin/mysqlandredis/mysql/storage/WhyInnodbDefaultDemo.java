package com.jinlin.mysqlandredis.mysql.storage;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 问题 03: MySQL 为什么 InnoDB 是默认引擎？
 * 
 * 核心考点：
 * 1. 历史沿革：从 MySQL 5.5 开始，InnoDB 成为默认引擎；MySQL 8.0 系统字典表全部改为 InnoDB。
 * 2. 6 大核心杀手锏：
 *    ① ACID 事务完整性：支持 COMMIT 与 ROLLBACK，保障电商与金融结算原子性。
 *    ② 行级锁 (Row-level Locking)：避免 MyISAM 表锁导致的读写互斥与并发瓶颈。
 *    ③ 崩溃安全 (Crash-Safe)：Redo Log (WAL) 保证意外宕机后零丢数据自动恢复。
 *    ④ MVCC 多版本并发控制：读不阻塞写，写不阻塞读，快照读提升数倍 QPS。
 *    ⑤ 统一 Buffer Pool 缓存：统一管理数据页与索引页，淘汰算法优异。
 *    ⑥ 聚簇索引 (Clustered Index)：主键点查 1 次寻道即可命中全部列。
 */
public class WhyInnodbDefaultDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 03】MySQL 为什么 InnoDB 是默认引擎？核心优势实机检验");
        System.out.println("====================================================================");

        // 1. 查询当前默认引擎配置
        DbConnectionHelper.printQueryResults("当前 MySQL 实例默认存储引擎", "SHOW VARIABLES LIKE 'default_storage_engine';");

        // 2. 初始化测试表
        String dropTable = "DROP TABLE IF EXISTS interview_why_innodb;";
        String createTable = "CREATE TABLE interview_why_innodb ("
                + "  id INT PRIMARY KEY AUTO_INCREMENT,"
                + "  account_name VARCHAR(50) NOT NULL,"
                + "  balance DECIMAL(10, 2) NOT NULL"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String insertData = "INSERT INTO interview_why_innodb (account_name, balance) VALUES ('张三', 1000.00), ('李四', 2000.00);";

        DbConnectionHelper.executeSqlScript(dropTable, createTable, insertData);

        // 3. 验证优势一：ACID 事务与异常回滚机制 (MyISAM 无法做到)
        System.out.println(">>> [验证 1] 事务原子性与回滚测试 (扣减张三 500 元后模拟异常回滚):");
        try (Connection conn = DbConnectionHelper.getConnection()) {
            conn.setAutoCommit(false); // 开启事务
            try (PreparedStatement ps = conn.prepareStatement("UPDATE interview_why_innodb SET balance = balance - 500 WHERE id = 1")) {
                ps.executeUpdate();
            }
            // 模拟发生异常，执行事务回滚
            conn.rollback();
            System.out.println("    事务回滚完成！");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        DbConnectionHelper.printQueryResults("回滚后张三的余额验证 (必须依然为初始的 1000.00)", 
                "SELECT id, account_name, balance FROM interview_why_innodb WHERE id = 1;");

        // 4. 验证优势二：行级并发更新互不阻塞 (行级锁验证)
        System.out.println(">>> [验证 2] 行级并发锁验证 (同时在两个独立连接分别更新 id=1 与 id=2):");
        try (Connection conn1 = DbConnectionHelper.getConnection();
             Connection conn2 = DbConnectionHelper.getConnection()) {
            conn1.setAutoCommit(false);
            conn2.setAutoCommit(false);

            // 连接 1 锁住 id=1
            try (PreparedStatement ps1 = conn1.prepareStatement("UPDATE interview_why_innodb SET balance = balance + 10 WHERE id = 1")) {
                ps1.executeUpdate();
            }

            // 连接 2 并发更新 id=2 (在 MyISAM 下会被整表锁阻塞超时，而在 InnoDB 下毫秒级成功！)
            try (PreparedStatement ps2 = conn2.prepareStatement("UPDATE interview_why_innodb SET balance = balance + 20 WHERE id = 2")) {
                ps2.executeUpdate();
                System.out.println("    连接 2 成功并发更新 id=2，完全未被连接 1 的行锁阻塞！");
            }

            conn1.commit();
            conn2.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        DbConnectionHelper.printQueryResults("两连接并发提交后的数据状态", 
                "SELECT id, account_name, balance FROM interview_why_innodb;");

        printWhyInnodbSummary();
    }

    private static void printWhyInnodbSummary() {
        System.out.println("---------------- 为什么 MyISAM 无法胜任默认引擎？ ----------------");
        System.out.println("1. 商业场景变迁: 互联网由只读静态内容(新闻/博客)快速过渡到交易型系统(电商/社交/支付)，事务与并发写成为刚需。");
        System.out.println("2. 宕机运维灾难: MyISAM 发生异常断电时索引经常大面积损毁，必须停机运行 myisamchk，而 InnoDB 凭借 Redo Log 实现秒级自动 Crash-Recovery。");
        System.out.println("3. 锁粒度差距: MyISAM 的表级锁使所有写操作排他，直接掐死了现代高并发架构的并发上限。");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

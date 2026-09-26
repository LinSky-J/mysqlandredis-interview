package com.jinlin.mysqlandredis.mysql.topic01;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 01: NOSQL 和 SQL 的区别？
 * 
 * 核心对比点：
 * 1. 数据模型与 Schema：SQL 严格预定义关系模式；NoSQL 动态无模式(键值/文档/列族/图)。
 * 2. 事务模型：SQL 严格遵循 ACID (原子性/一致性/隔离性/持久性)；NoSQL 遵循 CAP/BASE 最终一致性。
 * 3. 扩展能力：SQL 纵向提升单机硬件(Scale-up)；NoSQL 线性横向分片(Scale-out)。
 * 4. 查询与复杂关联：SQL 支持多表 JOIN、嵌套子查询与聚合；NoSQL 缺乏跨集合 JOIN，偏好嵌套冗余。
 */
public class NoSqlVsSqlExplanation {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 01】NOSQL 和 SQL 的核心区别深度解析与实机演示");
        System.out.println("====================================================================");

        // 1. 初始化演示数据表 (真实 MySQL 交互)
        String dropUserTable = "DROP TABLE IF EXISTS interview_sql_user;";
        String createUserTable = "CREATE TABLE interview_sql_user ("
                + "  id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "  username VARCHAR(50) NOT NULL UNIQUE,"
                + "  email VARCHAR(100) NOT NULL,"
                + "  balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00,"
                + "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String dropOrderTable = "DROP TABLE IF EXISTS interview_sql_order;";
        String createOrderTable = "CREATE TABLE interview_sql_order ("
                + "  order_id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "  user_id BIGINT NOT NULL,"
                + "  amount DECIMAL(10, 2) NOT NULL,"
                + "  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',"
                + "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "  INDEX idx_user_id (user_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String insertUser = "INSERT INTO interview_sql_user (username, email, balance) VALUES "
                + "('zhangsan', 'zhangsan@example.com', 1000.00),"
                + "('lisi', 'lisi@example.com', 2500.50);";

        String insertOrder = "INSERT INTO interview_sql_order (user_id, amount, status) VALUES "
                + "(1, 299.00, 'PAID'),"
                + "(1, 499.00, 'PAID'),"
                + "(2, 88.00, 'PENDING');";

        DbConnectionHelper.executeSqlScript(dropOrderTable, dropUserTable, createUserTable, createOrderTable, insertUser, insertOrder);

        // 2. 执行典型 SQL 关联聚合查询
        String joinAggregationSql = "SELECT "
                + "  u.id AS user_id, "
                + "  u.username, "
                + "  u.balance, "
                + "  COUNT(o.order_id) AS total_orders, "
                + "  COALESCE(SUM(o.amount), 0.00) AS total_spent "
                + "FROM interview_sql_user u "
                + "LEFT JOIN interview_sql_order o ON u.id = o.user_id AND o.status = 'PAID' "
                + "GROUP BY u.id, u.username, u.balance;";

        DbConnectionHelper.printQueryResults("SQL 优势特性：关系模型、复杂 JOIN 连表聚合与 ACID 数据一致性保障", joinAggregationSql);

        // 3. 核心对比总结打印
        printConceptualDifferences();
    }

    private static void printConceptualDifferences() {
        System.out.println("---------------- 对比维度总结 ----------------");
        System.out.println("1. 数据模型: SQL 强调关系完整性、外键、规范化; NoSQL 强调文档反范式(如 MongoDB 内嵌文档)或 Key-Value 极速存取(Redis)。");
        System.out.println("2. 一致性权衡: SQL 适合金融、订单、资金结算(ACID); NoSQL 适合秒杀库存扣减、热点缓存、社交 Feed 流、海量日志(BASE/最终一致)。");
        System.out.println("3. 架构结合: 现代企业级架构采用 Polyglot Persistence (多语言/多数据源持久化)：");
        System.out.println("   - MySQL 作为 Source of Truth (持久化事实数据源)");
        System.out.println("   - Redis 作为 高速缓存、分布式锁与计数器");
        System.out.println("   - Elasticsearch 作为 复杂全文搜索与组合过滤");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

package com.jinlin.mysqlandredis.mysql.sqlbase;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 01: NOSQL 和 SQL 的区别？
 * 
 * 说明：数据表结构及初始化数据已移至 /sql/01_mysql_basics_schema.sql 统一由 DataGrip 预先创建维护，
 *       本 Java 类专注面试题核心区别剖析与多表关联聚合查询实机验证。
 * 
 * 核心对比点：
 * 1. 数据模型与 Schema：SQL 严格预定义关系模式；NoSQL 动态无模式(键值/文档/列族/图)。
 * 2. 事务模型：SQL 严格遵循 ACID (原子性/一致性/隔离性/持久性)；NoSQL 遵循 CAP/BASE 最终一致性。
 * 3. 扩展能力：SQL 纵向提升单机硬件(Scale-up)；NoSQL 线性横向分片(Scale-out)。
 * 4. 查询与复杂关联：SQL 支持多表 JOIN、嵌套子查询与聚合；NoSQL 缺乏跨集合 JOIN，偏好嵌套冗余。
 */
public class Topic01_NoSqlVsSqlExplanation {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 01】NOSQL 和 SQL 的核心区别深度解析与实机演示");
        System.out.println("====================================================================");

        // 执行典型 SQL 关联聚合查询 (验证关系模型对复杂分析与 ACID 数据一致性的强大支持)
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


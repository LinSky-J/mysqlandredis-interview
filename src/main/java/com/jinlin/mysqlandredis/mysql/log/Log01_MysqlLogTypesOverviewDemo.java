package com.jinlin.mysqlandredis.mysql.log;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 【面试题 01】日志文件是分成了哪几种？
 *
 * 核心考点深度剖析：
 * MySQL 架构体系中存在 7 大核心日志文件，分布在 Server 层和 Storage Engine 层：
 *
 * 1. Redo Log (重做日志 - 引擎层特有)：
 *    - 属于 InnoDB 存储引擎特有，记录物理页修改。采用 WAL 机制，保证 MySQL 的崩溃恢复能力 (Crash-Safe) 与持久性 (Durability)。
 * 2. Undo Log (回滚日志 - 引擎层特有)：
 *    - 属于 InnoDB 存储引擎特有，记录数据逻辑修改的逆操作。用于事务的原子性回滚 (Rollback) 与 MVCC 多版本一致性快照读。
 * 3. Binlog (归档/二进制日志 - Server 层共有)：
 *    - 属于 Server 层所有存储引擎共有，记录所有修改表结构与表数据的 SQL 逻辑变更。用于主从复制 (Replication) 与数据时间点恢复 (Point-in-Time Recovery)。
 * 4. Error Log (错误日志 - Server 层)：
 *    - 记录 MySQL 服务启动、运行、关闭过程中的严重告警与错误信息，排查服务宕机崩溃的第一现场。
 * 5. Slow Query Log (慢查询日志 - Server 层)：
 *    - 记录执行耗时超过 `long_query_time` 秒或未使用索引的 SQL 语句，性能调优定位慢 SQL 的基石。
 * 6. General Query Log (通用查询日志 - Server 层)：
 *    - 记录客户端向 MySQL 服务器发送的所有 SQL 语句(包括建立连接和查询)，一般仅调试时开启，生产常关以防 I/O 暴增。
 * 7. Relay Log (中继日志 - 从库专属)：
 *    - 在主从复制架构中由从库 I/O 线程从主库拉取 binlog 并保存在本地，供从库 SQL 回放线程重放以实现主从数据同步。
 */
public class Log01_MysqlLogTypesOverviewDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 01】MySQL 7 大日志体系多维度归纳与全景实测");
        System.out.println("====================================================================");

        System.out.println(">>> 1. MySQL 核心日志分类全景矩阵：");
        System.out.println("----------------------------------------------------------------------------------------------------------");
        System.out.printf("%-14s | %-10s | %-12s | %-14s | %-35s%n",
                "日志名称", "所属层级", "记录类型", "写入特性", "核心解决痛点");
        System.out.println("----------------------------------------------------------------------------------------------------------");
        System.out.printf("%-14s | %-10s | %-12s | %-14s | %-35s%n", "Redo Log",   "InnoDB引擎", "物理日志(页修改)", "顺序循环覆盖写", "WAL 崩溃恢复 Crash-Safe 与事务持久性");
        System.out.printf("%-14s | %-10s | %-12s | %-14s | %-35s%n", "Undo Log",   "InnoDB引擎", "逻辑日志(反操作)", "追加写/MVCC链",  "事务原子性回滚与 MVCC 快照读版本链");
        System.out.printf("%-14s | %-10s | %-12s | %-14s | %-35s%n", "Binlog",     "Server层",  "逻辑日志(变更)",   "追加写永不覆盖", "主从复制与 Point-in-Time 灾难数据恢复");
        System.out.printf("%-14s | %-10s | %-12s | %-14s | %-35s%n", "Relay Log",  "从库Server", "逻辑日志(从Binlog)", "追加写/自动清理", "从库中继回放主库事务");
        System.out.printf("%-14s | %-10s | %-12s | %-14s | %-35s%n", "Error Log",  "Server层",  "文本日志",         "追加写入",       "服务器崩溃、启动与异常告警排查");
        System.out.printf("%-14s | %-10s | %-12s | %-14s | %-35s%n", "Slow Log",   "Server层",  "文本日志",         "追加写入",       "定位执行超时 SQL，性能分析调优");
        System.out.printf("%-14s | %-10s | %-12s | %-14s | %-35s%n", "General Log","Server层",  "文本日志",         "追加写入",       "审计客户端连接与全量查询历史");
        System.out.println("----------------------------------------------------------------------------------------------------------");

        // 实机核验 MySQL 当前各种日志开关状态
        DbConnectionHelper.printQueryResults("当前 MySQL 实例关键日志参数配置实机核验",
                "SHOW VARIABLES WHERE Variable_name IN ('log_error', 'slow_query_log', 'general_log', 'log_bin');"
        );
    }
}

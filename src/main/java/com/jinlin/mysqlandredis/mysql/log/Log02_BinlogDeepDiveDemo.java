package com.jinlin.mysqlandredis.mysql.log;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 【面试题 02】讲一下 Binlog (归档日志)？
 *
 * 核心考点深度剖析：
 * 1. 什么是 Binlog？
 *    - Binlog (Binary Log，二进制日志) 是 MySQL **Server 层**维护的逻辑日志，记录了所有修改数据库表结构(DDL)与表数据(DML)的语句事件。
 *    - 属于所有存储引擎(InnoDB / MyISAM / Memory)通用的日志体系。
 *
 * 2. Binlog 的三大核心特征：
 *    ① 逻辑日志：记录的是 SQL 语句的逻辑变动或行级数据的更新前后镜像。
 *    ② 追加写入：写满一个文件(如 `binlog.000001`，由 `max_binlog_size` 控制)后，自动开启新文件追加写，历史日志永久保留不被覆盖。
 *    ③ 核心用途：
 *       - 主从复制 (Replication)：从库通过 I/O 线程读取主库 binlog 事件，回放实现数据实时同步；
 *       - 数据恢复 (PITR)：结合全量物理备份与指定时间点的 binlog，可精确将数据库恢复到任意秒级历史状态。
 *
 * 3. Binlog 的 3 种记录格式 (binlog_format)：
 *    - STATEMENT (基于语句复制)：
 *      记录原始 SQL 语句。优点是日志量极小、I/O 开销低；致命缺点是若 SQL 包含动态函数(如 `UUID()`, `NOW()`, `RAND()`)，从库重放时生成的值与主库不一致，引发主从数据分叉！
 *    - ROW (基于行级变化)：
 *      记录每一行被修改前后的所有字段精准物理值。优点是绝对保证主从严格一致；缺点是日志量相对较大。
 *      【业界规范】：现代高并发企业级生产环境标配 `binlog_format = ROW`！
 *    - MIXED (混合模式)：
 *      默认用 STATEMENT，检测到可能引发主从不一致的非确定性函数时自动转为 ROW。
 *
 * 4. 关键刷盘参数 `sync_binlog`：
 *    - `0`：事务提交时仅写入操作系统内核缓存 (OS Page Cache)，不调用 fsync，由 OS 决定刷盘时机 (性能最高但宕机易丢数据)；
 *    - `1`：每次事务提交都必须执行 `fsync` 刷盘 (金融级强一致，配合 `innodb_flush_log_at_trx_commit=1` 构成双 1 保障)；
 *    - `N`：累积 N 个事务后执行一次 fsync 刷盘。
 */
public class Log02_BinlogDeepDiveDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 02】Binlog 核心架构、三种格式与刷盘机制深度剖析");
        System.out.println("====================================================================");

        System.out.println(">>> 1. Binlog 三种格式对比分析表：");
        System.out.println("----------------------------------------------------------------------------------");
        System.out.printf("%-12s | %-12s | %-15s | %-25s%n", "格式类型", "日志文件体积", "主从一致性保证", "生产推荐级别");
        System.out.println("----------------------------------------------------------------------------------");
        System.out.printf("%-12s | %-12s | %-15s | %-25s%n", "STATEMENT", "极小", "不可靠 (动态函数分叉)", "淘汰弃用");
        System.out.printf("%-12s | %-12s | %-15s | %-25s%n", "ROW",       "较大", "绝对精准一致 (100%)",  "★★★★★ 生产标准推荐");
        System.out.printf("%-12s | %-12s | %-15s | %-25s%n", "MIXED",     "中等", "较好 (智能切换)",      "★★★ 兼容过渡");
        System.out.println("----------------------------------------------------------------------------------");

        // 实机查询当前 MySQL 实例的 Binlog 配置
        DbConnectionHelper.printQueryResults("当前 MySQL 实例 Binlog 核心参数实机核验",
                "SHOW VARIABLES WHERE Variable_name IN ('log_bin', 'binlog_format', 'sync_binlog', 'max_binlog_size');"
        );
    }
}

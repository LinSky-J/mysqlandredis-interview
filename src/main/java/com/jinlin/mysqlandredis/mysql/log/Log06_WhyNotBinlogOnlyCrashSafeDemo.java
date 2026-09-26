package com.jinlin.mysqlandredis.mysql.log;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 【面试题 06】能不能只用 Binlog，不用 Redo Log？
 *
 * 核心考点深度剖析 (大厂高频致命逻辑质问)：
 * 【核心答案】：【绝对不能！因为 Binlog 天然不具备 Crash-Safe (崩溃恢复) 能力！】
 *
 * 为什么 Binlog 无法替代 Redo Log 实现崩溃恢复？四大根本原因：
 *
 * 1. 根本原因一：缺乏物理脏页状态感知与 Checkpoint (检查点) 机制
 *    - 在 InnoDB 运行期间，内存 Buffer Pool 中的数据页被修改后变成【脏页 (Dirty Pages)】，由后台线程异步离散刷盘；
 *    - 当突发断电时，内存中哪些数据页已经落盘了？哪些数据页完全没落盘？【Binlog 对此彻底一无所知】！
 *    - 而 Redo Log 维护了严格的 Checkpoint (检查点) 与 LSN (Log Sequence Number，日志逻辑序列号)。
 *      磁盘数据页头部同样记录了该页最后修改的 LSN。重启时，InnoDB 只要比对 `Page LSN` 和 `Redo Log LSN`，
 *      就能精准定位出哪几个数据页未落盘，从而前滚重做！Binlog 完全不具备这种机制。
 *
 * 2. 根本原因二：物理日志 VS 逻辑日志的恢复精度不同
 *    - Binlog 是【逻辑日志】(记录“把 id=1 的 balance 增加 100”)；
 *    - Redo Log 是【物理日志】(记录“在表空间 2、数据页 58、偏移量 120 处写入二进制字节 0x0F”)。
 *    - 若数据页在断电时处于不确定状态，逻辑日志直接重放会导致重复累加或计算错误，而物理日志具备幂等性与精准物理重放能力。
 *
 * 3. 根本原因三：空间写入模式不同 (环形覆盖 VS 无限追加)
 *    - Redo Log 是固定大小的环形缓冲区 (Circular Buffer)，写满即推动 Checkpoint 并覆盖历史已刷盘日志；
 *    - Binlog 是无限追加写入文件，用于长期归档与历史同步。
 *
 * 4. 架构分层历史原因：
 *    - Binlog 是 MySQL Server 层的机制，早期 MySQL 默认引擎是 MyISAM (不支持事务)；
 *    - InnoDB 是以插件形式引入的第三方高性能事务存储引擎，为了实现 ACID 和 Crash-Safe，
 *      必须在引擎内部自带一套完整的物理 Redo Log 与 WAL 机制。
 */
public class Log06_WhyNotBinlogOnlyCrashSafeDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 06】为什么不能只用 Binlog 而不用 Redo Log？(Crash-Safe 底层揭秘)");
        System.out.println("====================================================================");

        System.out.println(">>> Binlog 与 Redo Log 核心维度终极对比：");
        System.out.println("--------------------------------------------------------------------------------------------------");
        System.out.printf("%-14s | %-16s | %-16s | %-18s | %-16s%n",
                "对比维度", "Redo Log (重做日志)", "Binlog (归档日志)", "能否单靠该日志恢复", "技术核心原因");
        System.out.println("--------------------------------------------------------------------------------------------------");
        System.out.printf("%-14s | %-16s | %-16s | %-18s | %-16s%n", "所属架构层级", "InnoDB 存储引擎特有", "MySQL Server 层共有", "—", "分层架构设计");
        System.out.printf("%-14s | %-16s | %-16s | %-18s | %-16s%n", "记录物理内容", "物理页字节偏移量修改", "SQL 语句或行级镜像", "—", "物理 vs 逻辑");
        System.out.printf("%-14s | %-16s | %-16s | %-18s | %-16s%n", "崩溃恢复能力", "具备 (Crash-Safe)",   "不具备 (无感知脏页)", "Redo 胜任", "含 Checkpoint/LSN");
        System.out.printf("%-14s | %-16s | %-16s | %-18s | %-16s%n", "空间管理模式", "固定大小，循环覆盖写", "追加写入，归档保留",  "—", "实时WAL vs 历史备份");
        System.out.println("--------------------------------------------------------------------------------------------------");

        System.out.println("\n>>> 【核心结论总结】：");
        System.out.println("1. Redo Log 赋予了 InnoDB 强大的崩溃恢复能力 (Crash-Safe)，使数据库能从异常宕机中完好无损地复原；");
        System.out.println("2. Binlog 负责主从复制与点对点归档恢复，无法感知内存数据页刷盘状态，因此两者绝不可互相替代！");

        DbConnectionHelper.printQueryResults("当前 MySQL 实例 Redo Log 文件组状态核查",
                "SHOW VARIABLES LIKE 'innodb_log_file%';"
        );
    }
}

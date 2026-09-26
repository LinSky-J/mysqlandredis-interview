package com.jinlin.mysqlandredis.mysql.log;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 【面试题 05】Redo Log 怎么保证持久性的？(WAL 与三大刷盘策略)
 *
 * 核心考点深度剖析：
 * Redo Log (重做日志) 能够保障 ACID 中的持久性 (Durability)，核心依托于两大支柱：
 *
 * 一、 支柱一：WAL 机制 (Write-Ahead Logging，先写日志后写磁盘)
 * 1. 为什么不直接把修改后的数据页写回磁盘？
 *    - 因为将 Buffer Pool 中的 16KB 数据页刷回磁盘属于【随机 I/O】，开销巨大、性能极慢；
 * 2. WAL 的核心思想：
 *    - 当数据在内存中被修改(产生脏页)后，只要保证记录该物理修改的 Redo Log 【已经顺序写入磁盘】，该事务即可向客户端宣告提交成功！
 *    - 真正的 16KB 物理脏页可以由后台的 Page Cleaner 线程在稍后闲暇时异步慢慢刷盘。
 *    - 即使异步刷盘前服务器突发断电，由于磁盘上已有完整的 Redo Log，重启时前滚重做即可恢复！
 *
 * 二、 支柱二：三大刷盘控制参数 `innodb_flush_log_at_trx_commit`
 * Redo Log 从内存写入物理磁盘需经过三层结构：
 * [Redo Log Buffer (内存用户空间)] -> [OS Page Cache (操作系统内核缓存)] -> [物理磁盘 (ib_logfile)]
 *
 * 参数取值剖析：
 * 1. `0` (延迟刷盘，性能最高，安全性最低)：
 *    - 事务提交时不进行任何 write 和 fsync，仅写入 Redo Log Buffer；
 *    - 由后台主线程 Master Thread 每秒定时执行一次 write + fsync 刷入磁盘；
 *    - 风险：若 MySQL 进程崩溃或服务器宕机，最多会丢失过去 1 秒内的所有事务数据！
 *
 * 2. `1` (默认值，金融级强安全，性能适中)：
 *    - 事务每次提交都必须强制调用系统的 `write` + `fsync` 操作，将日志直接物理落盘并等待磁盘写入确认；
 *    - 收益：绝对满足 ACID 的持久性 (Durability)，即使操作系统崩溃断电，数据也 100% 绝不丢失！
 *
 * 3. `2` (折中模式，OS 缓存缓冲)：
 *    - 事务提交时调用系统的 `write` 将数据写入操作系统的内核缓存 (OS Page Cache)，但不调用 fsync；
 *    - 每秒由后台线程调用一次 fsync 实际落盘；
 *    - 收益：如果仅仅是 MySQL 服务挂了而操作系统没死，OS Page Cache 不会丢失，重启后依然能恢复；只有整台机器物理断电才会丢失 1 秒数据。
 */
public class Log05_RedoLogDurabilityWalDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 05】Redo Log WAL 机制与三大刷盘参数深度核验");
        System.out.println("====================================================================");

        System.out.println(">>> 1. `innodb_flush_log_at_trx_commit` 三大刷盘模式对比矩阵：");
        System.out.println("----------------------------------------------------------------------------------------------------");
        System.out.printf("%-6s | %-20s | %-16s | %-18s | %-25s%n",
                "参数值", "事务提交时行为", "崩溃丢失数据风险", "I/O 性能表现", "典型适用业务场景");
        System.out.println("----------------------------------------------------------------------------------------------------");
        System.out.printf("%-6s | %-20s | %-16s | %-18s | %-25s%n",
                "0", "不写OS缓存，不刷盘", "丢失 1 秒数据", "极高 (纯内存写入)", "日志采集、流水埋点等非核心业务");
        System.out.printf("%-6s | %-20s | %-16s | %-18s | %-25s%n",
                "1", "写OS缓存 + 强制fsync", "零丢失 (100%安全)", "标准 (受磁盘IOPS约束)", "★★★★★ 生产标准推荐 (金融核心)");
        System.out.printf("%-6s | %-20s | %-16s | %-18s | %-25s%n",
                "2", "写OS缓存，暂不fsync", "物理断电丢1秒", "很高 (无同步fsync阻塞)", "高并发要求、可容忍微量丢失业务");
        System.out.println("----------------------------------------------------------------------------------------------------");

        // 实机核验当前 MySQL 实例的刷盘策略
        DbConnectionHelper.printQueryResults("当前 MySQL 实例 Redo Log 刷盘参数核对",
                "SHOW VARIABLES WHERE Variable_name IN ('innodb_flush_log_at_trx_commit', 'innodb_log_buffer_size');"
        );
    }
}

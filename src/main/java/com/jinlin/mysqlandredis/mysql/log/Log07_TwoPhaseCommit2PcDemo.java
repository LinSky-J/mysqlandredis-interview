package com.jinlin.mysqlandredis.mysql.log;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 【面试题 07】Binlog 与 Redo Log 两阶段提交 (2PC) 过程是怎样的？
 *
 * 核心考点深度剖析 (大厂面试压轴架构考点)：
 *
 * 一、 为什么必须引入两阶段提交 (Two-Phase Commit, 2PC)？
 * 目标：【保证 Redo Log (物理引擎日志) 与 Binlog (逻辑服务日志) 的绝对原子一致性，防止主从数据分叉与主备不一致！】
 *
 * 若不采用两阶段提交的严重后果分析：
 * 1. 假设“先写 Redo Log，后写 Binlog”：
 *    - 事务写入 Redo Log 后突发宕机，Binlog 尚未写入。
 *    - 主库重启：依据 Redo Log 前滚恢复了该事务 (数据存在)；
 *    - 从库同步：由于 Binlog 没有记录该事务，从库从未同步执行 (数据丢失)；
 *    - 结果：主从数据严重不一致！
 * 2. 假设“先写 Binlog，后写 Redo Log”：
 *    - 事务写入 Binlog 后突发宕机，Redo Log 尚未写入。
 *    - 主库重启：因无 Redo Log，主库将该事务视为未完成而回滚 (数据丢失)；
 *    - 从库同步：从库读取到 Binlog 并成功重放了该事务 (数据存在)；
 *    - 结果：从库凭空多出数据，主从数据依然严重不一致！
 *
 * 二、 两阶段提交全链路执行过程：
 * 步骤 ① 【Prepare 阶段】：
 *     - 执行器调用引擎事务接口，InnoDB 将事务物理变更写入 Redo Log 并刷盘，并将 Redo Log 的状态标记置为 `prepare`；
 * 步骤 ② 【Commit 阶段 (分两步)】：
 *     - 第一步：Server 层执行器将该事务的逻辑事件写入 Binlog 文件并调用 `fsync` 刷盘 (写入全局统一事务 XID)；
 *     - 第二步：执行器调用存储引擎提交接口，InnoDB 将该事务在 Redo Log 中的状态改写为 `commit`，事务正式终结。
 *
 * 三、 崩溃恢复时的判定核心准绳 (以 Binlog 是否写入为分水岭)：
 * - 【断电发生在写 Binlog 之前】：
 *   重启时扫描发现 Redo Log 为 `prepare` 状态，但拿着 XID 查询 Binlog 发现【不存在该事件】-> 直接调用 `ROLLBACK` 回滚！
 * - 【断电发生在写 Binlog 之后、Redo commit 之前】：
 *   重启时扫描发现 Redo Log 为 `prepare` 状态，拿着 XID 查询 Binlog 发现【存在完整的该事件】-> 直接调用 `COMMIT` 提交！
 * 完美实现了主库物理状态与从库复制逻辑的 100% 严丝合缝！
 */
public class Log07_TwoPhaseCommit2PcDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 07】Binlog 与 Redo Log 两阶段提交 (2PC) 机制与崩溃决策矩阵");
        System.out.println("====================================================================");

        System.out.println(">>> 1. 两阶段提交 (2PC) 崩溃恢复判定矩阵：");
        System.out.println("--------------------------------------------------------------------------------------------------");
        System.out.printf("%-18s | %-16s | %-16s | %-14s | %-18s%n",
                "宕机崩溃发生时机", "Redo Log 状态", "Binlog 完整性", "重启恢复决策", "主从数据一致性");
        System.out.println("--------------------------------------------------------------------------------------------------");
        System.out.printf("%-18s | %-16s | %-16s | %-14s | %-18s%n",
                "写 Binlog 之前宕机", "prepare 状态", "无该事务 (缺失)", "ROLLBACK 回滚", "一致 (主从均无该数据)");
        System.out.printf("%-18s | %-16s | %-16s | %-14s | %-18s%n",
                "写 Binlog 之后宕机", "prepare 状态", "已完整写入 (含XID)", "COMMIT 自动提交", "一致 (主从均有该数据)");
        System.out.println("--------------------------------------------------------------------------------------------------");

        System.out.println("\n>>> 2. 组提交 (Group Commit) 性能优化机制：");
        System.out.println("   为了解决每个事务 2PC 都要进行两次 fsync 带来的磁盘 I/O 吞吐瓶颈，");
        System.out.println("   MySQL 引入了 Binlog 组提交机制 (FLUSH / SYNC / COMMIT 三阶段队列)，");
        System.out.println("   将多个并发事务的 Binlog 刷盘合并为单次 fsync，大幅提升高并发写吞吐！");

        DbConnectionHelper.printQueryResults("当前 MySQL 实例组提交参数核对",
                "SHOW VARIABLES LIKE 'binlog_group_commit%';"
        );
    }
}

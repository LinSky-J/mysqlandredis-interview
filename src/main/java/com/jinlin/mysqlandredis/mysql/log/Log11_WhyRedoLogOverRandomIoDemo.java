package com.jinlin.mysqlandredis.mysql.log;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 【面试题 11】为什么要写 Redo Log，而不是直接写到 B+ 树里面？
 *
 * 核心考点深度剖析 (经典系统架构设计哲学)：
 * 面试官常问：“既然事务最终都要把修改保存到 B+ 树数据文件 (.ibd) 中，为什么不省去中间商，直接在事务提交时把修改写进 B+ 树，反而要多写一次 Redo Log 呢？”
 *
 * 核心答案：【磁盘顺序 I/O (Sequential I/O) VS 磁盘随机 I/O (Random I/O) 的降维打击！】
 *
 * 如果直接写 B+ 树的致命弊端：
 * 1. 致命弊端一：极度高昂的磁盘随机 I/O 开销 (Random I/O)
 *    - B+ 树为了维护索引有序性，各个数据页在物理磁盘上的分布高度离散；
 *    - 如果每次事务提交都要直接写 B+ 树，磁头必须在磁盘的不同扇区之间来回剧烈寻道(Seek)和旋转延迟；
 *    - 机械硬盘单次寻道耗时 5~10 ms (每秒最多 100~200 次 I/O)，即使 NVMe SSD 在高并发离散写入下延迟也会剧增，系统吞吐量瞬间雪崩！
 *
 * 2. 致命弊端二：触目惊心的写放大 (Write Amplification)
 *    - InnoDB 与磁盘交互的最小物理单位是【16KB 数据页】；
 *    - 即使事务仅仅修改了一个 INT 字段 (4 字节)，若直接写 B+ 树，也必须将整整 16,384 字节的整张数据页全部刷盘写回！
 *    - 写放大比例高达 4000:1，极度浪费磁盘带宽与 I/O 寿命。
 *
 * 写 Redo Log 带来的降维提升：
 * 1. 纯粹的磁盘顺序追加写 (Append-Only)：
 *    - Redo Log 是在连续磁盘扇区末尾顺序写，磁头无需寻道，顺序 I/O 的吞吐速度接近内存级别；
 * 2. 极小的物理字节开销：
 *    - Redo Log 只记录本次修改的物理差量日志(仅几十字节)，相比刷写 16KB 数据页节省了 99.9% 的数据写入量；
 * 3. 终极架构设计美学：
 *    - “内存 Buffer Pool 吸收高频随机读写” + “Redo Log WAL 顺序写高速持久化保底” + “Page Cleaner 稍后异步合并刷脏页回 B+ 树”！
 */
public class Log11_WhyRedoLogOverRandomIoDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 11】为什么先写 Redo Log 而不是直接写 B+ 树？(顺序I/O原理剖析)");
        System.out.println("====================================================================");

        System.out.println(">>> 顺序追加写 Redo Log VS 离散写 B+ 树数据页对比：");
        System.out.println("--------------------------------------------------------------------------------------------------");
        System.out.printf("%-16s | %-16s | %-16s | %-18s | %-20s%n",
                "持久化写入方式", "I/O 模式", "单次写入数据量", "单次提交耗时", "高并发吞吐能力 (TPS)");
        System.out.println("--------------------------------------------------------------------------------------------------");
        System.out.printf("%-16s | %-16s | %-16s | %-18s | %-20s%n",
                "直接写 B+ 树", "磁盘随机 I/O (寻道)", "完整 16KB 数据页", "数毫秒级 (极慢)", "低 (几百 TPS 触顶)");
        System.out.printf("%-16s | %-16s | %-16s | %-18s | %-20s%n",
                "写 Redo Log (WAL)", "磁盘顺序追加写", "物理差量 (几十字节)", "微秒级 (极快)", "高 (数万甚至数十万 TPS)");
        System.out.println("--------------------------------------------------------------------------------------------------");

        // 实机核验 InnoDB Buffer Pool 数据页大小与刷新机制
        DbConnectionHelper.printQueryResults("当前 MySQL 实例数据页大小与 Buffer Pool 状态",
                "SHOW VARIABLES WHERE Variable_name IN ('innodb_page_size', 'innodb_buffer_pool_size', 'innodb_io_capacity');"
        );
    }
}

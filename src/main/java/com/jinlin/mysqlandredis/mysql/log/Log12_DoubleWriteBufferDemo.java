package com.jinlin.mysqlandredis.mysql.log;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 【面试题 12】MySQL 两次写 (Doublewrite Buffer) 了解吗？
 *
 * 核心考点深度剖析 (底层存储物理安全顶级考点)：
 *
 * 一、 解决的致命痛点：【页断裂 / 部分写失效 (Partial Page Write)】
 * 1. 为什么会发生页断裂？
 *    - InnoDB 的数据页管理单位是【16KB】；
 *    - 操作系统的文件系统块或物理磁盘扇区通常是【4KB】；
 *    - 当 InnoDB 将一个 16KB 的脏页刷写到磁盘 `.ibd` 文件时，底层需要调用 4 次独立的 4KB 扇区写入；
 *    - 若在写到第 2 个 4KB 扇区时(已写 8KB)，服务器突发停电、跳闸或内核宕机：
 *      磁盘上的该数据页只有一半是新数据、一半是旧数据，整页被【物理撕裂损坏 (页断裂)】，校验和 (Checksum) 彻底失效！
 *
 * 2. 为什么此时不能直接用 Redo Log 恢复？(高频面试陷阱)
 *    - 【核心原因】：Redo Log 记录的是物理页偏移量的【增量差量修改】(例如“在页偏移量 120 处写入 4 字节”)，
 *      它的重放前提是：【目标物理页必须是一个结构合法、校验和正确的完整基底页】！
 *    - 若数据页本身已经断裂损坏，在其上套用偏移量修改只会导致整张数据表彻底报废！
 *
 * 二、 Doublewrite Buffer (两次写) 架构与工作流程：
 * 1. 结构组成：
 *    - 内存部分：内存中的 Doublewrite Buffer，大小为 2MB (共 128 个页)；
 *    - 磁盘部分：系统表空间 (`ibdata1` 或独立双写文件) 中连续的 2MB 物理扇区 (共 128 个连续页)。
 * 2. 脏页落盘两步走：
 *    - 步骤 ① (第一写，连续顺序写)：
 *      脏页准备刷盘时，先复制到内存 Doublewrite Buffer，随后一次性【顺序写入】系统表空间的 2MB 连续磁盘区域，并调用 fsync 确认 (顺序 I/O 耗时极短)；
 *    - 步骤 ② (第二写，离散写回)：
 *      Doublewrite 刷盘成功后，再将脏页真正【离散写回】各个表各自对应的 `.ibd` 物理数据文件中。
 *
 * 三、 发生页断裂时的灾难恢复流程：
 * - 若在步骤 ② 写入 `.ibd` 时突发断电导致页断裂损坏：
 * - 重启时，InnoDB 发现数据页 Checksum 校验失败；
 * - InnoDB 立即从磁盘 Doublewrite 连续空间中，找到该页完好无损的【16KB 完整副本】，先还原覆盖回损坏的 `.ibd` 文件；
 * - 恢复出完整基底页后，再调用 Redo Log 进行正常的物理前滚重做！
 */
public class Log12_DoubleWriteBufferDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 12】MySQL 双写缓冲 (Doublewrite Buffer) 架构与页断裂防御全解");
        System.out.println("====================================================================");

        System.out.println(">>> 1. 两次写 (Doublewrite Buffer) 解决页断裂时序全景：");
        System.out.println("   [Buffer Pool 16KB 脏页] -> 复制至内存 Doublewrite Buffer (2MB)");
        System.out.println("   -> 【第一写】: 一次性连续顺序写磁盘 Doublewrite 区域 (极速落盘备份)");
        System.out.println("   -> 【第二写】: 离散写回用户表各自的 `.ibd` 数据文件");
        System.out.println("   -> 【若发生断电页断裂】: 从 Doublewrite 区域还原 16KB 副本 -> 再用 Redo Log 前滚重做！");

        // 实机核验 Doublewrite Buffer 开关配置
        DbConnectionHelper.printQueryResults("当前 MySQL 实例 Doublewrite Buffer 运行参数",
                "SHOW VARIABLES WHERE Variable_name IN ('innodb_doublewrite', 'innodb_doublewrite_pages', 'innodb_doublewrite_files');"
        );
    }
}

package com.jinlin.mysqlandredis.mysql.log;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 【面试题 10】Redo Log 是在内存里吗？
 *
 * 核心考点深度剖析：
 * 【核心答案】：【既在内存里，也在磁盘里！】
 * Redo Log 由【内存中的 Redo Log Buffer】和【磁盘上的物理文件组】两部分协同构成：
 *
 * 1. 内存形态：Redo Log Buffer (重做日志缓冲区)
 *    - 存储介质：服务器 RAM 物理内存 (属于 InnoDB Buffer Pool 旁路的核心缓冲区)；
 *    - 默认大小：通常为 16MB，由参数 `innodb_log_buffer_size` 显式控制；
 *    - 核心作用：当事务修改数据页时，物理修改首先追加缓存在内存中的 Redo Log Block (每个块 512 字节) 中，
 *               从而避免每一个微小的字节变更都立即调用操作系统 I/O 陷入内核态，极大削减 CPU 上下文切换与磁盘压力。
 *
 * 2. 磁盘形态：物理 Redo Log 文件组
 *    - 存储介质：服务器持久化存储介质 (SSD / NVMe / HDD)；
 *    - 物理文件名：
 *      ① MySQL 8.0.30 之前：数据目录下的 `ib_logfile0`, `ib_logfile1` 等固定文件组；
 *      ② MySQL 8.0.30+：引入动态 Redo Log 架构，自动存放在 `#innodb_redo/` 目录下的专用文件中；
 *    - 组织形式：环形覆盖写 (Circular Buffer)，由 `write pos` (当前写入位置) 与 `checkpoint` (已擦除落盘位置) 两大指针循环推进。
 *
 * 3. 内存与磁盘的流转时机：
 *    - 事务提交时 (根据 `innodb_flush_log_at_trx_commit=1`)；
 *    - Redo Log Buffer 内存占用超过 50% 时；
 *    - 后台 Master Thread 线程每秒定时刷盘；
 *    - MySQL 服务正常关闭时。
 */
public class Log10_RedoLogInMemAndDiskDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 10】Redo Log 内存缓冲区与磁盘物理文件架构核查");
        System.out.println("====================================================================");

        System.out.println(">>> 内存 Redo Log Buffer VS 磁盘 Redo Log Files 对比：");
        System.out.println("-------------------------------------------------------------------------------------------------");
        System.out.printf("%-18s | %-16s | %-20s | %-25s%n", "架构形态", "物理介质", "典型容量规模", "核心职能");
        System.out.println("-------------------------------------------------------------------------------------------------");
        System.out.printf("%-18s | %-16s | %-20s | %-25s%n", "Redo Log Buffer", "RAM 内存", "默认 16MB (可调)", "内存级极速写入，合并多个细小日志块");
        System.out.printf("%-18s | %-16s | %-20s | %-25s%n", "Redo Log Files",  "磁盘存储", "几百 MB 到几十 GB", "持久化保障，崩溃时前滚重做恢复");
        System.out.println("-------------------------------------------------------------------------------------------------");

        // 实机核验当前 MySQL 实例的 Redo Log Buffer 内存参数与文件大小配置
        DbConnectionHelper.printQueryResults("当前 MySQL 实例 Redo Log 内存与物理文件参数",
                "SHOW VARIABLES WHERE Variable_name IN ('innodb_log_buffer_size', 'innodb_log_file_size', 'innodb_redo_log_capacity');"
        );
    }
}

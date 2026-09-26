package com.jinlin.mysqlandredis.mysql.log;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 【面试题 09】MySQL 是如何保障数据不丢失的？
 *
 * 核心考点深度剖析 (金融级数据安全保障体系)：
 * 要在生产环境中实现数据零丢失 (RPO=0)，MySQL 从【单机存储引擎】到【分布式集群】构建了四重铁壁防护体系：
 *
 * 第一重防护：单机黄金准则 —— “双 1 配置” (Double 1)
 * 1. `innodb_flush_log_at_trx_commit = 1`：
 *    - 每一个事务提交时，必须强制将该事务的 Redo Log 真实调用 `fsync` 刷入磁盘物理介质。
 *    - 彻底杜绝因操作系统崩溃或服务器断电造成的未提交/已提交事务数据丢失。
 * 2. `sync_binlog = 1`：
 *    - 每一个事务提交时，必须强制将该事务的 Binlog 真实调用 `fsync` 刷入磁盘物理介质。
 *    - 确保主从复制事件与全量归档日志绝对不漏掉任何一条已提交事务。
 *
 * 第二重防护：两阶段提交 (2PC, Two-Phase Commit)
 * - 协调 Redo Log 与 Binlog 的写入顺序与原子性，杜绝单机恢复与从库重放之间的数据逻辑不一致。
 *
 * 第三重防护：双写缓冲机制 (Doublewrite Buffer)
 * - 解决 16KB 数据页在写入物理磁盘中途断电引发的“页断裂 (Partial Page Write)”，保证物理数据页底层的完整性。
 *
 * 第四重防护：高可用集群级防线 —— 半同步复制 (Semi-Sync) 与 MGR
 * - 避免单点硬件毁灭(如机房断电、硬盘彻底烧毁)导致数据丢失：
 *   主库提交事务前，必须确保至少一个从库已经成功接收 Binlog 并写入本地 Relay Log 确认 (ACK)，才向客户端响应成功。
 */
public class Log09_DataLossPreventionDouble1Demo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 09】MySQL 数据零丢失全景保障方案 (双 1 配置与实机参数核对)");
        System.out.println("====================================================================");

        System.out.println(">>> 1. MySQL 保证数据不丢失的“四重铁壁防线”：");
        System.out.println("   [第一道防线] 双 1 强刷盘配置 (innodb_flush_log_at_trx_commit=1 + sync_binlog=1)；");
        System.out.println("   [第二道防线] 两阶段提交 (2PC) 消除 Redo Log 与 Binlog 分叉；");
        System.out.println("   [第三道防线] 双写缓冲 (Doublewrite Buffer) 防止 16KB 页断裂；");
        System.out.println("   [第四道防线] 半同步复制 (Semi-Sync) / MGR 组复制杜绝单机硬件级物理损毁。");

        // 实机核验双 1 配置
        DbConnectionHelper.printQueryResults("当前 MySQL 实例“双 1”数据安全配置实机核验",
                "SHOW VARIABLES WHERE Variable_name IN ('innodb_flush_log_at_trx_commit', 'sync_binlog', 'innodb_doublewrite');"
        );
    }
}

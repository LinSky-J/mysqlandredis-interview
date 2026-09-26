package com.jinlin.mysqlandredis.mysql.architecture;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 架构篇 01: MySQL 主从复制核心原理与三大线程模型
 *
 * 面试真题：MySQL 主从复制了解吗？
 *
 * 核心回答要点：
 * 1. 为什么需要主从复制：
 *    - 读写分离：主库(Master)承接写流量与核心事务，从库(Slave)承接高并发读流量，极大提升系统吞吐量。
 *    - 高可用与故障切换(HA)：主库宕机时可迅速将从库提升为主库，保障业务不中断。
 *    - 数据备份与离线分析：在从库进行逻辑备份(mysqldump)或跑大数据离线报表，不影响主库在线 OLTP 性能。
 *
 * 2. 主从复制核心原理（三大线程与两大日志）：
 *    - Master:
 *      - 事务提交时，主库在执行引擎层写 Redo Log，并在 Server 层写入 Binary Log (Binlog)。
 *      - 主库启动一个【Binlog Dump Thread】（二进制日志转储线程）：负责监听从库连接，当 Binlog 有新事件时，读取并推送给从库。
 *    - Slave:
 *      - 从库启动一个【I/O Thread】：连接到主库，向 Dump 线程请求指定位点(GTID或File+Position)之后的 Binlog。
 *        收到 Binlog 事件后，顺序写入从库本地的【Relay Log (中继日志)】。
 *      - 从库启动一个【SQL Thread】（或 MTS 多线程工作池）：负责从 Relay Log 中读取事件并按顺序在本地执行重放，完成数据同步。
 *
 * 3. 复制模式演进：
 *    - 异步复制 (Asynchronous Replication, 默认)：主库写入 Binlog 后立即返回客户端成功，不等待从库确认。吞吐量最高，但主库宕机存在数据丢失风险。
 *    - 半同步复制 (Semi-Synchronous Replication)：主库至少等待一个从库将 Binlog 写入 Relay Log 并返回 ACK 后才返回客户端成功（after_sync 避免幻读）。
 *    - 组复制 (Group Replication / MGR)：基于 Paxos 协议实现强一致性多主/单主集群复制。
 */
public class Arch01_MasterSlaveReplicationDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [Arch01] MySQL 主从复制架构剖析与主库 Binlog 位点探测演示");
        System.out.println("================================================================================");

        try (Connection conn = DbConnectionHelper.getConnection()) {
            // 1. 检查主库当前二进制日志状态 (SHOW MASTER STATUS / SHOW BINARY LOG STATUS)
            System.out.println("[步骤 1] 查询当前实例的 Binlog 记录状态与文件位点信息...");
            String queryStatusSql = "SHOW MASTER STATUS";
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(queryStatusSql)) {
                    if (rs.next()) {
                        String file = rs.getString("File");
                        long position = rs.getLong("Position");
                        String binlogDoDb = rs.getString("Binlog_Do_DB");
                        String binlogIgnoreDb = rs.getString("Binlog_Ignore_DB");
                        System.out.println("  -> 当前主库 Binlog 文件: " + file);
                        System.out.println("  -> 当前写入位点 Position: " + position);
                        System.out.println("  -> 过滤白名单/黑名单: Do=" + binlogDoDb + ", Ignore=" + binlogIgnoreDb);
                    } else {
                        System.out.println("  -> 当前实例未开启 log_bin 或处于单机只读副本模式。");
                    }
                }
            } catch (SQLException ex) {
                // MySQL 8.4+ 推荐使用 SHOW BINARY LOG STATUS
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SHOW BINARY LOG STATUS")) {
                    if (rs.next()) {
                        System.out.println("  -> [MySQL 8.4+] Binlog File: " + rs.getString("File")
                                + ", Position: " + rs.getLong("Position"));
                    }
                } catch (Exception ignored) {
                    System.out.println("  -> 提示: 当前测试环境单机实例模拟主从探测。");
                }
            }

            // 2. 模拟主库产生写事务，记录心跳事件
            System.out.println("\n[步骤 2] 在主库写入心跳同步流水，模拟 Binlog 事件生成...");
            long seq = System.currentTimeMillis();
            String insertSql = "INSERT INTO arch_replication_heartbeat (master_node, heartbeat_time, trx_seq, payload) " +
                    "VALUES ('master-primary-node', NOW(3), ?, 'replication_packet_sync')";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setLong(1, seq);
                pstmt.executeUpdate();
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        long insertId = generatedKeys.getLong(1);
                        System.out.println("  -> 主库心跳流水写入成功, Heartbeat ID: " + insertId + ", Seq: " + seq);
                    }
                }
            }

            // 3. 架构对比总结输出
            System.out.println("\n[步骤 3] 主从复制三大线程及模式对比小结：");
            System.out.println("  +----------------------+--------------------+------------------------------------------+");
            System.out.println("  | 线程名称             | 所在节点           | 核心职责与工作机制                       |");
            System.out.println("  +----------------------+--------------------+------------------------------------------+");
            System.out.println("  | Binlog Dump Thread   | 主库 (Master)      | 监听从库长连接，读取 Binlog 并通过网络发送 |");
            System.out.println("  | I/O Thread           | 从库 (Slave)       | 接收网络 Binlog 数据包，顺序写入 Relay Log|");
            System.out.println("  | SQL Thread (MTS)     | 从库 (Slave)       | 读取 Relay Log 事件，在本地存储引擎中重放 |");
            System.out.println("  +----------------------+--------------------+------------------------------------------+");
            System.out.println("  - 异步复制：性能最高，主库不等待从库，极端宕机可能丢数据。");
            System.out.println("  - 半同步复制(after_sync)：至少一个从库写入 Relay Log 刷盘后主库才提交，兼顾性能与安全。");
            System.out.println("  - 组复制(MGR)：Paxos 强一致性仲裁协议，支持多数派仲裁与自动主从倒换。");

        } catch (SQLException e) {
            System.err.println(">> 执行主从复制演示异常: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}

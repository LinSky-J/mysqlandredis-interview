package com.jinlin.mysqlandredis.mysql.architecture;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 架构篇 03: 数据库分库分表核心概念、本质区别与架构挑战全景剖析
 *
 * 面试真题：分表和分库是什么？有什么区别？
 *
 * 核心理论精析：
 * 1. 什么是分表 (Table Sharding)？
 *    - 垂直分表 (Vertical Table Sharding)：
 *      - 做法：根据“字段冷热”与“字段体积”，将原大表字段切分到不同的子表中。核心字段保留在主表，大文本/低频字段放到扩展表。
 *      - 目的：InnoDB 默认页大小 16KB，如果单行记录过长（包含 TEXT/VARCHAR 大字段），单页容纳行数极少，
 *             甚至触发行溢出存储。垂直分表可缩小单行字节数，使得单个 16KB 数据页容纳更多数据，提升 Buffer Pool 缓存命中率，减少磁盘 I/O。
 *    - 水平分表 (Horizontal Table Sharding)：
 *      - 做法：表结构完全相同，按某个分片键 (Sharding Key，如 user_id % N) 将千万/亿级行记录切分到同一库内的多个物理子表 (如 t_order_0, t_order_1)。
 *      - 目的：突破 InnoDB 单表 B+ 树性能拐点（单表超 2000 万-3000 万行时，B+ 树高度由 3 层升至 4 层，单次寻道 I/O 增加，且索引占用内存过大）；降低单表 DDL 锁表风险。
 *
 * 2. 什么是分库 (Database Sharding)？
 *    - 垂直分库 (Vertical Database Sharding)：
 *      - 做法：按微服务业务领域划分，将不同业务表放置于独立的数据库实例（如 user_db, order_db, goods_db）。
 *      - 目的：解决单一数据库实例的 CPU、内存、网络带宽以及最大并发连接数（max_connections）上限，实现业务隔离与故障解耦。
 *    - 水平分库 (Horizontal Database Sharding)：
 *      - 做法：业务表结构相同，按分片键将数据分散到部署在不同物理机器上的多个数据库实例中（如 order_db_0 在机台 A，order_db_1 在机台 B）。
 *      - 目的：突破单台物理机服务器的硬件总写入 TPS 吞吐极限，分担海量写并发压力。
 *
 * 3. 分库 vs 分表的本质区别：
 *    - 解决的主要矛盾不同：
 *      - 分表：解决单表行数过多或字段过宽带来的【存储与索引检索性能】问题（B+树高度、数据页缓存）。
 *      - 分库：解决单台数据库服务器承载的【高并发网络连接、硬件资源 (CPU/内存/磁盘IO吞吐)】瓶颈。
 *    - 复杂度与副作用不同：
 *      - 同库分表：依然处于同一个数据库实例内，事务仍为本地 ACID 事务，支持同库跨表 JOIN，复杂度可控。
 *      - 分库（跨库）：打破了数据库单机物理边界，带来分布式系统的四大核心挑战：
 *        1) 跨库分布式事务 (需要 2PC / Seata AT / TCC / 本地消息表最终一致性)；
 *        2) 跨库 JOIN 失效 (需冗余字段、字段绑定或业务代码多查拼装)；
 *        3) 分布式全局唯一 ID (单机自增主键失效，需引入雪花算法 Snowflake、Leaf)；
 *        4) 跨库多维度分片与排序分页归并 (COUNT/ORDER BY 需要向各库广播后在内存二次合并排序)。
 */
public class Arch03_ShardingDatabaseAndTableDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [Arch03] 分表与分库核心概念、本质区别对比与模拟路由实测");
        System.out.println("================================================================================");

        try (Connection conn = DbConnectionHelper.getConnection()) {
            // 1. 验证垂直分表示例：基础表查询 vs 大文本扩展表按需延迟关联
            System.out.println("[步骤 1] 垂直分表示例：高频列表页只查紧凑主表，详情页才按需 JOIN 大字段扩展表：");
            String baseSql = "SELECT order_id, user_id, order_sn, total_amount, order_status FROM arch_order_base WHERE user_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(baseSql)) {
                pstmt.setLong(1, 2001L);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        System.out.println("  -> [列表查询-极低IO] 订单ID: " + rs.getLong("order_id")
                                + ", 编号: " + rs.getString("order_sn")
                                + ", 金额: " + rs.getBigDecimal("total_amount"));
                    }
                }
            }

            // 详情按需关联
            String detailSql = "SELECT b.order_id, b.order_sn, e.shipping_address, e.snapshot_json " +
                    "FROM arch_order_base b INNER JOIN arch_order_ext e ON b.order_id = e.order_id WHERE b.order_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(detailSql)) {
                pstmt.setLong(1, 1001L);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("  -> [详情按需查询-宽表] 收货地址: " + rs.getString("shipping_address")
                                + ", 快照JSON: " + rs.getString("snapshot_json"));
                    }
                }
            }

            // 2. 验证水平分表示例：按分片键 (user_id % 2) 路由查询对应子表
            System.out.println("\n[步骤 2] 水平分表示例：Sharding Key 路由机制模拟 (分片规则: user_id % 2)：");
            long[] testUserIds = {1000L, 1001L};
            for (long uid : testUserIds) {
                int shard = (int) (uid % 2);
                String tableName = "arch_order_h" + shard;
                String querySql = "SELECT order_id, user_id, order_sn, amount FROM " + tableName + " WHERE user_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(querySql)) {
                    pstmt.setLong(1, uid);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            System.out.println("  -> [路由成功] 用户 " + uid + " -> 子表 " + tableName
                                    + " -> 订单: " + rs.getString("order_sn")
                                    + ", 金额: " + rs.getBigDecimal("amount"));
                        }
                    }
                }
            }

            // 3. 核心差异矩阵小结
            System.out.println("\n[步骤 3] 分库与分表核心对比矩阵：");
            System.out.println("  +----------------------+------------------------------------+------------------------------------------+");
            System.out.println("  | 对比维度             | 分表 (Table Sharding)              | 分库 (Database Sharding)                 |");
            System.out.println("  +----------------------+------------------------------------+------------------------------------------+");
            System.out.println("  | 解决的主要瓶颈       | B+树层高变深、单页容纳行少、单表慢 | 单机 CPU/内存/连接数上限、磁盘总写入瓶颈 |");
            System.out.println("  | 物理实例分布         | 往往在同一个物理数据库实例内       | 分布在不同的独立物理服务器/集群节点上    |");
            System.out.println("  | 本地事务支持         | 依然保留单机 ACID 本地事务         | 跨库破坏单机事务，必须引入分布式事务方案 |");
            System.out.println("  | 表关联 JOIN          | 同库依然可执行多表 JOIN 关联查询   | 跨物理库无法直接 JOIN，需应用组装或冗余  |");
            System.out.println("  | 架构改造成本         | 较低（仅需同库改写 SQL 表名路由）  | 极高（需分布式ID、分布式事务、跨库分页） |");
            System.out.println("  +----------------------+------------------------------------+------------------------------------------+");

        } catch (SQLException e) {
            System.err.println(">> 执行分库分表演示异常: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}

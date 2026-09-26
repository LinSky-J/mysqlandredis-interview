package com.jinlin.mysqlandredis.redis.cluster;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Properties;

/**
 * Redis 集群篇 05: Redis 集群的模式了解吗？优缺点了解吗？(主从、哨兵与分片 Cluster 演进对比)
 *
 * 面试真题：Redis集群的模式了解吗优缺点了解吗
 *
 * 核心原理深度剖析：
 * Redis 在分布式高可用与水平扩展的演化历程中，历经了三大经典架构模式：
 *
 * 一、主从复制模式 (Master-Slave Replication)：
 * 1. 架构形态：1 个 Master 处理所有写操作，多个 Slave 异步复制 Master 数据并分担读操作 (读写分离)。
 * 2. 优点：
 *    - 读吞吐可线性扩展：通过增加从库数量轻松分摊海量只读流量；
 *    - 数据热备容灾：从库保留主库数据完整镜像，主库物理损坏时有副本备份；
 *    - 架构简洁：配置门槛极低，无复杂分布式协调组件。
 * 3. 缺点：
 *    - 【无自动化故障恢复能力 (No Auto-Failover)】：Master 一旦宕机，系统写功能彻底瘫痪，必须人工深夜手动切主；
 *    - 存储容量受限：所有节点存储的都是全量数据，受限于单机物理内存上限；
 *    - 写吞吐瓶颈：仅单节点可写，无法支撑超高并发写入。
 *
 * 二、哨兵高可用模式 (Redis Sentinel)：
 * 1. 架构形态：在主从复制架构的基础上，引入一个由奇数个节点 (至少 3 个) 组成的独立哨兵集群，专门负责监控、仲裁与自动切主。
 * 2. 优点：
 *    - 【高可用性 (High Availability)】：Master 故障秒级自动检测、客观下线并自动化选主提升，实现 7*24 小时无人值守；
 *    - 客户端解耦：微服务客户端连接哨兵集群动态解析获取最新 Master 地址，切主无需重启应用。
 * 3. 缺点：
 *    - 【依然受限于单点写入与单机内存】：本质上仍然是“一主多从”，写性能无法水平扩展，存储容量无法超过单机物理内存；
 *    - 主从切换期间存在数秒的写不可用窗口与潜在的数据丢失风险 (异步复制与脑裂)。
 *
 * 三、分片集群模式 (Redis Cluster，3.0+ 官方原生)：
 * 1. 架构形态：无中心化 P2P 架构，通过 Gossip 协议互相通信。
 *    将整个键空间逻辑切分为 16384 个哈希槽 (Hash Slot)，均匀分布在多个主分片 (Master) 节点上，每个 Master 配备 1~N 个 Slave。
 * 2. 优点：
 *    - 【真正的水平弹性扩展 (Scale-out)】：写吞吐与内存容量随分片数量增加实现线性倍增，突破 TB 级内存与百万 QPS 瓶颈；
 *    - 【内置分布式高可用】：分片内的 Master 宕机时，其对应的 Slave 自动发起选举升主，仅影响该分片槽位，其他分片完全不受影响。
 * 3. 缺点：
 *    - 【多键与跨节点操作受限】：跨节点的 MGET、MSET、事务与 Lua 脚本在不同分片上无法直接执行，必须使用 Hash Tag `{...}` 绑定同一槽位；
 *    - 【仅支持 DB 0】：无法使用 `SELECT` 切换多数据库；
 *    - 【集群运维复杂度较高】：扩容缩容需要进行在线槽位迁移 (reshard)，网络广播在节点过多时有 Gossip 心跳风暴开销 (建议节点数控制在 1000 以内)。
 */
public class RedisCluster05_ClusterModesAndTradeoffsDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisCluster05] Redis 3 大架构模式演进与优缺点横向全景对比");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 模拟写入测试数据
        System.out.println("[步骤 1] 写入测试数据：");
        String modeKey = "demo:cluster:mode:test";

        // Redis 原生指令: SET demo:cluster:mode:test ArchitectureModeData
        redisTemplate.opsForValue().set(modeKey, "ArchitectureModeData");

        // Redis 原生指令: GET demo:cluster:mode:test
        String modeVal = redisTemplate.opsForValue().get(modeKey);
        System.out.println("  已写入键: " + modeKey + ", value=" + modeVal);

        // 2. 探测集群状态 (cluster_enabled)
        System.out.println("\n[步骤 2] 探测当前实例集群运行状态：");
        redisTemplate.execute((RedisConnection connection) -> {
            // Redis 原生指令: INFO cluster
            Properties clusterInfo = connection.info("cluster");
            if (clusterInfo != null) {
                System.out.println("  [集群模式探测] cluster_enabled=" + clusterInfo.getProperty("cluster_enabled")
                        + " (0 表示单机/主从/哨兵模式，1 表示已启用分布式 Cluster 分片集群)");
            }
            return null;
        });

        // 3. 3 大模式全面横向对比表
        System.out.println("\n[步骤 3] Redis 三大部署模式横向优缺点矩阵速查：");
        System.out.println("  +----------------+-------------------------------+-------------------------------+-------------------------------+");
        System.out.println("  | 对比维度       | 主从复制模式 (Master-Slave)   | 哨兵模式 (Sentinel HA)        | Cluster 分片集群模式          |");
        System.out.println("  +----------------+-------------------------------+-------------------------------+-------------------------------+");
        System.out.println("  | 核心目标       | 读写分离、数据冷热备          | 自动化故障检测与秒级切主 (HA) | 海量数据分布式水平扩容 (Scale)|");
        System.out.println("  | 故障转移能力   | 无 (必须人工手动恢复)         | 自动 (哨兵集群 Raft 选主)     | 自动 (集群内部 Gossip 选举)   |");
        System.out.println("  | 写入水平扩展   | 不支持 (单 Master 瓶颈)       | 不支持 (单 Master 瓶颈)       | 支持 (多个 Master 分片并发写) |");
        System.out.println("  | 内存容量上限   | 单机物理内存上限              | 单机物理内存上限              | 多节点聚合 (轻松突破 TB 级)   |");
        System.out.println("  | 多键操作支持   | 完整支持 MGET/事务/Lua        | 完整支持 MGET/事务/Lua        | 需使用 Hash Tag 限制在同一槽  |");
        System.out.println("  | 适用业务场景   | 读多写少、容忍短暂停机系统    | 中小规模、要求 7*24h 高可用   | 超大规模海量并发、数据量超单机|");
        System.out.println("================================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

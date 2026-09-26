package com.jinlin.mysqlandredis.redis.cluster;

/**
 * Redis 集群、主从复制与高可用哨兵篇 (Replication, Sentinel & Cluster)
 *
 * 核心面试问题与内容索引：
 * 1. {@link RedisCluster01_ReplicationFullAndIncrementalDemo}
 *    - 问题: Redis主从同步中的增量和完全同步怎么实现?
 *    - 核心涵盖: PSYNC 命令协议、RDB 镜像生成与 replication buffer、环形复制积压缓冲区 repl_backlog_buffer、master_replid 与 repl_offset 对齐机制。
 *
 * 2. {@link RedisCluster02_DataConsistencyGuaranteesDemo}
 *    - 问题: redis主从和集群可以保证数据一致性吗？
 *    - 核心涵盖: 无法保证强一致性！异步复制数据丢失窗口、网络分区脑裂 (Split-Brain) 风险；min-replicas-to-write 与 min-replicas-max-lag 缓解配置。
 *
 * 3. {@link RedisCluster03_SentinelMechanismPrinciplesDemo}
 *    - 问题: 哨兵机制原理是什么?
 *    - 核心涵盖: 监控 (PING 心跳)、主观下线 (SDOWN) 与客观下线 (ODOWN)、法定投票 Quorum、通知与自动故障转移。
 *
 * 4. {@link RedisCluster04_SentinelLeaderElectionDemo}
 *    - 问题: 哨兵机制的选主节点的算法介绍一下
 *    - 核心涵盖: 两层选主架构——① Raft 算法选举哨兵 Leader (先到先得、半数多数派投票)；② 哨兵 Leader 从存活从库筛选新 Master 4 步规则 (过滤断连 -> replica-priority -> repl_offset -> runid 字典序)。
 *
 * 5. {@link RedisCluster05_ClusterModesAndTradeoffsDemo}
 *    - 问题: Redis集群的模式了解吗优缺点了解吗
 *    - 核心涵盖: 主从复制、哨兵模式 (Sentinel)、Cluster 分片集群三大多维对比 (高可用、读写扩展、运维复杂度、内存上限与多键事务局限)。
 *
 * 6. {@link RedisCluster06_SlotRoutingAndRedirectDemo}
 *    - 问题: cluster集群客户端是怎样知道该访问哪个分片的?
 *    - 核心涵盖: 16384 哈希槽分片与 CRC16 算法、Hash Tag 机制 ({...})、Smart Client 拓扑缓存与直连路由、MOVED 与 ASK 重定向协议细节。
 */

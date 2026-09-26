package com.jinlin.mysqlandredis.redis;

import com.jinlin.mysqlandredis.redis.cluster.RedisCluster01_ReplicationFullAndIncrementalDemo;
import com.jinlin.mysqlandredis.redis.cluster.RedisCluster02_DataConsistencyGuaranteesDemo;
import com.jinlin.mysqlandredis.redis.cluster.RedisCluster03_SentinelMechanismPrinciplesDemo;
import com.jinlin.mysqlandredis.redis.cluster.RedisCluster04_SentinelLeaderElectionDemo;
import com.jinlin.mysqlandredis.redis.cluster.RedisCluster05_ClusterModesAndTradeoffsDemo;
import com.jinlin.mysqlandredis.redis.cluster.RedisCluster06_SlotRoutingAndRedirectDemo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Redis 集群、主从与哨兵高可用 6 问自动化执行与实机数据库验证测试套件
 */
@SpringBootTest
public class RedisClusterInterviewTest {

    @Test
    @DisplayName("01. Redis主从同步中的增量和完全同步怎么实现? (RDB全量同步 vs repl_backlog环形缓冲区增量同步)")
    void test01_ReplicationFullAndIncremental() {
        assertDoesNotThrow(RedisCluster01_ReplicationFullAndIncrementalDemo::runDemo);
    }

    @Test
    @DisplayName("02. redis主从和集群可以保证数据一致性吗？(异步复制数据丢失、脑裂风险与min-replicas保护)")
    void test02_DataConsistencyGuarantees() {
        assertDoesNotThrow(RedisCluster02_DataConsistencyGuaranteesDemo::runDemo);
    }

    @Test
    @DisplayName("03. 哨兵机制原理是什么? (心跳监控、SDOWN主观下线、ODOWN客观下线与自动故障转移)")
    void test03_SentinelMechanismPrinciples() {
        assertDoesNotThrow(RedisCluster03_SentinelMechanismPrinciplesDemo::runDemo);
    }

    @Test
    @DisplayName("04. 哨兵机制的选主节点的算法介绍一下 (Raft选哨兵Leader与新Master四步筛选算法)")
    void test04_SentinelLeaderElection() {
        assertDoesNotThrow(RedisCluster04_SentinelLeaderElectionDemo::runDemo);
    }

    @Test
    @DisplayName("05. Redis集群的模式了解吗优缺点了解吗 (主从复制 vs 哨兵模式 vs Cluster分片集群)")
    void test05_ClusterModesAndTradeoffs() {
        assertDoesNotThrow(RedisCluster05_ClusterModesAndTradeoffsDemo::runDemo);
    }

    @Test
    @DisplayName("06. cluster集群客户端是怎样知道该访问哪个分片的? (16384哈希槽、CRC16、Hash Tag与MOVED/ASK重定向)")
    void test06_SlotRoutingAndRedirect() {
        assertDoesNotThrow(RedisCluster06_SlotRoutingAndRedirectDemo::runDemo);
    }
}

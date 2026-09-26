package com.jinlin.mysqlandredis.redis.cluster;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Properties;

/**
 * Redis 集群篇 02: Redis 主从和集群可以保证数据一致性吗？
 *
 * 面试真题：redis主从和集群可以保证数据一致性吗？
 *
 * 核心原理深度剖析：
 *
 * 一、核心结论：
 * 【绝对不能保证强一致性 (Strong Consistency)！】
 * Redis 的主从复制与集群架构在 CAP 定理中优先选择了 AP (高可用性与分区容错性)，
 * 只能在网络通畅时保证【最终一致性 (Eventual Consistency)】，在极端网络分区或宕机故障下存在明确的数据丢失窗口！
 *
 * 二、数据无法保证强一致性的两大经典场景根因：
 *
 * 1. 【主从异步复制导致的数据丢失】：
 *    - 执行机制：主节点 (Master) 处理写命令并写入本地内存后，会【立即向客户端返回执行成功响应】，
 *      随后再将写命令异步发送给从节点 (Slave)；
 *    - 丢失场景：如果主节点在向客户端返回成功后、尚未把该命令发送给从节点时突发宕机或断电；
 *      哨兵或集群会检测到主库不可用，并将其中一个从库晋升为新 Master；
 *    - 结果：新 Master 的数据集中完全不存在上述写命令，这部分数据便永久丢失了！
 *
 * 2. 【集群脑裂 (Split-Brain) 导致的数据丢失】：
 *    - 发生背景：当主节点与哨兵集群/多数派节点发生网络分区隔离，但主节点依然能被部分客户端正常连接；
 *    - 双主并存：哨兵在另一分区判定老 Master 客观下线，并选举出了一个新 Master；此时老 Master 仍在接收部分客户端的高频写入；
 *    - 彻底丢失：当网络分区恢复后，老 Master 收到哨兵集群的通知，被强制降级为 Slave 重新挂载到新 Master；
 *      作为 Slave，它必须首先执行 FLUSHALL 清空本地全部数据，再去全量同步新 Master 的 RDB 快照；
 *      脑裂期间写入老 Master 的所有业务数据将瞬间被彻底抹除！
 *
 * 三、工程层面的缓解与止损手段：
 * 1. 核心服务端防护配置：
 *    - `min-replicas-to-write 1`：主库必须至少与 1 个从库保持正常心跳连接；
 *    - `min-replicas-max-lag 10`：从库向主库同步的心跳/复制延迟不得超过 10 秒；
 *    - 防护原理：一旦主库与从库断开或延迟超标 (如发生脑裂)，主库立即【拒绝处理客户端后续的所有写请求】，
 *      宁可牺牲局部可用性 (报错)，也绝不允许盲目接收无法持久化的写数据！
 *
 * 2. 客户端主动同步确认：`WAIT numreplicas timeout`：
 *    - 客户端在执行关键写命令后调用 `WAIT 1 1000`，阻塞等待至少 1 个从库将该写命令成功同步并落盘；
 *    - 虽能大幅降低丢失概率，但本质依然是伪同步 (若超时客户端依然会返回，无法做到强一致)，且会显著增加写响应耗时。
 */
public class RedisCluster02_DataConsistencyGuaranteesDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisCluster02] Redis 主从与集群一致性辨析：异步复制丢失与脑裂风险剖析");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 模拟写入关键业务数据 (如扣减库存)
        System.out.println("[步骤 1] 写入关键库存数据 (观察主从复制异步传播)：");
        String stockKey = "demo:cluster:consistency:stock_counter";

        // Redis 原生指令: SET demo:cluster:consistency:stock_counter 99
        redisTemplate.opsForValue().set(stockKey, "99");

        // Redis 原生指令: GET demo:cluster:consistency:stock_counter
        String currentStock = redisTemplate.opsForValue().get(stockKey);
        System.out.println("  已写入商品库存: key=" + stockKey + ", 当前库存=" + currentStock
                + " (主库内存写成功即返回客户端，复制到从库属于异步操作)");

        // 2. 探测防止脑裂的核心配置参数 (min-replicas-to-write / min-replicas-max-lag)
        System.out.println("\n[步骤 2] 探测当前实例防止数据严重丢失与脑裂的保护配置：");
        redisTemplate.execute((RedisConnection connection) -> {
            // Redis 原生指令: CONFIG GET min-replicas-to-write
            Properties minReplicas = connection.getConfig("min-replicas-to-write");
            // Redis 原生指令: CONFIG GET min-replicas-max-lag
            Properties maxLag = connection.getConfig("min-replicas-max-lag");

            System.out.println("  [脑裂防护] min-replicas-to-write: " + (minReplicas != null ? minReplicas.getProperty("min-replicas-to-write") : "0 (未限制)"));
            System.out.println("  [延迟阈值] min-replicas-max-lag: " + (maxLag != null ? maxLag.getProperty("min-replicas-max-lag") : "10 秒"));
            return null;
        });

        // 3. 面试回答总结
        System.out.println("\n[步骤 3] 面试回答“主从和集群能保证一致性吗？”标准权威解析：");
        System.out.println("  1. 【断言定位】：不能保证强一致性，只保证最终一致性 (遵循 AP 架构取舍)。");
        System.out.println("  2. 【两大丢数据核心根因】：");
        System.out.println("     - 根因 1 (异步复制)：Master 写成功即响应客户端，向 Slave 传播途中宕机导致新 Master 丢数据；");
        System.out.println("     - 根因 2 (集群脑裂)：网络分区形成双主，老 Master 降级为 Slave 全量同步时清空脑裂期新写入。");
        System.out.println("  3. 【生产解决方案】：");
        System.out.println("     - 调配 `min-replicas-to-write 1` 和 `min-replicas-max-lag 10` 控制写降级；");
        System.out.println("     - 关键金融/交易业务若要求绝对零丢失，数据强一致必须依托底层关系型数据库 (MySQL/Oracle) 事务或 Raft 协议组件。");
        System.out.println("================================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

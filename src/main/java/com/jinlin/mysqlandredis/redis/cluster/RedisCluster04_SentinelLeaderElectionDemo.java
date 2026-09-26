package com.jinlin.mysqlandredis.redis.cluster;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Properties;

/**
 * Redis 集群篇 04: 哨兵机制的选主节点的算法介绍一下？(两层选主架构与四步筛选算法)
 *
 * 面试真题：哨兵机制的选主节点的算法介绍一下
 *
 * 核心原理深度剖析：
 *
 * 【重难点提示：面试回答该问题时，必须清晰指出“选主”包含两个完全不同维度的层次，绝不能混淆！】
 * 层次一：选出执行故障转移的【哨兵 Leader】(Raft 选举算法)；
 * 层次二：由该哨兵 Leader 从众多 Slave 中挑选出【新 Redis Master】(四步过滤排序算法)。
 *
 * 一、第一层：哨兵集群如何选举“哨兵 Leader”？(Raft 共识算法实现)：
 * 1. 触发时机：Master 被判定为客观下线 (ODOWN) 后。
 * 2. 选举流程：
 *    - 发现客观下线的哨兵会向其他所有哨兵发送 `SENTINEL is-master-down-by-addr` 命令，
 *      带上自己的当前纪元 `epoch` 和自身 runid，申请成为 Leader；
 *    - 【先到先得原则】：其他哨兵收到投票申请后，只要自己在该纪元内尚未投过票，就会把票投给第一个向它索要选票的候选者；
 *    - 【胜出条件】：候选哨兵必须同时满足两个条件才能当选：
 *      1) 获得的总票数达到预设的法定人数 (`quorum`)；
 *      2) 获得的总票数必须超过所有哨兵总数的一半 (`> sentinel_nodes / 2`，严格多数派)。
 *    - 若本轮未分出胜负 (如多节点同时发起拉票导致平票)，则增加 epoch 并在随机休眠一段时间后重新竞选。
 *
 * 二、第二层：哨兵 Leader 如何从从库中选出“新 Master”？(严格的四步选主算法)：
 * 哨兵 Leader 按照以下顺序，对所有存活的 Slave 执行逐层淘汰筛选，最终留下的唯一节点即为新 Master：
 *
 * 1. 【第一步：健康初筛 (过滤劣质节点)】：
 *    - 剔除所有处于下线、断连状态的 Slave；
 *    - 剔除在最近 5 秒内未响应过 INFO 命令的 Slave；
 *    - 剔除与原 Master 断开连接时间过长 (超过 `down-after-milliseconds * 10`) 的 Slave (避免其数据陈旧度过高)。
 *
 * 2. 【第二步：比较从库优先级 (replica-priority / slave-priority)】：
 *    - 查看每个 Slave 在配置文件中设置的 `replica-priority` (默认 100)；
 *    - 【数值越小，优先级越高】！
 *    - 注意：若设为 0，则代表该从节点作为纯查询从库，永远不参与竞选升主！
 *    - 优先级数值最小的 Slave 优先当选；若出现数值并列相同，进入第三步。
 *
 * 3. 【第三步：比较复制偏移量 (slave_repl_offset)】：
 *    - 比对并列节点的 `slave_repl_offset`；
 *    - 【偏移量数值越大，说明从主库同步的数据量越完整、最新，丢数据最少】！
 *    - offset 最大的 Slave 当选；若 offset 依然完全一致，进入第四步。
 *
 * 4. 【第四步：比较运行 ID 字典序 (runid)】：
 *    - 比较各个 Slave 在启动时生成的 40 位十六进制 `runid`；
 *    - 【选择 ASCII 字典序最小的那个 Slave 晋升为新 Master】！
 *    - (由于 runid 全局唯一且不可变，字典序比较必定能打破平局，选出唯一新主！)。
 */
public class RedisCluster04_SentinelLeaderElectionDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisCluster04] 哨兵选主算法深度剖析：Raft 选哨兵 Leader 与新 Master 四步筛选");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 模拟写入测试数据
        System.out.println("[步骤 1] 写入测试数据：");
        String testKey = "demo:cluster:election:test_node";

        // Redis 原生指令: SET demo:cluster:election:test_node MasterElectedData
        redisTemplate.opsForValue().set(testKey, "MasterElectedData");

        // Redis 原生指令: GET demo:cluster:election:test_node
        String value = redisTemplate.opsForValue().get(testKey);
        System.out.println("  已写入测试键: " + testKey + ", value=" + value);

        // 2. 底层探测当前节点的 replica-priority 与运行参数
        System.out.println("\n[步骤 2] 探测当前节点的从节点晋升优先级配置 (replica-priority)：");
        redisTemplate.execute((RedisConnection connection) -> {
            // Redis 原生指令: CONFIG GET replica-priority
            Properties priorityConfig = connection.getConfig("replica-priority");
            System.out.println("  [选主参数] replica-priority: "
                    + (priorityConfig != null ? priorityConfig.getProperty("replica-priority") : "100")
                    + " (数值越小优先级越高，0 表示永不晋升)");
            return null;
        });

        // 3. 面试回答总结
        System.out.println("\n[步骤 3] 面试官提问“哨兵选主算法”的标准结构化答题要点：");
        System.out.println("  1. 【指明层次】：包含“选哨兵 Leader”和“选新 Master 节点”两个阶段，缺一不可！");
        System.out.println("  2. 【选哨兵 Leader】：Raft 算法，先到先得拉票，需满足 >= Quorum 且过半数 (> N/2) 胜出；");
        System.out.println("  3. 【选新 Master 4 步规则 (口诀：滤 -> 优 -> 偏 -> 序)】：");
        System.out.println("     - 滤 (健康过滤)：过滤断连、下线以及与主库断开超时 10 倍的从库；");
        System.out.println("     - 优 (优先级)：比较 replica-priority，数值越小越优先；");
        System.out.println("     - 偏 (复制偏移)：比较 slave_repl_offset，偏移量越大说明数据越新，越优先；");
        System.out.println("     - 序 (字典序兜底)：比较 runid，ASCII 字典序最小的必定胜出！");
        System.out.println("================================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

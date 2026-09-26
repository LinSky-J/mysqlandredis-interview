package com.jinlin.mysqlandredis.redis.cluster;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import io.lettuce.core.cluster.SlotHash;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 集群篇 06: Cluster 集群客户端是怎样知道该访问哪个分片的？(哈希槽路由、Smart Client 本地缓存与 MOVED/ASK 重定向)
 *
 * 面试真题：cluster集群客户端是怎样知道该访问哪个分片的?
 *
 * 核心原理深度剖析：
 *
 * 一、16384 个哈希槽 (Hash Slots) 与分片寻址计算：
 * 1. 为什么是 16384 个槽位？
 *    - Redis Cluster 将整个集群的键空间固定划分为 16384 (2^14) 个逻辑哈希槽 (编号 0 ~ 16383)；
 *    - 寻址计算公式：`Slot = CRC16(key) % 16384`；
 * 2. 【Hash Tag 机制 (哈希标签)】：
 *    - 如果 Key 包含花括号 `{...}`，则只对大括号内部的有效字符计算 CRC16；
 *    - 作用：确保相关联的多个 Key 必定被分配到同一个哈希槽和同一个分片节点上，
 *      例如 `{user:1001}:info`、`{user:1001}:orders` 和 `{user:1001}:wallet` 必然落入同一个 Slot，
 *      从而可以在 Cluster 环境下完美支持多键 MGET/MSET、事务与原子 Lua 脚本！
 *
 * 二、智能客户端 (Smart Client) 如何直接定位目标分片？：
 * 现代高并发 Redis 客户端 (如 Lettuce、JedisCluster、Redisson) 绝不是通过中间代理 Proxy 进行低效转发的，
 * 而是全部采用【客户端本地拓扑缓存 + 本地即时计算直连】的 Smart Client 架构：
 *
 * 1. 【初始化时拉取拓扑路由表并本地缓存】：
 *    - 客户端启动建立连接时，会向集群中任意一个存活节点发送 `CLUSTER SLOTS` (或 Redis 7.0+ `CLUSTER SHARDS`) 命令；
 *    - 获取集群中所有分片的槽位区间分布列表 (例如: 节点 A 负责 0~5460，节点 B 负责 5461~10922，节点 C 负责 10923~16383)；
 *    - 客户端在本地内存中构建一份全局槽位到目标 Master IP:Port 的映射数组 (如 `slots[16384]`)。
 *
 * 2. 【本地路由直接发送，零额外网关开销】：
 *    - 当业务发起写或读请求时，客户端在本地应用层执行 `CRC16(key) % 16384` 得到槽位编号；
 *    - 根据槽位编号在本地路由缓存中 O(1) 检索出对应的目标 Master 节点连接；
 *    - 直接通过该 TCP 连接向目标节点发送指令，真正做到了直连访问，完全无中间层转发损耗！
 *
 * 三、拓扑变更与槽迁移时的重定向机制：【MOVED 重定向 vs ASK 重定向】：
 * 当集群扩容、缩容或槽位在线迁移 (reshard) 时，客户端本地缓存可能会出现短期的“信息滞后”，此时由服务端通过重定向协议纠正：
 *
 * 1. 【MOVED 重定向 (永久性迁移完成)】：
 *    - 场景：槽位 3999 已经完全迁移到节点 B，但客户端本地缓存仍记录为节点 A；
 *    - 交互：
 *      1) 客户端将请求发送给节点 A；
 *      2) 节点 A 发现该槽位已不归自己负责，回复错误：`(error) MOVED 3999 192.168.1.102:6379`；
 *      3) 客户端行为：【立即向新地址 192.168.1.102 重新发送该请求，并且【立即更新本地槽位缓存】】，
 *         后续所有针对槽位 3999 的请求将直接发往新节点。
 *
 * 2. 【ASK 重定向 (临时迁移过渡中)】：
 *    - 场景：槽位 3999 正在从节点 A 向节点 B 迁移过程中，部分数据已在新节点，部分数据仍在老节点；
 *    - 交互：
 *      1) 客户端请求发送给节点 A；
 *      2) 节点 A 发现 Key 属于迁移中的槽位，但本地找不到该 Key；
 *      3) 节点 A 回复错误：`(error) ASK 3999 192.168.1.102:6379`；
 *      4) 客户端行为：
 *         ① 先向新节点 B 发送一条 `ASKING` 预热指令 (通知节点 B 即使该槽位处于 IMPORTING 状态也临时接收后续单次请求)；
 *         ② 紧接着向节点 B 发送原业务命令；
 *         ③ 【关键区别：客户端绝对【不会更新本地槽位缓存】】！因为迁移未完成，下次访问该槽位依然默认先向节点 A 发起。
 */
public class RedisCluster06_SlotRoutingAndRedirectDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisCluster06] Cluster 客户端分片路由原理解析：CRC16、Hash Tag 与 MOVED/ASK");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 实机写入测试数据
        System.out.println("[步骤 1] 写入测试数据并通过 Lettuce SlotHash 计算哈希槽：");
        String key1 = "user:order:1001";
        String key2 = "user:wallet:1001";

        // Redis 原生指令: SET user:order:1001 OrderDetail_1001
        redisTemplate.opsForValue().set(key1, "OrderDetail_1001");
        // Redis 原生指令: SET user:wallet:1001 WalletBalance_1001
        redisTemplate.opsForValue().set(key2, "WalletBalance_1001");

        // 计算常规 Key 的槽位
        int slot1 = SlotHash.getSlot(key1);
        int slot2 = SlotHash.getSlot(key2);
        System.out.println("  普通 Key 槽位分布计算：");
        System.out.println("    key1 [" + key1 + "] -> CRC16 % 16384 = 哈希槽 " + slot1);
        System.out.println("    key2 [" + key2 + "] -> CRC16 % 16384 = 哈希槽 " + slot2);
        System.out.println("    (两个普通 Key 计算出的槽位通常不同，可能落在不同的分片 Master 上)");

        // 2. 演示 Hash Tag 机制如何保证多键汇聚在同一槽位
        System.out.println("\n[步骤 2] 演示 Hash Tag {...} 机制实现槽位聚合：");
        String taggedKey1 = "{user:1001}:profile";
        String taggedKey2 = "{user:1001}:orders";
        String taggedKey3 = "{user:1001}:cart";

        // Redis 原生指令: SET {user:1001}:profile ProfileData
        redisTemplate.opsForValue().set(taggedKey1, "ProfileData");
        // Redis 原生指令: SET {user:1001}:orders OrderData
        redisTemplate.opsForValue().set(taggedKey2, "OrderData");
        // Redis 原生指令: SET {user:1001}:cart CartData
        redisTemplate.opsForValue().set(taggedKey3, "CartData");

        int tagSlot1 = SlotHash.getSlot(taggedKey1);
        int tagSlot2 = SlotHash.getSlot(taggedKey2);
        int tagSlot3 = SlotHash.getSlot(taggedKey3);

        System.out.println("  带 Hash Tag 键槽位计算：");
        System.out.println("    taggedKey1 [" + taggedKey1 + "] -> 哈希槽 " + tagSlot1);
        System.out.println("    taggedKey2 [" + taggedKey2 + "] -> 哈希槽 " + tagSlot2);
        System.out.println("    taggedKey3 [" + taggedKey3 + "] -> 哈希槽 " + tagSlot3);
        System.out.println("  => 验证结果: 三者仅依据花括号内部的 'user:1001' 计算槽位，槽位完全相同 (" + tagSlot1 + ")！支持集群事务与多键操作！");

        // 3. 梳理面试回答标准逻辑
        System.out.println("\n[步骤 3] 面试官提问“Cluster 客户端怎么知道访问哪个分片？”高分回答骨架：");
        System.out.println("  1. 【寻址算法】：整个集群划分 16384 个哈希槽，公式为 Slot = CRC16(key) % 16384。可通过 {...} Hash Tag 强制聚集同一槽；");
        System.out.println("  2. 【Smart Client 拓扑缓存与直连】：");
        System.out.println("     - 客户端启动时向集群节点发送 `CLUSTER SLOTS`，将槽位到节点的映射表缓存在本地；");
        System.out.println("     - 发起操作时，客户端在本地算出槽位，直接查缓存直连对应分片 Master 发送命令，无需网关代理；");
        System.out.println("  3. 【扩缩容与槽迁移重定向保障】：");
        System.out.println("     - MOVED 重定向：槽位已完全迁移，客户端重定向执行并【更新本地槽位缓存】；");
        System.out.println("     - ASK 重定向：槽位正在临时迁移中，客户端发送 ASKING 执行单次请求，【不更新本地槽位缓存】。");
        System.out.println("================================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

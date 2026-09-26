package com.jinlin.mysqlandredis.redis.eviction;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存淘汰与过期删除篇 01: 过期删除策略和内存淘汰策略有什么区别？
 *
 * 面试真题：过期删除策略和内存淘汰策略有什么区别?
 *
 * 核心原理深度剖析：
 * 很多初学者甚至中级开发经常将“过期删除”与“内存淘汰”混为一谈，它们在 Redis 中是两套完全独立、分工明确的内存回收机制：
 *
 * 1. 触发维度与根本触发原因不同：
 *    - 【过期删除策略 (Expiration Deletion)】：
 *      - 关注点是【时间的流逝 (TTL 到期)】；
 *      - 当某个 Key 达到了通过 EXPIRE / PEXPIRE / EXPIREAT 设定的生存周期后，由 Redis 的过期检查机制将其从内存中释放；
 *      - 哪怕 Redis 内存非常充裕 (比如只用了 100MB / 16GB)，过期 Key 依然会按过期删除机制被清理。
 *    - 【内存淘汰策略 (Memory Eviction)】：
 *      - 关注点是【物理内存容量越界 (Maxmemory 触及)】；
 *      - 只有当 Redis 物理内存使用量达到了配置文件设置的 `maxmemory` 阈值，并且客户端又发起了新的写命令 (需要分配新内存) 时，
 *        Redis 才会主动触发淘汰循环 (`performEvictions`)，按照配置的淘汰算法强制淘汰一部分 Key，以腾出空间接纳新写入。
 *
 * 2. 作用的数据集范围不同：
 *    - 【过期删除策略】：仅作用于 Redis 的过期字典 (`redisDb.expires`)，没有设置 TTL 的永久 Key 绝对不会进入该流程；
 *    - 【内存淘汰策略】：不仅可以作用于带过期时间的 Key (`volatile-*`)，还可以作用于全局全量 Key 集合 (`allkeys-*`)，
 *      甚至可以直接报错拒绝写入 (`noeviction`)。
 *
 * 3. 运行的时序与执行代价不同：
 *    - 【过期删除策略】：属于日常平摊开销。通过读写请求时的“惰性删除”结合后台 serverCron 的“定期抽样删除 (限时 25ms)”，
 *      将 CPU 开销平摊在整个运行周期中；
 *    - 【内存淘汰策略】：属于突发写时阻塞。当内存满时，写命令必须在当前线程同步遍历抽样并释放内存，
 *      如果一次性淘汰大量大 Key，会造成写操作的瞬间时延飙升。
 */
public class RedisEvict01_ExpireVsEvictDifferenceDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisEvict01] 过期删除策略 vs 内存淘汰策略 核心区别剖析与实机探测");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 模拟设置带过期时间的 Key (受过期删除策略管控)
        System.out.println("[步骤 1] 写入带过期时间的 Key (观察过期字典与 TTL 状态)：");
        String ttlKey = "demo:evict:diff:ttl_key";

        // Redis 原生指令: SET demo:evict:diff:ttl_key SessionData EX 60
        redisTemplate.opsForValue().set(ttlKey, "SessionData", Duration.ofSeconds(60));

        // Redis 原生指令: TTL demo:evict:diff:ttl_key
        Long remainingTtl = redisTemplate.getExpire(ttlKey, TimeUnit.SECONDS);
        System.out.println("  已写入带 TTL 键: " + ttlKey + ", 当前剩余有效时间: " + remainingTtl + " 秒 (受过期删除策略管理)");

        // 2. 模拟写入永久不过期的 Key (不受过期删除管理，但受内存淘汰策略 allkeys-* 管理)
        System.out.println("\n[步骤 2] 写入永久有效 Key (TTL = -1，仅受全局内存淘汰策略管控)：");
        String persistentKey = "demo:evict:diff:persistent_key";

        // Redis 原生指令: SET demo:evict:diff:persistent_key ConfigData
        redisTemplate.opsForValue().set(persistentKey, "ConfigData");

        // Redis 原生指令: TTL demo:evict:diff:persistent_key
        Long persistentTtl = redisTemplate.getExpire(persistentKey, TimeUnit.SECONDS);
        System.out.println("  已写入永久键: " + persistentKey + ", TTL=" + persistentTtl + " (-1 表示永久存储，仅在内存满且为 allkeys 策略时被淘汰)");

        // 3. 探测当前服务器的 maxmemory 与 maxmemory-policy 状态
        System.out.println("\n[步骤 3] 探测当前服务器的内存上限与内存淘汰策略：");
        redisTemplate.execute((RedisConnection connection) -> {
            // Redis 原生指令: CONFIG GET maxmemory
            Properties maxmemConfig = connection.getConfig("maxmemory");
            // Redis 原生指令: CONFIG GET maxmemory-policy
            Properties policyConfig = connection.getConfig("maxmemory-policy");

            System.out.println("  [内存配置] maxmemory 上限字节: " + (maxmemConfig != null ? maxmemConfig.getProperty("maxmemory") : "0 (无限制)"));
            System.out.println("  [淘汰策略] maxmemory-policy: " + (policyConfig != null ? policyConfig.getProperty("maxmemory-policy") : "noeviction"));

            // Redis 原生指令: INFO memory
            Properties memInfo = connection.info("memory");
            if (memInfo != null) {
                System.out.println("  [内存指标] used_memory_human=" + memInfo.getProperty("used_memory_human")
                        + ", used_memory_peak_human=" + memInfo.getProperty("used_memory_peak_human")
                        + ", total_system_memory_human=" + memInfo.getProperty("total_system_memory_human"));
            }
            return null;
        });

        // 4. 面试核心考点结构化对比
        System.out.println("\n[步骤 4] 面试高频对比表速记：");
        System.out.println("  +--------------------+---------------------------------------+---------------------------------------+");
        System.out.println("  | 对比维度           | 过期删除策略 (Expiration Deletion)    | 内存淘汰策略 (Memory Eviction)        |");
        System.out.println("  +--------------------+---------------------------------------+---------------------------------------+");
        System.out.println("  | 触发原因           | Key 存活时间 TTL 归零到期             | 物理内存用量达到 maxmemory 阈值       |");
        System.out.println("  | 关注指标           | 时间维度 (Time-To-Live)               | 空间维度 (Memory Consumption)         |");
        System.out.println("  | 作用目标集合       | 仅限设置了过期的 Key (expires 字典)   | 全量键(allkeys) 或 过期键(volatile)   |");
        System.out.println("  | 执行机制           | 惰性删除 (访问时查) + 定期抽样轮询    | 写命令同步循环抽样淘汰 (8大策略)      |");
        System.out.println("  | 内存充裕时表现     | 依然按 TTL 机制主动/被动删除          | 处于休眠状态，绝对不会触发淘汰        |");
        System.out.println("================================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

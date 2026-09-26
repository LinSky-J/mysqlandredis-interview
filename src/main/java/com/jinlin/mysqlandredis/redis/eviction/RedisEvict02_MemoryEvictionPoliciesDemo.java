package com.jinlin.mysqlandredis.redis.eviction;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Redis 缓存淘汰与过期删除篇 02: 介绍一下 Redis 内存淘汰策略？
 *
 * 面试真题：介绍一下Redis 内存淘汰策略
 *
 * 核心原理深度剖析：
 *
 * 一、Redis 8 大内存淘汰策略全景图 (Redis 4.0+ 起)：
 * 当 Redis 内存达到 `maxmemory` 阈值，且有新写请求到来时，Redis 将按配置的策略进行淘汰：
 *
 * 1. 【不淘汰策略】 (默认):
 *    - `noeviction`：不淘汰任何数据！新写入命令直接抛出 OOM 异常 `(error) OOM command not allowed when used memory > 'maxmemory'`。
 *      只允许读命令和 DEL / UNLINK 删除命令正常运行。常用于纯内存数据库场景，保障数据绝不静默丢失。
 *
 * 2. 【LRU 策略 (Least Recently Used，最近最少使用)】：
 *    - `allkeys-lru`：在所有 Key 中，淘汰最近最少使用的 Key (生产纯缓存系统最常用、最推荐的策略)；
 *    - `volatile-lru`：仅在设置了过期时间 (TTL) 的 Key 中，淘汰最近最少使用的 Key。适合既做持久化元数据又做临时缓存的混合场景。
 *
 * 3. 【LFU 策略 (Least Frequently Used，最不经常使用/访问频率最低，Redis 4.0 新增)】：
 *    - `allkeys-lfu`：在所有 Key 中，淘汰访问频次最低的 Key；
 *    - `volatile-lfu`：仅在设置了过期时间 (TTL) 的 Key 中，淘汰访问频次最低的 Key。
 *    - LFU 对比 LRU 优势：LRU 容易因一次偶发的批量扫描 (如全量遍历)，将真正的高频热点 Key 冲刷出缓存池；LFU 结合了历史访问频次与时间衰减，更加抗突发抖动。
 *
 * 4. 【随机策略 (Random)】：
 *    - `allkeys-random`：在所有 Key 中随机选择一个淘汰；
 *    - `volatile-random`：仅在设置了过期时间的 Key 中随机选择一个淘汰。
 *
 * 5. 【TTL 策略】：
 *    - `volatile-ttl`：仅在设置了过期时间的 Key 中，淘汰剩余存活时间 (TTL) 最短的 Key。
 *
 * 二、底层机制：Redis 为什么采用【近似 LRU / LFU】而不是传统双向链表？
 * 1. 传统 LRU 的弊端：
 *    - 需要维护一个全局双向链表，每次有 Key 被访问，都要将其节点摘除并移动到链表头部；
 *    - 每一个 Key 需要两个额外的指针空间 (prev/next)，占用巨大的内存元数据开销；
 *    - 在多线程/高并发环境下，频繁操作全局链表需要加重锁，严重拉低吞吐量。
 * 2. Redis 的近似 LRU 实现 (Approximated LRU)：
 *    - 在 `redisObject` 头部保留了一个仅占用 24 bit 的 `lru` 字段；
 *    - 记录该对象最近一次被访问的系统 LRU 时钟；
 *    - 淘汰时，Redis 随机抽取 `maxmemory-samples` (默认 5 个) 个 Key 放入候选池，
 *      计算其空闲时间 `idle_time = current_lru_clock - lru_clock`，淘汰空闲时间最大的那一个！
 *    - 效果：抽样数设为 10 时，精度已经极其逼近真实 LRU，但省去了海量指针内存与链表移动开销。
 * 3. Redis 的近似 LFU 实现 (Redis 4.0+)：
 *    - 巧妙复用这 24 位的 `lru` 字段：
 *      - 高 16 位：`ldt` (Last Decrement Time，上次衰减时间戳，精度为分钟)；
 *      - 低 8 位：`logc` (Logistic Counter，对数访问计数器，0~255)。
 *    - 计算方式：新键初始化计数为 5，每次被访问时根据 `lfu-log-factor` 概率递增；随着时间推移，依据 `lfu-decay-time` 自动衰减。
 */
public class RedisEvict02_MemoryEvictionPoliciesDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisEvict02] Redis 8 大内存淘汰策略与底层近似 LRU/LFU 算法原理解析");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 写入样本 Key 并观察 LRU 空闲时间 (IDLETIME)
        System.out.println("[步骤 1] 写入样本 Key，通过 OBJECT IDLETIME 观察 24bit LRU 时钟：");
        String sampleKey = "demo:evict:policy:sample_lru_key";

        // Redis 原生指令: SET demo:evict:policy:sample_lru_key SampleLRUData
        redisTemplate.opsForValue().set(sampleKey, "SampleLRUData");

        // Redis 原生指令: OBJECT IDLETIME demo:evict:policy:sample_lru_key
        Long idleTime = RedisConnectionHelper.getCommands().objectIdletime(sampleKey);

        System.out.println("  已写入键: " + sampleKey + ", 当前空闲时间 (OBJECT IDLETIME): " + idleTime + " 秒");

        // 2. 探测服务器的内存淘汰策略及抽样参数
        System.out.println("\n[步骤 2] 探测当前 Redis 实例的内存淘汰配置参数：");
        redisTemplate.execute((RedisConnection connection) -> {
            // Redis 原生指令: CONFIG GET maxmemory-policy
            Properties policyProp = connection.getConfig("maxmemory-policy");
            // Redis 原生指令: CONFIG GET maxmemory-samples
            Properties samplesProp = connection.getConfig("maxmemory-samples");
            // Redis 原生指令: CONFIG GET maxmemory
            Properties maxmemProp = connection.getConfig("maxmemory");

            System.out.println("  [淘汰策略] maxmemory-policy: " + (policyProp != null ? policyProp.getProperty("maxmemory-policy") : "noeviction"));
            System.out.println("  [抽样精度] maxmemory-samples: " + (samplesProp != null ? samplesProp.getProperty("maxmemory-samples") : "5"));
            System.out.println("  [内存上限] maxmemory: " + (maxmemProp != null ? maxmemProp.getProperty("maxmemory") : "0"));
            return null;
        });

        // 3. 面试高频总结梳理
        System.out.println("\n[步骤 3] 生产环境内存淘汰策略选型指南：");
        System.out.println("  1. 【纯缓存系统】 (无数据持久化要求，丢了可从 DB 查)：");
        System.out.println("     - 优先选择 `allkeys-lru` 或 `allkeys-lfu`；如果存在访问分布严重倾斜或热点明显，`allkeys-lfu` 表现更好。");
        System.out.println("  2. 【混合存储系统】 (一部分 Key 为必须保全的持久化业务数据，另一部分为临时缓存)：");
        System.out.println("     - 必须选择 `volatile-lru` / `volatile-lfu` 或 `volatile-ttl`，必须确保持久化 Key 不设 TTL，临时 Key 设 TTL。");
        System.out.println("  3. 【作为核心内存主库/金融账户余额等严禁丢数据场景】：");
        System.out.println("     - 必须选择 `noeviction`，宁可写失败告警人工介入，也绝不能让 Redis 私自淘汰业务数据。");
        System.out.println("================================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

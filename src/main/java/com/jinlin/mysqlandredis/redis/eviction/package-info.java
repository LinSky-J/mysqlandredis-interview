package com.jinlin.mysqlandredis.redis.eviction;

/**
 * Redis 缓存淘汰与过期删除篇 (Eviction & Expiration)
 *
 * 核心面试问题与内容索引：
 * 1. {@link RedisEvict01_ExpireVsEvictDifferenceDemo}
 *    - 问题: 过期删除策略和内存淘汰策略有什么区别?
 *    - 核心对比: 触发维度 (生命周期 TTL 到期 vs 内存触及 maxmemory 阈值)、作用范围 (过期字典 vs 全量/部分键空间)、执行时机与代价。
 *
 * 2. {@link RedisEvict02_MemoryEvictionPoliciesDemo}
 *    - 问题: 介绍一下 Redis 内存淘汰策略
 *    - 核心涵盖: 8 大淘汰策略全解 (noeviction, allkeys-lru, volatile-lru, allkeys-lfu, volatile-lfu, allkeys-random, volatile-random, volatile-ttl)；
 *      Redis 近似 LRU/LFU 实现原理 (24bit lru 字段、抽样池 maxmemory-samples、对数计数器 logc 与衰减 ldt)。
 *
 * 3. {@link RedisEvict03_ExpirationDeletionStrategiesDemo}
 *    - 问题: 介绍一下 Redis 过期删除策略
 *    - 核心涵盖: 定时删除、惰性删除、定期删除优缺点深度对比；
 *      Redis 的权衡方案:【惰性删除 (expireIfNeeded) + 定期抽样删除 (activeExpireCycle)】组合拳与时间片保护 (25ms 限时)。
 *
 * 4. {@link RedisEvict04_WillExpiredKeyBeDeletedImmediatelyDemo}
 *    - 问题: Redis 的缓存失效会不会立即删除?
 *    - 核心解析: 绝对不会立即删除！TTL 归零后仍在物理内存中，直到被客户端访问被动触发惰性删除，或被 serverCron 周期轮询抽样命中才会被物理释放。
 *
 * 5. {@link RedisEvict05_WhyNotDeleteImmediatelyDemo}
 *    - 问题: 那为什么我不过期立即删除?
 *    - 核心解析: 单线程事件循环制约、海量定时器对 CPU 资源的毁灭性抢占、数据结构 (时间轮/最小堆) 的内存与计算开销、CPU 吞吐量优先的设计哲学。
 */

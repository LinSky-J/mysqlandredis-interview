package com.jinlin.mysqlandredis.redis.scenario;

/**
 * Redis 业务高并发实战与生产场景篇 (Business Scenarios & High Concurrency)
 *
 * 核心面试问题与内容索引：
 * 1. {@link RedisScenario01_WhyRedisAndPerfComparisonDemo}
 *    - 问题 1: 为什么使用 redis?
 *    - 问题 2: 为什么 redis 比 mysql 要快?
 *    - 问题 3: 本地缓存和 Redis 缓存的区别?
 *    - 问题 4: 高并发场景，Redis单节点 + MySQL单节点能有多大的并发量?
 *
 * 2. {@link RedisScenario02_BusinessScenariosAndConcurrencyDemo}
 *    - 问题 1: redis 应用场景是什么?
 *    - 问题 2: Redis 除了缓存，还有哪些应用?
 *    - 问题 3: Redis 支持并发操作吗?
 *
 * 3. {@link RedisScenario03_DistributedLockPrinciplesDemo}
 *    - 问题: Redis 分布式锁的实现原理？什么场景下用到分布式锁?
 *    - 核心涵盖: SET NX EX、UUID 防误删、Lua 脚本释放、Redisson 看门狗 (Watchdog) 自动续期与红锁 (Redlock) 优缺点。
 *
 * 4. {@link RedisScenario04_BigKeyProblemAndSolutionsDemo}
 *    - 问题 1: Redis 的大 Key 问题是什么?
 *    - 问题 2: 大 Key 问题的缺点?
 *    - 问题 3: Redis 大 key 如何解决?
 *    - 核心涵盖: 判定阈值、网络阻塞/CPU单线程卡顿/OOM风险、--bigkeys 与 MEMORY USAGE 探测、数据拆分与 UNLINK 异步惰性释放。
 *
 * 5. {@link RedisScenario05_HotKeyProblemAndSolutionsDemo}
 *    - 问题 1: 什么是热 key?
 *    - 问题 2: 如何解决热 key 问题?
 *    - 核心涵盖: 热点成因、单分片网卡与 CPU 击穿危害、多级缓存 (Caffeine/Guava)、热 Key 随机后缀副本分散打散方案。
 *
 * 6. {@link RedisScenario06_CacheConsistencyWithMysqlDemo}
 *    - 问题: 如何保证 redis 和 mysql 数据缓存一致性问题?
 *    - 核心涵盖: Cache-Aside 模式深度剖析 (为什么先更 DB 再删缓存？为什么不能先删缓存？)、延时双删原理与弊端、Canal 监听 Binlog 异步删除保证最终一致性。
 *
 * 7. {@link RedisScenario07_AvalancheBreakdownPenetrationBloomDemo}
 *    - 问题 1: 缓存雪崩、击穿、穿透是什么? 怎么解决?
 *    - 问题 2: 布隆过滤器原理介绍一下
 *    - 核心涵盖: 三大缓存灾难对比防线；布隆过滤器原理 (Bitmap、K个哈希函数、容错率冲突与无法直接物理删除局限)。
 *
 * 8. {@link RedisScenario08_SeckillArchitectureAndAntiOversellDemo}
 *    - 问题: 如何设计秒杀场景处理高并发以及超卖现象?
 *    - 核心涵盖: 秒杀多级漏斗防护、Redis Lua 脚本原子扣减库存、MQ 异步解耦落库、MySQL 乐观锁/行锁版本号防超卖。
 */

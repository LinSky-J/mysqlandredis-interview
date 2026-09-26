/**
 * Redis 事务与原子性控制专题模块
 * <p>
 * 涵盖知识点：
 * 1. [RedisTx01] 如何实现 Redis 原子性？（单命令原子性、Lua 脚本强原子性保障、MULTI/EXEC 弱原子性与不回滚机制深度剖析）
 * 2. [RedisTx02] 除了 Lua 有没有什么也能保证 Redis 的原子性？（WATCH + MULTI/EXEC 乐观锁 CAS、原生复合原子命令 SET NX EX / MSETNX、Redis 7.0 Functions、Redisson 分布式锁）
 */
package com.jinlin.mysqlandredis.redis.transaction;

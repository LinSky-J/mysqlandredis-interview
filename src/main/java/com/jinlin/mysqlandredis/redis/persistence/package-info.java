package com.jinlin.mysqlandredis.redis.persistence;

/**
 * Redis 持久化与日志篇 (Persistence & Logging)
 *
 * 核心面试问题与内容索引：
 * 1. {@link RedisLog01_RdbVsAofPersistenceDemo}
 *    - 问题: Redis有哪2种持久化方式？分别的优缺点是什么?
 *    - 深度涵盖:
 *      1) RDB (Redis DataBase 快照持久化): 触发机制 (SAVE / BGSAVE / save 秒数 修改数)、COW (写时复制) 原理、二进制文件压缩优势与数据丢失窗口短板；
 *      2) AOF (Append Only File 追加日志): 写入流程 (aof_buf -> page cache -> fsync)、三种刷盘策略 (always, everysec, no) 权衡、AOF 重写 (BGREWRITEAOF) 机制；
 *      3) Redis 4.0+ 混合持久化 (Hybrid Persistence): aof-use-rdb-preamble 配置、RDB 二进制镜像 + 增量 AOF 日志的最佳实践；
 *      4) 实机联动: RedisTemplate 与底层的 INFO persistence、LASTSAVE、CONFIG GET 参数探测演示。
 */

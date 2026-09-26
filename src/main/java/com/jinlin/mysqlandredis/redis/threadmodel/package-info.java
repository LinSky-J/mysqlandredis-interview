/**
 * Redis 线程模型与网络 I/O 架构专题模块
 * <p>
 * 涵盖知识点：
 * 1. [RedisTm01] Redis 为什么快？（纯内存操作、单线程避免竞争与上下文切换、epoll I/O 多路复用、高效数据结构设计）
 * 2. [RedisTm02] Redis 哪些地方使用了多线程？（Redis 4.0 BIO 后台异步线程 UNLINK/FLUSHALL ASYNC，Redis 6.0 多线程网络 I/O 读写）
 * 3. [RedisTm03] Redis 怎么实现的 I/O 多路复用？（ae 事件驱动库、Linux epoll 三大系统调用、select vs poll vs epoll 深度对比）
 * 4. [RedisTm04] Redis 的网络模型是怎样的？（Reactor 单反应堆事件驱动架构、文件事件分派器与连接/读/写处理器完整生命周期）
 */
package com.jinlin.mysqlandredis.redis.threadmodel;

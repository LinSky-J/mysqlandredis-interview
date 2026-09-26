package com.jinlin.mysqlandredis.redis.threadmodel;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 线程模型篇 03: Redis 怎么实现的 I/O 多路复用？(ae 事件库/epoll底层机制/select vs epoll对比剖析)
 *
 * 面试真题：Redis 怎么实现的 io 多路复用？
 *
 * 核心原理深度剖析：
 * 1. 传统阻塞 I/O (BIO) 的致命缺陷：
 *    - 若采用同步阻塞模式，单线程调用 `read()` 时，如果客户端尚未发送数据，线程就会被挂起阻塞；
 *    - 此时其他成千上万个客户端的连接和请求将完全无法得到响应。
 *
 * 2. 什么是 I/O 多路复用 (I/O Multiplexing)？
 *    - “多路”：指多个网络客户端连接 (多个 Socket 文件描述符 FD)；
 *    - “复用”：指多个网络连接复用同一个线程来监听与处理；
 *    - 核心机制：让内核协助监听成百上千个 Socket。当没有任何 Socket 就绪时，线程休眠；
 *      一旦有一个或多个 Socket 有数据可读或可写，内核唤醒线程，线程仅遍历处理已就绪的连接。
 *
 * 3. Redis 源码级事件驱动库 (ae - A simple event-driven library)：
 *    - Redis 没有直接依赖 libevent 或 libuv，而是轻量级自研了 ae 事件驱动库 (ae.c)；
 *    - 跨平台自动多路复用选型机制：在编译阶段根据宿主操作系统能力，按性能优先级挑选最佳实现：
 *      #ifdef HAVE_EVPORT -> ae_evport.c (Solaris)
 *      #elif defined(HAVE_EPOLL) -> ae_epoll.c (Linux 首选，红黑树 + 就绪链表)
 *      #elif defined(HAVE_KQUEUE) -> ae_kqueue.c (macOS / BSD)
 *      #else -> ae_select.c (兜底方案，全平台支持但为 O(N) 遍历)
 *
 * 4. Linux epoll 三大系统调用在 Redis 中的映射关系：
 *    1) `epoll_create(size)` -> `aeCreateEventLoop()`: 创建 epoll 文件描述符与事件循环核心上下文；
 *    2) `epoll_ctl(epfd, op, fd, event)` -> `aeCreateFileEvent()`:
 *       将客户端 Socket FD 及关心的事件 (AE_READABLE 可读, AE_WRITABLE 可写) 挂载到内核红黑树上；
 *    3) `epoll_wait(epfd, events, maxevents, timeout)` -> `aeProcessEvents()`:
 *       事件主循环阻塞等待，当网络就绪时直接获取内核就绪链表中的就绪事件，逐个调用绑定的处理器函数。
 */
public class RedisTm03_IoMultiplexingEpollDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisTm03] Redis I/O 多路复用机制与 Linux epoll 核心原理解析");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 实机验证 Client Socket 交互
        System.out.println("[步骤 1] 演示通过 Spring Data Redis 客户端与本地 Redis 完成网络交互：");
        String testKey = "demo:tm:epoll_test";

        // Redis 原生指令: SET demo:tm:epoll_test epoll_active_event
        redisTemplate.opsForValue().set(testKey, "epoll_active_event");

        // Redis 原生指令: GET demo:tm:epoll_test
        String val = redisTemplate.opsForValue().get(testKey);
        System.out.println("  -> 成功写入并通过网络交互取回 Key: " + testKey + ", 值: " + val);

        // 2. select / poll 与 epoll 核心机制技术对比
        System.out.println("\n[步骤 2] 操作系统 I/O 多路复用机制演进全景对比 (select vs poll vs epoll)：");
        System.out.println("  +-------------------+----------------------+----------------------+------------------------------------------+");
        System.out.println("  | 对比维度          | select               | poll                 | epoll (Linux 2.6+，Redis 核心底座)       |");
        System.out.println("  +-------------------+----------------------+----------------------+------------------------------------------+");
        System.out.println("  | 最大连接数限制    | 默认 1024 (FD_SETSIZE)| 基于链表，无固定上限 | 仅受限于系统最大文件句柄数 (通常数十万)  |");
        System.out.println("  | 内核数据拷贝开销  | 每次调用全量拷贝所有FD| 每次调用全量拷贝所有FD| epoll_ctl 仅注册一次红黑树，无需重复拷贝 |");
        System.out.println("  | 事件检索时间复杂度| O(N) 线性轮询遍历整表| O(N) 线性轮询遍历整表| O(1) 直接读取就绪链表 (rdllist)          |");
        System.out.println("  | 触发机制模式      | 仅支持水平触发 (LT)  | 仅支持水平触发 (LT)  | 支持水平触发 (LT) 与边缘触发 (ET)        |");
        System.out.println("  | 连接规模与性能关联| 连接数越大，性能急剧衰退| 连接数越大，性能急剧衰退| 连接数增加基本不影响就绪处理耗时，吞吐稳定|");
        System.out.println("  +-------------------+----------------------+----------------------+------------------------------------------+");

        // 3. Redis ae 库伪代码运行拓扑展示
        System.out.println("\n[步骤 3] Redis 事件循环 (aeEventLoop) 运行拓扑 ASCII 图解：");
        System.out.println("  Client 1 (Socket FD 6)  ----\\");
        System.out.println("  Client 2 (Socket FD 8)  ----->  [ Linux epoll 内核红黑树监听池 ]");
        System.out.println("  Client 3 (Socket FD 15) ----/");
        System.out.println("                                               |");
        System.out.println("                                               v 当某个客户端发来数据时产生 EPOLLIN 事件");
        System.out.println("                                  [ epoll 就绪双向链表 (rdllist) ]");
        System.out.println("                                               |");
        System.out.println("                                               v epoll_wait 仅返回就绪的 FD 数组");
        System.out.println("                                  [ Redis 单线程主循环 aeProcessEvents() ]");
        System.out.println("                                               |");
        System.out.println("                                               v 根据事件类型分派处理");
        System.out.println("                                      readQueryFromClient() -> 命令解析与内存执行");
    }
}

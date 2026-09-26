package com.jinlin.mysqlandredis.redis.threadmodel;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 线程模型篇 02: Redis 哪些地方使用了多线程？(Redis 4.0 BIO 异步机制与 Redis 6.0 多线程 I/O 深度解析)
 *
 * 面试真题：Redis 哪些地方使用了多线程？
 *
 * 核心原理深度剖析：
 * 1. Redis 4.0 之前的纯粹单线程痛点：
 *    - 遇到大 Key (例如包含 100 万个元素的 Hash 或 ZSet)，执行 `DEL bigkey` 或 `FLUSHALL` 时，
 *      单线程必须同步释放这 100 万个节点的内存指针，耗时可能长达数秒，导致整个 Redis 实例彻底卡死！
 *
 * 2. Redis 4.0 引入的【BIO 后台辅助线程池 (Background I/O)】：
 *    - Redis 4.0 在主线程之外引入了三个独立的后台线程 (BIO 线程)：
 *      1) BIO_CLOSE_FILE: 负责后台异步关闭大文件描述符；
 *      2) BIO_AOF_FSYNC: 负责后台执行 AOF 磁盘同步刷盘 (fsync 系统调用)，避免磁盘写卡死主线程；
 *      3) BIO_LAZY_FREE: 负责后台异步释放大 Key 内存！
 *         - 指令支撑: `UNLINK` 异步删除 Key、`FLUSHALL ASYNC`、`FLUSHDB ASYNC`；
 *         - 机制: 主线程只做元数据摘链 (从全局字典解除引用)，耗时为 O(1)，实际大块内存由 BIO_LAZY_FREE 线程在后台慢慢回收。
 *
 * 3. Redis 6.0 引入的【I/O 多线程模型 (Multi-threaded I/O)】：
 *    - 引入背景: 随着网络硬件发展至 10G/40G 网卡，Redis 的性能瓶颈变成了【网络 Socket 读写的系统调用 CPU 开销】；
 *    - 分工架构设计 (核心！)：
 *      - 【命令执行引擎 (Memory Operation)】：依然由【主线程单线程执行】，绝对保持无锁原子性与执行顺序；
 *      - 【网络 I/O 读写 (Network Reading & Writing)】：由【I/O 线程池并发处理】！
 *        a. 读取阶段: 多个 I/O 线程并发从各个 Client Socket 读取原始字节流，并解析为客户端命令参数列表；
 *        b. 执行阶段: 主线程按顺序执行解析出来的命令，直接在内存中读写数据；
 *        c. 写回阶段: 多个 I/O 线程并发将执行结果打包为 RESP 协议并写回对应的客户端 Socket。
 */
public class RedisTm02_MultiThreadingInRedisDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisTm02] Redis 多线程演进路线 (Redis 4.0 异步 BIO 机制与 6.0 多线程 I/O 剖析)");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 实机操作演示: UNLINK 异步延迟删除与 DEL 同步删除对比
        System.out.println("[步骤 1] 演示 UNLINK 异步懒释放 (对应 Redis 4.0 BIO_LAZY_FREE 机制)：");
        String syncKey = "demo:tm:sync_del_key";
        String lazyKey = "demo:tm:async_unlink_key";

        // Redis 原生指令: SET demo:tm:sync_del_key sync_data_content
        redisTemplate.opsForValue().set(syncKey, "sync_data_content");
        // Redis 原生指令: SET demo:tm:async_unlink_key async_data_content
        redisTemplate.opsForValue().set(lazyKey, "async_data_content");

        // 同步删除: DEL 指令在主线程中同步释放内存
        // Redis 原生指令: DEL demo:tm:sync_del_key
        Boolean delResult = redisTemplate.delete(syncKey);
        System.out.println("  -> [DEL 同步删除] 主线程原地回收 Key: " + syncKey + ", 结果: " + delResult);

        // 异步删除: UNLINK 仅在主线程摘链，内存释放交由后台 BIO 线程池完成
        // Redis 原生指令: UNLINK demo:tm:async_unlink_key
        Boolean unlinkResult = redisTemplate.unlink(lazyKey);
        System.out.println("  -> [UNLINK 异步删除] 主线程 O(1) 摘除索引，后台线程异步释放: " + lazyKey + ", 结果: " + unlinkResult);

        // 2. Redis 发展版本中线程模型演进架构表
        System.out.println("\n[步骤 2] Redis 线程模型演进全景对照矩阵：");
        System.out.println("  +-----------------+---------------------------+----------------------------------------------+");
        System.out.println("  | Redis 版本      | 线程架构设计              | 核心定位与职责分工                           |");
        System.out.println("  +-----------------+---------------------------+----------------------------------------------+");
        System.out.println("  | Redis 4.0 之前  | 纯粹单线程                | 网络 IO、内存命令执行、持久化清理均在主线程  |");
        System.out.println("  | Redis 4.0 时代  | 单主线程 + BIO 后台线程池 | 主线程负责核心业务，引入 3 个后台线程异步处理|");
        System.out.println("  |                 |                           | 文件关闭、AOF 刷盘与 UNLINK 大 Key 异步释放  |");
        System.out.println("  | Redis 6.0 时代  | 多线程 I/O + 单线程命令执行| 多线程并发负责网络读写与协议解析，核心内存   |");
        System.out.println("  |                 |                           | 数据操作依然由单线程无锁执行，兼顾性能与安全 |");
        System.out.println("  +-----------------+---------------------------+----------------------------------------------+");

        // 3. 面试高频辨析: Redis 6.0 开启多线程会存在并发安全问题吗？
        System.out.println("\n[步骤 3] 经典面试追问: Redis 6.0 引入多线程后，还会是线程安全的吗？");
        System.out.println("  答: 【绝对依然是线程安全的】！因为 Redis 6.0 的多线程仅仅用于分担网络 I/O 读写 (Socket read/write)");
        System.out.println("      以及网络协议解析 (RESP 解析)，而【所有核心数据的读写命令执行】依然严格由【单线程】串行执行，");
        System.out.println("      因此对于开发者来说，Redis 依然完全不需要加分布式锁去防御 Redis 内部的并发竞态！");
    }
}

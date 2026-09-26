package com.jinlin.mysqlandredis.redis.threadmodel;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 线程模型篇 01: Redis 为什么快？(纯内存/单线程/非阻塞多路复用/高效数据结构深度剖析)
 *
 * 面试真题：Redis 为什么快？
 *
 * 核心原理深度剖析：
 * 1. 极致的【纯内存操作】(Memory-First Architecture)：
 *    - 内存的寻道与读写延迟在 100ns 纳秒级别；
 *    - 对比传统的机械磁盘 (10ms) 或 SSD (100μs)，内存存取速度高出 3 到 5 个数量级；
 *    - Redis 绝大多数读写操作纯粹是在内存中完成指针重定向与哈希定位，没有磁盘 I/O 阻塞。
 *
 * 2. 核心操作引擎采用【单线程模型】：
 *    - 杜绝上下文切换开销：多线程并发时，操作系统频繁切换线程 CPU 寄存器与栈指针，带来巨大 CPU 损耗；
 *    - 杜绝锁竞争与死锁：不需要任何互斥锁 (Mutex)、读写锁或并发控制，没有加锁、释放锁与死锁排查成本；
 *    - CPU 高速缓存 (L1/L2 Cache) 极其友好，缓存命中率极高。
 *
 * 3. 基于【I/O 多路复用 (I/O Multiplexing)】的 Reactor 事件驱动模型：
 *    - 利用操作系统底层机制 (Linux 下为 epoll，macOS 下为 kqueue)；
 *    - 一个线程监听成千上万个客户端 Socket 套接字，哪个连接有事件就通知处理哪个，避免线程阻塞在网络 I/O 读写上。
 *
 * 4. 极致精巧的【底层数据结构设计】：
 *    - 简单动态字符串 (SDS)：O(1) 获取长度，空间预分配与惰性释放；
 *    - 压缩列表 / 紧凑列表 (ziplist / listpack)：连续内存分配，内存利用率极高，提升 CPU 缓存局部性；
 *    - 字典 (dict)：支持渐进式 rehash，避免单次扩容阻塞单线程；
 *    - 跳跃表 (skiplist)：O(logN) 对数级查找与排名，实现复杂度远低于红黑树与 B+ 树。
 *
 * 5. 极简高效的【RESP 通信协议】：
 *    - Redis 自研的 RESP (REdis Serialization Protocol) 协议二进制安全、解析复杂度极低。
 */
public class RedisTm01_WhyRedisIsFastDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisTm01] Redis 为什么快？(纯内存访问基准性能与多维度底层根因解析)");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 实机性能微基准演练：验证内存极速写入响应
        System.out.println("[步骤 1] 本地实测 RedisTemplate 单线程连续读写 500 次的极速性能：");
        String benchKey = "demo:fast:counter";

        // Redis 原生指令: DEL demo:fast:counter
        redisTemplate.delete(benchKey);

        long start = System.nanoTime();
        int iterations = 500;
        for (int i = 0; i < iterations; i++) {
            // Redis 原生指令: INCR demo:fast:counter
            redisTemplate.opsForValue().increment(benchKey);
        }
        long durationNano = System.nanoTime() - start;
        double durationMs = durationNano / 1_000_000.0;
        double opsPerSec = (iterations / durationMs) * 1000.0;

        // Redis 原生指令: GET demo:fast:counter
        String finalValue = redisTemplate.opsForValue().get(benchKey);
        System.out.println("  -> 连续执行 500 次 INCR 耗时: " + String.format("%.2f", durationMs) + " ms");
        System.out.println("  -> 最终计数器数值: " + finalValue + ", 测算 QPS (含单次网络往返): " + String.format("%.0f", opsPerSec) + " ops/sec");

        // 2. 硬件层级访问延迟对照全景
        System.out.println("\n[步骤 2] 硬件层级访问延迟对比 (为什么内存比磁盘快上万倍？)：");
        System.out.println("  +----------------------+----------------------+----------------------------------------+");
        System.out.println("  | 存储介质层级         | 典型访问耗时         | 相对纳秒比率 (折算比例)                |");
        System.out.println("  +----------------------+----------------------+----------------------------------------+");
        System.out.println("  | CPU L1 Cache 缓存    | ~0.5 ns              | 1 (基准)                               |");
        System.out.println("  | CPU L2 Cache 缓存    | ~7 ns                | 14 倍                                  |");
        System.out.println("  | 主内存 (RAM - Redis) | ~100 ns              | 200 倍 (纳秒级，极速)                   |");
        System.out.println("  | NVMe SSD 固态硬盘    | ~100,000 ns (100 μs) | 200,000 倍 (微秒级)                    |");
        System.out.println("  | 传统机械硬盘 (HDD)   | ~10,000,000 ns (10ms)| 20,000,000 倍 (毫秒级，存在机械磁头寻道)|");
        System.out.println("  +----------------------+----------------------+----------------------------------------+");

        // 3. 为什么单线程反而更快？
        System.out.println("\n[步骤 3] 为什么多线程在 Redis 场景下并不一定更好？");
        System.out.println("  1) 内存瓶颈分析: Redis 的瓶颈主要在于【机器内存大小】与【网络带宽】，而非 CPU 计算能力；");
        System.out.println("  2) 线程开销分析: 如果引入多线程并发操作同一哈希表，必须对全局 DB、键空间、节点链表加互斥锁；");
        System.out.println("  3) 锁开销逆转: 高并发下锁争抢与线程上下文切换损耗将远超单线程内存执行耗时，得不偿失！");
    }
}

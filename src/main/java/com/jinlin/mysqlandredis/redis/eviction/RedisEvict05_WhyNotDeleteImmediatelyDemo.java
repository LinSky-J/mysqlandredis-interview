package com.jinlin.mysqlandredis.redis.eviction;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Properties;

/**
 * Redis 缓存淘汰与过期删除篇 05: 那为什么我不过期立即删除？(深度架构取舍与性能根因)
 *
 * 面试真题：那为什么我不过期立即删除?
 *
 * 核心原理深度剖析：
 *
 * 一、架构设计的根本哲学：【CPU 算力与吞吐量 >> 局部暂存的内存空间】
 * Redis 官方在设计之初就明确确立了核心原则：
 * “Redis 首先是一个追求极致高吞吐、微秒级超低延迟的内存数据存储系统。
 *  任何会打断、拖慢核心读写事件循环的设计，都必须被坚决舍弃！”
 *
 * 二、不过期立即删除的 3 大核心技术根因：
 *
 * 1. 【CPU 资源被严重挤占与雪崩式中断开销】：
 *    - 若采取“过期立即删除”，必须为每一个设置了 TTL 的 Key 分配一个定时器 (Timer)；
 *    - 生产环境中一个中大型 Redis 实例往往存储数百万甚至数千万个带过期时间的 Key (如 Token、验证码、热点商品缓存)；
 *    - 如果有数万个 Key 在同一秒或微秒内集中到期 (如整点批量推送、同一批缓存失效)，将引发定时器海啸！
 *    - CPU 将陷入无休止的中断处理、上下文切换与删除逻辑中，此时客户端发来的关键读写请求将严重积压、超时甚至连接耗尽瘫痪。
 *
 * 2. 【高精度定时器数据结构维护成本过于高昂】：
 *    - 要支撑海量定时器的精确触发，底层必须维护如【最小堆 (Min-Heap)】或【多级时间轮 (Timing Wheel)】：
 *      1) 算法复杂度开销：当数百万 Key 频繁 SETEX、EXPIRE 调整 TTL、或者显式 DEL 时，
 *         最小堆需要频繁进行 O(log N) 的上浮和下沉调整，高并发下这一开销会严重拉低单线程处理能力；
 *      2) 内存膨胀开销：每个定时器事件节点需要额外维护前驱、后继或堆指针等元数据，
 *         在海量小对象场景下，定时器结构所耗费的内存甚至可能超过业务数据本身！
 *
 * 3. 【单线程 Reactor 事件循环体系的天然互斥】：
 *    - Redis 核心读写由单个事件循环主线程驱动 (aeProcessEvents)；
 *    - 如果主线程被海量定时器的删除回调所霸占，事件循环就无法及时处理网络套接字的 READ / WRITE 事件；
 *    - 内存是廉价且可控的 (有 maxmemory 兜底保护)，但单线程 CPU 的黄金时间片是绝对不能被浪费的稀缺资源！
 *
 * 三、Redis 最终交出的优雅工程答卷：
 * - 【按需惰性删除】：把判断逻辑平摊到每个业务请求中 (几乎 0 开销)；
 * - 【限时定期抽样】：通过 activeExpireCycle 严格限制抽样轮询时间 (最多执行 25ms 立即交还 CPU)，
 *   既保证了过期数据最终一定会收敛清理，又死死守住了“绝不卡顿主业务”的底线！
 */
public class RedisEvict05_WhyNotDeleteImmediatelyDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisEvict05] 深度解答：为什么 Redis 不过期立即删除？(性能/数据结构/CPU权衡)");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 模拟批量写入带过期时间的 Key 并观察服务器状态
        System.out.println("[步骤 1] 写入多个模拟会话 Key (观察过期键统计指标)：");
        String prefix = "demo:evict:why:session_";

        for (int i = 1; i <= 5; i++) {
            String key = prefix + i;
            // Redis 原生指令: SET demo:evict:why:session_1 SessionToken_1 EX 30
            // Redis 原生指令: SET demo:evict:why:session_2 SessionToken_2 EX 30
            // Redis 原生指令: SET demo:evict:why:session_3 SessionToken_3 EX 30
            // Redis 原生指令: SET demo:evict:why:session_4 SessionToken_4 EX 30
            // Redis 原生指令: SET demo:evict:why:session_5 SessionToken_5 EX 30
            redisTemplate.opsForValue().set(key, "SessionToken_" + i, Duration.ofSeconds(30));
        }
        System.out.println("  已批量写入 5 个带 30 秒 TTL 的业务 Session Key。");

        // 2. 底层探测当前数据库的 Key 统计与过期字典状态
        System.out.println("\n[步骤 2] 探测当前数据库的 Key 总数与 expires 统计：");
        redisTemplate.execute((RedisConnection connection) -> {
            // Redis 原生指令: INFO keyspace
            Properties keyspaceInfo = connection.info("keyspace");
            if (keyspaceInfo != null) {
                System.out.println("  [Keyspace 分区状态] db0: " + keyspaceInfo.getProperty("db0"));
            }
            // Redis 原生指令: INFO stats
            Properties statsInfo = connection.info("stats");
            if (statsInfo != null) {
                System.out.println("  [过期删除历史统计] expired_keys=" + statsInfo.getProperty("expired_keys")
                        + ", evicted_keys=" + statsInfo.getProperty("evicted_keys"));
            }
            return null;
        });

        // 3. 面试回答满分话术归纳
        System.out.println("\n[步骤 3] 面试官提问“为什么不过期立即删除？”的标准高分回答思路：");
        System.out.println("  1. 【核心本质】：这是 CPU 资源与内存资源的经典权衡取舍 (Trade-off)。");
        System.out.println("  2. 【三大致命痛点】：");
        System.out.println("     - 痛点一 (CPU 抢占)：立即删除需要为几千万 Key 创建定时器，集中到期将产生中断风暴，直接拖垮主线程吞吐量；");
        System.out.println("     - 痛点二 (数据结构沉重)：维护海量定时器需要最小堆或时间轮，O(log N) 调整成本与元数据内存开销不可接受；");
        System.out.println("     - 痛点三 (单线程模型互斥)：Redis 的单线程 Reactor 必须将最高优先级算力分配给网络读写，不能被清理阻塞。");
        System.out.println("  3. 【工程解决】：");
        System.out.println("     - 采用【惰性删除 + 严格限时 25ms 的定期抽样删除】，辅以【maxmemory 内存淘汰策略】兜底，");
        System.out.println("       在 CPU 性能最高化与内存可控之间达成了最优解。");
        System.out.println("================================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

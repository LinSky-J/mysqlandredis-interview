package com.jinlin.mysqlandredis.redis.eviction;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Properties;

/**
 * Redis 缓存淘汰与过期删除篇 03: 介绍一下 Redis 过期删除策略？
 *
 * 面试真题：介绍一下Redis过期删除策略
 *
 * 核心原理深度剖析：
 *
 * 一、计算机领域常见的 3 种过期清理理论方案：
 * 1. 【定时删除 (Timer-Based Immediate Deletion)】：
 *    - 机制：在设置 Key 过期时间的同时，创建一个定时器 (Timer)，当到达过期时间瞬间，由定时器立即执行物理删除；
 *    - 优点：对内存最友好，内存中的无用数据能第一时间被释放；
 *    - 致命缺点：对 CPU 极不友好！在海量 Key 过期时，大量定时器同时触发会霸占宝贵的 CPU 时间片，使 Redis 吞吐量断崖式暴跌。
 *
 * 2. 【惰性删除 (Lazy Deletion)】：
 *    - 机制：Key 过期后完全不予理睬；直到有客户端发起命令访问该 Key (如 GET/HGET/EXISTS) 时，
 *      Redis 的 `expireIfNeeded()` 函数才会检查该 Key 是否过期，若已过期则当场删除并向客户端返回 nil；
 *    - 优点：对 CPU 最友好，绝不花费多余的 CPU 时间去遍历搜索，只在被访问时顺手处理；
 *    - 致命缺点：对内存极不友好！若有大量 Key 过期后“再也不会被任何业务读取”，这些冷数据将永久滞留内存，等同于严重的内存泄漏。
 *
 * 3. 【定期删除 (Periodic / Active Deletion)】：
 *    - 机制：每隔一段时间，由后台定时任务主动抽取一部分过期字典中的 Key 进行检查并清理；
 *    - 优点：折中平衡方案，通过限制抽样数量与执行时长，既防止内存无限泄漏，又不过度消耗 CPU。
 *
 * 二、Redis 生产级实现：【惰性删除 + 定期抽样删除】黄金组合拳：
 * Redis 综合权衡后，坚决弃用了“定时删除”，而是采用【惰性删除 + 定期抽样删除】协同工作：
 *
 * 1. 惰性删除的底层运转 (`db.c / expireIfNeeded`)：
 *    - 所有读写命令在定位 Key 前，必先经过 `expireIfNeeded()`；
 *    - 若 Key 已超时，主节点执行 DEL (Redis 4.0+ 支持 `lazyfree-lazy-expire` 异步删除)，向客户端返回空；
 *    - 从节点 (Slave) 不执行惰性删除，而是等待主节点同步发送 DEL 命令 (严格服从主节点命令)。
 *
 * 2. 定期删除的底层运转 (`expire.c / activeExpireCycle`)：
 *    - 由后台主循环定时器 `serverCron` 周期性触发 (默认每秒运行 10 次，即 `hz 10`，每 100ms 一次)；
 *    - 每次运行的核心逻辑 (抽样 20 个法)：
 *      1) 从过期字典 `expires` 中随机抽取 20 个 Key；
 *      2) 遍历检查并删除其中已经过期的 Key；
 *      3) 判断：若过期 Key 的比例超过 25% (即 > 5 个)，说明内存中过期键非常多，立即循环重复执行步骤 1；
 *      4) 【时间片熔断保护机制】：为避免循环过久导致事件循环饥饿卡顿，activeExpireCycle 设有严格的执行时间上限 (默认不超过 25ms)。
 */
public class RedisEvict03_ExpirationDeletionStrategiesDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisEvict03] Redis 过期删除策略剖析：惰性删除 + 定期删除 activeExpireCycle");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 实机验证惰性删除行为
        System.out.println("[步骤 1] 演示惰性删除 (Lazy Deletion) 的实机工作流程：");
        String shortLivedKey = "demo:evict:strategy:lazy_test_key";

        // Redis 原生指令: SET demo:evict:strategy:lazy_test_key TemporaryValue EX 1
        redisTemplate.opsForValue().set(shortLivedKey, "TemporaryValue", Duration.ofSeconds(1));

        // 立即读取 (未过期)
        // Redis 原生指令: GET demo:evict:strategy:lazy_test_key
        String immediateVal = redisTemplate.opsForValue().get(shortLivedKey);
        System.out.println("  1) 刚写入立即读取: key=" + shortLivedKey + ", value=" + immediateVal + " (正常命中)");

        // 等待 1200 毫秒，使其自然过期
        System.out.println("  2) 线程休眠 1200ms 等待 TTL 自然到期...");
        try {
            Thread.sleep(1200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 此时 Key 已过期，再次 GET 将触发 expireIfNeeded() 检查并物理删除
        // Redis 原生指令: GET demo:evict:strategy:lazy_test_key
        String expiredVal = redisTemplate.opsForValue().get(shortLivedKey);
        System.out.println("  3) 过期后发起 GET 请求: value=" + expiredVal + " (触发 expireIfNeeded，被动删除并返回 null)");

        // 2. 探测定期删除的核心参数 (hz 配置)
        System.out.println("\n[步骤 2] 探测定期删除运行频率 hz 参数：");
        redisTemplate.execute((RedisConnection connection) -> {
            // Redis 原生指令: CONFIG GET hz
            Properties hzConfig = connection.getConfig("hz");
            System.out.println("  [定期删除调度频次] hz 参数: " + (hzConfig != null ? hzConfig.getProperty("hz") : "10")
                    + " (表示 serverCron 每秒触发 10 次，平均每 100ms 运行一次 activeExpireCycle 检查)");
            return null;
        });

        // 3. 架构对比总结
        System.out.println("\n[步骤 3] 三种过期删除策略横向对比总结：");
        System.out.println("  +--------------+-----------------------+-----------------------+-----------------------+");
        System.out.println("  | 策略类型     | 定时删除 (Immediate)  | 惰性删除 (Lazy)       | 定期删除 (Periodic)   |");
        System.out.println("  +--------------+-----------------------+-----------------------+-----------------------+");
        System.out.println("  | CPU 消耗     | 极高 (海量定时器竞争) | 极低 (仅访问时判断)   | 适中 (抽样可控，限时) |");
        System.out.println("  | 内存回收速度 | 极快 (到期立即释放)   | 极慢 (不访问永不释放) | 适中 (抽样比例推进)   |");
        System.out.println("  | 潜在风险     | 突发流量下 CPU 瘫痪   | 冷数据导致内存泄漏    | 需控制抽样频率与耗时  |");
        System.out.println("  | Redis 采用态 | 坚决不使用            | 核心组合之一          | 核心组合之一          |");
        System.out.println("================================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

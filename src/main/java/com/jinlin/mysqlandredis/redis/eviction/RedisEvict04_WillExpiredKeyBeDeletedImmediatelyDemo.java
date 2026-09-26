package com.jinlin.mysqlandredis.redis.eviction;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Redis 缓存淘汰与过期删除篇 04: Redis 的缓存失效会不会立即删除？
 *
 * 面试真题：Redis的缓存失效会不会立即删除?
 *
 * 核心原理深度剖析：
 *
 * 一、核心定性结论：
 * 【绝对不会立即删除！】
 * 当一个 Key 的生存时间 (TTL) 耗尽到达 0 时，该 Key 在 Redis 物理内存中依然完整地存在，
 * Redis 不会、也没有能力在 TTL 归零的精确瞬间去触发物理内存回收！
 *
 * 二、底层生命周期真相剖析：
 * 1. 为什么“不会立即删除”？
 *    - Redis 没有在 Key 上挂载硬件/软件级别的高精度实时定时器 (Timer)；
 *    - 当时间到达 TTL 的到期时间戳时，没有任何事件或中断去主动抹去该 Key。
 *
 * 2. 既然不立即删除，过期的 Key 是何时被物理释放的？
 *    它只有在以下三种时机之一被物理清除出内存：
 *    - 【时机 1：被动触发（惰性删除）】：
 *      当有任何客户端发起针对该 Key 的读写指令 (例如 GET / EXISTS / HGET / INCR) 时，
 *      Redis 的内部函数 `expireIfNeeded(redisDb *db, robj *key)` 会拦截请求并计算 Key 是否已过期。
 *      如果发现已到期，主库立即执行同步删除或异步释放 (Redis 4.0+ lazyfree)，并向客户端返回 nil。
 *    - 【时机 2：主动周期抽样（定期删除）】：
 *      后台主循环定时器 `serverCron` 按照 `hz` 频率 (默认每秒 10 次) 运行 `activeExpireCycle`。
 *      每次随机抽样 20 个带过期时间的 Key，如果抽中了该 Key 且判断已过期，则将其物理删除。
 *    - 【时机 3：内存溢出兜底（内存淘汰）】：
 *      若该 Key 既未被再次访问，又由于随机抽样的概率问题一直没被定期扫描抽中，
 *      当物理内存达到 `maxmemory` 阈值时，若配置了 `volatile-ttl` 或 `volatile-lru`，它会被内存淘汰算法强制剔除。
 *
 * 3. 面试高频追问：主从架构下从节点 (Slave) 会主动删除过期 Key 吗？
 *    - 答案：【绝对不会！】
 *    - 为了保障主从数据的一致性，从节点永远是被动的：
 *      即使客户端直接去读取 Slave 节点上一个已经超时的 Key，Slave 也绝不会在本地执行 DEL；
 *      Slave 只有接收到来自 Master 同步复制过来的显式 `DEL` 或 `UNLINK` 命令时，才将其从内存中移除！
 *      (注: Redis 3.2 之后，客户端读 Slave 上的过期键时，Slave 会基于本地时钟返回 nil，但内部物理删除仍然严格等待 Master 的 DEL 命令)。
 */
public class RedisEvict04_WillExpiredKeyBeDeletedImmediatelyDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisEvict04] 探究核心问题：Redis 的缓存失效会不会立即删除？");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 实机模拟：设置短暂过期的 Key 并揭示删除时机
        System.out.println("[步骤 1] 写入 1 秒过期的测试 Key，揭示其内存留存与释放真相：");
        String testKey = "demo:evict:del:stale_key";

        // Redis 原生指令: SET demo:evict:del:stale_key StalePayload EX 1
        redisTemplate.opsForValue().set(testKey, "StalePayload", Duration.ofSeconds(1));

        System.out.println("  1) 成功写入键: " + testKey + ", TTL 设为 1 秒。");

        // 等待 1500ms
        System.out.println("  2) 线程休眠 1500ms... 此时时间戳已经超过了 TTL 设定期限。");
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("  3) 深度揭秘：此时若没有任何请求访问该 Key，且未被定期抽样抽中，它依然安然停留在物理内存字典中！");

        // 客户端发起读取，此时被动触发 expireIfNeeded
        // Redis 原生指令: GET demo:evict:del:stale_key
        String value = redisTemplate.opsForValue().get(testKey);
        System.out.println("  4) 客户端发起 GET 访问，触发惰性删除机制，返回结果: " + value + " (物理删除在此刻发生并释放内存)");

        // 2. 梳理面试回答标准框架
        System.out.println("\n[步骤 2] 面试标准回答要点提炼：");
        System.out.println("  1. 【断然否定】：不会立即删除。Redis 不会为每个 Key 设置定时器。");
        System.out.println("  2. 【三大释放时机】：");
        System.out.println("     - 惰性删除：有客户端访问时被动触发 expireIfNeeded；");
        System.out.println("     - 定期删除：serverCron 周期性抽样 20 个 Key 并清理；");
        System.out.println("     - 内存淘汰：maxmemory 爆满时触发淘汰策略强制释放。");
        System.out.println("  3. 【主从特性补充 (亮点加分项)】：");
        System.out.println("     - 从库不具备过期清理自主权，严格等待主库下发 DEL 指令。");
        System.out.println("================================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

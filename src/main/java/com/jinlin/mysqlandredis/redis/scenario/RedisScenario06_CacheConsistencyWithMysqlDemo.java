package com.jinlin.mysqlandredis.redis.scenario;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Redis 业务场景篇 06: 如何保证 Redis 和 MySQL 数据缓存一致性？(Cache-Aside、延时双删与 Canal Binlog 架构)
 *
 * 面试真题：如何保证 redis 和 mysql 数据缓存一致性问题?
 *
 * 核心原理深度剖析：
 *
 * 一、核心定性认知：
 * - Redis 和 MySQL 是两个物理隔离、独立运行的网络分布式组件，天然无法做到无损的“强一致性 (Strong Consistency)”；
 * - 引入 2PC/XA 分布式事务虽然能在理论上保证强一致，但其加锁、网络协商和等待提交的开销会使 QPS 断崖式下跌，完全违背了引入缓存追求极致性能的初心；
 * - 因此，工业界的核心追求是：【保障数据的最终一致性 (Eventual Consistency)，并尽可能缩短不一致的时间窗口】！
 *
 * 二、为什么是【删除缓存】，而不是【更新缓存】？
 * 1. 避免无意义的频繁计算 (写多读少场景)：
 *    - 若缓存的构造需要经过多表复杂计算 (例如聚合订单、统计指标)，每次写 DB 都同步计算一次新缓存极度浪费 CPU；
 * 2. 避免并发写导致的永久数据混乱 (脏数据覆盖)：
 *    - 场景：线程 A 更新 DB -> 线程 B 更新 DB -> 线程 B 先更新了 Redis -> 线程 A 因网络延迟后更新了 Redis；
 *    - 结果：DB 中保存的是 B 的最新值，而 Redis 却被 A 的旧值永久覆盖！
 *    - 而“删除缓存”具备天然幂等性，无论顺序如何，后续的读请求都会从 DB 重新加载最新数据。
 *
 * 三、更新时序推演：【先删缓存 vs 先更数据库】？
 *
 * 1. 【方案一：先删缓存，再更新数据库 (有致命缺陷)】：
 *    - 并发缺陷时序：
 *      1) 线程 A 先删除了 Redis 缓存；
 *      2) 此时线程 B 进来读数据，发现缓存为空，便去查询 MySQL (此时查到了老旧数据)；
 *      3) 线程 B 把查到的旧数据重新写入了 Redis；
 *      4) 随后线程 A 才把新数据写入 MySQL；
 *      5) 结果：MySQL 存的是新数据，Redis 存的是旧数据，缓存被永久污染！
 *    - 补救手段：【延时双删 (Delayed Double Deletion)】：
 *      - 线程 A 更新完 MySQL 后，休眠一段时间 (如 500ms，等待读线程执行完毕)，再次删除一次缓存；
 *      - 缺点：Sleep 时间极难精确评估，消耗线程吞吐，且第二次删除依然有失败概率。
 *
 * 2. 【方案二：先更新数据库，再删除缓存 (Cache-Aside 经典推荐模式)】：
 *    - 理论上的并发冲突窗口极其狭窄 (几乎不可能发生)：
 *      - 只有在“读线程查缓存未命中 -> 查 MySQL 旧值 -> 此时写线程更新 MySQL -> 写线程删除缓存 -> 读线程把旧值写入缓存”；
 *      - 因为写 MySQL (涉及磁盘刷盘与行锁) 的耗时远大于读 MySQL 并在内存写缓存的耗时，读线程几乎总会在写线程更新前完成；
 *    - 实际最大风险点：【更新数据库成功了，但随后的 Redis DEL 因网络抖动失败了】，导致旧缓存残留。
 *
 * 四、生产级解决缓存删除失败的终极方案：
 *
 * 1. 【方案 1：消息队列 (MQ) 异步重试保障】：
 *    - 更新数据库成功后，发送一条包含 Key 的消息到 MQ；
 *    - 专门的消费服务监听 MQ 并执行 `redis.del(key)`；若网络失败，利用 MQ 的重试与死信队列机制重试直至删除成功。
 *
 * 2. 【方案 2：基于 Canal 监听 MySQL Binlog 异步删除 (大厂核心通用架构)】：
 *    - 业务系统只需纯粹专注于写 MySQL 数据库，完全无需侵入编写删除 Redis 的代码；
 *    - 部署 Alibaba Canal 中间件，将其伪装成 MySQL Slave 实时监听并解析 MySQL 的 `Row-based Binlog`；
 *    - 当数据表发生 UPDATE/DELETE 时，Canal 抓取主键并将其发送到 Kafka/RocketMQ，由独立消费者调用 Redis 执行 `DEL key`；
 *    - 优势：代码解耦、即使 Redis 宕机重启也能通过消费 Binlog 追溯补偿，彻底保障最终一致性！
 *
 * 3. 【所有 Key 必须配置兜底 TTL】：
 *    - 无论采用何种一致性机制，所有缓存 Key 必须强制设置合理的过期时间 (如 30 分钟或 2 小时) 作为终极安全兜底网。
 */
public class RedisScenario06_CacheConsistencyWithMysqlDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisScenario06] Redis 与 MySQL 缓存一致性：Cache-Aside、延时双删与 Canal 架构");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 实机模拟：Cache-Aside 模式下的读取与缓存重建
        System.out.println("[步骤 1] 演示 Cache-Aside 读链路：读缓存未命中时从 DB 加载并写入带 TTL 缓存：");
        String userCacheKey = "demo:scenario:user:profile:9001";

        // 先尝试读取缓存
        // Redis 原生指令: GET demo:scenario:user:profile:9001
        String cachedVal = redisTemplate.opsForValue().get(userCacheKey);
        System.out.println("  1) 首次读取缓存: key=" + userCacheKey + ", value=" + cachedVal + " (Cache Miss 缓存未命中)");

        // 模拟从 MySQL 数据库查询得到最新数据: "User_Alice_Dept_IT"
        String dbData = "User_Alice_Dept_IT";
        System.out.println("  2) 查 MySQL 获取数据: " + dbData + ", 写入 Redis 并设置 1 小时安全兜底 TTL");

        // Redis 原生指令: SET demo:scenario:user:profile:9001 User_Alice_Dept_IT EX 3600
        redisTemplate.opsForValue().set(userCacheKey, dbData, Duration.ofHours(1));

        // 2. 模拟 Cache-Aside 模式下的写链路：先更新 MySQL，再删除 Redis 缓存
        System.out.println("\n[步骤 2] 演示 Cache-Aside 写链路：业务修改数据，先写 MySQL，随后删除缓存：");
        String updatedDbData = "User_Alice_Dept_Finance";
        System.out.println("  1) 业务执行写操作：更新 MySQL 数据库成功 (新数据: " + updatedDbData + ")");

        // 紧接着删除 Redis 缓存，保证后续读请求重新从 DB 获取最新数据
        // Redis 原生指令: DEL demo:scenario:user:profile:9001
        Boolean deleteSuccess = redisTemplate.delete(userCacheKey);
        System.out.println("  2) 删除 Redis 缓存结果: " + deleteSuccess + " (后续读请求将平滑从 DB 重新加载最新数据)");

        // 3. 架构对比总结
        System.out.println("\n[步骤 3] 面试回答“如何保证 Redis 与 MySQL 一致性”高分答题框架：");
        System.out.println("  1. 【定性原则】：无法做到低成本的强一致性，核心是以极小代价追求“最终一致性”；");
        System.out.println("  2. 【删还是更？】：坚决选择【删除缓存】而非更新缓存，避免无效计算和并发脏写覆盖；");
        System.out.println("  3. 【时序选择】：坚决选择【先更 MySQL，再删 Redis】(Cache-Aside)，先删缓存容易因并发读引起永久脏数据污染；");
        System.out.println("  4. 【删失败兜底保障】：");
        System.out.println("     - 机制 A：MQ 异步重试消费；");
        System.out.println("     - 机制 B (生产推荐)：使用 Alibaba Canal 监听解析 MySQL Binlog 异步解耦删除；");
        System.out.println("     - 机制 C (不可或缺的底线)：所有缓存必须加兜底 TTL 过期时间！");
        System.out.println("================================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

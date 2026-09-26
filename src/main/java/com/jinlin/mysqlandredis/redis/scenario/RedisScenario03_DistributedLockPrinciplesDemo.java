package com.jinlin.mysqlandredis.redis.scenario;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * Redis 业务场景篇 03: Redis 分布式锁的实现原理？什么场景下用到分布式锁？(全景演进、看门狗续期与红锁)
 *
 * 面试真题：Redis分布式锁的实现原理？什么场景下用到分布式锁?
 *
 * 核心原理深度剖析：
 *
 * 一、什么场景下用到分布式锁？
 * 1. 单机 JVM 锁的局限性：
 *    - Java 自带的 `synchronized` 和 `ReentrantLock` 基于单进程 JVM 内存对象头或 AQS 实现，
 *      只能约束同一个 JVM 内部的并发线程；
 *    - 在现代分布式微服务集群部署下，同一个业务接口部署在多个 Pod/服务器节点上，
 *      单机锁完全失效，必须依赖跨网络集中的【分布式锁】进行全局资源协调。
 * 2. 经典高频业务应用场景：
 *    - 电商防并发重复下单、防重复点击扣款；
 *    - 分布式定时任务调度：多个相同服务节点只能有且仅有 1 个节点执行日终对账或批量发券；
 *    - 秒杀/特惠活动防超卖与防刷单；
 *    - 缓存热点击穿防御：当热点缓存失效时，通过分布式锁只允许 1 个线程去查 MySQL 重建缓存，保护数据库。
 *
 * 二、Redis 分布式锁的 4 代演进历程与核心原理：
 *
 * 1. 【第一代 (错误初版)：SETNX + EXPIRE 分步执行】：
 *    - 缺陷：若客户端执行 SETNX 成功后，突然断电或重启，导致 EXPIRE 未能执行，该锁将变为“永不过期”，引发永久死锁 (Deadlock)！
 *
 * 2. 【第二代 (原生复合原子指令)：SET key value NX EX seconds】：
 *    - 原理：将“判断不存在加锁”与“设置自动超时时间”合并为单条底层 Redis 指令原子执行；
 *    - 杜绝了加锁与设超时的中间断裂，保证极端宕机下锁最终一定会由超时机制兜底释放。
 *
 * 3. 【第三代 (防误删机制)：注入唯一客户端线程标识 (UUID)】：
 *    - 痛点场景：Client A 加锁 (TTL=30s)，但由于业务处理卡顿耗时 35s。30s 时锁因超时被 Redis 自动删除；
 *      此时 Client B 成功抢锁；随后 Client A 业务执行结束调用 DEL，直接把 Client B 的锁给误删了！
 *    - 解决方案：Value 存入当前客户端生成的唯一凭证 (UUID + 线程ID)，释放时先核验是否为当前线程持有。
 *
 * 4. 【第四代 (原子安全释放)：Lua 脚本 Compare-And-Delete】：
 *    - 痛点：Java 中“判断 Value 相等”和“DEL 释放”是两个独立操作，高并发下仍然存在时间窗口竞态；
 *    - 方案：通过 Lua 脚本原子执行：`if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end`。
 *
 * 三、生产级黄金解决方案：Redisson 看门狗 (Watchdog) 自动续期机制：
 * 1. 解决痛点：业务执行时间不确定，锁的有效时间设太长影响故障恢复，设太短业务没跑完就提前释放；
 * 2. 看门狗运行机制：
 *    - 客户端抢锁成功后，若未显式指定 leaseTime，Redisson 默认设置超时时间为 30 秒 (`lockWatchdogTimeout`)；
 *    - 启动后台定时任务 (Netty Timer)，每隔 10 秒 (即超时时间的 1/3) 检查主线程是否仍在执行业务；
 *    - 若主业务未结束，自动向 Redis 发送 `PEXPIRE` 将锁的存活时间重置刷新回 30 秒；
 *    - 当业务结束调用 unlock() 时，看门狗定时器随之被注销；若服务突发宕机，看门狗停止续期，30 秒后锁自然释放，完美平衡！
 * 3. 红锁 (Redlock) 算法：
 *    - 针对 Redis 主从异步复制导致的主库加锁未同步至从库即宕机的极端场景；
 *    - 客户端向 N 个 (通常 5 个) 独立部署的 Master 发送加锁命令，超过半数成功才判定加锁成功；
 *    - 生产评价：因运维复杂度过高且对时钟跳跃敏感，业界在金融强一致场景更青睐 ZooKeeper/Etcd 或关系数据库。
 */
public class RedisScenario03_DistributedLockPrinciplesDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisScenario03] Redis 分布式锁核心原理：SET NX EX、UUID防误删、Lua释放与看门狗");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 实机模拟加锁过程
        System.out.println("[步骤 1] 模拟客户端 A 抢占分布式锁 (SET NX EX)：");
        String lockKey = "demo:scenario:lock:order:5001";
        String clientA_Token = UUID.randomUUID().toString() + "_ThreadA";
        String clientB_Token = UUID.randomUUID().toString() + "_ThreadB";

        // Redis 原生指令: SET demo:scenario:lock:order:5001 UUID_TOKEN_CLIENT_A NX EX 30
        Boolean isLockSuccessA = redisTemplate.opsForValue().setIfAbsent(lockKey, clientA_Token, Duration.ofSeconds(30));
        System.out.println("  Client A 加锁尝试结果: " + isLockSuccessA + " (持有锁 Token: " + clientA_Token + ")");

        // 2. 模拟客户端 B 并发争抢同一把锁 (互斥拦截)
        System.out.println("\n[步骤 2] 模拟客户端 B 在锁被占用期间争抢同一把锁：");
        // Redis 原生指令: SET demo:scenario:lock:order:5001 UUID_TOKEN_CLIENT_B NX EX 30
        Boolean isLockSuccessB = redisTemplate.opsForValue().setIfAbsent(lockKey, clientB_Token, Duration.ofSeconds(30));
        System.out.println("  Client B 加锁尝试结果: " + isLockSuccessB + " (成功实现分布式资源互斥拦截)");

        // 3. 模拟客户端 B 试图误删客户端 A 的锁 (防误删核验)
        System.out.println("\n[步骤 3] 验证防误删机制：Client B 尝试通过 Lua 脚本删除非自己持有的锁：");
        String luaScript =
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "    return redis.call('del', KEYS[1]) " +
                "else " +
                "    return 0 " +
                "end";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(luaScript, Long.class);

        Long deleteResultB = redisTemplate.execute(redisScript, Collections.singletonList(lockKey), clientB_Token);
        System.out.println("  Client B 执行删除结果: " + deleteResultB + " (Token 不匹配，拒绝删除，防止误删他人锁)");

        // 4. 客户端 A 正常执行完毕，安全释放自己的锁
        System.out.println("\n[步骤 4] Client A 业务执行完毕，通过原子 Lua 脚本安全释放自身持有的锁：");
        Long deleteResultA = redisTemplate.execute(redisScript, Collections.singletonList(lockKey), clientA_Token);
        System.out.println("  Client A 执行删除结果: " + deleteResultA + " (Token 匹配成功，锁被原子安全删除)");

        // 5. 总结答题要点
        System.out.println("\n[步骤 5] 面试“Redis 分布式锁”标准高分回答骨架：");
        System.out.println("  1. 【使用场景】：微服务多节点并发、分布式定时任务抢占、防重复下单、热点击穿互斥重建；");
        System.out.println("  2. 【加锁核心】：原生原子指令 `SET key value NX EX seconds`，杜绝死锁与两步断裂；");
        System.out.println("  3. 【防误删机制】：Value 存入唯一客户端 Token/UUID，释放时先比较再删除；");
        System.out.println("  4. 【释放核心】：必须通过 Lua 脚本实现 Compare-And-Delete 原子性；");
        System.out.println("  5. 【生产标准】：引入 Redisson Watchdog 看门狗，每隔 1/3 超时时间后台自动续期，彻底解决业务耗时超出锁有效期的行业痛点！");
        System.out.println("================================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

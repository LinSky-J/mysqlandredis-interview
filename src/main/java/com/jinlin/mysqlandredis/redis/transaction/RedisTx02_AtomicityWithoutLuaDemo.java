package com.jinlin.mysqlandredis.redis.transaction;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis 事务与原子性篇 02: 除了 Lua 有什么也能保证 Redis 的原子性？(WATCH乐观锁/原生复合原子指令/Functions/分布式锁)
 *
 * 面试真题：除了 lua 有没有什么也能保证 redis 的原子性?
 *
 * 核心回答要点与原理：
 * 1. 【WATCH + MULTI / EXEC 乐观锁 CAS (Check-And-Set) 机制】：
 *    - 机制：客户端在开启事务前，使用 `WATCH key` 监听一个或多个 Key；
 *    - 当客户端提交 `EXEC` 时，Redis 检查被 WATCH 的 Key 自监听后是否被其他客户端修改过：
 *      - 若被修改过：整个事务被全部驳回，EXEC 返回 nil (空应答)，客户端可结合业务自旋重试；
 *      - 若未被修改：事务内部命令正常执行提交；
 *    - 本质：利用乐观锁 CAS 机制保障复合操作的数据一致性与原子性。
 *
 * 2. 【Redis 原生原生复合原子命令 (Composite Atomic Commands)】：
 *    - 场景 1: `SET key value NX EX seconds`
 *      - 作用: 原子实现【不存在才设置 + 附带过期时间】；
 *      - 价值: 彻底消除了早期 `SETNX` 成功但随后 `EXPIRE` 失败导致死锁的非原子性隐患！
 *    - 场景 2: `MSETNX key1 val1 key2 val2 ...`
 *      - 作用: 原子性批量设置多个不存在的键，只要有一个键已存在，所有键都不会被写入（全成功或全失败）。
 *    - 场景 3: `GETSET key value` (Redis 6.2+ 演进为 `SET key value GET`)
 *      - 作用: 原子获取旧值并写入新值，常用于系统计数复位或无锁化 Leader 选举。
 *
 * 3. 【Redis 7.0 引入的 Redis Functions (服务端持久化函数)】：
 *    - 演进背景: 弥补普通 EVAL Lua 脚本每次传输冗长源码、EVALSHA 容易在从节点未加载报错的缺陷；
 *    - 原理: 使用 `FUNCTION LOAD` 将函数持久化嵌入 Redis 引擎（写入 AOF 与 RDB 同步到从库），使用 `FCALL` 调用，同样由单线程原子串行执行。
 *
 * 4. 【应用层分布式锁 (如 Redisson 分布式锁 / 分布式信号量)】：
 *    - 在应用服务层通过加分布式锁 (如 Redisson 的 RLock)，将并发请求在客户端侧串行化，从外部保障业务原子性。
 */
public class RedisTx02_AtomicityWithoutLuaDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisTx02] 除了 Lua 之外实现 Redis 原子性的三大替代方案实机演示");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 实机演示方案一: 原生复合原子指令 SET key value NX EX
        System.out.println("[方案 1] 使用原生复合原子指令 SET NX EX 保证分布式加锁原子性：");
        String lockKey = "demo:tx:atomic_lock";

        // Redis 原生指令: DEL demo:tx:atomic_lock
        redisTemplate.delete(lockKey);

        // 第一次抢占锁 (设置不存在才写入，且设置 30 秒过期时间)
        // Redis 原生指令: SET demo:tx:atomic_lock client_001 NX EX 30
        Boolean lockAcquired1 = redisTemplate.opsForValue().setIfAbsent(lockKey, "client_001", Duration.ofSeconds(30));
        System.out.println("  -> [Client 1 抢锁] 执行 SET NX EX 30s 结果: " + lockAcquired1 + " (成功加锁)");

        // 第二个客户端尝试抢占同一把锁 (预期返回 false)
        // Redis 原生指令: SET demo:tx:atomic_lock client_002 NX EX 30
        Boolean lockAcquired2 = redisTemplate.opsForValue().setIfAbsent(lockKey, "client_002", Duration.ofSeconds(30));
        System.out.println("  -> [Client 2 抢锁] 执行 SET NX EX 30s 结果: " + lockAcquired2 + " (被互斥拦截，原子性生效)");

        // 2. 实机演示方案二: 原生多键批量原子命令 MSETNX
        System.out.println("\n[方案 2] 使用 MSETNX 原生命令实现多键全成全败原子写入：");
        String keyA = "demo:tx:msetnx_a";
        String keyB = "demo:tx:msetnx_b";

        // Redis 原生指令: DEL demo:tx:msetnx_a demo:tx:msetnx_b
        redisTemplate.delete(java.util.Arrays.asList(keyA, keyB));

        Map<String, String> initialBatch = new HashMap<>();
        initialBatch.put(keyA, "val_a");
        initialBatch.put(keyB, "val_b");

        // 第一次批量插入 (两键都不存在，预期成功写入)
        // Redis 原生指令: MSETNX demo:tx:msetnx_a val_a demo:tx:msetnx_b val_b
        Boolean msetnxResult1 = redisTemplate.opsForValue().multiSetIfAbsent(initialBatch);
        System.out.println("  -> 首次执行 MSETNX 两键均不存在，写入结果: " + msetnxResult1);

        // 再次尝试插入 (由于 keyA 已存在，整批操作都将放弃)
        Map<String, String> secondBatch = new HashMap<>();
        secondBatch.put(keyA, "new_val_a");
        secondBatch.put("demo:tx:msetnx_c", "val_c");

        // Redis 原生指令: MSETNX demo:tx:msetnx_a new_val_a demo:tx:msetnx_c val_c
        Boolean msetnxResult2 = redisTemplate.opsForValue().multiSetIfAbsent(secondBatch);
        System.out.println("  -> 二次执行 MSETNX 因 keyA 已存在，写入结果: " + msetnxResult2 + " (全成或全败原子性保障)");

        // 3. 实机演示方案三: WATCH + MULTI/EXEC 乐观锁并发检测
        System.out.println("\n[方案 3] 使用 WATCH 乐观锁实现 CAS 冲突拦截：");
        String watchKey = "demo:tx:stock";

        // Redis 原生指令: SET demo:tx:stock 10
        redisTemplate.opsForValue().set(watchKey, "10");

        List<Object> watchTxResult = redisTemplate.execute(new SessionCallback<List<Object>>() {
            @Override
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                // Redis 原生指令: WATCH demo:tx:stock
                operations.watch(watchKey);

                // 开启事务
                // Redis 原生指令: MULTI
                operations.multi();

                // Redis 原生指令: DECR demo:tx:stock
                operations.opsForValue().decrement(watchKey);

                // Redis 原生指令: EXEC
                return operations.exec();
            }
        });
        System.out.println("  -> WATCH 未受外部修改打扰，EXEC 提交结果: " + watchTxResult);

        // 4. 对比小结
        System.out.println("\n[步骤 4] 除了 Lua 外的原子性方案对比总结：");
        System.out.println("  1) 原生复合指令 (SET NX EX, MSETNX, INCR): 性能最极致、无额外开销，优先选用；");
        System.out.println("  2) WATCH + 事务: 适合并发冲突率较低的简单 CAS 变更，冲突高时自旋重试开销大；");
        System.out.println("  3) Redis 7.0 Functions: 解决脚本管理痛点，未来主流方向；");
        System.out.println("  4) Redisson 分布式锁: 适合业务跨系统、涉及 DB 等多种外部资源的粗粒度原子事务控制。");
    }
}

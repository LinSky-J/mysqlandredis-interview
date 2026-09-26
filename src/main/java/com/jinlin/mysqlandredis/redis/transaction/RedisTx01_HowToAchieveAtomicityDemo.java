package com.jinlin.mysqlandredis.redis.transaction;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.List;

/**
 * Redis 事务与原子性篇 01: 如何实现 Redis 原子性？(单命令/Lua脚本原子性与 MULTI/EXEC 弱原子性局限剖析)
 *
 * 面试真题：如何实现 redis 原子性？
 *
 * 核心原理深度剖析：
 * 1. Redis 单命令的天然原子性：
 *    - 因为 Redis 核心内存执行引擎是【单线程串行处理】的，任何单条独立命令 (如 INCR, DECR, HSET, LPUSH 等)
 *      在执行过程中绝对不会被其他客户端插队，单命令天然具备 100% 原子性。
 *
 * 2. 多命令组合的【Lua 脚本强原子性保障】(生产最推荐)：
 *    - 原理: Redis 内嵌 Lua 解释器 (Lua 5.1)，当客户端通过 `EVAL` 或 `EVALSHA` 执行一段 Lua 脚本时，
 *      Redis 会将整段脚本视为一个原子工作单元整体调度入队；
 *    - 在该脚本执行完毕之前，Redis 单线程绝不处理任何其他客户端发来的命令，彻底杜绝了并发竞态！
 *    - 经典应用: 分布式锁原子释放 (先比较 UUID 是否匹配，匹配才 DEL)、秒杀原子扣减库存。
 *
 * 3. Redis 自带事务 (MULTI / EXEC) 的“弱原子性”与不回滚机制：
 *    - `MULTI`: 开启事务；
 *    - 客户端后续命令不立即执行，而是放入服务端的 FIFO 事务队列 (返回 QUEUED)；
 *    - `EXEC`: 批量执行事务队列中的所有命令。
 *    - 【重要考点：Redis 事务为什么不支持回滚 (No Rollback)？】：
 *      1) 语法入队错误 (Compile Error，如命令拼写错误): 在 EXEC 触发时，整个事务全部拒绝执行；
 *      2) 运行时类型错误 (Runtime Error，如对 String 类型执行 HSET):
 *         错误的命令报错，但【其余正确的命令依然正常执行并提交更改】，不会自动回滚！
 *      3) 官方设计哲学: Redis 认为运行时类型错误是客户端程序员的逻辑 BUG (不应在生产发生)，
 *         不支持回滚可以免除保存还原点与 Undo 机制，极大维持了 Redis 极致的精简与超高性能。
 */
public class RedisTx01_HowToAchieveAtomicityDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisTx01] Redis 原子性实现手段：单命令天然原子、Lua 脚本与 MULTI/EXEC 机制");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 实机演示 Lua 脚本原子比较与删除 (分布式锁安全释放经典场景)
        System.out.println("[步骤 1] 演示 Lua 脚本执行原子判断并删除 (Compare-And-Delete)：");
        String lockKey = "demo:tx:lock_owner";
        String correctToken = "UUID_TOKEN_CLIENT_A";
        String wrongToken = "UUID_TOKEN_CLIENT_B";

        // Redis 原生指令: SET demo:tx:lock_owner UUID_TOKEN_CLIENT_A
        redisTemplate.opsForValue().set(lockKey, correctToken);

        // 编写 Lua 脚本: 只有当 Key 的当前值等于传入的 ARGV[1] 时，才执行 DEL 删除，否则返回 0
        String luaScript =
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "    return redis.call('del', KEYS[1]) " +
                "else " +
                "    return 0 " +
                "end";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(luaScript, Long.class);

        // 第一次尝试使用错误 Token 释放锁 (预期返回 0，Key 依然保留)
        // Redis 原生指令: EVAL "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end" 1 demo:tx:lock_owner UUID_TOKEN_CLIENT_B
        Long failResult = redisTemplate.execute(redisScript, Collections.singletonList(lockKey), wrongToken);
        System.out.println("  -> [Lua 执行 1] 持有者 Token 不匹配尝试删除，结果: " + failResult + " (成功防范误删他人锁)");

        // 第二次使用正确 Token 释放锁 (预期返回 1，Key 被删除)
        // Redis 原生指令: EVAL "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end" 1 demo:tx:lock_owner UUID_TOKEN_CLIENT_A
        Long successResult = redisTemplate.execute(redisScript, Collections.singletonList(lockKey), correctToken);
        System.out.println("  -> [Lua 执行 2] 持有者 Token 匹配原子删除锁，结果: " + successResult + " (原子操作成功)");

        // 2. 实机演示 MULTI / EXEC 事务批处理及其不回滚特性
        System.out.println("\n[步骤 2] 演示 MULTI / EXEC 事务队列执行机制：");
        String txKey1 = "demo:tx:account_a";
        String txKey2 = "demo:tx:account_b";

        // Redis 原生指令: SET demo:tx:account_a 100
        redisTemplate.opsForValue().set(txKey1, "100");
        // Redis 原生指令: SET demo:tx:account_b 200
        redisTemplate.opsForValue().set(txKey2, "200");

        // 在同一连接 Session 内执行 MULTI / EXEC
        List<Object> txResults = redisTemplate.execute(new SessionCallback<List<Object>>() {
            @Override
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                // Redis 原生指令: MULTI
                operations.multi();

                // 批处理指令进队
                // Redis 原生指令: DECRBY demo:tx:account_a 50
                operations.opsForValue().decrement(txKey1, 50);
                // Redis 原生指令: INCRBY demo:tx:account_b 50
                operations.opsForValue().increment(txKey2, 50);

                // Redis 原生指令: EXEC
                return operations.exec();
            }
        });

        // Redis 原生指令: GET demo:tx:account_a
        String aVal = redisTemplate.opsForValue().get(txKey1);
        // Redis 原生指令: GET demo:tx:account_b
        String bVal = redisTemplate.opsForValue().get(txKey2);
        System.out.println("  -> 事务批量执行返回结果列表: " + txResults);
        System.out.println("  -> 转账事务执行后: A 账户余额=" + aVal + ", B 账户余额=" + bVal);

        // 3. 对比总结矩阵
        System.out.println("\n[步骤 3] Lua 脚本 vs MULTI/EXEC 事务原子性与能力对比：");
        System.out.println("  +-----------------+---------------------------+----------------------------------------------+");
        System.out.println("  | 对比指标        | Redis 事务 (MULTI/EXEC)   | Lua 脚本 (EVAL / EVALSHA)                    |");
        System.out.println("  +-----------------+---------------------------+----------------------------------------------+");
        System.out.println("  | 原子性保证      | 弱原子性 (排他性执行，但  | 强原子性 (整段脚本作为一个原子单元执行，中途 |");
        System.out.println("  |                 | 运行时命令出错不回滚)     | 不会被任何外部命令插队)                      |");
        System.out.println("  | 逻辑分支控制    | 不支持 (无法基于中间结果  | 完整支持 (具备完整的 if-else、循环、变量逻辑)|");
        System.out.println("  |                 | 做 if-else 条件判断)      |                                              |");
        System.out.println("  | 生产推荐度      | 低 (多场景逐渐被 Lua 取代)| 极高 (分布式锁、复杂秒杀扣减黄金标准)        |");
        System.out.println("  +-----------------+---------------------------+----------------------------------------------+");
    }
}

package com.jinlin.mysqlandredis.redis.scenario;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Set;

/**
 * Redis 业务场景篇 02: Redis 核心应用场景 (除缓存外)、全景业务版图与并发操作本质剖析
 *
 * 面试真题：
 * 1. redis应用场景是什么?
 * 2. Redis除了缓存，还有哪些应用?
 * 3. Redis支持并发操作吗?
 *
 * 核心原理深度剖析：
 *
 * 一、Redis 除了作为常规缓存，还有哪些核心应用场景？(全景业务版图)：
 * 1. 【高并发计数器与全局自增 ID】 (String -> INCR / INCRBY)：
 *    - 场景：文章阅读量、视频播放数、秒杀库存实时扣减、分布式订单流水号生成；
 *    - 优势：单线程原子递增，毫秒级响应，彻底解放 MySQL 的 `UPDATE ... SET count=count+1` 磁盘行锁瓶颈。
 * 2. 【分布式锁 (Distributed Lock)】 (String -> SET NX EX / Redisson)：
 *    - 场景：电商防止重复下单、跨服务定时任务调度互斥、并发支付防超卖。
 * 3. 【实时排行榜 (Leaderboard)】 (ZSet -> ZADD / ZREVRANGE)：
 *    - 场景：游戏玩家战力排行、电商热卖爆品榜、微博热搜榜；
 *    - 优势：底层跳表保持 $O(\log N)$ 的实时动态排序，免去 MySQL 昂贵的 `ORDER BY score DESC LIMIT 10`。
 * 4. 【分布式 Session / Token 集中共享】 (String / Hash)：
 *    - 场景：微服务多实例部署下用户登录态 (JWT/Session) 的集中持久化与验证。
 * 5. 【社交关系计算与抽奖去重】 (Set -> SADD / SINTER / SDIFF / SRANDMEMBER)：
 *    - 场景：微博共同关注 (`SINTER`)、可能认识的人 (`SDIFF`)、年会随机抽奖 (`SRANDMEMBER` / `SPOP`)。
 * 6. 【轻量级消息队列 (Message Queue)】：
 *    - `List` (LPUSH + BRPOP)：构建点对点阻塞任务队列；
 *    - `Pub/Sub`：跨进程配置变更广播；
 *    - `Stream` (Redis 5.0+)：具备持久化、消费者组 (Consumer Group)、ACK 确认机制的专业消息队列。
 * 7. 【海量用户签到打卡与在线状态】 (Bitmap -> SETBIT / BITCOUNT)：
 *    - 场景：几千万用户全年月签到，每个用户 1 年仅耗费 365 bit (约 46 字节)；
 * 8. 【亿级 UV 近似基数统计】 (HyperLogLog -> PFADD / PFCOUNT)：
 *    - 场景：统计全网独立访客 (UV)，仅占用 12KB 内存即可统计 $2^{64}$ 个元素，标准误差仅 0.81%；
 * 9. 【LBS 地理位置检索】 (GEO -> GEOADD / GEORADIUS)：
 *    - 场景：外卖骑手与用户距离测算、高德地图“附近加油站”。
 *
 * 二、Redis 支持并发操作吗？(深度架构答疑)：
 * 核心结论：【宏观网络接入完全支持高并发；微观核心执行单线程串行保证绝对线程安全】。
 *
 * 1. 【宏观层面 (客户端并发接入)】：
 *    - 完全支持高并发！Redis 基于 I/O 多路复用 (epoll) 技术，一个实例可以同时维持数万个并发 Socket 连接，
 *      轻松应对每秒 10 万+ 的高并发吞吐请求。
 * 2. 【微观层面 (服务端内部命令执行)】：
 *    - 核心执行引擎是【单线程串行处理】的；
 *    - 所有客户端并发提交的命令，都会由事件循环分派器排入单线程执行队列，逐条串行执行；
 *    - 因此对于单条命令 (如 INCR, LPUSH, HSET)，天然绝无并发竞态 (Data Race) 与线程安全风险。
 * 3. 【业务必须警惕的“应用层伪并发安全陷阱”】：
 *    - 虽然单命令原子，但若业务存在复合逻辑：`val = redis.get(); if (val > 0) redis.set(val - 1);`；
 *    - 在多客户端高并发下，不同客户端的 GET 和 SET 会交错执行，导致超卖或脏写！
 *    - 应对方案：必须使用 【Lua 脚本】 或 【分布式锁】 将多步复合操作打包为一个原子不可分割的操作单元！
 */
public class RedisScenario02_BusinessScenariosAndConcurrencyDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisScenario02] Redis 核心业务版图 (除了缓存还有什么) 与并发操作本质辨析");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 模拟业务场景 1: 原子计数器 (点赞数/浏览量)
        System.out.println("[步骤 1] 演示场景 1：高并发原子计数器 (INCR)：");
        String counterKey = "demo:scenario:post:views:2001";

        // Redis 原生指令: SET demo:scenario:post:views:2001 0
        redisTemplate.opsForValue().set(counterKey, "0");

        // Redis 原生指令: INCR demo:scenario:post:views:2001
        Long count1 = redisTemplate.opsForValue().increment(counterKey);
        // Redis 原生指令: INCR demo:scenario:post:views:2001
        Long count2 = redisTemplate.opsForValue().increment(counterKey);
        System.out.println("  文章浏览量连续自增: key=" + counterKey + ", 当前数值=" + count2 + " (单线程串行原子执行)");

        // 2. 模拟业务场景 2: 实时游戏战力排行榜 (ZSet)
        System.out.println("\n[步骤 2] 演示场景 2：实时排行榜 (ZSet)：");
        String rankKey = "demo:scenario:game:leaderboard";

        // Redis 原生指令: ZADD demo:scenario:game:leaderboard 9500 Player_Knight
        redisTemplate.opsForZSet().add(rankKey, "Player_Knight", 9500.0);
        // Redis 原生指令: ZADD demo:scenario:game:leaderboard 9800 Player_Mage
        redisTemplate.opsForZSet().add(rankKey, "Player_Mage", 9800.0);
        // Redis 原生指令: ZADD demo:scenario:game:leaderboard 9200 Player_Archer
        redisTemplate.opsForZSet().add(rankKey, "Player_Archer", 9200.0);

        // 获取前两名排行榜
        // Redis 原生指令: ZREVRANGE demo:scenario:game:leaderboard 0 1 WITHSCORES
        Set<ZSetOperations.TypedTuple<String>> topPlayers = redisTemplate.opsForZSet().reverseRangeWithScores(rankKey, 0, 1);
        System.out.println("  游戏排行榜 Top 2 (实时基于跳表动态排序):");
        if (topPlayers != null) {
            int rank = 1;
            for (ZSetOperations.TypedTuple<String> tuple : topPlayers) {
                System.out.println("    第 " + rank++ + " 名: 玩家=" + tuple.getValue() + ", 战力=" + tuple.getScore());
            }
        }

        // 3. 模拟业务场景 3: 用户每日签到打卡 (Bitmap)
        System.out.println("\n[步骤 3] 演示场景 3：海量用户打卡签到 (Bitmap)：");
        String signKey = "demo:scenario:user:sign:101:202609";

        // 模拟 9 月 1 号 (offset 0)、9 月 2 号 (offset 1) 签到
        // Redis 原生指令: SETBIT demo:scenario:user:sign:101:202609 0 1
        redisTemplate.opsForValue().setBit(signKey, 0, true);
        // Redis 原生指令: SETBIT demo:scenario:user:sign:101:202609 1 1
        redisTemplate.opsForValue().setBit(signKey, 1, true);

        // 统计总签到天数 (BITCOUNT)
        Long totalSigns = redisTemplate.execute((RedisConnection connection) -> {
            // Redis 原生指令: BITCOUNT demo:scenario:user:sign:101:202609
            return connection.bitCount(signKey.getBytes());
        });
        System.out.println("  用户 9 月签到天数统计 (BITCOUNT): " + totalSigns + " 天 (极省内存，1年仅需约 46 字节)");

        // 4. 并发问题结论总结
        System.out.println("\n[步骤 4] 面试答题核心结构提炼：");
        System.out.println("  1. 【业务全景图】：除了缓存，还能做原子计数器/限流、分布式锁、实时排行榜(ZSet)、消息队列(Stream)、社交关系(Set)、海量打卡(Bitmap)、UV统计(HyperLogLog)及地理围栏(GEO)；");
        System.out.println("  2. 【并发支持特性】：外部并发网络接入支持万级并发，内部单线程核心执行引擎天然杜绝并发数据竞争；");
        System.out.println("  3. 【业务级陷阱】：业务端复合非原子操作 (GET + SET) 仍会并发冲突，必须通过 Lua 或分布式锁加固。");
        System.out.println("================================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

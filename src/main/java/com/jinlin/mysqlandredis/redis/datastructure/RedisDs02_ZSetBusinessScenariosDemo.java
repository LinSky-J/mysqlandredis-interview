package com.jinlin.mysqlandredis.redis.datastructure;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Set;

/**
 * Redis 数据结构篇 02: ZSet (有序集合) 生产级实战应用场景精讲
 *
 * 面试真题：ZSet 用过吗？
 *
 * 核心回答要点：
 * 1. 业务场景 1：全网实时积分 / 财富排行榜 (Leaderboard)
 *    - 场景：游戏段位排行、直播间打赏榜、热搜榜、销售额英雄榜。
 *    - 指令集：
 *      - ZADD key score member: 录入选手分数；
 *      - ZINCRBY key increment member: 动态追加积分；
 *      - ZREVRANGE key 0 9 WITHSCORES: 获取 Top 10 榜单（按分数从高到低）；
 *      - ZREVRANK key member: 获取某用户的当前实时排名（0-based）。
 *
 * 2. 业务场景 2：高性能延迟任务队列 (Delay Queue / 订单超时自动取消)
 *    - 场景：用户下单 30 分钟未支付自动取消、优惠券到期提醒、定时推送通知。
 *    - 实现方案：
 *      - 将任务 ID 或订单号作为 member，任务计划触发的【未来绝对时间戳(毫秒)】作为 score 存入 ZSet；
 *      - 消费线程周期轮询：ZRANGEBYSCORE key 0 当前时间戳 LIMIT 0 10 捞取已到期的任务并执行；
 *      - 使用 ZREM key member 抢占或原子消费任务，避免重复执行。
 *
 * 3. 业务场景 3：滑动窗口高并发限流器 (Sliding Window Rate Limiter)
 *    - 场景：限制某个 IP 每分钟最多访问 100 次。
 *    - 实现方案：
 *      - member 为唯一的请求 UUID，score 为请求到达时间戳；
 *      - 每次请求进入：ZREMRANGEBYSCORE key 0 (now - 60s) 清理 1 分钟以前的旧请求；
 *      - ZCARD key 统计当前窗口内剩余的请求总数：若 count < 100 则放行并 ZADD 当前请求，否则拒绝服务。
 */
public class RedisDs02_ZSetBusinessScenariosDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisDs02] ZSet 生产三大经典业务场景实机演练 (排行榜 / 延迟队列 / 滑动窗口)");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 实机演练场景 1: 实时热搜排行榜 (基于 RedisTemplate.opsForZSet)
        System.out.println("[场景 1] 游戏战力 / 热搜排行榜实操 (基于 RedisTemplate.opsForZSet)：");
        String rankKey = "demo:zset:leaderboard";
        // Redis 原生指令: DEL demo:zset:leaderboard
        redisTemplate.delete(rankKey);

        // 录入初始榜单 (opsForZSet.add)
        // Redis 原生指令: ZADD demo:zset:leaderboard 8500.0 Player_Zhang
        redisTemplate.opsForZSet().add(rankKey, "Player_Zhang", 8500.0);
        // Redis 原生指令: ZADD demo:zset:leaderboard 9200.0 Player_Wang
        redisTemplate.opsForZSet().add(rankKey, "Player_Wang", 9200.0);
        // Redis 原生指令: ZADD demo:zset:leaderboard 7600.0 Player_Li
        redisTemplate.opsForZSet().add(rankKey, "Player_Li", 7600.0);
        // Redis 原生指令: ZADD demo:zset:leaderboard 9800.0 Player_Zhao
        redisTemplate.opsForZSet().add(rankKey, "Player_Zhao", 9800.0);

        // 动态提升玩家分数 (opsForZSet.incrementScore 即 ZINCRBY)
        // Redis 原生指令: ZINCRBY demo:zset:leaderboard 1000.0 Player_Zhang
        redisTemplate.opsForZSet().incrementScore(rankKey, "Player_Zhang", 1000.0); // 8500 + 1000 = 9500

        // 打印全服前三名 (opsForZSet.reverseRangeWithScores 即 ZREVRANGE WITHSCORES)
        // Redis 原生指令: ZREVRANGE demo:zset:leaderboard 0 2 WITHSCORES
        Set<ZSetOperations.TypedTuple<String>> top3 = redisTemplate.opsForZSet().reverseRangeWithScores(rankKey, 0, 2);
        System.out.println("  -> [Top 3 排行榜结果]:");
        int rank = 1;
        if (top3 != null) {
            for (ZSetOperations.TypedTuple<String> tuple : top3) {
                System.out.println("     第 " + rank++ + " 名: " + tuple.getValue() + ", 战力值: " + tuple.getScore());
            }
        }

        // 查询特定用户的排名与分数 (opsForZSet.reverseRank 与 score)
        // Redis 原生指令: ZREVRANK demo:zset:leaderboard Player_Zhang
        Long playerRank = redisTemplate.opsForZSet().reverseRank(rankKey, "Player_Zhang");
        // Redis 原生指令: ZSCORE demo:zset:leaderboard Player_Zhang
        Double playerScore = redisTemplate.opsForZSet().score(rankKey, "Player_Zhang");
        System.out.println("  -> 玩家 Player_Zhang 实时全服排名: 第 " + ((playerRank != null ? playerRank : 0) + 1) + " 名 (0-based: " + playerRank + "), 分数: " + playerScore);

        // 2. 实机演练场景 2: 订单超时延迟队列模型 (opsForZSet.rangeByScore)
        System.out.println("\n[场景 2] 订单超时未支付自动取消延迟队列模拟 (opsForZSet.rangeByScore)：");
        String delayQueueKey = "demo:zset:delay_queue";
        // Redis 原生指令: DEL demo:zset:delay_queue
        redisTemplate.delete(delayQueueKey);

        long now = System.currentTimeMillis();
        // 模拟放入三个订单，到期时间戳分别为：过去(已到期)、现在、未来(未到期)
        // Redis 原生指令 (示例时间戳 1700000000000): ZADD demo:zset:delay_queue 1700000000000 ORDER_EXPIRED_001
        redisTemplate.opsForZSet().add(delayQueueKey, "ORDER_EXPIRED_001", (double) (now - 5000));
        // Redis 原生指令 (示例时间戳 1700000004000): ZADD demo:zset:delay_queue 1700000004000 ORDER_EXPIRED_002
        redisTemplate.opsForZSet().add(delayQueueKey, "ORDER_EXPIRED_002", (double) (now - 1000));
        // Redis 原生指令 (示例时间戳 1700000065000): ZADD demo:zset:delay_queue 1700000065000 ORDER_FUTURE_003
        redisTemplate.opsForZSet().add(delayQueueKey, "ORDER_FUTURE_003", (double) (now + 60000));

        // 捞取已到期的订单 (score <= now)
        // Redis 原生指令 (查询分值范围 0 至当前时间戳): ZRANGEBYSCORE demo:zset:delay_queue 0 1700000005000
        Set<String> expiredOrders = redisTemplate.opsForZSet().rangeByScore(delayQueueKey, 0.0, (double) now);
        System.out.println("  -> 当前时间截点捞出已超时的订单: " + expiredOrders);
        if (expiredOrders != null) {
            for (String orderId : expiredOrders) {
                // Redis 原生指令: ZREM demo:zset:delay_queue ORDER_EXPIRED_001
                redisTemplate.opsForZSet().remove(delayQueueKey, orderId);
                System.out.println("     [任务执行] 成功原子抢占并消费超时订单: " + orderId + " -> 触发取消订单释放库存逻辑");
            }
        }
    }
}

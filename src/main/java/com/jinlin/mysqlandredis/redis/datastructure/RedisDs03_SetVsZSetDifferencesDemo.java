package com.jinlin.mysqlandredis.redis.datastructure;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Arrays;
import java.util.Set;

/**
 * Redis 数据结构篇 03: Set (无序集合) 与 ZSet (有序集合) 核心差异全方位对比
 *
 * 面试真题：Redis 中 Set 和 ZSet 区别是什么？
 *
 * 核心考点与原理：
 * 1. 数据模型与是否有序：
 *    - Set (无序集合)：仅存储成员 (member)，所有元素全局唯一且无序；
 *    - ZSet (有序集合)：不仅存储唯一的 member，还为每个 member 关联一个浮点型分值 (score)，
 *      集合内部严格根据 score 从小到大排序（分值相同时按 member 字典序排序）。
 *
 * 2. 底层数据结构不同：
 *    - Set 底层：
 *      - 整数集合 (intset)：当集合所有元素都是整数且数量较少时采用，紧凑内存，使用二分查找定位元素；
 *      - 哈希表 (hashtable/dict)：当元素包含非数字或数量较多时转为字典，Key 为 member，Value 为 NULL，查询为 O(1)。
 *    - ZSet 底层：
 *      - 压缩列表 (ziplist) / 紧凑列表 (listpack)：小数据量时采用连续内存块；
 *      - 跳表 (skiplist) + 字典 (dict)：大数据量时采用跳表保障 O(logN) 范围与排名操作，同时配合字典保障 O(1) 查找单个 score。
 *
 * 3. 业务功能侧重点：
 *    - Set 核心优势在于【数学集合运算】：交集 (SINTER)、并集 (SUNION)、差集 (SDIFF)，适合好友共同关注、标签归类、抽奖去重；
 *    - ZSet 核心优势在于【带权重的范围与排序】：分页排行 (ZREVRANGE)、按分值区间过滤 (ZRANGEBYSCORE)、延迟任务等。
 */
public class RedisDs03_SetVsZSetDifferencesDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisDs03] Redis Set 与 ZSet 多维度差异对比与本地 Redis 底层编码实测");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 实机操作 Set: 演示无序、唯一与集合交集运算 (opsForSet)
        System.out.println("[步骤 1] 演示 Set 的无序性与集合运算 (如共同好友 SINTER -> opsForSet().intersect)：");
        String user1Follows = "demo:set:user1_follows";
        String user2Follows = "demo:set:user2_follows";
        redisTemplate.delete(Arrays.asList(user1Follows, user2Follows));

        redisTemplate.opsForSet().add(user1Follows, "Java", "Redis", "MySQL", "Kafka");
        redisTemplate.opsForSet().add(user2Follows, "Redis", "MySQL", "Golang", "Docker");

        Set<String> commonInter = redisTemplate.opsForSet().intersect(user1Follows, user2Follows);
        System.out.println("  -> [Set 集合运算] 用户 1 与用户 2 共同关注的话题 (交集 SINTER): " + commonInter);
        System.out.println("  -> Set 底层编码 (纯字符串): " + RedisConnectionHelper.getObjectEncoding(user1Follows));

        // 2. 实机操作 ZSet: 演示有序性与基于 Score 的精准范围过滤 (opsForZSet)
        System.out.println("\n[步骤 2] 演示 ZSet 的严格有序性与范围查找 (按分数过滤 ZRANGEBYSCORE -> opsForZSet().rangeByScore)：");
        String goodsPriceKey = "demo:zset:goods_price";
        redisTemplate.delete(goodsPriceKey);

        redisTemplate.opsForZSet().add(goodsPriceKey, "Notebook", 19.9);
        redisTemplate.opsForZSet().add(goodsPriceKey, "Keyboard", 99.0);
        redisTemplate.opsForZSet().add(goodsPriceKey, "Earphone", 299.0);
        redisTemplate.opsForZSet().add(goodsPriceKey, "Monitor", 1999.0);

        // 过滤价格在 50 到 300 之间的商品
        Set<String> midPriceGoods = redisTemplate.opsForZSet().rangeByScore(goodsPriceKey, 50.0, 300.0);
        System.out.println("  -> [ZSet 范围过滤] 价格在 50~300 元之间的商品: " + midPriceGoods);
        System.out.println("  -> ZSet 底层编码: " + RedisConnectionHelper.getObjectEncoding(goodsPriceKey));

        // 3. 对比总结矩阵
        System.out.println("\n[步骤 3] Set 与 ZSet 核心对比矩阵小结：");
        System.out.println("  +-----------------+-------------------------------+--------------------------------------------+");
        System.out.println("  | 对比维度        | Set (无序集合)                | ZSet (有序集合)                            |");
        System.out.println("  +-----------------+-------------------------------+--------------------------------------------+");
        System.out.println("  | 排序特性        | 元素无序，不可排序            | 严格按照关联的 score 从小到大排序          |");
        System.out.println("  | 元素结构        | 单纯存储 member               | 存储 (member, score) 键值分值对            |");
        System.out.println("  | 底层编码        | intset (纯整数) / dict        | ziplist(listpack) / skiplist + dict        |");
        System.out.println("  | 时间复杂度      | 增删查单元素均为 O(1)         | 增删为 O(logN)，单查 score 借由 dict 为 O(1)|");
        System.out.println("  | 擅长业务场景    | 共同好友、抽奖去重、黑白名单  | 积分排行榜、延迟消息队列、滑动窗口限流     |");
        System.out.println("  +-----------------+-------------------------------+--------------------------------------------+");
    }
}

package com.jinlin.mysqlandredis.redis.datastructure;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 数据结构篇 04: ZSet (有序集合) 双底层实现机制 (ziplist/listpack 与 dict+skiplist)
 *
 * 面试真题：ZSet 底层是怎么实现的？
 *
 * 核心考点与原理：
 * 1. ZSet 的两套底层数据结构：
 *    - 模式 A：连续紧凑内存结构（压缩列表 ziplist / 紧凑列表 listpack）
 *      - 触发条件：同时满足两个阈值：
 *        1) 元素数量小于 zset-max-ziplist-entries（默认 128 个）；
 *        2) 所有成员 member 的长度均小于 zset-max-ziplist-value（默认 64 字节）。
 *      - 存储形式：在连续的内存块中，每个元素占用两个相邻节点，第一个存 member，第二个存 score，按分值从小到大紧凑排序。
 *    - 模式 B：复合结构（跳跃表 skiplist + 字典 dict）
 *      - 触发条件：元素数量或成员体积只要超过上述任一阈值，底层编码立即升级为 skiplist+dict，且不可逆退回。
 *
 * 2. 深度追问：为什么 ZSet 同时使用跳表和字典，而不是单独使用其中一种？
 *    - 假设【只使用字典 dict】：
 *      - 优点：执行 ZSCORE member 查询单个成员分数的时间复杂度是极致的 O(1)；
 *      - 缺点：字典是无序散列表，执行范围查找 (ZRANGEBYSCORE) 或排名 (ZRANK) 时，必须遍历所有数据在内存中全量排序，
 *        时间复杂度高达 O(NlogN)，严重阻塞 Redis 单线程！
 *    - 假设【只使用跳跃表 skiplist】：
 *      - 优点：范围查询与排名时间复杂度为对数级别的 O(logN)；
 *      - 缺点：执行单个成员分数查询 (ZSCORE member) 时，必须从最高层开始按节点搜索，时间复杂度退化为 O(logN)。
 *    - 【Redis 的巧妙结合】：
 *      - 跳表与字典共同持有同一个 member 字符串对象和 double 分值指针，**绝不存储两份冗余数据**，内存开销极低；
 *      - 字典负责以 O(1) 响应根据 member 查 score 的高频请求；
 *      - 跳表负责以 O(logN) 响应范围查找、排名计算与区间删除；
 *      - 两者结合，达成了所有 ZSet API 的最优理论时间复杂度！
 */
public class RedisDs04_ZSetUnderlyingImplementationDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisDs04] ZSet 双底层实现机制 (ziplist/listpack 与 skiplist+dict) 阈值跃迁实测");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        String zsetKey = "demo:zset:encoding_transition";
        // Redis 原生指令: DEL demo:zset:encoding_transition
        redisTemplate.delete(zsetKey);

        // 1. 插入少量短数据，观察小数据量编码 (ziplist / listpack)
        System.out.println("[步骤 1] 写入 3 个短文本元素，检查初始底层编码：");
        // Redis 原生指令: ZADD demo:zset:encoding_transition 10.0 item_A
        redisTemplate.opsForZSet().add(zsetKey, "item_A", 10.0);
        // Redis 原生指令: ZADD demo:zset:encoding_transition 20.0 item_B
        redisTemplate.opsForZSet().add(zsetKey, "item_B", 20.0);
        // Redis 原生指令: ZADD demo:zset:encoding_transition 30.0 item_C
        redisTemplate.opsForZSet().add(zsetKey, "item_C", 30.0);

        // Redis 原生指令: OBJECT ENCODING demo:zset:encoding_transition
        String initialEncoding = RedisConnectionHelper.getObjectEncoding(zsetKey);
        System.out.println("  -> 当前元素数量: 3, 底层编码为: " + initialEncoding + " (紧凑连续内存存储)");

        // 2. 写入一个超长字符串 (超过 64 字节的阈值)，触发编码跃迁为 skiplist
        System.out.println("\n[步骤 2] 插入一个长度超过 64 字节的超长成员，观察底层编码跃迁：");
        StringBuilder longMember = new StringBuilder("LONG_STRING_PAYLOAD_EXCEEDING_SIXTY_FOUR_BYTES_THRESHOLD_");
        for (int i = 0; i < 5; i++) {
            longMember.append("1234567890");
        }
        // Redis 原生指令: ZADD demo:zset:encoding_transition 999.0 "LONG_STRING_PAYLOAD_EXCEEDING_SIXTY_FOUR_BYTES_THRESHOLD_12345678901234567890"
        redisTemplate.opsForZSet().add(zsetKey, longMember.toString(), 999.0);

        // Redis 原生指令: OBJECT ENCODING demo:zset:encoding_transition
        String upgradedEncoding = RedisConnectionHelper.getObjectEncoding(zsetKey);
        System.out.println("  -> 插入超长 member 后，底层编码跃迁为: " + upgradedEncoding + " (跳表 skiplist + 字典 dict 复合结构)");

        // 3. 架构对比小结
        System.out.println("\n[步骤 3] ZSet 双底层结构分工对比总结：");
        System.out.println("  +-------------------+--------------------+---------------------------------------------+");
        System.out.println("  | 复合内部组件      | 职责与定位         | 核心优势与时间复杂度                        |");
        System.out.println("  +-------------------+--------------------+---------------------------------------------+");
        System.out.println("  | 字典 (dict)       | Key-Value 映射     | ZSCORE member 分数查询达到极致的 O(1)       |");
        System.out.println("  | 跳表 (skiplist)   | 多层有序链表       | ZRANGEBYSCORE, ZRANK 等范围与排名达到 O(logN)|");
        System.out.println("  | 内存共享机制      | 指针引用复用       | 两个数据结构共享相同的 Member 和 Score 内存  |");
        System.out.println("  +-------------------+--------------------+---------------------------------------------+");
    }
}

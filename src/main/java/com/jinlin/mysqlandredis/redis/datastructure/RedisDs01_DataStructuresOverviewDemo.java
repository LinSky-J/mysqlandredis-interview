package com.jinlin.mysqlandredis.redis.datastructure;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 数据结构篇 01: Redis 底层数据结构与对象系统全景剖析
 *
 * 面试真题：讲一下 Redis 底层的数据结构
 *
 * 核心考点与原理：
 * 1. Redis 对象系统 (redisObject)：
 *    Redis 中每个键值对的 Value 并不是直接暴露底层裸数据结构的，而是被包装为一个 redisObject 对象：
 *    - type: 4 位，记录外部数据类型 (String, List, Hash, Set, ZSet 等)；
 *    - encoding: 4 位，记录当前对象采用的底层编码与数据结构 (如 int, embstr, raw, hashtable, skiplist, ziplist, listpack, intset 等)；
 *    - ptr: 指针，指向实际的底层内存数据结构；
 *    - refcount: 引用计数，用于内存回收与共享对象；
 *    - lru / lfu: 24 位，记录对象最后访问时间戳或访问频次，用于内存淘汰策略 (LRU / LFU)。
 *
 * 2. Redis 八大底层数据结构：
 *    (1) 简单动态字符串 (SDS, Simple Dynamic String)：用于存储字符串、键名、AOF 缓冲区等；
 *    (2) 双向无环链表 (linkedlist)：早期 List 底层结构，带头尾指针，支持前后遍历；
 *    (3) 压缩列表 (ziplist)：连续内存块，用于早期元素较少的 List/Hash/ZSet，节省内存；
 *    (4) 紧凑列表 (listpack)：Redis 5.0 引入、7.0 全面取代 ziplist，彻底解决连锁更新问题；
 *    (5) 字典 / 哈希表 (dict)：渐进式 rehash 机制，支撑 Hash 类型及全局键空间 DB；
 *    (6) 跳跃表 (skiplist)：多层链表结构，平均 O(logN) 查询，支撑 ZSet 范围与排名操作；
 *    (7) 整数集合 (intset)：当 Set 全为整数且数量少时采用，紧凑且支持二分查找；
 *    (8) 快速列表 (quicklist)：双向链表 + 节点内部为 ziplist/listpack 的混合结构，List 核心实现。
 */
public class RedisDs01_DataStructuresOverviewDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisDs01] Redis 底层数据结构全景与对象系统 (redisObject) 编码核验");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 实机写入不同类型的 Key 并探测底层 encoding
        System.out.println("[步骤 1] 在本地 Redis 中写入不同类型的键，使用 RedisTemplate 与 OBJECT ENCODING 查看底层数据结构：");

        // 整数字符串 -> int (opsForValue)
        redisTemplate.opsForValue().set("demo:ds:num", "10086");
        String numEnc = RedisConnectionHelper.getObjectEncoding("demo:ds:num");
        System.out.println("  -> [String 短整数] Key: demo:ds:num -> 底层编码: " + numEnc + " (内存优化型直接存储在 ptr 指针中)");

        // 短字符串 -> embstr (SDS) (opsForValue)
        redisTemplate.opsForValue().set("demo:ds:str", "hello_redis");
        String strEnc = RedisConnectionHelper.getObjectEncoding("demo:ds:str");
        System.out.println("  -> [String 短文本] Key: demo:ds:str -> 底层编码: " + strEnc + " (redisObject 与 SDS 内存连续分配)");

        // 纯整数集合 -> intset (opsForSet)
        redisTemplate.delete("demo:ds:set");
        redisTemplate.opsForSet().add("demo:ds:set", "10", "20", "30");
        String setEnc = RedisConnectionHelper.getObjectEncoding("demo:ds:set");
        System.out.println("  -> [Set 整数集合] Key: demo:ds:set -> 底层编码: " + setEnc + " (小整数紧凑数组，二分查找)");

        // 小数据量哈希 -> ziplist 或 listpack (opsForHash)
        redisTemplate.delete("demo:ds:hash");
        redisTemplate.opsForHash().put("demo:ds:hash", "name", "antigravity");
        String hashEnc = RedisConnectionHelper.getObjectEncoding("demo:ds:hash");
        System.out.println("  -> [Hash 小数据量] Key: demo:ds:hash -> 底层编码: " + hashEnc + " (连续紧凑内存块)");

        // 小数据量有序集合 -> ziplist 或 listpack (opsForZSet)
        redisTemplate.delete("demo:ds:zset");
        redisTemplate.opsForZSet().add("demo:ds:zset", "Alice", 99.5);
        String zsetEnc = RedisConnectionHelper.getObjectEncoding("demo:ds:zset");
        System.out.println("  -> [ZSet 小数据量] Key: demo:ds:zset -> 底层编码: " + zsetEnc + " (连续内存存储 member 与 score)");

        // 2. 知识结构对比表格
        System.out.println("\n[步骤 2] Redis 常见数据类型与底层存储结构映射总览：");
        System.out.println("  +-----------------+---------------------------+----------------------------------------------+");
        System.out.println("  | 外部业务类型     | 底层编码实现 (Encoding)    | 适用场景与机制                                |");
        System.out.println("  +-----------------+---------------------------+----------------------------------------------+");
        System.out.println("  | String          | int / embstr / raw        | 整数存储在指针 / <=44B连续内存 / >44B单独SDS |");
        System.out.println("  | List            | quicklist                 | 双向链表 + 节点内嵌 ziplist/listpack         |");
        System.out.println("  | Hash            | ziplist(listpack) / dict  | 小数据量紧凑内存 / 大数据量哈希表渐进式rehash|");
        System.out.println("  | Set             | intset / hashtable        | 纯整数二分查找 / 字符串集合哈希表            |");
        System.out.println("  | ZSet (SortedSet)| ziplist(listpack) /       | 小数据量连续紧凑 /                           |");
        System.out.println("  |                 | skiplist + dict           | 大数据量跳表做范围排名 + 字典做 O(1) 分数查找|");
        System.out.println("  +-----------------+---------------------------+----------------------------------------------+");
    }
}

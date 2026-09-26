package com.jinlin.mysqlandredis.redis.scenario;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;

/**
 * Redis 业务场景篇 04: Redis 大 Key 问题是什么？缺点危害及彻底治理解决方案
 *
 * 面试真题：
 * 1. Redis的大Key问题是什么?
 * 2. 大Key问题的缺点?
 * 3. Redis大key如何解决?
 *
 * 核心原理深度剖析：
 *
 * 一、什么是 Redis 的大 Key (BigKey)？
 * - 大 Key 并不是指 Key 的字符名称长，而是指【Key 对应的 Value 内存占用极大，或集合类数据结构的元素数量极大】：
 * - 业界行业通行判定标准：
 *   1) 【String 类型】：单个 Key 的 Value 大小超过 10 KB (超过 5MB 属于极高危重大事故)；
 *   2) 【复合容器结构 (Hash / List / Set / ZSet)】：元素总数量超过 5,000 ~ 10,000 个，或整体占用内存超过 10 MB。
 *
 * 二、大 Key 存在哪些严重缺点与生产危害？
 * 1. 【核心单线程阻塞 (阻塞主事件循环)】：
 *    - 对大 Key 执行读取 (如 `HGETALL` / `LRANGE 0 -1`) 或删除 (`DEL`) 时，需要遍历释放数万个连续内存节点；
 *    - 耗时可长达几百毫秒甚至数秒，期间主线程被完全冻结，后续所有其他业务请求陷入积压排队，导致系统雪崩！
 * 2. 【网卡带宽被打爆 (Network Bandwidth Exhaustion)】：
 *    - 假设一个 BigKey 体积为 5MB，如果有 100 个并发请求读取，瞬间产生 500MB/s 的出口流量，直接打满万兆网卡，造成整机服务不可用；
 * 3. 【分片集群内存倾斜 (Data Skew)】：
 *    - 在 Redis Cluster 集群中，一个 Key 只能分配给一个槽位和一个分片 Master，大 Key 导致该分片节点内存严重倾斜甚至 OOM 宕机，而其他节点大量空闲；
 * 4. 【主从同步与 AOF 重写卡顿】：
 *    - 全量同步传输大 Key 容易引起主从网络超时重连死循环；写时复制 (COW) 期间修改大 Key 会触发操作系统大量内存页拷贝，导致物理内存消耗翻倍。
 *
 * 三、Redis 大 Key 的全生命周期解决方案：
 *
 * 1. 【事前预防 (架构与数据拆分)】：
 *    - 【数据结构拆分 (分桶思想)】：
 *      - 若 Hash 包含 100 万个字段，按 HashCode 取模拆分为 100 个小 Hash：`user:info:00` ~ `user:info:99`；
 *      - 查找时：`bucketId = userId % 100; hget("user:info:" + bucketId, userId)`，将大集合拆散为小集合；
 *    - 【大文本压缩存储】：若必须存大 JSON，业务端使用 Gzip/Snappy 算法压缩为二进制再存入 Redis。
 *
 * 2. 【事中排查 (精准定位大 Key)】：
 *    - 命令行扫描：`redis-cli -h 127.0.0.1 -p 6379 -a 1234 --bigkeys` (基于 SCAN 命令非阻塞抽样扫描各类型最大 Key)；
 *    - 内存用量探测：`MEMORY USAGE <key>` (精准统计单个 Key 的物理内存占用字节)；
 *    - 离线分析：使用开源工具 `redis-rdb-tools` 离线分析 dump.rdb 文件，生成大 Key 详细报表。
 *
 * 3. 【事后治理与安全清理】：
 *    - 【严禁全量遍历】：生产严禁使用 `KEYS *`、`HGETALL`、`SMEMBERS`，必须使用渐进式游标命令 `HSCAN`、`SSCAN`、`ZSCAN`；
 *    - 【严禁直接同步 DEL】：严禁对包含数万元素的大 Key 执行 `DEL`！
 *    - 【使用异步删除 UNLINK (Redis 4.0+)】：
 *      - `UNLINK key` 会先在 $O(1)$ 时间内将 Key 从主字典中逻辑摘除，随后将物理内存页释放委托给后台 `bio` 异步子线程处理，主线程瞬间返回，零卡顿！
 *    - 【开启服务端惰性释放配置】：
 *      - `lazyfree-lazy-eviction yes` (内存淘汰时异步释放)；
 *      - `lazyfree-lazy-expire yes` (过期删除时异步释放)。
 */
public class RedisScenario04_BigKeyProblemAndSolutionsDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisScenario04] Redis 大 Key (BigKey) 判定标准、系统危害与 UNLINK 异步治理");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 实机模拟：构造一个包含多元素的 Hash 容器 Key
        System.out.println("[步骤 1] 写入测试业务 Hash 容器 Key：");
        String bigHashKey = "demo:scenario:bigkey:user_tags:1001";

        // Redis 原生指令: HSET demo:scenario:bigkey:user_tags:1001 tag_1 electronics
        redisTemplate.opsForHash().put(bigHashKey, "tag_1", "electronics");
        // Redis 原生指令: HSET demo:scenario:bigkey:user_tags:1001 tag_2 VIP_Customer
        redisTemplate.opsForHash().put(bigHashKey, "tag_2", "VIP_Customer");
        // Redis 原生指令: HSET demo:scenario:bigkey:user_tags:1001 tag_3 high_frequency_buyer
        redisTemplate.opsForHash().put(bigHashKey, "tag_3", "high_frequency_buyer");

        System.out.println("  已写入测试 Hash 容器: " + bigHashKey);

        // 2. 底层使用 MEMORY USAGE 指令探测该 Key 实际占用字节
        System.out.println("\n[步骤 2] 执行 MEMORY USAGE 探测 Key 物理内存占用：");
        // Redis 原生指令: MEMORY USAGE demo:scenario:bigkey:user_tags:1001
        Long memoryBytes = RedisConnectionHelper.getCommands().memoryUsage(bigHashKey);
        System.out.println("  Key [" + bigHashKey + "] 实际物理内存占用: " + memoryBytes + " 字节 (可通过该命令探测定位大 Key)");

        // 3. 演示非阻塞安全释放：使用 UNLINK 代替 DEL 异步清理
        System.out.println("\n[步骤 3] 生产治理演练：使用 UNLINK 异步非阻塞释放该 Key：");
        // Redis 原生指令: UNLINK demo:scenario:bigkey:user_tags:1001
        Boolean unlinkSuccess = redisTemplate.unlink(bigHashKey);
        System.out.println("  执行 UNLINK 结果: " + unlinkSuccess + " (底层逻辑摘除并交由后台 bio 线程异步回收内存，绝不阻塞主线程)");

        // 4. 总结面试高分要点
        System.out.println("\n[步骤 4] 面试答题核心骨架梳理：");
        System.out.println("  1. 【概念定义】：String > 10KB，集合类元素 > 5000~10000 或内存 > 10MB；");
        System.out.println("  2. 【四大危害】：单线程阻塞拖垮主循环、出口网络带宽打满、分片集群内存倾斜(OOM)、主从同步重连死循环；");
        System.out.println("  3. 【解决三板斧】：");
        System.out.println("     - 事前：业务拆分(Hash分桶/String切块) + 序列化高压；");
        System.out.println("     - 事中：`redis-cli --bigkeys` 扫描定位 + `MEMORY USAGE` 检查；");
        System.out.println("     - 事后：读取用游标 `HSCAN`/`SSCAN`，删除坚决用 `UNLINK` 异步释放！");
        System.out.println("================================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

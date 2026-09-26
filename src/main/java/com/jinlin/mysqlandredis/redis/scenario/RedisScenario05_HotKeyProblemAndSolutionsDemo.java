package com.jinlin.mysqlandredis.redis.scenario;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Random;

/**
 * Redis 业务场景篇 05: 什么是热 Key (HotKey)？单节点过载危害与两大终极解决方案 (多级缓存与加随机后缀散列)
 *
 * 面试真题：
 * 1. 什么是热key?
 * 2. 如何解决热key问题?
 *
 * 核心原理深度剖析：
 *
 * 一、什么是 Redis 的热 Key (HotKey)？
 * - 指在极短的时间窗口内，受到远超常规水平的高并发访问、且访问量高度集中的单个或少数几个 Key；
 * - 典型场景：
 *   1) 电商爆款秒杀单品 (如茅台/限定款球鞋抢购，数万人并发读取同一款商品详情)；
 *   2) 突发爆料新闻 (如微博热搜突发事件，数万次/秒读取同一条微博帖子)；
 *   3) 大促全平台通用的通用满减优惠券。
 * - 判定阈值：通常单个 Key 的并发 QPS 达到数千至数万次/秒，或占用单节点总处理能力的 20% 以上。
 *
 * 二、热 Key 带来的致命缺点与危害？
 * 1. 【单节点 CPU 瞬间飙到 100%】：
 *    - 无论 Redis Cluster 分片集群规模多大 (即使有 100 个节点)，根据哈希槽算法 `CRC16(key) % 16384`，
 *      【同一个 Key 必定只能被路由并存储在某一台特定的 Master 节点上】；
 *    - 面对瞬时几十万 QPS 的突发洪峰，所有的网络流量全部硬砸在这一台机器上，该节点 CPU 瞬间打满瘫痪，而其余 99 个节点完全闲置！
 * 2. 【单机网卡出口带宽打爆】：
 *    - 产生严重的网络丢包，导致部署在该节点上的其他完全无关的正常业务 Key 也全部超时中断；
 * 3. 【级联引发 MySQL 缓存雪崩与击穿】：
 *    - 若该 Redis 节点因过载宕机，或者该热 Key 在峰值瞬间刚好过期，这股海量请求将失去阻拦，
 *      瞬间全部击穿打在底层的 MySQL 数据库上，造成数据库连接池耗尽、CPU 100% 崩溃。
 *
 * 三、如何解决热 Key 问题？(行业两大黄金实践)：
 *
 * 1. 【终极方案 1：引入应用层多级缓存 (Multi-Level Local Cache / Caffeine) —— 最推荐】：
 *    - 在微服务业务端 (JVM 内部) 引入本地内存缓存 (如 Caffeine)，设置极短的过期时间 (例如 1~3 秒)；
 *    - 当外部突发热点流量涌入时，99.9% 的只读请求在微服务进程内被直接拦截并瞬间返回 (纳秒级响应)；
 *    - 真正打到 Redis 上的请求可能从几十万次骤降至几百次，Redis 负载几乎为零！
 *
 * 2. 【终极方案 2：热 Key 拆分与加随机后缀散列分流 (Random Suffix Sharding)】：
 *    - 既然单个 Key 只能落在一台机器上，我们可以将该热 Key 复制多份，分别赋予不同的随机后缀：
 *      例如将商品 `item:hot:6001` 拆分为 5 个完全相同的副本：
 *      `item:hot:6001_0`、`item:hot:6001_1`、`item:hot:6001_2`、`item:hot:6001_3`、`item:hot:6001_4`；
 *    - 因为后缀不同，CRC16 算出的哈希槽完全不同，这 5 个副本会【被均匀分散到集群中的不同 Master 节点】；
 *    - 客户端在读取数据时，在本地随机生成 0~4 的后缀发起查询：
 *      `int index = ThreadLocalRandom.current().nextInt(5);`
 *      `String val = redis.get("item:hot:6001_" + index);`
 *    - 效果：将原本集中冲击在单节点的万级流量，瞬间均匀稀释均摊到 5 台不同的机器上！
 *
 * 3. 【方案 3：读写分离】：
 *    - 为分片 Master 挂载多个只读 Slave，客户端配置只读连接分流。
 */
public class RedisScenario05_HotKeyProblemAndSolutionsDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisScenario05] 热 Key (HotKey) 过载危害剖析与随机后缀散列分流实战演示");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 实机模拟：将原本单个热 Key 复制为 3 个加随机后缀的分散副本
        System.out.println("[步骤 1] 演示热 Key 副本散列方案：向不同副本写入同一份热点数据：");
        String baseKey = "demo:scenario:hotkey:iphone";
        String hotPayload = "iPhone16_FlashSale_Price_5999";
        int replicaCount = 3;

        for (int i = 0; i < replicaCount; i++) {
            String replicaKey = baseKey + "_" + i;
            // Redis 原生指令: SET demo:scenario:hotkey:iphone_0 iPhone16_FlashSale_Price_5999 EX 600
            // Redis 原生指令: SET demo:scenario:hotkey:iphone_1 iPhone16_FlashSale_Price_5999 EX 600
            // Redis 原生指令: SET demo:scenario:hotkey:iphone_2 iPhone16_FlashSale_Price_5999 EX 600
            redisTemplate.opsForValue().set(replicaKey, hotPayload, Duration.ofMinutes(10));
            System.out.println("  成功创建热 Key 副本: " + replicaKey + " (不同后缀使 CRC16 槽位不同，分散到集群不同节点)");
        }

        // 2. 模拟客户端发起高并发读取：通过随机后缀均摊流量
        System.out.println("\n[步骤 2] 模拟客户端发起 5 次并发请求，随机路由到不同副本以分散负载：");
        Random random = new Random();
        for (int req = 1; req <= 5; req++) {
            int randomSuffix = random.nextInt(replicaCount);
            String targetKey = baseKey + "_" + randomSuffix;

            // Redis 原生指令: GET demo:scenario:hotkey:iphone_0 (或 _1, _2 随机访问)
            String value = redisTemplate.opsForValue().get(targetKey);
            System.out.println("  第 " + req + " 次读取 -> 路由访问副本 [" + targetKey + "], 获取内容=" + value);
        }

        // 3. 架构对比总结
        System.out.println("\n[步骤 3] 面试高分回答总结：");
        System.out.println("  1. 【定义成因】：极短时间窗口内访问频次极高的 Key，由于 CRC16 取模必定落入单节点，导致该节点 CPU 100%、网卡打爆、波及其他业务；");
        System.out.println("  2. 【发现途径】：事前申报、客户端埋点计数、`redis-cli --hotkeys` 抽样扫描；");
        System.out.println("  3. 【解决两大杀手锏】：");
        System.out.println("     - 杀手锏 1 (本地多级缓存)：JVM 内使用 Caffeine 设置 1~3 秒短缓存，直接拦截 99.9% 读流量；");
        System.out.println("     - 杀手锏 2 (随机后缀散列)：将 Key 复制为 `key_0` ~ `key_N`，客户端随机读取，使流量均匀分摊到不同集群分片！");
        System.out.println("================================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

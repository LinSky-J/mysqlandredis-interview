package com.jinlin.mysqlandredis.redis.scenario;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Redis 业务场景篇 07: 缓存雪崩、击穿、穿透是什么？怎么解决？与布隆过滤器底层原理深度解析
 *
 * 面试真题：
 * 1. 缓存雪崩、击穿、穿透是什么? 怎么解决?
 * 2. 布隆过滤器原理介绍一下
 *
 * 核心原理深度剖析：
 *
 * 一、三大缓存灾难对比及系统防御战术：
 *
 * 1. 【缓存雪崩 (Cache Avalanche)】：
 *    - 现象：大量缓存数据在【同一时间点集中大面积到期失效】，或者 【Redis 节点突发宕机断电】；
 *      导致原本由 Redis 拦截的巨量读流量瞬间如同雪崩倾泻至 MySQL，数据库 CPU 瞬间 100% 并崩溃；
 *    - 解决方案：
 *      1) 离散 TTL (加随机扰动)：基础过期时间 + 随机时间 (例如 1 小时 + 1~10 分钟随机数)，防止集体到期；
 *      2) 高可用架构保障：部署 Redis 哨兵或分片 Cluster，从库多副本热备，杜绝单点宕机；
 *      3) 限流降级保底：在网关或应用层 (Sentinel) 对 MySQL 查询实施并发信号量隔离与限流降级。
 *
 * 2. 【缓存击穿 (Cache Breakdown)】：
 *    - 现象：某个【超高频访问的单点热 Key (如热搜爆料/秒杀爆品)】在某一瞬间【刚好 TTL 到期失效】；
 *      在失效的极短时间窗口内，成千上万个并发线程同时未命中缓存，蜂拥涌入 MySQL 抢占排队去查库重建缓存，将数据库行锁与连接池挤爆；
 *    - 解决方案：
 *      1) 互斥锁 (Mutex Lock / 分布式锁)：未命中时，只有成功拿到分布式锁 (`SET NX EX`) 的 1 个线程去查库写缓存，
 *         其余线程自旋等待重试，将并发查询收敛为单次查询；
 *      2) 逻辑永不过期：物理上不设 TTL，Value 包装逻辑过期时间戳，由后台异步守护线程定期检测并平滑刷新。
 *
 * 3. 【缓存穿透 (Cache Penetration)】：
 *    - 现象：客户端疯狂请求查询一个【在 Redis 中不存在、且在 MySQL 中也根本不存在的非法 Key】(如恶意黑客构造 `id = -9999` 或非法 UUID 扫库)；
 *      导致每次请求查缓存未命中、查 DB 亦未命中 (因而也不会写入缓存)，每次恶意请求都长驱直入打在数据库上；
 *    - 解决方案：
 *      1) 业务合法性前置强校验：Controller/网关层校验参数规则 (如 ID 必须正整数)；
 *      2) 缓存空对象 (Cache Null Object)：DB 查为空时，依然向 Redis 写入空占位符并设短暂 TTL (如 2 分钟)；
 *      3) 布隆过滤器 (Bloom Filter) 前置护城河：将所有合法 ID 预先加载至布隆过滤器，未通过者直接当场拦截！
 *
 * 二、布隆过滤器 (Bloom Filter) 底层原理解析：
 * 1. 结构组成：
 *    - 一个固定长度的【二进制位图 (Bitset / Bitmap)】；
 *    - 一组平行的【无偏哈希函数 (K 个独立的 Hash 函数)】。
 * 2. 元素写入机制：
 *    - 当写入元素 (如 `userId="2001"`) 时，用 K 个哈希函数分别计算该元素，得到 K 个位图下标位置；
 *    - 将位图中对应这 K 个位置的 bit 统一涂抹置为 `1`。
 * 3. 元素检索判断机制：
 *    - 检索某元素是否存在时，同样使用这 K 个哈希函数算出 K 个下标并检查位图状态：
 *      - 【只要有任意一个 bit 为 0】：该元素【必定 100% 绝对不存在于系统中】！(直接拦截请求，终结穿透)；
 *      - 【若所有的 bit 全都为 1】：该元素【大概率存在，但存在微小的误判可能 (False Positive)】。
 * 4. 局限性与缺点：
 *    - 存在哈希冲突与误判率 (可通过扩大位图长度 m 和增加哈希函数个数 k 将误判率压制至 0.01% 以下)；
 *    - 【无法直接物理删除元素】：因为某个 bit 位很可能同时被多个不同元素的哈希映射所共享，直接将某位抹 0 会导致其他元素被“误杀”！
 */
public class RedisScenario07_AvalancheBreakdownPenetrationBloomDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisScenario07] 缓存雪崩、击穿、穿透定义与防御实战，及布隆过滤器原理解析");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 实机防御演练 1：针对缓存穿透的“空值占位缓存”策略
        System.out.println("[步骤 1] 演示防御缓存穿透：当 DB 查无此数据时，向 Redis 写入短暂空值占位符：");
        String nonExistKey = "demo:scenario:item:illegal:-9999";

        // 模拟查 DB 返回 null，写入带 2 分钟 TTL 的 NULL 占位符
        // Redis 原生指令: SET demo:scenario:item:illegal:-9999 NULL_PLACEHOLDER EX 120
        redisTemplate.opsForValue().set(nonExistKey, "NULL_PLACEHOLDER", Duration.ofMinutes(2));

        // Redis 原生指令: GET demo:scenario:item:illegal:-9999
        String val = redisTemplate.opsForValue().get(nonExistKey);
        System.out.println("  恶意请求再次命中缓存: key=" + nonExistKey + ", 获取到空值占位: " + val + " (成功阻断请求击穿至 MySQL)");

        // 2. 实机防御演练 2：基于 Redis Bitmap 模拟布隆过滤器位图机制
        System.out.println("\n[步骤 2] 演示布隆过滤器位图机制：向指定 Hash 槽位设置标记位并检测：");
        String bloomFilterKey = "demo:scenario:bloom:whitelist";

        // 模拟合法数据 "Item_1001" 经过两个独立 Hash 函数计算，命中了位图下标 18 和 35
        // Redis 原生指令: SETBIT demo:scenario:bloom:whitelist 18 1
        redisTemplate.opsForValue().setBit(bloomFilterKey, 18, true);
        // Redis 原生指令: SETBIT demo:scenario:bloom:whitelist 35 1
        redisTemplate.opsForValue().setBit(bloomFilterKey, 35, true);

        // 检测合法元素 (18 和 35 均为 true，判断大概率存在)
        // Redis 原生指令: GETBIT demo:scenario:bloom:whitelist 18
        Boolean bit18 = redisTemplate.opsForValue().getBit(bloomFilterKey, 18);
        // Redis 原生指令: GETBIT demo:scenario:bloom:whitelist 35
        Boolean bit35 = redisTemplate.opsForValue().getBit(bloomFilterKey, 35);
        System.out.println("  合法 Key 探测结果: bit[18]=" + bit18 + ", bit[35]=" + bit35 + " => 布隆过滤器判定: 数据可能存在，允许放行");

        // 检测非法元素 (计算出的下标 99 为 false，判定绝对不存在)
        // Redis 原生指令: GETBIT demo:scenario:bloom:whitelist 99
        Boolean bit99 = redisTemplate.opsForValue().getBit(bloomFilterKey, 99);
        System.out.println("  非法 Key 探测结果: bit[99]=" + bit99 + " => 布隆过滤器判定: 数据【绝对 100% 不存在】，直接拦截，严禁查库！");

        // 3. 架构对比总结
        System.out.println("\n[步骤 3] 面试三大缓存灾难对比与速记口诀：");
        System.out.println("  +----------+-----------------------------------+-----------------------------------+");
        System.out.println("  | 灾难类型 | 根本成因                          | 生产核心解决方案                  |");
        System.out.println("  +----------+-----------------------------------+-----------------------------------+");
        System.out.println("  | 缓存雪崩 | 大量 Key 同时过期、或 Redis 宕机  | TTL 叠加随机扰动值、哨兵/集群高可用|");
        System.out.println("  +----------+-----------------------------------+-----------------------------------+");
        System.out.println("  | 缓存击穿 | 单个超热 Key 到期瞬间海量并发涌入 | 分布式互斥锁只许 1 人查库、逻辑不过期");
        System.out.println("  +----------+-----------------------------------+-----------------------------------+");
        System.out.println("  | 缓存穿透 | 查缓存和查库都不存在的恶意非法 Key| 参数校验拦截、缓存空对象、布隆过滤器");
        System.out.println("  +----------+-----------------------------------+-----------------------------------+");
        System.out.println("================================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

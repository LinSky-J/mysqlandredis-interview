package com.jinlin.mysqlandredis.redis.scenario;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Redis 业务场景篇 01: 为什么使用 Redis？为什么比 MySQL 快？本地缓存与 Redis 对比及并发量级量化
 *
 * 面试真题：
 * 1. 为什么使用 redis?
 * 2. 为什么 redis 比 mysql 要快?
 * 3. 本地缓存和 Redis 缓存的区别?
 * 4. 高并发场景，Redis单节点 + MySQL单节点能有多大的并发量?
 *
 * 核心原理深度剖析：
 *
 * 一、为什么使用 Redis？(业务价值)：
 * 1. 【高性能 (High Performance)】：
 *    - 纯内存运行，读写响应时间通常在数十微秒 (µs) 到几毫秒内，为用户端带来极致流畅的交互体验；
 * 2. 【高并发 (High Concurrency)】：
 *    - 关系型数据库 (MySQL) 单机连接数有限且受限于磁盘 I/O，无法承受海量峰值请求；
 *    - 将数据放入 Redis，让缓存吸收 90% 以上的高频读与热点计数请求，充当 MySQL 坚不可摧的“护城河”；
 * 3. 【丰富易用的数据结构】：
 *    - String、Hash、List、Set、ZSet、Bitmap、HyperLogLog、Geospatial 等，能以极高开发效率实现排行榜、消息队列、UV 统计、附近的人等。
 *
 * 二、为什么 Redis 比 MySQL 要快？(底层三大底层架构差异)：
 * 1. 【存储介质不同 (物理内存 vs 磁盘文件)】：
 *    - Redis 是纯内存数据库，直接对物理内存进行字节级寻址读写；
 *    - MySQL 虽然有 Buffer Pool 内存缓冲，但其本质是面向磁盘的数据库，任何写入操作都必须涉及 Redo Log 顺序刷盘、Undo Log、以及脏页异步刷盘，存在物理 I/O 开销；
 * 2. 【线程模型与并发锁竞争不同】：
 *    - Redis 核心命令执行是【单线程 Reactor 模型】，不存在多线程上下文切换、线程创建销毁、以及读写锁/死锁竞争开销；
 *    - MySQL 是多线程模型，高并发下大量线程并发争抢行锁 (Row Lock)、间隙锁 (Gap Lock)、表锁和 Buffer Pool 互斥量，锁竞争与线程阻塞代价极高；
 * 3. 【数据检索与索引复杂度不同】：
 *    - Redis 是纯 Key-Value 键值对系统，基于哈希表寻址 ($O(1)$) 或跳表 ($O(\log N)$) 内存指针跳转；
 *    - MySQL 是通用关系数据库，采用 B+ 树多层磁盘页结构，查询常涉及多表关联、回表查询、辅助索引扫描与磁盘页换入换出。
 *
 * 三、本地缓存 (Caffeine / Guava) 和 Redis 缓存的区别？
 * 1. 访问延迟：
 *    - 本地缓存：JVM 内存对象指针引用，纳秒 (ns) 级，无任何网络开销与对象序列化；
 *    - Redis 缓存：需经过进程间网络通信 (TCP) 与数据序列化/反序列化，微秒 (µs) 级。
 * 2. 存储容量与 GC 影响：
 *    - 本地缓存：受制于 JVM 堆内存大小 (通常几 GB)，存放大对象易引起频繁的 GC 停顿 (Stop-The-World) 甚至 OOM；
 *    - Redis 缓存：独立进程运行，单机可分配数十 GB 甚至数百 GB 内存，对 Java 应用本身的 GC 完全零干扰。
 * 3. 分布式一致性与多实例共享：
 *    - 本地缓存：各个微服务实例彼此隔离，同一个商品在 Node A 更新后，Node B 无法感知，容易产生“脏读”；
 *    - Redis 缓存：集中式集中存储，所有微服务实例共享全局统一视图，天然保障多实例间的数据一致。
 * 4. 重启与持久化：
 *    - 本地缓存：随微服务应用重启、发布而瞬间蒸发，造成缓存瞬时击穿；
 *    - Redis 缓存：具备 RDB/AOF 持久化与主从哨兵高可用，重启或节点漂移不丢失数据。
 * => 生产最佳方案：【多级缓存架构】——Caffeine (本地拦截 Top 1% 超热点) + Redis (集中共享缓存) + MySQL (持久兜底)。
 *
 * 四、高并发场景，Redis 单节点 + MySQL 单节点能有多大并发量？(量化指标)：
 * 1. 【Redis 单节点】：
 *    - 读 QPS：通常在 **80,000 ~ 120,000 QPS** (每秒 8万~12万次)；
 *    - 若开启 Redis 6.0 多线程 I/O 并采用流水线 (Pipeline)，单节点可逼近 **200,000 ~ 300,000 QPS**。
 * 2. 【MySQL 单节点】 (标准云主机 8C16G / SSD)：
 *    - 读 QPS (简单单行索引命中 SELECT)：约为 **3,000 ~ 6,000 QPS**；
 *    - 写 TPS (涉及事务与 Redo Log 刷盘)：约为 **800 ~ 2,000 TPS**；
 *    - 极端高规格物理机 + NVMe SSD + 深度参数优化下，读写混合通常也只能支撑 **10,000 ~ 15,000 QPS**。
 * => 核心对比结论：Redis 单节点的并发承载能力是 MySQL 单节点的 **50 ~ 100 倍**！
 */
public class RedisScenario01_WhyRedisAndPerfComparisonDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisScenario01] Redis 业务价值：为什么快、与 MySQL 对比、本地缓存差异与量化指标");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 实机模拟：向 Redis 写入高频热点商品缓存
        System.out.println("[步骤 1] 写入高并发热点商品缓存 (验证微秒级极速读写)：");
        String itemKey = "demo:scenario:item:8801";

        // Redis 原生指令: SET demo:scenario:item:8801 iPhone15_Pro_Max EX 3600
        redisTemplate.opsForValue().set(itemKey, "iPhone15_Pro_Max", Duration.ofHours(1));

        // Redis 原生指令: GET demo:scenario:item:8801
        String itemName = redisTemplate.opsForValue().get(itemKey);
        System.out.println("  成功从 Redis 极速读取缓存商品: key=" + itemKey + ", value=" + itemName);

        // 2. 打印多级缓存横向对比表
        System.out.println("\n[步骤 2] 本地缓存 (Caffeine) vs 分布式缓存 (Redis) 横向对比：");
        System.out.println("  +----------------+-------------------------------+-------------------------------+");
        System.out.println("  | 对比指标       | 本地缓存 (如 Caffeine / Guava)| 分布式缓存 (Redis)            |");
        System.out.println("  +----------------+-------------------------------+-------------------------------+");
        System.out.println("  | 响应延迟       | 极快 (纳秒级，直接 JVM 寻址)  | 极快 (微秒级，受网络开销制约) |");
        System.out.println("  | 跨节点共享     | 不支持 (各微服务节点各自独立) | 天然支持 (全局唯一统一数据视图|");
        System.out.println("  | 内存容量上限   | 较小 (受限于 JVM 堆大小/GC)   | 庞大 (独立服务器，数十至上百G)|");
        System.out.println("  | 数据一致性维护 | 极难 (需广播消息同步失效)     | 容易 (集中更新或删除单点)     |");
        System.out.println("  | 生产最佳实践   | 多级缓存 L1 (过滤亿级超热点)  | 多级缓存 L2 (承载主力集中共享)|");
        System.out.println("  +----------------+-------------------------------+-------------------------------+");

        // 3. 并发量级对照
        System.out.println("\n[步骤 3] Redis 单机 vs MySQL 单机 生产并发量级对照标杆：");
        System.out.println("  - Redis 单节点 QPS: 80,000 ~ 120,000+ (纯内存无锁单线程 Reactor)");
        System.out.println("  - MySQL 单节点 读 QPS: 3,000 ~ 6,000 (走主键/唯一索引简单查询)");
        System.out.println("  - MySQL 单节点 写 TPS: 800 ~ 2,000 (受事务行锁、Redo Log 刷盘与磁盘寻道限制)");
        System.out.println("  => 结论: Redis 并发性能达到 MySQL 的 50 ~ 100 倍，是高并发架构的必备防波堤！");
        System.out.println("================================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

package com.jinlin.mysqlandredis.redis.datastructure;

/**
 * Redis 数据结构篇 10: 字典 (dict) 哈希表扩容触发条件、负载因子与 COW 保护机制
 *
 * 面试真题：哈希表是怎么扩容的？
 *
 * 核心考点与原理：
 * 1. 双哈希表结构 (ht[0] 与 ht[1])：
 *    - 每个 Redis 字典结构体 dict 内部都常备两个哈希表：dictht ht[2]；
 *    - 正常未扩容状态下，数据全部存放在 ht[0] 中，ht[1] 的尺寸为 0 且指针为空；
 *    - 当满足扩容条件时，Redis 为 ht[1] 分配新的更大内存空间，并开启渐进式搬迁。
 *
 * 2. 扩容触发的核心指标：负载因子 (Load Factor)
 *    - 计算公式：load_factor = ht[0].used (键值对数量) / ht[0].size (哈希桶总数)。
 *
 * 3. 扩容触发的两种阈值情况：
 *    - 情况 A（日常常规状态）：
 *      当服务器【没有】在执行 BGSAVE (RDB 生成) 或 BGREWRITEAOF (AOF 重写) 子进程时，
 *      只要 load_factor >= 1，立即触发哈希表扩容。
 *    - 情况 B（持久化子进程运行中）：
 *      当服务器【正在】执行 BGSAVE 或 BGREWRITEAOF 时，
 *      扩容门槛大幅提高至 load_factor >= 5 才会触发扩容！
 *
 * 4. 深度追问：为什么在 BGSAVE/BGREWRITEAOF 时要将负载因子提高到 5？
 *    - 核心机制：Linux 操作系统的 Copy-On-Write (COW, 写时复制) 机制。
 *    - Redis 在 fork() 出子进程进行持久化时，父子进程共享同一份物理内存页；
 *    - 若此时进行哈希表扩容与 rehash，频繁移动节点会修改大量内存页，导致 Linux 内核触发巨量物理页拷贝，
 *      造成 Redis 内存占用瞬间翻倍甚至引发 OOM 崩溃；
 *    - 因此 Redis 故意在持久化期间提高扩容门槛（提高到 5），最大限度减少 COW 内存分裂。
 *
 * 5. 扩容后新表容量确定：
 *    - ht[1] 的大小为：大于等于 ht[0].used * 2 的【第一个 2 的 N 次幂】(2^N)；
 *    - 例如 used = 1500，1500 * 2 = 3000，最近的 2 的幂即为 4096 (2^12)。
 *    - 分配完成后将 rehashidx 设为 0，正式进入渐进式 rehash 阶段。
 */
public class RedisDs10_DictRehashExpansionDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisDs10] 字典 (dict) 哈希表扩容机制、负载因子与 COW 写时复制保护深度剖析");
        System.out.println("================================================================================");

        // 1. 扩容触发条件对比表
        System.out.println("[步骤 1] 哈希表扩容与缩容触发条件矩阵：");
        System.out.println("  +----------------------+--------------------+------------------------------------------+");
        System.out.println("  | 操作类型             | 触发负载因子门槛   | 系统保护机制与考量                       |");
        System.out.println("  +----------------------+--------------------+------------------------------------------+");
        System.out.println("  | 常规扩容             | load_factor >= 1   | 无后台持久化任务，轻快扩容保证 O(1) 效率 |");
        System.out.println("  | 避让持久化扩容       | load_factor >= 5   | 避免因 rehash 大量写内存触发 Linux COW 爆内存 |");
        System.out.println("  | 自动收缩 (缩容)      | load_factor < 0.1  | 键值对删除较多时，收缩 ht[1] 释放空闲内存|");
        System.out.println("  +----------------------+--------------------+------------------------------------------+");

        // 2. 容量计算模拟
        System.out.println("\n[步骤 2] 新表 ht[1] 扩容容量 2^N 计算模拟：");
        int[] testUsedSizes = {3, 100, 1500, 60000};
        for (int used : testUsedSizes) {
            int target = used * 2;
            int n = 1;
            while (n < target) {
                n <<= 1;
            }
            System.out.println("  -> 当前 used = " + used + " -> 目标 2*used = " + target + " -> ht[1] 扩容分配容量: " + n);
        }

        // 3. 面试答题核心要点总结
        System.out.println("\n[步骤 3] 哈希表扩容面试核心回答三部曲：");
        System.out.println("  1. 'Redis 字典包含 ht[0] 和 ht[1] 两个哈希表，通过负载因子 = used / size 判定扩容；'");
        System.out.println("  2. '常规情况下 >=1 扩容，但如果有 BGSAVE/BGREWRITEAOF 时为了避免触发 Linux COW 写时复制内存暴增，门槛提升至 >=5；'");
        System.out.println("  3. '新表容量为大于等于 used * 2 的最小 2^N，分配后通过渐进式 rehash 迁移数据。'");
    }
}

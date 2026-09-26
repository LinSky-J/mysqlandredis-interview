package com.jinlin.mysqlandredis.redis.datastructure;

/**
 * Redis 数据结构篇 08: 压缩列表 (Ziplist) 物理内存布局与连锁更新 (Cascade Update) 缺陷剖析
 *
 * 面试真题：压缩列表是怎么实现的？
 *
 * 核心考点与原理：
 * 1. 压缩列表的设计初衷：
 *    - 传统双向链表每个节点需要存储 2 个 8 字节指针 (prev, next)，且离散 malloc 会导致大量的内存碎片；
 *    - 压缩列表通过【一片完全连续的物理内存空间】来顺序存储一系列数据项，零指针开销，是典型的以 CPU 计算换取内存空间的极致优化。
 *
 * 2. 压缩列表整体物理结构：
 *    <zlbytes> <zltail> <zllen> <entry 1> <entry 2> ... <entry N> <zlend>
 *    - zlbytes: 4 字节 (uint32_t)，记录整个压缩列表所占用的总内存字节数；
 *    - zltail:  4 字节 (uint32_t)，记录表尾 entry 距离列表起始地址的字节偏移量，支撑 O(1) 快速定位表尾节点；
 *    - zllen:   2 字节 (uint16_t)，记录包含的节点数量 (若超过 65535，需遍历全表计算)；
 *    - entry:   具体的各数据节点，长度可变；
 *    - zlend:   1 字节 (uint8_t，固定为 0xFF 即 255)，标识压缩列表的物理结束边界。
 *
 * 3. 每一个 Entry 节点的内部结构：
 *    <prevlen> <encoding> <entry-data>
 *    - prevlen (前驱节点长度)：
 *      - 若前一节点长度 < 254 字节，prevlen 占用 1 字节；
 *      - 若前一节点长度 >= 254 字节，prevlen 占用 5 字节 (首字节为 0xFE，后 4 字节存长度)；
 *      - 作用：支持反向遍历！当前节点指针减去 prevlen 即可直接跳到前一节点首地址。
 *    - encoding: 记录内容的编码类型 (整数 or 字符串) 以及数据的实际长度；
 *    - entry-data: 实际存储的数据内容。
 *
 * 4. 压缩列表的核心痛点：连锁更新 (Cascade Update)
 *    - 触发场景：多个连续节点的长度恰好在 250~253 字节之间（它们的 prevlen 原本都只占 1 字节）；
 *    - 此时若在表头插入一个长度大于等于 254 字节的新节点，后方节点 entry1 的 prevlen 必须扩展到 5 字节；
 *    - 这使得 entry1 的总长度也突破了 254 字节，导致其后方的 entry2 的 prevlen 也必须扩容为 5 字节...
 *    - 连续触发多米诺骨牌式的 realloc 内存重新分配，单次插入的时间复杂度由 O(N) 瞬间恶化为 O(N^2)！
 */
public class RedisDs08_ZiplistStructureAndCascadeUpdateDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisDs08] 压缩列表 (Ziplist) 连续内存布局与连锁更新 (Cascade Update) 深度剖析");
        System.out.println("================================================================================");

        // 1. 打印物理内存布局图解
        System.out.println("[步骤 1] 压缩列表物理内存连续布局示意图：");
        System.out.println("  +------------+------------+---------+------------------+------------------+-------+");
        System.out.println("  | zlbytes    | zltail     | zllen   | entry 1          | entry 2 ...      | zlend |");
        System.out.println("  | (4 字节)   | (4 字节)   | (2字节) | (变长数据节点)   | (变长数据节点)   | (1字节|");
        System.out.println("  +------------+------------+---------+------------------+------------------+-------+");
        System.out.println("  - zlbytes: 整个 ziplist 占用的总字节数");
        System.out.println("  - zltail : 表尾节点相对起始地址的偏移量 (使得 rpush / rpop 无需遍历直接定位尾节点)");
        System.out.println("  - zllen  : 节点数量，小于 65535 时 O(1) 获取");
        System.out.println("  - zlend  : 恒为 0xFF (255)，标记内存结束");

        // 2. 单个 Entry 结构与反向遍历原理
        System.out.println("\n[步骤 2] 单个 Entry 节点内部字段与反向遍历实现机制：");
        System.out.println("  Entry 布局: [ prevlen (1或5B) ] [ encoding (1/2/5B) ] [ entry-data (实际内容) ]");
        System.out.println("  - 前向遍历: 当前指针 + prevlen + encoding_len + data_len -> 下一个节点首地址");
        System.out.println("  - 反向遍历: 当前指针 - prevlen -> 前一个节点首地址 (极其高效)");

        // 3. 连锁更新雪崩原理
        System.out.println("\n[步骤 3] 连锁更新 (Cascade Update) 雪崩机理分析：");
        System.out.println("  节点 e1 (252B) -> 节点 e2 (252B) -> 节点 e3 (252B)... (各节点 prevlen 均占用 1B)");
        System.out.println("  1. 插入新节点 New_Node (300B, >= 254B);");
        System.out.println("  2. e1 需要存储 New_Node 长度，e1 的 prevlen 从 1B 扩为 5B (+4B);");
        System.out.println("  3. e1 总长度变为 252 + 4 = 256B，也跨越了 254B 门槛！");
        System.out.println("  4. e2 同样被迫扩展 prevlen 为 5B，总长度也变为 256B 并继续波及 e3...;");
        System.out.println("  5. 结果: 每次插入引起连续 N 次内存 realloc 拷贝，性能发生灾难性雪崩。");
        System.out.println("  -> 解决方案: Redis 5.0 提出并在 7.0 全面用【listpack (紧凑列表)】彻底终结该缺陷！");
    }
}

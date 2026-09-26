package com.jinlin.mysqlandredis.redis.datastructure;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import io.lettuce.core.api.sync.RedisCommands;

/**
 * Redis 数据结构篇 05: 跳跃表 (Skiplist) 物理结构、多级索引与跨度 (Span) 排名实现
 *
 * 面试真题：跳表是怎么实现的？
 *
 * 核心考点与源码级结构：
 * 1. 跳表的本质：
 *    跳表是在普通有序单链表之上，构建了“多层稀疏索引”的链表数据结构。
 *    最底层（第 0 层）包含所有的元素；越往高层，节点越稀疏，查询时从最高层向右、向下逐层缩小搜索范围，
 *    将原本普通单链表 O(N) 的线性查找复杂度，降维到了对数级别的 O(logN)（堪比二分查找）。
 *
 * 2. Redis 跳表节点 (zskiplistNode) 核心结构体：
 *    - sds ele: 存放实际的 member 字符串；
 *    - double score: 存放分值，用于排布节点顺序；
 *    - struct zskiplistNode *backward: 后退指针，第 0 层指向前驱节点，支撑 ZREVRANGE 从大到小逆向遍历；
 *    - struct zskiplistLevel level[]: 层级数组（柔性数组，1~32 层，Redis 7.0 为 1~64 层）：
 *      - struct zskiplistNode *forward: 前进指针，指向同一层的下一个节点；
 *      - unsigned long span: 【跨度】，记录当前前进指针跨过了多少个底层节点。
 *
 * 3. 为什么 Redis 专门设计了跨度 (Span) 属性？
 *    - 学术界原始跳表只用来做查找，并不包含 span 字段；
 *    - Redis 在每个 level 结构中额外维护了 span：在沿最高层向右、向下搜索目标节点时，
 *      只需将沿途经过的所有层级前进指针上的 span 进行累加，就能在 O(logN) 的时间复杂度内，
 *      精确计算出当前元素在整个集合中的全局排名（Rank，即 ZRANK 命令的底层核心机制）！
 */
public class RedisDs05_SkiplistStructureAndSearchDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisDs05] 跳表 (Skiplist) 物理层级模型、多级向前指针与跨度 (Span) 排名实测");
        System.out.println("================================================================================");

        RedisCommands<String, String> commands = RedisConnectionHelper.getCommands();

        String skipKey = "demo:skiplist:simulation";
        commands.del(skipKey);

        // 写入具备清晰分值阶梯的数据
        commands.zadd(skipKey, 10.0, "Node_10");
        commands.zadd(skipKey, 25.0, "Node_25");
        commands.zadd(skipKey, 40.0, "Node_40");
        commands.zadd(skipKey, 55.0, "Node_55");
        commands.zadd(skipKey, 70.0, "Node_70");
        commands.zadd(skipKey, 85.0, "Node_85");

        System.out.println("[步骤 1] 模拟跳表底层自顶向下的查找路径与 Span 累加计算全局 Rank：");
        System.out.println("  - 目标: 查找 Node_55 (score=55.0)");
        System.out.println("  - 查找路径 (从最高层开始向右跳跃 -> 遇到更大分值则下沉):");
        System.out.println("    [Level 3] Header -> (forward, span=3) -> Node_40 (score=40 < 55) [累加 Span = 3]");
        System.out.println("    [Level 3] Node_40 -> (forward 指向 Node_85, score=85 > 55, 超出目标!) -> 下沉至 Level 2");
        System.out.println("    [Level 2] Node_40 -> (forward 指向 Node_70, score=70 > 55, 超出目标!) -> 下沉至 Level 1");
        System.out.println("    [Level 1] Node_40 -> (forward, span=1) -> 成功命中 Node_55! [累加 Span = 3 + 1 = 4]");
        System.out.println("  - 结论: 经历极少次跨越，直接精确定位目标，并且累加的 Span=4 即为该元素在底层升序中的排名！");

        // 验证 Lettuce ZRANK 执行结果
        Long rank = commands.zrank(skipKey, "Node_55");
        System.out.println("  -> 本地 Redis 执行 ZRANK 返回的 0-based 排名: " + rank + " (对应累计跨度 4 个节点)");

        // 3. 跳表结构 ASCII 可视化图解
        System.out.println("\n[步骤 2] 跳跃表内部多级索引模型示意图：");
        System.out.println("  Level 3: [Header] -------------------------> [Node_40] -----------------------> [Node_85]");
        System.out.println("  Level 2: [Header] -------------> [Node_25] -> [Node_40] ----------> [Node_70] -> [Node_85]");
        System.out.println("  Level 1: [Header] -> [Node_10] -> [Node_25] -> [Node_40] -> [Node_55] -> [Node_70] -> [Node_85]");
        System.out.println("  Level 0: [Header] <-> [N_10] <-> [N_25] <-> [N_40] <-> [N_55] <-> [N_70] <-> [N_85] (双向后退指针)");
    }
}

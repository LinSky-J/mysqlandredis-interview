package com.jinlin.mysqlandredis.redis.datastructure;

import java.util.Random;

/**
 * Redis 数据结构篇 06: 跳表随机层高算法 (zslRandomLevel) 与幂次定律 (p=0.25) 概率模型
 *
 * 面试真题：跳表是怎么设置层高的？
 *
 * 核心考点与原理：
 * 1. 为什么层高必须随机？
 *    - 若采用固定规则（如每两个节点必有一个 2 层节点），插入或删除一个节点会打破固定的几何分布，
 *      导致大量相邻节点的层高被连锁重构，引发类似平衡树的繁重调整；
 *    - 随机层高（Random Level）使得每个节点在被创建时独立决定层高，终生不变，
 *      插入和删除仅涉及局部前驱后继指针修改，彻底解耦。
 *
 * 2. 源码级算法逻辑 (Redis zslRandomLevel)：
 *    - 每个节点初始层高至少为 1（Level 1 包含全部节点）；
 *    - 循环掷骰子：以概率 p（Redis 设定为 0.25，即 25%）决定是否增加一层；
 *    - 如果随机值命中 p，层数 +1 并继续循环判断；一旦未命中立即终止；
 *    - 最大层高上限受宏定义保护：ZSKIPLIST_MAXLEVEL = 32（Redis 7.0 升级为 64）。
 *
 * 3. 为什么 Redis 选择 p = 0.25 而不是 0.5？
 *    - 若 p = 0.5（抛硬币正反面），节点平均指针数期望为 1 / (1 - 0.5) = 2 个指针；
 *    - 若 p = 0.25，节点平均指针数期望为 1 / (1 - 0.25) = 1.33 个指针！
 *    - 这使得每个跳表节点平均只花费 1.33 个指针空间，比 p=0.5 节约近 34% 的内存，
 *      而时间复杂度依然稳稳保持在对数级别的 O(log_4 N) = O(logN)！
 */
public class RedisDs06_SkiplistRandomLevelAlgorithmDemo {

    private static final int ZSKIPLIST_MAXLEVEL = 32;
    private static final double ZSKIPLIST_P = 0.25;
    private static final Random random = new Random();

    /**
     * 100% 还原 Redis C 语言底层 zslRandomLevel() 随机层高算法
     */
    public static int simulateZslRandomLevel() {
        int level = 1;
        // 模拟 (random() & 0xFFFF) < (ZSKIPLIST_P * 0xFFFF)
        while (random.nextDouble() < ZSKIPLIST_P) {
            level += 1;
        }
        return Math.min(level, ZSKIPLIST_MAXLEVEL);
    }

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisDs06] 跳表随机层高算法 (zslRandomLevel) 蒙特卡洛统计实测");
        System.out.println("================================================================================");

        // 模拟生成 100,000 个跳表节点并统计层高分布
        int totalNodes = 100000;
        int[] levelCounts = new int[ZSKIPLIST_MAXLEVEL + 1];
        long totalPointers = 0;

        for (int i = 0; i < totalNodes; i++) {
            int lvl = simulateZslRandomLevel();
            levelCounts[lvl]++;
            totalPointers += lvl;
        }

        System.out.println("[步骤 1] 模拟生成 " + totalNodes + " 个跳表节点的随机层高统计分布：");
        for (int lvl = 1; lvl <= 6; lvl++) {
            double percentage = (levelCounts[lvl] * 100.0) / totalNodes;
            System.out.printf("  -> 层高 Level %d: %6d 个节点 (%5.2f%%, 理论期望: %.2f%%)\n",
                    lvl, levelCounts[lvl], percentage, (Math.pow(ZSKIPLIST_P, lvl - 1) * (1 - ZSKIPLIST_P) * 100.0));
        }

        double avgPointers = (double) totalPointers / totalNodes;
        System.out.printf("\n[步骤 2] 节点平均指针数量统计: %.4f (数学期望理论值: 1 / (1 - 0.25) = 1.3333)\n", avgPointers);

        System.out.println("\n[步骤 3] 随机层高算法优势小结：");
        System.out.println("  1. 零级联调整：插入删除无旋转变色，指针修改限制在 O(logN) 局部范围；");
        System.out.println("  2. 极致内存节省：在 p=0.25 策略下，平均仅消耗 1.33 个指针/节点，内存占用远低于平衡二叉树；");
        System.out.println("  3. 最大 32/64 层限制：足以轻松支撑 2^64 个海量元素的索引寻址。");
    }
}

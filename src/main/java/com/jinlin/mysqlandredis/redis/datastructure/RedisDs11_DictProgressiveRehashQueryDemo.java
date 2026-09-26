package com.jinlin.mysqlandredis.redis.datastructure;

/**
 * Redis 数据结构篇 11: 渐进式 Rehash 期间读写操作流转机制与双表查询逻辑
 *
 * 面试真题：哈希表扩容的时候，有读请求怎么查？
 *
 * 核心考点与原理：
 * 1. 为什么必须采用渐进式 rehash (Progressive Rehash)？
 *    - 若字典包含数百万、数千万个键值对，若一次性集中迁移，庞大的计算量和内存拷贝会导致 Redis 单线程主事件循环
 *      卡顿数十毫秒甚至数秒，引发客户端连接超时与雪崩断开；
 *    - 渐进式 rehash 将庞大的迁移任务切分为微小单元，分摊在后续每一次用户请求和后台定时任务中平滑完成。
 *
 * 2. 扩容期间【读请求 (查询)】的精确执行流程：
 *    - 第一步：先在老表 ht[0] 中计算哈希索引，在对应的哈希桶中遍历链表查找；
 *    - 第二步：若在 ht[0] 中找到了，直接返回值，查询结束；
 *    - 第三步：若在 ht[0] 中未找到，则去新表 ht[1] 中计算哈希索引并查找；
 *    - 结论：读请求采用“双表查找”机制，时间复杂度依然是极速的 O(1)（最多只多计算一次哈希值），
 *      无论目标数据是尚未搬迁（在 ht[0]）还是已经搬迁完毕（在 ht[1]），都能被瞬时准确检索到！
 *
 * 3. 扩容期间其他操作的行为准则：
 *    - 【新增 (Insert)】：一律直接写入新表 ht[1] 中，绝不向老表 ht[0] 写入任何新数据，确保 ht[0] 的键只减不增；
 *    - 【修改 (Update) / 删除 (Delete)】：先查 ht[0]，若有则在 ht[0] 操作；否则在 ht[1] 操作。
 *
 * 4. 渐进式迁移的两大推进引擎：
 *    - 引擎 A (被动触发，化整为零)：每次客户端发起对该字典的增删改查时，Redis 顺带将当前 rehashidx 桶上的所有链表节点
 *      一次性迁移到 ht[1]，随后 rehashidx++；
 *    - 引擎 B (主动定时任务，防饥饿)：在没有业务读写请求时，Redis 内部时间事件定时器 (serverCron) 会周期性地抽取
 *      1 毫秒 (dictRehashMilliseconds(1)) 的空闲 CPU，连续搬迁 100 个桶，确保在低峰期也能顺畅推进迁移；
 *    - 迁移结束：当 ht[0] 的所有桶都被掏空迁移后，释放 ht[0] 内存，将 ht[1] 设置为新的 ht[0]，重置 rehashidx = -1。
 */
public class RedisDs11_DictProgressiveRehashQueryDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisDs11] 渐进式 Rehash 期间读写操作执行逻辑与双表查询路由机制剖析");
        System.out.println("================================================================================");

        // 1. 读请求路由流程模拟
        System.out.println("[步骤 1] 渐进式 Rehash 期间【读请求】路由寻道伪代码逻辑演示：");
        System.out.println("  public Object get(String key) {");
        System.out.println("      // 1. 如果正在执行 rehash，顺带被动执行一步迁移 (单步 rehash)");
        System.out.println("      if (isRehashing()) {");
        System.out.println("          _dictRehashStep(); // 搬迁当前 rehashidx 桶，rehashidx++");
        System.out.println("      }");
        System.out.println("      // 2. 先查老表 ht[0]");
        System.out.println("      Object val = ht[0].find(key);");
        System.out.println("      if (val != null) {");
        System.out.println("          return val; // 命中老表直接返回");
        System.out.println("      }");
        System.out.println("      // 3. 老表未命中，且当前正处于 rehash 期间，再去新表 ht[1] 查");
        System.out.println("      if (isRehashing()) {");
        System.out.println("          return ht[1].find(key);");
        System.out.println("      }");
        System.out.println("      return null;");
        System.out.println("  }");

        // 2. 四大增删改查动作行为对照表
        System.out.println("\n[步骤 2] 扩容迁移期间增删改查四大动作行为对照矩阵：");
        System.out.println("  +-----------------+-------------------------------+--------------------------------------------+");
        System.out.println("  | 操作类型        | 目标哈希表路由规则            | 核心目的与原理                             |");
        System.out.println("  +-----------------+-------------------------------+--------------------------------------------+");
        System.out.println("  | 查询 (Read)     | 先查 ht[0]，找不到再查 ht[1]  | 保障数据 100% 不丢，常数级 O(1) 检索       |");
        System.out.println("  | 新增 (Insert)   | 绝对只写入新表 ht[1]          | 确保老表 ht[0] 只减不增，加速收敛搬迁过程  |");
        System.out.println("  | 更新 (Update)   | 先查 ht[0]，若有则更新；无则去| 确保已有旧数据原地更新，不产生双版本脏数据 |");
        System.out.println("  |                 | 新表 ht[1] 查找并更新         |                                            |");
        System.out.println("  | 删除 (Delete)   | 依次在两表中查找并删除        | 彻底抹除目标键值                           |");
        System.out.println("  +-----------------+-------------------------------+--------------------------------------------+");

        // 3. 面试标准高分回答总结
        System.out.println("\n[步骤 3] 面试高分精炼回答：");
        System.out.println("  '在渐进式 rehash 期间，读请求采用双表查询机制：先在老表 ht[0] 中查找，如果找到立即返回；如果没找到，再去新表 ht[1] 中查找；'");
        System.out.println("  '同时，读写操作都会顺带协助搬迁当前索引桶上的节点（单步 rehash），而写操作一律直接写入新表 ht[1]，保证老表只减不增，最终由定时任务兜底完成全部迁移。'");
    }
}

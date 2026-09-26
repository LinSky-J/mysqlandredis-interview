package com.jinlin.mysqlandredis.mysql.transaction;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 【面试题 02】MySQL 可能出现什么和并发相关问题？
 *
 * 核心考点剖析：
 * 当多个事务在无并发控制或隔离级别不足时，会产生以下 4 类典型并发异象 (Concurrency Anomalies)：
 *
 * 1. 脏读 (Dirty Read)：
 *    - 定义：事务 B 读取到了事务 A【尚未提交】的修改数据。
 *    - 危害：若事务 A 随后发生回滚(ROLLBACK)，事务 B 所读到的数据在物理上从未正式存在过，属于脏数据。
 *    - 发生隔离级别：Read Uncommitted (读未提交)。
 *
 * 2. 不可重复读 (Non-Repeatable Read)：
 *    - 定义：在同一个事务内，多次执行同一条【单行/特定记录】查询语句，得到的字段数据却不一致。
 *    - 原因：在两次查询的间隔期间，另一个事务修改(UPDATE)或删除(DELETE)了该行记录并【已提交】。
 *    - 侧重点：同一行数据的【值发生了改变】或行被删除了。
 *    - 发生隔离级别：Read Uncommitted、Read Committed。
 *
 * 3. 幻读 (Phantom Read)：
 *    - 定义：在同一个事务内，多次执行同一条【范围查询条件】(如 balance > 100)，第二次查询却读到了之前不存在的新增行。
 *    - 原因：在查询间隔期间，另一个并发事务在该查询范围内插入(INSERT)了新记录并【已提交】。
 *    - 侧重点：范围查询时记录的【行数变多了】(产生了幽灵记录)。
 *    - 发生隔离级别：Read Uncommitted、Read Committed (RR 级别下通过 MVCC + Next-Key Lock 解决)。
 *
 * 4. 丢失更新 (Lost Update)：
 *    - 第一类丢失更新 (回滚丢失/回滚覆盖)：事务 A 回滚时，把并发事务 B 已经提交的数据也一并抹去覆盖。
 *      (在任何现代支持锁机制的 RDBMS 中，由于修改操作必须加 X 锁，均已杜绝此现象)。
 *    - 第二类丢失更新 (提交覆盖)：事务 A 和 B 同时读取同一商品的库存为 10，A 减 1 扣为 9 并提交；
 *      B 也在内存中减 1 扣为 9 并提交，导致实际卖出 2 件商品但库存只减了 1 件，A 的更新被 B 覆盖。
 *      (解决机制：悲观锁 SELECT ... FOR UPDATE 或 乐观锁 WHERE version = old_version)。
 */
public class Tx02_ConcurrencyAnomaliesDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 02】MySQL 并发事务 4 大核心问题与对比全景");
        System.out.println("====================================================================");

        System.out.println(">>> 并发问题矩阵对照表：");
        System.out.println("-----------------------------------------------------------------------------------------");
        System.out.printf("%-20s | %-12s | %-12s | %-12s | %-15s%n",
                "隔离级别 (Isolation Level)", "脏读 (Dirty Read)", "不可重复读", "幻读 (Phantom)", "第二类丢失更新");
        System.out.println("-----------------------------------------------------------------------------------------");
        System.out.printf("%-20s | %-16s | %-16s | %-16s | %-15s%n", "Read Uncommitted (RU)", "可能发生 (×)", "可能发生 (×)", "可能发生 (×)", "需应用层锁防护");
        System.out.printf("%-20s | %-16s | %-16s | %-16s | %-15s%n", "Read Committed (RC)",   "已解决 (√)",   "可能发生 (×)", "可能发生 (×)", "需应用层锁防护");
        System.out.printf("%-20s | %-16s | %-16s | %-16s | %-15s%n", "Repeatable Read (RR)",   "已解决 (√)",   "已解决 (√)",   "基本/完全解决(√)", "需应用层锁防护");
        System.out.printf("%-20s | %-16s | %-16s | %-16s | %-15s%n", "Serializable (串行化)", "已解决 (√)",   "已解决 (√)",   "已解决 (√)",   "自动规避 (√)");
        System.out.println("-----------------------------------------------------------------------------------------");

        System.out.println("\n>>> 【核心考点追问】不可重复读 VS 幻读 的本质区别：");
        System.out.println("1. 关注点不同：");
        System.out.println("   - 不可重复读关注的是已有数据的【修改(UPDATE)】或【删除(DELETE)】，针对的是同一条记录的字段内容变了。");
        System.out.println("   - 幻读关注的是【新增(INSERT)】，针对的是同一个范围检索条件下的记录行数凭空增多了。");
        System.out.println("2. 锁解决手段不同：");
        System.out.println("   - 解决不可重复读只需要加【记录锁 (Record Lock)】锁住这一行，阻止其他事务 UPDATE 即可。");
        System.out.println("   - 解决幻读仅锁住已有记录毫无用处(因为新增的行在表中根本还不存在！)，必须使用【间隙锁 (Gap Lock)】锁定记录之间的缝隙，阻止 INSERT 插入！");

        DbConnectionHelper.printQueryResults("当前测试环境数据库事务隔离级别核查",
                "SELECT @@transaction_isolation AS 'Session_Isolation', @@global.transaction_isolation AS 'Global_Isolation';"
        );
    }
}

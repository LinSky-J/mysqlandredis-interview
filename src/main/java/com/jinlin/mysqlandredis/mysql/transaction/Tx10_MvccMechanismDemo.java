package com.jinlin.mysqlandredis.mysql.transaction;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 【面试题 10】介绍 MVCC 实现原理 (隐藏列 + UndoLog版本链 + ReadView 核心算法)
 *
 * 核心考点深度剖析 (大厂面试天花板考点)：
 * MVCC (Multi-Version Concurrency Control, 多版本并发控制)，用于在无需加锁的情况下解决【读-写并发冲突】。
 * 其底层依赖三大核心支柱协同工作：
 *
 * 一、 聚簇索引隐藏列 (Hidden Columns)：
 *     1. DB_TRX_ID (6 字节)：最近一次修改(INSERT/UPDATE)该行记录的事务 ID。
 *     2. DB_ROLL_PTR (7 字节)：回滚指针，指向该记录写入 Undo Log 的上一版本历史快照。
 *     3. DB_ROW_ID (6 字节)：表无主键时自动生成的隐藏主键。
 *
 * 二、 Undo Log 版本链 (Version Chain)：
 *     每次记录被 UPDATE 时，旧版本数据写入 Undo Log，并由最新记录的 DB_ROLL_PTR 指向前一版本，
 *     串联成一条从【最新版本 -> 最老版本】的单向版本链表。
 *
 * 三、 ReadView (一致性读视图结构)：
 *     当事务执行快照读时生成，包含 4 大关键参数：
 *     1. m_ids：生成 ReadView 时，当前系统中所有【活跃 (未提交)】的事务 ID 列表。
 *     2. min_trx_id：m_ids 中的最小值。
 *     3. max_trx_id：系统即将分配给下一个事务的 ID (即已分配最大事务 ID + 1)。
 *     4. creator_trx_id：创建该 ReadView 的事务 ID。
 *
 * 四、 可见性比对 4 步法则 (Visibility Comparison Rules)：
 *     遍历版本链中的每个版本，获取其 DB_TRX_ID 进行比对：
 *     ① 若 trx_id == creator_trx_id：本事务自己改的，【可见】。
 *     ② 若 trx_id < min_trx_id：说明该事务在 ReadView 生成前已提交，【可见】。
 *     ③ 若 trx_id >= max_trx_id：说明该事务在 ReadView 生成后才开启(未来事务)，【不可见】。
 *     ④ 若 min_trx_id <= trx_id < max_trx_id：
 *        - 若 trx_id 在 m_ids 活跃列表中：说明 ReadView 生成时还未提交，【不可见】；
 *        - 若 trx_id 不在 m_ids 活跃列表中：说明在 ReadView 生成前已提交完毕，【可见】。
 *     ⑤ 若不可见，则顺着 DB_ROLL_PTR 找前一个版本继续套用上述规则判定，直到找到可见版本或返回空！
 *
 * 五、 RC 与 RR 在 MVCC 上的根本区别：
 *     - RC (读已提交)：【每次执行 SELECT】时都会生成一个全新的 ReadView！
 *     - RR (可重复读)：【仅在第一次执行 SELECT】时生成 ReadView，后续查询全程复用该 ReadView！
 */
public class Tx10_MvccMechanismDemo {

    /**
     * 模拟行记录的历史版本快照
     */
    static class RecordVersion {
        String data;               // 数据内容
        long dbTrxId;              // 事务 ID
        RecordVersion dbRollPtr;   // 回滚指针，指向上一版本

        public RecordVersion(String data, long dbTrxId, RecordVersion dbRollPtr) {
            this.data = data;
            this.dbTrxId = dbTrxId;
            this.dbRollPtr = dbRollPtr;
        }
    }

    /**
     * 模拟 ReadView 读视图结构与判定引擎
     */
    static class SimulatedReadView {
        List<Long> mIds;       // 活跃事务列表
        long minTrxId;         // 活跃最小 trx_id
        long maxTrxId;         // 预分配下一事务 ID
        long creatorTrxId;     // 创建者事务 ID

        public SimulatedReadView(List<Long> activeTrxIds, long nextTrxId, long creatorId) {
            this.mIds = new ArrayList<>(activeTrxIds);
            this.minTrxId = mIds.isEmpty() ? nextTrxId : Collections.min(mIds);
            this.maxTrxId = nextTrxId;
            this.creatorTrxId = creatorId;
        }

        /**
         * 纯代码高仿真实现 MVCC 4 步可见性判定算法
         */
        public RecordVersion findVisibleVersion(RecordVersion latestVersion) {
            RecordVersion current = latestVersion;
            while (current != null) {
                long trxId = current.dbTrxId;

                // 规则 1: 自己修改的数据，自己可见
                if (trxId == creatorTrxId) {
                    System.out.printf("   [可见性判定] trx_id=%d == creator_trx_id=%d -> 【本事务修改可见】 (数据: %s)%n",
                            trxId, creatorTrxId, current.data);
                    return current;
                }

                // 规则 2: trx_id < min_trx_id，说明生成 ReadView 前已提交
                if (trxId < minTrxId) {
                    System.out.printf("   [可见性判定] trx_id=%d < min_trx_id=%d -> 【已提前提交可见】 (数据: %s)%n",
                            trxId, minTrxId, current.data);
                    return current;
                }

                // 规则 3: trx_id >= max_trx_id，未来事务不可见
                if (trxId >= maxTrxId) {
                    System.out.printf("   [可见性判定] trx_id=%d >= max_trx_id=%d -> 【未来事务不可见】，回溯 Undo Log...%n",
                            trxId, maxTrxId);
                    current = current.dbRollPtr;
                    continue;
                }

                // 规则 4: min_trx_id <= trx_id < max_trx_id
                if (mIds.contains(trxId)) {
                    System.out.printf("   [可见性判定] trx_id=%d 在活跃事务 m_ids%s 中未提交 -> 【未提交不可见】，回溯 Undo Log...%n",
                            trxId, mIds);
                    current = current.dbRollPtr;
                } else {
                    System.out.printf("   [可见性判定] trx_id=%d 不在活跃事务 m_ids%s 中 -> 【已提交可见】 (数据: %s)%n",
                            trxId, mIds, current.data);
                    return current;
                }
            }
            return null;
        }
    }

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 10】MVCC 实现原理深度剖析与高仿真 ReadView 算法演算");
        System.out.println("====================================================================");

        // 1. 构建 Undo Log 版本链
        // 初始版本: 事务 50 插入 "余额: 1000 元" (最老版本)
        RecordVersion v1 = new RecordVersion("张三余额: 1000 元", 50, null);
        // 历史修改 1: 事务 60 更新为 "余额: 2000 元"
        RecordVersion v2 = new RecordVersion("张三余额: 2000 元", 60, v1);
        // 当前最新记录: 事务 80 更新为 "余额: 5000 元" (最新版本)
        RecordVersion v3 = new RecordVersion("张三余额: 5000 元", 80, v2);

        System.out.println(">>> [Undo Log 版本链结构模拟]:");
        System.out.println("   [最新记录 v3] trx_id=80, 内容='张三余额: 5000 元' -> 指向 v2");
        System.out.println("   [历史快照 v2] trx_id=60, 内容='张三余额: 2000 元' -> 指向 v1");
        System.out.println("   [初始版本 v1] trx_id=50, 内容='张三余额: 1000 元' -> NULL\n");

        // 2. 模拟场景 A: 事务 70 开启快照读 (此时活跃事务有 [60, 80]，系统下一分配 trx_id=90)
        System.out.println(">>> [场景 A] 事务 70 开启并执行快照读:");
        System.out.println("   生成 ReadView: m_ids=[60, 80], min_trx_id=60, max_trx_id=90, creator_trx_id=70");
        SimulatedReadView readViewA = new SimulatedReadView(Arrays.asList(60L, 80L), 90L, 70L);
        RecordVersion visibleA = readViewA.findVisibleVersion(v3);
        System.out.println("   --> 事务 70 最终读取结果: " + (visibleA != null ? visibleA.data : "无记录"));

        // 3. 模拟场景 B: 事务 80 此时提交，但事务 70 在 REPEATABLE READ 下第二次查询
        System.out.println("\n>>> [场景 B] 事务 80 提交后，事务 70 在 REPEATABLE READ (可重复读) 下再次查询:");
        System.out.println("   由于 RR 隔离级别复用第一次生成的 ReadView，因此活跃列表依然视 80 为未提交！");
        RecordVersion visibleB = readViewA.findVisibleVersion(v3);
        System.out.println("   --> RR 级别复用 ReadView 读到结果: " + (visibleB != null ? visibleB.data : "无记录") + " (严格保持可重复读)");

        // 4. 模拟场景 C: 事务 70 在 READ COMMITTED (读已提交) 下再次查询
        System.out.println("\n>>> [场景 C] 事务 70 在 READ COMMITTED (读已提交) 下再次查询:");
        System.out.println("   RC 级别每次 SELECT 生成全新 ReadView！此时 80 已提交，活跃列表只剩 [60]，下一 trx_id=90");
        SimulatedReadView readViewC = new SimulatedReadView(Arrays.asList(60L), 90L, 70L);
        RecordVersion visibleC = readViewC.findVisibleVersion(v3);
        System.out.println("   --> RC 级别全新 ReadView 读到结果: " + (visibleC != null ? visibleC.data : "无记录") + " (读到了最新提交)");

        DbConnectionHelper.printQueryResults("当前数据库 MySQL 版本核对", "SELECT VERSION();");
    }
}

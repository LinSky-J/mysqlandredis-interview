package com.jinlin.mysqlandredis.mysql.log;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 【面试题 04】有了 Undo Log 为啥还需要 Redo Log 呢？
 *
 * 核心考点深度剖析 (顶级高频面试追问)：
 * 【核心答案】：
 * 1. Undo Log 负责【原子性 (Atomicity)】—— 保证未提交或失败事务能完全回滚；
 * 2. Redo Log 负责【持久性 (Durability)】—— 保证已提交事务哪怕遇到断电崩溃也绝不丢失；
 * 两者互为表里，共同构成了 InnoDB 崩溃恢复 (Crash Recovery) 的完整闭环！
 *
 * 为什么单靠 Undo Log 无法保证持久性？
 * 1. Undo Log 自身也是 Buffer Pool 中的“内存脏页”：
 *    - 当修改数据生成 Undo Log 时，Undo 内容首先也是保存在 Buffer Pool 的 Undo 数据页中的；
 *    - 如果服务器此时突发断电关机，内存中的数据页和 Undo 页将【同时瞬间灰飞烟灭】！
 *    - 如果没有 Redo Log，断电重启后连 Undo Log 自身都已经彻底丢失，数据库根本不知道哪些数据提交了、哪些没落盘！
 *
 * 2. 物理与逻辑日志的本质差异：
 *    - Undo Log 是【逻辑日志】(记录如何逆向还原数据，如 INSERT 记 DELETE)；
 *    - Redo Log 是【物理日志】(记录某个具体数据页偏移量上的物理字节变动，不仅保护业务数据页，也保护 Undo 页自身！)。
 *
 * 3. 崩溃恢复的黄金法则：“前滚 (Redo) + 后滚 (Undo)”：
 *    - 步骤 ① 前滚重做 (Roll-Forward)：
 *      MySQL 重启时，首先扫描磁盘上的 Redo Log，将所有在 Buffer Pool 中修改了但未来得及异步落盘的脏页(包括数据页和 Undo 页)，
 *      全部严格按照物理变更原样重做一遍，恢复到服务器崩溃前一瞬间的绝对真实状态！
 *    - 步骤 ② 后滚撤销 (Roll-Backward)：
 *      在所有数据页与 Undo 页被 Redo 救活后，InnoDB 利用完好无损的 Undo Log，
 *      将那些在崩溃时【尚未完成提交的孤儿活跃事务】顺着 Undo Log 全部撤销回滚清理干净！
 *
 * 业界神级总结：
 * “Redo 负责救活所有已提交的活人，Undo 负责埋葬所有未提交的死尸”！
 */
public class Log04_WhyBothUndoAndRedoDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 04】有了 Undo Log 为何还需要 Redo Log？(崩溃恢复闭环全解)");
        System.out.println("====================================================================");

        System.out.println(">>> 1. Undo Log 与 Redo Log 协同矩阵对比：");
        System.out.println("-------------------------------------------------------------------------------------------------");
        System.out.printf("%-12s | %-12s | %-14s | %-18s | %-25s%n",
                "日志类型", "记录维度", "保障 ACID 特性", "恢复时动作", "作用对象");
        System.out.println("-------------------------------------------------------------------------------------------------");
        System.out.printf("%-12s | %-12s | %-14s | %-18s | %-25s%n",
                "Undo Log", "逻辑日志", "A (原子性) + MVCC", "后滚撤销 (Roll-Backward)", "回滚未提交的脏数据与事务");
        System.out.printf("%-12s | %-12s | %-14s | %-18s | %-25s%n",
                "Redo Log", "物理日志", "D (持久性) + WAL",  "前滚重做 (Roll-Forward)",  "重做已提交但未落盘的内存脏页");
        System.out.println("-------------------------------------------------------------------------------------------------");

        System.out.println("\n>>> 2. 崩溃恢复 (Crash Recovery) 执行时序全链路：");
        System.out.println("   [宕机断电发生] -> 重启 MySQL 实例 -> 读取磁盘物理 Redo Log 文件组 ->");
        System.out.println("   -> ① 阶段一 (Redo 前滚): 将已提交的脏页与 Undo 页原路全部重做，内存与物理状态完美重现；");
        System.out.println("   -> ② 阶段二 (Undo 后滚): 依据恢复的 Undo Log，逐一回滚断电瞬间尚未提交的活跃未决事务；");
        System.out.println("   -> 数据库恢复至 100% 合法一致的稳定状态，正式开放对外客户端连接！");

        DbConnectionHelper.printQueryResults("当前 MySQL 实例 Checkpoint 检查点与 LSN 状态核对",
                "SHOW ENGINE INNODB STATUS;"
        );
    }
}

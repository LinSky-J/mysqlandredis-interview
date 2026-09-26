package com.jinlin.mysqlandredis.mysql.transaction;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 【面试题 06】可重复读隔离级别下，A事务提交的数据，在B事务能看见吗？
 *
 * 核心考点剖析 (高频面试致命陷阱题)：
 * 绝不能简单回答“能看见”或“看不见”！
 * 正确且惊艳面试官的标准回答是：
 * 【核心答案】：取决于 B 事务使用的是【快照读 (Snapshot Read)】还是【当前读 (Current Read)】！
 *
 * 1. 场景一：B 事务使用【快照读】(即普通 SELECT 语句)：
 *    - 【结论】：看不见！
 *    - 【底层机制】：在 REPEATABLE READ 隔离级别下，B 事务在执行第一条 SELECT 时会生成一个 ReadView。
 *      在 B 事务后续的整个生命周期中，只要是普通 SELECT，都会【严格复用同一个 ReadView】。
 *      即使 A 事务在此期间更新或插入了数据并成功 COMMIT，根据 ReadView 的可见性算法规则，
 *      A 事务属于“未来事务”，B 事务只能沿着 Undo Log 版本链追溯到 A 修改前的历史快照，因此绝对看不见！
 *
 * 2. 场景二：B 事务使用【当前读】(SELECT ... FOR UPDATE / LOCK IN SHARE MODE / UPDATE / DELETE)：
 *    - 【结论】：能看见最新提交的数据！
 *    - 【底层机制】：当前读不走 MVCC ReadView 快照，它要求必须读取数据库中最新的物理版本，并对读取到的记录加锁。
 *      因此当 B 事务执行 SELECT ... FOR UPDATE 时，能直接看到 A 事务刚刚提交的修改值。
 */
public class Tx06_RrReadCommittedVisibilityDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 06】可重复读下 A 提交的数据 B 能否看见？(快照读 VS 当前读 实机对比)");
        System.out.println("====================================================================");

        // 1. 初始化张三(id=1)余额为 1000.00
        DbConnectionHelper.executeSqlScript("UPDATE tx_account SET balance = 1000.00 WHERE id = 1;");

        try (Connection connB = DbConnectionHelper.getConnection();
             Connection connA = DbConnectionHelper.getConnection()) {

            // 确保双连接处于默认 REPEATABLE READ 隔离级别
            connB.setAutoCommit(false);
            connA.setAutoCommit(false);

            // 步骤 1: 事务 B 开启事务，执行第一次普通 SELECT (此时在 InnoDB 生成 ReadView)
            System.out.println(">>> 步骤 1: 事务 B 开启事务，执行第一次普通 SELECT (快照读生成 ReadView)...");
            double balanceStep1 = queryBalance(connB, "SELECT balance FROM tx_account WHERE id = 1;");
            System.out.println("   [事务 B 第 1 次查询结果] 张三余额 = " + balanceStep1 + " 元");

            // 步骤 2: 事务 A 开启事务，将张三余额修改为 8888.00 并 COMMIT 提交
            System.out.println("\n>>> 步骤 2: 事务 A 修改张三余额为 8888.00 元，并执行 COMMIT 提交...");
            try (PreparedStatement ps = connA.prepareStatement(
                    "UPDATE tx_account SET balance = 8888.00 WHERE id = 1;")) {
                ps.executeUpdate();
            }
            connA.commit();
            System.out.println("   [事务 A 提交完毕] 物理磁盘/Buffer Pool 中张三余额已变为 8888.00！");

            // 步骤 3: 事务 B 再次执行普通 SELECT (快照读)
            System.out.println("\n>>> 步骤 3: 事务 B 再次执行普通 SELECT (验证快照读是否能看见 A 提交的数据)...");
            double balanceStep3 = queryBalance(connB, "SELECT balance FROM tx_account WHERE id = 1;");
            System.out.println("   [事务 B 普通 SELECT 结果] 张三余额 = " + balanceStep3 + " 元");
            if (balanceStep3 == 1000.00) {
                System.out.println("   --> 【结论 1】快照读：看不见！复用了事务初期的 ReadView，依旧读取 Undo Log 中的历史快照。");
            }

            // 步骤 4: 事务 B 执行 SELECT ... FOR UPDATE (当前读)
            System.out.println("\n>>> 步骤 4: 事务 B 执行 SELECT ... FOR UPDATE (验证当前读是否能看见 A 提交的数据)...");
            double balanceStep4 = queryBalance(connB, "SELECT balance FROM tx_account WHERE id = 1 FOR UPDATE;");
            System.out.println("   [事务 B FOR UPDATE 结果] 张三余额 = " + balanceStep4 + " 元");
            if (balanceStep4 == 8888.00) {
                System.out.println("   --> 【结论 2】当前读：能看见！当前读绕过 ReadView，直接读取数据库中最新已提交的物理版本并加排他锁！");
            }

            connB.commit(); // 事务 B 结束
        } catch (SQLException e) {
            System.err.println("实测异常: " + e.getMessage());
        }
    }

    private static double queryBalance(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
            }
        }
        return -1.0;
    }
}

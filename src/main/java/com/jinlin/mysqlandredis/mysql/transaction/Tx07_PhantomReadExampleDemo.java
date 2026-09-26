package com.jinlin.mysqlandredis.mysql.transaction;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 【面试题 07】举个例子说可重复读下的幻读问题
 *
 * 核心考点剖析 (顶级深度高频考点)：
 * 面试官追问：“既然可重复读 (RR) 下快照读复用 ReadView，A 事务 INSERT 提交后，B 事务普通 SELECT 根本查不到，为什么还说 RR 会有幻读？”
 *
 * 到底什么是真正意义上的幻读？
 * 幻读并不是指两次普通 SELECT 查出的行数不同(因为 MVCC 保证了快照读行数严格一致)，
 * 真正的幻读现象是指：【由于当前读 (UPDATE/DELETE/SELECT FOR UPDATE) 打破了 MVCC 快照的一致性状态，导致幽灵数据显形或操作冲突】！
 *
 * 经典幻读复现 5 步全过程：
 * 步骤 1: 事务 B 开启事务，快照读查询 `id >= 30` 的记录，结果返回 0 条。
 * 步骤 2: 事务 A 开启事务，插入一条 `id=30` 的新记录并成功 COMMIT 提交。
 * 步骤 3: 事务 B 再次执行快照读，受 ReadView 保护，结果依然为 0 条(以为不存在)。
 * 步骤 4: 事务 B 执行范围更新: `UPDATE ... WHERE id >= 30`。
 *         【底层内幕】：UPDATE 属于【当前读】，它会跨越 ReadView 抓到 A 刚刚提交的最新记录，
 *         并将其隐藏的 DB_TRX_ID 悄悄改写为事务 B 自己的事务 ID！
 * 步骤 5: 事务 B 再次执行原普通快照读查询 `id >= 30`：
 *         根据 ReadView 核心法则(本事务修改的数据对自己永远可见)，这行记录奇迹般地凭空出现了！
 *         从 0 条突然变成 1 条，幽灵记录被彻底激活，实测复现幻读！
 */
public class Tx07_PhantomReadExampleDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 07】可重复读隔离级别下【幻读幽灵现象】实机经典复现");
        System.out.println("====================================================================");

        // 1. 清理测试环境：删除 id >= 30 的数据
        DbConnectionHelper.executeSqlScript("DELETE FROM tx_account WHERE id >= 30;");

        try (Connection connB = DbConnectionHelper.getConnection();
             Connection connA = DbConnectionHelper.getConnection()) {

            connB.setAutoCommit(false);
            connA.setAutoCommit(false);

            // 步骤 1: 事务 B 开启事务，执行第一次范围快照读 (id >= 30)
            System.out.println(">>> 步骤 1: 事务 B 开启事务，执行快照读查询 (WHERE id >= 30)...");
            int count1 = countAccounts(connB, "SELECT count(*) FROM tx_account WHERE id >= 30;");
            System.out.println("   [事务 B 第 1 次查询结果] 记录数 = " + count1 + " 条 (目前为空)");

            // 步骤 2: 事务 A 插入一条 id=30 的新数据并 COMMIT
            System.out.println("\n>>> 步骤 2: 事务 A 并发插入新用户 (id=30, name='幽灵新用户', balance=888.00) 并提交 COMMIT...");
            try (PreparedStatement ps = connA.prepareStatement(
                    "INSERT INTO tx_account (id, user_name, balance, version) VALUES (30, '幽灵新用户', 888.00, 0);")) {
                ps.executeUpdate();
            }
            connA.commit();
            System.out.println("   [事务 A 提交成功] 数据已实际写入磁盘与 Buffer Pool！");

            // 步骤 3: 事务 B 再次执行普通快照读
            System.out.println("\n>>> 步骤 3: 事务 B 再次执行相同快照读 (验证 MVCC 保护)...");
            int count2 = countAccounts(connB, "SELECT count(*) FROM tx_account WHERE id >= 30;");
            System.out.println("   [事务 B 第 2 次快照读结果] 记录数 = " + count2 + " 条 (由于 ReadView 保护，依然查不到 A 插入的新行)");

            // 步骤 4: 事务 B 执行一次全范围 UPDATE (触发当前读 + 改写 trx_id)
            System.out.println("\n>>> 步骤 4: 事务 B 执行 UPDATE 操作: 将 id >= 30 的记录名称改为 'B事务改写版'...");
            int affectedRows = 0;
            try (PreparedStatement ps = connB.prepareStatement(
                    "UPDATE tx_account SET user_name = 'B事务改写版' WHERE id >= 30;")) {
                affectedRows = ps.executeUpdate();
            }
            System.out.printf("   [事务 B 当前读 UPDATE 结果] 影响行数 = %d 行！(UPDATE 读取到了 A 插入的最新数据，并将该行 trx_id 换成了 B 自己！)%n",
                    affectedRows);

            // 步骤 5: 事务 B 再次执行原普通快照读 (见证奇迹的时刻)
            System.out.println("\n>>> 步骤 5: 事务 B 再次执行原快照读 SELECT count(*) FROM tx_account WHERE id >= 30...");
            int count3 = countAccounts(connB, "SELECT count(*) FROM tx_account WHERE id >= 30;");
            System.out.println("   [事务 B 第 3 次快照读结果] 记录数 = " + count3 + " 条！");
            if (count3 > 0) {
                System.out.println("   --> 【幻读实测发生！】原本查不到的记录，在经历一次 UPDATE 后，由于自身 trx_id 变为可见，");
                System.out.println("       在快照读中像幻影幽灵一样突然显形！从 0 条变成了 " + count3 + " 条！");
            }

            connB.commit();
        } catch (SQLException e) {
            System.err.println("实测异常: " + e.getMessage());
        }

        // 清理测试数据
        DbConnectionHelper.executeSqlScript("DELETE FROM tx_account WHERE id >= 30;");
    }

    private static int countAccounts(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
}

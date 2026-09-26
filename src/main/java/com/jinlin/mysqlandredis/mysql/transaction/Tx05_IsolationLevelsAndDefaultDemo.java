package com.jinlin.mysqlandredis.mysql.transaction;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 【面试题 05】事务的隔离级别有哪些？MySQL 默认级别是什么？
 *
 * 核心考点剖析：
 * 1. SQL-92 标准四大隔离级别：
 *    ① Read Uncommitted (读未提交)：最低级别，事务可以读取到其他并发事务尚未提交的修改(脏读)。
 *    ② Read Committed (读已提交, RC)：只能读取到已提交的数据。解决了脏读，但同一事务内两次读取可能数值不同(不可重复读)。
 *    ③ Repeatable Read (可重复读, RR)：同一事务内多次读取相同数据得到的结果严格一致。解决了脏读和不可重复读，并通过 MVCC + Next-Key Lock 极大程度/完全消除幻读。
 *    ④ Serializable (串行化)：最高级别，所有普通 SELECT 自动加共享锁(S锁)，事务按序串行化执行，杜绝一切并发异常，但吞吐量极低。
 *
 * 2. MySQL 的默认隔离级别是什么？
 *    - 【答案】：MySQL InnoDB 引擎的默认隔离级别是 **REPEATABLE READ (可重复读)**！
 *    - 对比：Oracle、SQL Server、PostgreSQL 的默认隔离级别通常是 **READ COMMITTED (读已提交)**。
 *
 * 3. 【高频深挖追问】为什么 MySQL 默认选择 Repeatable Read，而不是 Read Committed？
 *    - 历史根源 (Binlog 与主从复制)：
 *      在 MySQL 5.0 及更早版本，Binlog 只有一种格式：Statement (基于执行 SQL 语句复制)。
 *      在 RC 级别下没有间隙锁(Gap Lock)，当事务并发执行时，若事务 A 先插入未提交，事务 B 插入后先提交，
 *      写入 Binlog 的顺序与主库实际在 Buffer Pool 中执行的物理顺序可能倒置，导致从库重放时产生严重的数据主从不一致！
 *      而在 RR 级别下，InnoDB 通过 Next-Key Lock (临键锁) 锁死间隙，强行使并发插入序列化，保证了 Statement 格式 Binlog 的主从严格一致。
 *    - 现代实践：
 *      现代业务普遍采用 Row 格式 Binlog (binlog_format = ROW)。因此在很多高并发互联网大厂(如阿里巴巴)，
 *      会将生产环境默认隔离级别调整为 **READ COMMITTED**，以避免间隙锁导致的死锁和性能开销，提升并发吞吐。
 */
public class Tx05_IsolationLevelsAndDefaultDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 05】事务的隔离级别有哪些？MySQL 默认级别实机验证与深挖");
        System.out.println("====================================================================");

        // 1. 实机查询 MySQL 默认隔离级别
        System.out.println(">>> 查询当前 MySQL 实例的会话与全局事务隔离级别:");
        String checkIsolationSql = "SELECT @@transaction_isolation AS 'Session_Level', @@global.transaction_isolation AS 'Global_Level';";
        DbConnectionHelper.printQueryResults("MySQL 实例默认隔离级别实机查询", checkIsolationSql);

        // 2. 演示动态切换当前会话的隔离级别并核验
        try (Connection conn = DbConnectionHelper.getConnection()) {
            System.out.println("\n>>> [实测] 动态设置当前会话隔离级别为 READ COMMITTED...");
            try (java.sql.Statement stmt = conn.createStatement()) {
                stmt.execute("SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;");
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT @@transaction_isolation;")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("   当前连接切换后的隔离级别为: " + rs.getString(1));
                    }
                }
            }

            System.out.println(">>> [实测] 恢复当前会话隔离级别为默认 REPEATABLE READ...");
            try (java.sql.Statement stmt = conn.createStatement()) {
                stmt.execute("SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;");
            }
        } catch (SQLException e) {
            System.err.println("切换隔离级别失败: " + e.getMessage());
        }

        System.out.println("\n>>> 总结面试官常考点：");
        System.out.println("1. Oracle/PG 默认 RC，MySQL InnoDB 默认 RR。");
        System.out.println("2. 历史上为了解决 Statement 格式 Binlog 下的主从数据不一致，MySQL 选择了 RR 依靠间隙锁保证顺序。");
        System.out.println("3. 如今切换为 Row 格式 Binlog 后，很多公司选择改用 RC 消除 Gap Lock 降低死锁率。");
    }
}

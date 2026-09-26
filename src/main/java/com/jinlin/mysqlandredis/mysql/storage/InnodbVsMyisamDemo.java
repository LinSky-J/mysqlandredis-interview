package com.jinlin.mysqlandredis.mysql.storage;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 问题 04: 说一下 MySQL 的 InnoDB 与 MyISAM 的区别？
 * 
 * 核心考点深度对比：
 * 1. 事务支持：InnoDB 强力支持 ACID；MyISAM 不支持任何事务操作。
 * 2. 锁级别：InnoDB 支持行级锁 (Record Lock/Gap Lock)；MyISAM 仅支持全表锁。
 * 3. 崩溃恢复：InnoDB 基于 Redo Log 自动 Crash-Safe 恢复；MyISAM 易损毁需手工运行 myisamchk。
 * 4. 索引模型：
 *    - InnoDB: 聚簇索引 (主键索引叶子节点包含整行数据，按主键点查极快；二级索引叶子存主键值需回表)。
 *    - MyISAM: 非聚簇索引 (主键索引与普通索引叶子节点均存放行数据的磁盘物理地址指针)。
 * 5. COUNT(*) 性能差异：
 *    - MyISAM 在表头元数据记录了总行数，无 WHERE 条件时读取复杂度为 O(1)。
 *    - InnoDB 受 MVCC 多版本并发可见性影响，必须通过遍历索引逐行统计 O(N)。
 */
public class InnodbVsMyisamDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 04】InnoDB vs MyISAM 核心技术差异实机验证与对比矩阵");
        System.out.println("====================================================================");

        // 1. 初始化两张结构完全相同的对比表
        String dropInnodb = "DROP TABLE IF EXISTS interview_cmp_innodb;";
        String dropMyisam = "DROP TABLE IF EXISTS interview_cmp_myisam;";

        String createInnodb = "CREATE TABLE interview_cmp_innodb ("
                + "  id INT PRIMARY KEY AUTO_INCREMENT,"
                + "  username VARCHAR(50) NOT NULL,"
                + "  score INT NOT NULL"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String createMyisam = "CREATE TABLE interview_cmp_myisam ("
                + "  id INT PRIMARY KEY AUTO_INCREMENT,"
                + "  username VARCHAR(50) NOT NULL,"
                + "  score INT NOT NULL"
                + ") ENGINE=MyISAM DEFAULT CHARSET=utf8mb4;";

        String insertDataInnodb = "INSERT INTO interview_cmp_innodb (username, score) VALUES ('Alice', 95), ('Bob', 88), ('Charlie', 91);";
        String insertDataMyisam = "INSERT INTO interview_cmp_myisam (username, score) VALUES ('Alice', 95), ('Bob', 88), ('Charlie', 91);";

        DbConnectionHelper.executeSqlScript(
                dropInnodb, dropMyisam,
                createInnodb, createMyisam,
                insertDataInnodb, insertDataMyisam
        );

        // 2. 验证 COUNT(*) 执行计划差异
        DbConnectionHelper.printQueryResults("MyISAM 执行 COUNT(*) 的执行计划 (注意 Extra 列为 Select tables optimized away)", 
                "EXPLAIN SELECT COUNT(*) FROM interview_cmp_myisam;");

        DbConnectionHelper.printQueryResults("InnoDB 执行 COUNT(*) 的执行计划 (必须走索引逐行计算符合当前事务版本的数据)", 
                "EXPLAIN SELECT COUNT(*) FROM interview_cmp_innodb;");

        // 3. 验证 MyISAM 不支持事务与回滚 (插入后 rollback 无效)
        System.out.println(">>> [验证 3] 测试 MyISAM 对事务回滚的反应:");
        try (Connection conn = DbConnectionHelper.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO interview_cmp_myisam (username, score) VALUES ('Dave(MyISAM测试)', 70)")) {
                ps.executeUpdate();
            }
            conn.rollback(); // 尝试在 MyISAM 表上回滚
            System.out.println("    对 MyISAM 表执行了 conn.rollback()！");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        DbConnectionHelper.printQueryResults("查询 MyISAM 表记录 (Dave 依然被永久写入，回滚完全失效！)", 
                "SELECT * FROM interview_cmp_myisam;");

        printComparisonMatrix();
    }

    private static void printComparisonMatrix() {
        System.out.println("---------------- InnoDB 与 MyISAM 终极对比矩阵 ----------------");
        System.out.printf("%-18s | %-32s | %-32s\n", "比较维度", "InnoDB", "MyISAM");
        System.out.println("-".repeat(88));
        System.out.printf("%-18s | %-32s | %-32s\n", "事务支持", "完整支持 ACID (COMMIT/ROLLBACK)", "完全不支持任何事务");
        System.out.printf("%-18s | %-32s | %-32s\n", "锁粒度", "行级锁、间隙锁、Next-Key Lock", "仅支持全表锁 (Table Lock)");
        System.out.printf("%-18s | %-32s | %-32s\n", "崩溃安全 (Crash-Safe)", "Redo Log + 2PC 自动恢复，零丢数据", "断电极易索引损坏，需 myisamchk");
        System.out.printf("%-18s | %-32s | %-32s\n", "索引组织结构", "聚簇索引 (数据+主键索引同存叶子)", "非聚簇索引 (索引叶子仅存物理行指针)");
        System.out.printf("%-18s | %-32s | %-32s\n", "外键约束", "支持物理 FOREIGN KEY 约束", "不支持");
        System.out.printf("%-18s | %-32s | %-32s\n", "COUNT(*) 效率", "受 MVCC 影响需扫描索引 O(N)", "元数据内置计数器，秒出 O(1)");
        System.out.printf("%-18s | %-32s | %-32s\n", "物理文件构成", ".ibd (独立表空间文件存数据与索引)", ".MYD (数据) + .MYI (索引) + .sdi");
        System.out.printf("%-18s | %-32s | %-32s\n", "内存缓存机制", "Buffer Pool 统一缓存数据与索引", "Key Buffer 仅缓存索引，数据靠 OS");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

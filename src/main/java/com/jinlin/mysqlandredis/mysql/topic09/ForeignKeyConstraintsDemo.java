package com.jinlin.mysqlandredis.mysql.topic09;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 问题 09: 说一下外键约束
 * 
 * 核心考点：
 * 1. 作用：强制保障主子表之间的引用完整性 (Referential Integrity)。
 * 2. 行为策略：CASCADE (级联)、SET NULL (设空)、RESTRICT/NO ACTION (拒绝操作)。
 * 3. 阿里开发手册规约：【强制】不得使用物理外键与级联，一切外键概念必须在应用层解决。
 * 4. 废弃物理外键原因：
 *    - 写入性能剧烈损耗 (每次 DML 触发外键索引检索与检查)；
 *    - 跨表加共享锁导致死锁率极高；
 *    - 分布式分库分表与微服务场景彻底失效；
 *    - 历史冷数据归档清洗难以处理。
 */
public class ForeignKeyConstraintsDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 09】MySQL 物理外键约束、级联删除与阿里开发规范实战");
        System.out.println("====================================================================");

        // 1. 初始化主表与带外键约束的子表
        String dropStudent = "DROP TABLE IF EXISTS interview_fk_student;";
        String dropCollege = "DROP TABLE IF EXISTS interview_fk_college;";

        String createCollege = "CREATE TABLE interview_fk_college ("
                + "  college_id INT PRIMARY KEY AUTO_INCREMENT,"
                + "  college_name VARCHAR(50) NOT NULL"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String createStudent = "CREATE TABLE interview_fk_student ("
                + "  student_id INT PRIMARY KEY AUTO_INCREMENT,"
                + "  student_name VARCHAR(50) NOT NULL,"
                + "  college_id INT NOT NULL,"
                + "  CONSTRAINT fk_student_college FOREIGN KEY (college_id) "
                + "    REFERENCES interview_fk_college (college_id) "
                + "    ON DELETE CASCADE ON UPDATE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String insertCollege = "INSERT INTO interview_fk_college (college_id, college_name) VALUES "
                + "(1, '计算机学院'), (2, '软件学院');";

        String insertStudent = "INSERT INTO interview_fk_student (student_name, college_id) VALUES "
                + "('张三', 1), ('李四', 1), ('王五', 2);";

        DbConnectionHelper.executeSqlScript(dropStudent, dropCollege, createCollege, createStudent, insertCollege, insertStudent);
        DbConnectionHelper.printQueryResults("初始学生与学院数据", 
                "SELECT s.student_id, s.student_name, c.college_name "
                + "FROM interview_fk_student s JOIN interview_fk_college c ON s.college_id = c.college_id;");

        // 2. 模拟外键拦截：尝试插入不存在的学院 ID 99
        System.out.println(">>> 尝试插入非法 college_id=99，观察外键约束拦截效果:");
        try (Connection conn = DbConnectionHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO interview_fk_student (student_name, college_id) VALUES (?, ?)")) {
            ps.setString(1, "非法外键学生");
            ps.setInt(2, 99);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("【成功捕获外键拦截异常】: " + e.getMessage());
        }

        // 3. 模拟级联删除 (CASCADE)
        System.out.println("\n>>> 删除主表 计算机学院 (college_id=1)，观察子表自动级联删除:");
        DbConnectionHelper.executeSqlScript("DELETE FROM interview_fk_college WHERE college_id = 1;");
        DbConnectionHelper.printQueryResults("删除学院 1 后的学生表 (张三、李四被级联删除，仅剩王五)", 
                "SELECT student_id, student_name, college_id FROM interview_fk_student;");

        printAlibabaStandard();
    }

    private static void printAlibabaStandard() {
        System.out.println("---------------- 为什么互联网大厂全面禁止物理外键？ ----------------");
        System.out.println("1. 死锁风险: 插入子表需要对父表对应行加 S (共享锁)，并发高时与父表的 X 锁极易互斥死锁。");
        System.out.println("2. 性能损耗: 单表 TPS 可由数千下跌 30%~50%，所有的插入/更新/删除操作都有隐式联检开销。");
        System.out.println("3. 分布式死穴: 分库分表后，父表和子表分布在不同机器/库中，物理外键完全不支持跨库！");
        System.out.println("4. 现代实践: 使用【逻辑外键】(普通索引) + 应用层代码 Service / Transaction 事务保证一致性。");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

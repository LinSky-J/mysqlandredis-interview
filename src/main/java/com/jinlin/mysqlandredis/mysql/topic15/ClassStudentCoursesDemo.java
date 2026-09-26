package com.jinlin.mysqlandredis.mysql.topic15;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 15: SQL 题：查某个班级下所有学生的选课情况
 * 
 * 核心考点：
 * 1. 为什么必须用 LEFT JOIN？
 *    - 如果使用 INNER JOIN，没有选课的学生会被内连接自动过滤丢失！
 *    - 题目要求“所有学生的选课情况”，必须保证学生主表的完整呈现。
 * 2. 报表输出技巧：
 *    - 形式 A：多行展开明细。
 *    - 形式 B：GROUP_CONCAT 聚合合并选课名称，并用 COUNT(co.course_id) 准确计算选课门数。
 */
public class ClassStudentCoursesDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 15】查指定班级下所有学生选课情况实机演示 (LEFT JOIN 避坑)");
        System.out.println("====================================================================");

        // 1. 测试数据重置 (表结构已通过 DataGrip 执行 sql/01_mysql_basics_schema.sql 创建)
        DbConnectionHelper.executeSqlScript(
                "DELETE FROM interview_student_course;",
                "DELETE FROM interview_course;",
                "DELETE FROM interview_cls_student;",
                "DELETE FROM interview_class;",
                "INSERT INTO interview_class (class_id, class_name) VALUES (1, '高三(1)班'), (2, '高三(2)班');",
                "INSERT INTO interview_cls_student (student_id, student_name, class_id) VALUES "
                        + "(1, '张三', 1), (2, '李四', 1), (3, '王五 (未选任何课程)', 1), (4, '赵六 (2班学生)', 2);",
                "INSERT INTO interview_course (course_id, course_name) VALUES (101, '高等数学'), (102, '大学物理'), (103, '大学英语');",
                "INSERT INTO interview_student_course (student_id, course_id) VALUES (1, 101), (1, 102), (2, 103);"
        );

        // 2. 查询形式 1: 平铺明细表
        String detailSql = "SELECT "
                + "  c.class_name, "
                + "  s.student_id, "
                + "  s.student_name, "
                + "  COALESCE(co.course_name, '【未选任何课程】') AS course_name "
                + "FROM interview_class c "
                + "JOIN interview_cls_student s ON c.class_id = s.class_id "
                + "LEFT JOIN interview_student_course sc ON s.student_id = sc.student_id "
                + "LEFT JOIN interview_course co ON sc.course_id = co.course_id "
                + "WHERE c.class_id = 1 "
                + "ORDER BY s.student_id;";
        DbConnectionHelper.printQueryResults("输出形态 1: 明细列表 (LEFT JOIN 保证未选课学生不丢失)", detailSql);

        // 3. 查询形式 2: 汇总报表 (GROUP_CONCAT 聚合)
        String summarySql = "SELECT "
                + "  c.class_name, "
                + "  s.student_id, "
                + "  s.student_name, "
                + "  COUNT(co.course_id) AS total_courses_selected, "
                + "  COALESCE(GROUP_CONCAT(co.course_name ORDER BY co.course_id SEPARATOR '、'), '【暂无选课】') AS courses_list "
                + "FROM interview_class c "
                + "JOIN interview_cls_student s ON c.class_id = s.class_id "
                + "LEFT JOIN interview_student_course sc ON s.student_id = sc.student_id "
                + "LEFT JOIN interview_course co ON sc.course_id = co.course_id "
                + "WHERE c.class_id = 1 "
                + "GROUP BY c.class_name, s.student_id, s.student_name "
                + "ORDER BY s.student_id;";
        DbConnectionHelper.printQueryResults("输出形态 2: 班级学生选课聚合汇总报表", summarySql);

        System.out.println("【得分要点】COUNT(co.course_id) 绝不能写成 COUNT(*)！因为未选课学生该字段为 NULL，COUNT(*) 会误计为 1 门！");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

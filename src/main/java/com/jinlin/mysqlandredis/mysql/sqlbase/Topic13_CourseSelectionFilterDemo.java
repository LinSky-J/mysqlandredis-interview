package com.jinlin.mysqlandredis.mysql.sqlbase;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 13: SQL题：给学生表、课程成绩表，求不存在01课程但存在02课程的学生的成绩
 * 
 * 核心考点：
 * 1. 存在性与排除性子查询的复合使用。
 * 2. 解法 1 (EXISTS / NOT EXISTS)：走 (student_id, course_id) 联合索引，短路求值，性能最优。
 * 3. 解法 2 (IN / NOT IN)：最符合直觉，但需警惕子查询 NULL 值陷阱。
 * 4. 解法 3 (GROUP BY + HAVING SUM(CASE...))：行转列条件聚合的高级技巧。
 */
public class Topic13_CourseSelectionFilterDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 13】SQL题：求不存在01课程但存在02课程的学生成绩实战");
        System.out.println("====================================================================");

        // 1. 测试数据重置 (表结构已通过 DataGrip 执行 sql/01_mysql_basics_schema.sql 创建)
        DbConnectionHelper.executeSqlScript(
                "DELETE FROM interview_course_score;",
                "DELETE FROM interview_student;",
                "INSERT INTO interview_student (student_id, student_name) VALUES "
                        + "('s01', '张三 (选了01, 02)'), "
                        + "('s02', '李四 (选了02, 03 -> 目标命中)'), "
                        + "('s03', '王五 (选了01, 03)'), "
                        + "('s04', '赵六 (仅选02 -> 目标命中)'), "
                        + "('s05', '孙七 (仅选03)');",
                "INSERT INTO interview_course_score (student_id, course_id, score) VALUES "
                        + "('s01', '01', 88.0), "
                        + "('s01', '02', 90.0), "
                        + "('s02', '02', 85.5), "
                        + "('s02', '03', 92.0), "
                        + "('s03', '01', 76.0), "
                        + "('s03', '03', 81.0), "
                        + "('s04', '02', 95.0), "
                        + "('s05', '03', 89.0);"
        );

        // 2. 解法 1: EXISTS + NOT EXISTS
        String solution1 = "SELECT "
                + "  st.student_id, "
                + "  st.student_name, "
                + "  cs.course_id, "
                + "  cs.score "
                + "FROM interview_student st "
                + "JOIN interview_course_score cs ON st.student_id = cs.student_id "
                + "WHERE EXISTS ("
                + "  SELECT 1 FROM interview_course_score c2 "
                + "  WHERE c2.student_id = st.student_id AND c2.course_id = '02'"
                + ") AND NOT EXISTS ("
                + "  SELECT 1 FROM interview_course_score c1 "
                + "  WHERE c1.student_id = st.student_id AND c1.course_id = '01'"
                + ");";
        DbConnectionHelper.printQueryResults("解法 1: EXISTS + NOT EXISTS 联合索引过滤", solution1);

        // 3. 解法 2: IN + NOT IN
        String solution2 = "SELECT "
                + "  st.student_id, "
                + "  st.student_name, "
                + "  cs.course_id, "
                + "  cs.score "
                + "FROM interview_student st "
                + "JOIN interview_course_score cs ON st.student_id = cs.student_id "
                + "WHERE st.student_id IN ("
                + "  SELECT student_id FROM interview_course_score WHERE course_id = '02'"
                + ") AND st.student_id NOT IN ("
                + "  SELECT student_id FROM interview_course_score WHERE course_id = '01'"
                + ");";
        DbConnectionHelper.printQueryResults("解法 2: IN + NOT IN 子查询过滤", solution2);

        // 4. 解法 3: GROUP BY ... HAVING 聚集筛选目标学生 ID
        String solution3 = "SELECT "
                + "  student_id, "
                + "  SUM(CASE WHEN course_id = '01' THEN 1 ELSE 0 END) AS cnt_01, "
                + "  SUM(CASE WHEN course_id = '02' THEN 1 ELSE 0 END) AS cnt_02 "
                + "FROM interview_course_score "
                + "GROUP BY student_id "
                + "HAVING cnt_01 = 0 AND cnt_02 > 0;";
        DbConnectionHelper.printQueryResults("解法 3: GROUP BY + HAVING 条件聚合命中学生", solution3);

        System.out.println("【结论】三种解法均精准过滤出【李四 (s02)】和【赵六 (s04)】的成绩！");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}


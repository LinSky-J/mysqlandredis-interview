package com.jinlin.mysqlandredis.mysql.topic14;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 14: 给定一个学生表 student_score (stu_id, subject_id, score), 
 *          查询总分排名在 5-10 名的学生 id 及对应的总分
 * 
 * 核心考点：
 * 1. 聚合求和：GROUP BY stu_id + SUM(score)。
 * 2. 传统分页：ORDER BY total_score DESC LIMIT 4, 6 (跳过前 4 名，取 6 条对应 5~10 名)。
 * 3. 窗口函数 (MySQL 8.0+)：DENSE_RANK() OVER (ORDER BY total_score DESC)，优雅支持并列分处理。
 */
public class Topic14_StudentScoreRankingDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 14】学生总分排名 5-10 名查询实战 (LIMIT 偏移法 vs 窗口函数)");
        System.out.println("====================================================================");

        // 1. 测试数据重置 (表结构已通过 DataGrip 执行 sql/01_mysql_basics_schema.sql 创建)
        DbConnectionHelper.executeSqlScript(
                "DELETE FROM student_score;",
                "INSERT INTO student_score (stu_id, subject_id, score) VALUES "
                        + "(101, 1, 98), (101, 2, 99), " // 197 (第1名)
                        + "(102, 1, 95), (102, 2, 96), " // 191 (第2名)
                        + "(103, 1, 92), (103, 2, 94), " // 186 (第3名)
                        + "(104, 1, 90), (104, 2, 91), " // 181 (第4名)
                        + "(105, 1, 88), (105, 2, 89), " // 177 (第5名)
                        + "(106, 1, 85), (106, 2, 86), " // 171 (第6名)
                        + "(107, 1, 82), (107, 2, 83), " // 165 (第7名)
                        + "(108, 1, 80), (108, 2, 80), " // 160 (第8名)
                        + "(109, 1, 78), (109, 2, 77), " // 155 (第9名)
                        + "(110, 1, 75), (110, 2, 74), " // 149 (第10名)
                        + "(111, 1, 70), (111, 2, 70), " // 140 (第11名)
                        + "(112, 1, 60), (112, 2, 65);"  // 125 (第12名)
        );

        // 2. 解法 1: LIMIT 4, 6
        String limitSql = "SELECT "
                + "  stu_id, "
                + "  SUM(score) AS total_score "
                + "FROM student_score "
                + "GROUP BY stu_id "
                + "ORDER BY total_score DESC "
                + "LIMIT 4, 6;";
        DbConnectionHelper.printQueryResults("解法 1: LIMIT 4, 6 分页偏移法 (全版本兼容)", limitSql);

        // 3. 解法 2: 窗口函数 DENSE_RANK()
        String windowSql = "SELECT "
                + "  stu_id, "
                + "  total_score, "
                + "  score_rank "
                + "FROM ("
                + "  SELECT "
                + "    stu_id, "
                + "    SUM(score) AS total_score, "
                + "    DENSE_RANK() OVER (ORDER BY SUM(score) DESC) AS score_rank "
                + "  FROM student_score "
                + "  GROUP BY stu_id"
                + ") rank_table "
                + "WHERE score_rank BETWEEN 5 AND 10;";
        DbConnectionHelper.printQueryResults("解法 2: 窗口函数 DENSE_RANK() (解决并列同分)", windowSql);

        System.out.println("【解析】LIMIT 4, 6 中：4 代表跳过前 4 条记录 (1~4名)，6 代表获取接下来的 6 条记录 (5~10名)！");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

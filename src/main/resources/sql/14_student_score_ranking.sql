-- ====================================================================
-- 问题 14: 给定一个学生表 student_score (stu_id, subject_id, score), 
--          查询总分排名在5-10名的学生id及对应的总分
-- ====================================================================
-- 考点:
-- 1. GROUP BY stu_id 聚合求总分 SUM(score)
-- 2. 解法一 (兼容所有版本): ORDER BY total_score DESC LIMIT 4, 6 (偏移4条，取6条即 5~10名)
-- 3. 解法二 (MySQL 8.0+ 窗口函数): DENSE_RANK() / ROW_NUMBER() OVER (ORDER BY total_score DESC)
-- ====================================================================

DROP TABLE IF EXISTS student_score;
CREATE TABLE student_score (
    stu_id INT NOT NULL,
    subject_id INT NOT NULL,
    score DECIMAL(5, 2) NOT NULL,
    PRIMARY KEY (stu_id, subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入 12 位学生的各科成绩 (模拟总分从高到低梯度排列)
INSERT INTO student_score (stu_id, subject_id, score) VALUES
(101, 1, 98), (101, 2, 99), -- 总分 197 (第1名)
(102, 1, 95), (102, 2, 96), -- 总分 191 (第2名)
(103, 1, 92), (103, 2, 94), -- 总分 186 (第3名)
(104, 1, 90), (104, 2, 91), -- 总分 181 (第4名)
(105, 1, 88), (105, 2, 89), -- 总分 177 (第5名 - 命中)
(106, 1, 85), (106, 2, 86), -- 总分 171 (第6名 - 命中)
(107, 1, 82), (107, 2, 83), -- 总分 165 (第7名 - 命中)
(108, 1, 80), (108, 2, 80), -- 总分 160 (第8名 - 命中)
(109, 1, 78), (109, 2, 77), -- 总分 155 (第9名 - 命中)
(110, 1, 75), (110, 2, 74), -- 总分 149 (第10名 - 命中)
(111, 1, 70), (111, 2, 70), -- 总分 140 (第11名)
(112, 1, 60), (112, 2, 65); -- 总分 125 (第12名)

-- --------------------------------------------------------------------
-- 解法 1: LIMIT 分页法 (LIMIT 4, 6)
-- 解释: LIMIT offset, count
--       offset=4 (跳过前4名: 1, 2, 3, 4)，count=6 (获取接下来的6位: 5, 6, 7, 8, 9, 10)
-- --------------------------------------------------------------------
SELECT 
    stu_id,
    SUM(score) AS total_score
FROM student_score
GROUP BY stu_id
ORDER BY total_score DESC
LIMIT 4, 6;

-- --------------------------------------------------------------------
-- 解法 2: 窗口函数 DENSE_RANK() (优雅支持并列同分排名)
-- --------------------------------------------------------------------
SELECT 
    stu_id,
    total_score,
    score_rank
FROM (
    SELECT 
        stu_id,
        SUM(score) AS total_score,
        DENSE_RANK() OVER (ORDER BY SUM(score) DESC) AS score_rank
    FROM student_score
    GROUP BY stu_id
) rank_table
WHERE score_rank BETWEEN 5 AND 10;

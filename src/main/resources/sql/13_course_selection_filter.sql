-- ====================================================================
-- 问题 13: SQL 题：给学生表、课程成绩表，求不存在01课程但存在02课程的学生的成绩
-- ====================================================================
-- 表结构:
--   student (student_id, student_name)
--   course_score (student_id, course_id, score)
-- 
-- 业务逻辑:
--   1. 必须选了 02 号课程
--   2. 绝对不能选 01 号课程
--   3. 输出满足条件学生的所有成绩或详细信息
-- ====================================================================

DROP TABLE IF EXISTS interview_course_score;
DROP TABLE IF EXISTS interview_student;

CREATE TABLE interview_student (
    student_id VARCHAR(20) PRIMARY KEY,
    student_name VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_course_score (
    student_id VARCHAR(20) NOT NULL,
    course_id VARCHAR(20) NOT NULL,
    score DECIMAL(5, 2) NOT NULL,
    PRIMARY KEY (student_id, course_id),
    INDEX idx_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入测试数据:
INSERT INTO interview_student (student_id, student_name) VALUES
('s01', '张三 (选了01, 02)'),
('s02', '李四 (选了02, 03 -> 命中目标)'),
('s03', '王五 (选了01, 03)'),
('s04', '赵六 (仅选02 -> 命中目标)'),
('s05', '孙七 (仅选03)');

INSERT INTO interview_course_score (student_id, course_id, score) VALUES
('s01', '01', 88.0),
('s01', '02', 90.0),
('s02', '02', 85.5),
('s02', '03', 92.0),
('s03', '01', 76.0),
('s03', '03', 81.0),
('s04', '02', 95.0),
('s05', '03', 89.0);

-- --------------------------------------------------------------------
-- 解法 1: 使用 EXISTS 和 NOT EXISTS (性能最佳，走索引且对 NULL 安全)
-- --------------------------------------------------------------------
SELECT 
    st.student_id,
    st.student_name,
    cs.course_id,
    cs.score
FROM interview_student st
JOIN interview_course_score cs ON st.student_id = cs.student_id
WHERE EXISTS (
    -- 必须存在 02 课程
    SELECT 1 FROM interview_course_score c2 
    WHERE c2.student_id = st.student_id AND c2.course_id = '02'
)
AND NOT EXISTS (
    -- 绝对不能存在 01 课程
    SELECT 1 FROM interview_course_score c1 
    WHERE c1.student_id = st.student_id AND c1.course_id = '01'
);

-- --------------------------------------------------------------------
-- 解法 2: 使用子查询 IN 和 NOT IN
-- --------------------------------------------------------------------
SELECT 
    st.student_id,
    st.student_name,
    cs.course_id,
    cs.score
FROM interview_student st
JOIN interview_course_score cs ON st.student_id = cs.student_id
WHERE st.student_id IN (
    SELECT student_id FROM interview_course_score WHERE course_id = '02'
)
AND st.student_id NOT IN (
    SELECT student_id FROM interview_course_score WHERE course_id = '01'
);

-- --------------------------------------------------------------------
-- 解法 3: 使用 GROUP BY ... HAVING 条件聚合 (求出目标学生 ID 集合)
-- --------------------------------------------------------------------
SELECT 
    student_id
FROM interview_course_score
GROUP BY student_id
HAVING 
    SUM(CASE WHEN course_id = '01' THEN 1 ELSE 0 END) = 0
    AND SUM(CASE WHEN course_id = '02' THEN 1 ELSE 0 END) > 0;

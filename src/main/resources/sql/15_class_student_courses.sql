-- ====================================================================
-- 问题 15: SQL 题：查某个班级下所有学生的选课情况
-- ====================================================================
-- 考点核心:
-- 1. 班级下【所有】学生: 必须使用 LEFT JOIN 关联选课表，确保【未选任何课程的学生】不被漏掉！
-- 2. 两种报表输出形态:
--    - 形态 A (明细平铺): 每个学生每门课一行。
--    - 形态 B (聚合汇编): 使用 GROUP_CONCAT 将学生所选课程合并为字符串，展现选课清单。
-- ====================================================================

DROP TABLE IF EXISTS interview_student_course;
DROP TABLE IF EXISTS interview_course;
DROP TABLE IF EXISTS interview_cls_student;
DROP TABLE IF EXISTS interview_class;

CREATE TABLE interview_class (
    class_id INT PRIMARY KEY AUTO_INCREMENT,
    class_name VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_cls_student (
    student_id INT PRIMARY KEY AUTO_INCREMENT,
    student_name VARCHAR(50) NOT NULL,
    class_id INT NOT NULL,
    INDEX idx_class (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_course (
    course_id INT PRIMARY KEY AUTO_INCREMENT,
    course_name VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_student_course (
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    PRIMARY KEY (student_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入测试数据:
-- 班级
INSERT INTO interview_class (class_id, class_name) VALUES 
(1, '高三(1)班'), 
(2, '高三(2)班');

-- 学生 (注意: 3 号学生 王五 在 1 班，但未选任何课程！)
INSERT INTO interview_cls_student (student_id, student_name, class_id) VALUES 
(1, '张三', 1),
(2, '李四', 1),
(3, '王五 (未选任何课程)', 1),
(4, '赵六 (2班学生)', 2);

-- 课程
INSERT INTO interview_course (course_id, course_name) VALUES 
(101, '高等数学'), 
(102, '大学物理'), 
(103, '大学英语');

-- 选课记录
INSERT INTO interview_student_course (student_id, course_id) VALUES 
(1, 101), (1, 102), -- 张三选了高数、物理
(2, 103);          -- 李四选了英语

-- --------------------------------------------------------------------
-- 查询形态 1: 明细查询 (查询 1 班所有学生的选课明细，未选课显示 NULL)
-- --------------------------------------------------------------------
SELECT 
    c.class_name,
    s.student_id,
    s.student_name,
    COALESCE(co.course_name, '【未选任何课程】') AS course_name
FROM interview_class c
JOIN interview_cls_student s ON c.class_id = s.class_id
LEFT JOIN interview_student_course sc ON s.student_id = sc.student_id
LEFT JOIN interview_course co ON sc.course_id = co.course_id
WHERE c.class_id = 1
ORDER BY s.student_id;

-- --------------------------------------------------------------------
-- 查询形态 2: 汇总报表 (GROUP_CONCAT 聚合学生选课清单与总门数)
-- --------------------------------------------------------------------
SELECT 
    c.class_name,
    s.student_id,
    s.student_name,
    COUNT(co.course_id) AS total_courses_selected,
    COALESCE(GROUP_CONCAT(co.course_name ORDER BY co.course_id SEPARATOR '、'), '【暂无选课】') AS courses_list
FROM interview_class c
JOIN interview_cls_student s ON c.class_id = s.class_id
LEFT JOIN interview_student_course sc ON s.student_id = sc.student_id
LEFT JOIN interview_course co ON sc.course_id = co.course_id
WHERE c.class_id = 1
GROUP BY c.class_name, s.student_id, s.student_name
ORDER BY s.student_id;

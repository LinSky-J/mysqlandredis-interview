-- ====================================================================
-- 问题 09: 说一下外键约束
-- ====================================================================
-- 1. 作用: 保证关系数据库中多表之间的【参照完整性 (Referential Integrity)】。
-- 2. 级联操作类型:
--    - CASCADE:    主表删除/修改，子表跟着自动删除/修改。
--    - SET NULL:   主表删除/修改，子表关联字段设为 NULL。
--    - RESTRICT / NO ACTION (默认): 子表有引用时，严格阻止主表修改/删除。
-- 3. 阿里巴巴 Java 开发手册明文规约:
--    【强制】不得使用外键与级联，一切外键概念必须在应用层解决。
--    原因: 性能开销、隐式加锁与死锁、分布式分库分表无法跨实例、数据迁移阻碍。
-- ====================================================================

-- 准备演示表: 学院表 (主表) 与 学生表 (子表)
DROP TABLE IF EXISTS interview_fk_student;
DROP TABLE IF EXISTS interview_fk_college;

CREATE TABLE interview_fk_college (
    college_id INT PRIMARY KEY AUTO_INCREMENT,
    college_name VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE interview_fk_student (
    student_id INT PRIMARY KEY AUTO_INCREMENT,
    student_name VARCHAR(50) NOT NULL,
    college_id INT NOT NULL,
    -- 声明物理外键约束与级联删除 (CASCADE)
    CONSTRAINT fk_student_college FOREIGN KEY (college_id) 
        REFERENCES interview_fk_college (college_id) 
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入基准数据
INSERT INTO interview_fk_college (college_id, college_name) VALUES 
(1, '计算机学院'), 
(2, '软件学院');

INSERT INTO interview_fk_student (student_name, college_id) VALUES 
('张三', 1), 
('李四', 1), 
('王五', 2);

-- 查询当前数据
SELECT * FROM interview_fk_student;

-- 演示 1: 物理约束拦截非法插入 (插入一个不存在的学院 ID=99) -> 将直接报错拦截！
-- INSERT INTO interview_fk_student (student_name, college_id) VALUES ('非法学生', 99);

-- 演示 2: 级联删除 (CASCADE) 的威力与危险性
-- 删除计算机学院 (id=1)，其下属的学生 张三、李四 会被隐式自动物理删除！
DELETE FROM interview_fk_college WHERE college_id = 1;

-- 验证学生表: 张三、李四 已被级联抹除，仅剩软件学院的王五
SELECT * FROM interview_fk_student;

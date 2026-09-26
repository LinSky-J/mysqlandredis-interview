-- ====================================================================
-- 问题 06: int(1) 和 int(10) 在 MySQL 中有什么不同？
-- ====================================================================
-- 结论总结:
-- 1. 存储空间相同: 无论括号里写几，都是 4 字节 (32 位)。
-- 2. 取值范围相同: -2147483648 ~ 2147483647 (有符号) 或 0 ~ 4294967295 (无符号)。
-- 3. 作用仅仅是【显示宽度 (Display Width)】，只有配合 ZEROFILL (零填充) 时才可见区别。
-- 4. MySQL 8.0.17 开始已正式废弃该显示宽度语法。
-- ====================================================================

DROP TABLE IF EXISTS interview_int_display;
CREATE TABLE interview_int_display (
    id INT PRIMARY KEY AUTO_INCREMENT,
    col_int1 INT(1) COMMENT 'int(1)',
    col_int10 INT(10) COMMENT 'int(10)',
    col_zerofill_5 INT(5) ZEROFILL COMMENT '带 ZEROFILL 宽度的 5 位'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 验证 1: 存入 7 位大数字 1234567，证明 int(1) 绝不是只能存 1 位数字！
INSERT INTO interview_int_display (col_int1, col_int10, col_zerofill_5) 
VALUES (1234567, 1234567, 12);

-- 验证 2: 存入 1 位小数字 9
INSERT INTO interview_int_display (col_int1, col_int10, col_zerofill_5) 
VALUES (9, 9, 9);

-- 查询结果查看
SELECT 
    id,
    col_int1,
    col_int10,
    col_zerofill_5
FROM interview_int_display;

-- 观察点：
-- 1) col_int1 即使设置 (1)，也能完整保存 1234567；
-- 2) col_zerofill_5 存 12 时展示为 00012 (前导补零凑齐 5 位)；存 9 时展示为 00009；
-- 3) 如果没有 ZEROFILL，int(1) 与 int(10) 显示与存储无任何区别。

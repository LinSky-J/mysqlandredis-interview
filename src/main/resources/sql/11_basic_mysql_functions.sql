-- ====================================================================
-- 问题 11: MySQL 中的一些基本函数，你知道哪些？
-- ====================================================================
-- 涵盖分类:
-- 1. 字符串函数 (CONCAT, CONCAT_WS, SUBSTRING, CHAR_LENGTH, TRIM, REPLACE)
-- 2. 数值函数 (ROUND, CEIL, FLOOR, ABS, MOD)
-- 3. 日期时间函数 (NOW, DATE_ADD, DATEDIFF, TIMESTAMPDIFF, DATE_FORMAT)
-- 4. 控制流与判空函数 (IF, IFNULL, COALESCE, CASE WHEN)
-- 5. 聚合与字符串合并 (COUNT, SUM, AVG, GROUP_CONCAT)
-- 6. 窗口函数 (ROW_NUMBER, DENSE_RANK)
-- ====================================================================

-- 1. 字符串函数演练
SELECT 
    CONCAT('MySQL', '-', '8.0') AS str_concat,
    CONCAT_WS(';', 'Java', 'Python', 'Go') AS str_concat_ws,
    SUBSTRING('Hello World', 1, 5) AS str_sub,
    CHAR_LENGTH('中国人') AS char_len,
    LENGTH('中国人') AS byte_len_utf8mb4,
    REPLACE('abc_test', 'test', 'demo') AS str_replace;

-- 2. 数值函数演练
SELECT 
    ROUND(3.1415926, 2) AS round_val,
    CEIL(4.1) AS ceil_val,
    FLOOR(4.9) AS floor_val,
    ABS(-100) AS abs_val,
    MOD(10, 3) AS mod_val;

-- 3. 日期时间函数演练
SELECT 
    NOW() AS current_datetime,
    DATE_FORMAT(NOW(), '%Y年%m月%d日 %H时%i分%s秒') AS formatted_date,
    DATE_ADD(NOW(), INTERVAL 7 DAY) AS plus_7_days,
    DATEDIFF(CURRENT_DATE, '2026-01-01') AS diff_days_since_new_year,
    TIMESTAMPDIFF(HOUR, '2026-09-25 08:00:00', '2026-09-26 12:00:00') AS diff_hours;

-- 4. 控制流与判空函数演练
SELECT 
    IF(10 > 5, '大于', '小于等于') AS if_result,
    IFNULL(NULL, '默认值') AS ifnull_result,
    COALESCE(NULL, NULL, '第一非空值', '备用') AS coalesce_result,
    CASE 
        WHEN 85 >= 90 THEN '优秀'
        WHEN 85 >= 80 THEN '良好'
        ELSE '一般'
    END AS case_result;

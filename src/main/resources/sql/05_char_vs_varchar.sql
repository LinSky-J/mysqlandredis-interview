-- ====================================================================
-- 问题 05: CHAR 和 VARCHAR 有什么区别？varchar 后面代表字节还是字符？
-- ====================================================================
-- 1. 核心区别:
--    - CHAR(M): 定长字符串 (0~255)。不足 M 字符时在右侧用空格填充，检索时默认剥离尾随空格。
--    - VARCHAR(M): 变长字符串 (0~65535 字节限制)。按实际字符长度存储，额外用 1~2 字节存储长度前缀。
-- 2. varchar(M) 后面是字符还是字节？
--    - 明确是【字符数 (Characters)】！
--    - 例如 VARCHAR(5) 可以存 5 个英文字母 'abcde'，也可以存 5 个汉字 '你好世界呀'！
-- ====================================================================

DROP TABLE IF EXISTS interview_char_varchar;
CREATE TABLE interview_char_varchar (
    id INT PRIMARY KEY AUTO_INCREMENT,
    c_fixed CHAR(10) COMMENT '定长 CHAR(10)',
    v_variable VARCHAR(10) COMMENT '变长 VARCHAR(10)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入包含尾随空格的数据
INSERT INTO interview_char_varchar (c_fixed, v_variable) VALUES 
('test  ', 'test  '),
('中国加油', '中国加油');

-- 验证存储机制:
-- 1. CHAR_LENGTH: 字符个数
-- 2. LENGTH: 实际占用的字节数 (utf8mb4 下每个汉字占用 3 字节)
SELECT 
    id,
    c_fixed,
    CHAR_LENGTH(c_fixed) AS char_len_char,
    LENGTH(c_fixed) AS byte_len_char,
    v_variable,
    CHAR_LENGTH(v_variable) AS char_len_varchar,
    LENGTH(v_variable) AS byte_len_varchar
FROM interview_char_varchar;

-- 观察点：
-- 1) 对于 'test  '：CHAR(10) 查出来的字符长度是 4 (尾随空格被截断)，而 VARCHAR(10) 是 6 (保留了空格)。
-- 2) 对于 '中国加油'：4个汉字，CHAR_LENGTH 均为 4，但在 utf8mb4 下占用了 12 个字节！证明 VARCHAR(10) 指的是 10 个字符！

-- ====================================================================
-- 问题 07: Text 数据类型可以无限大吗？
-- ====================================================================
-- 结论: 绝对不能无限大！
-- 1. 4 种 TEXT 类型的存储上限:
--    - TINYTEXT:   255 字节 (2^8 - 1 ≈ 0.25 KB)
--    - TEXT:       65,535 字节 (2^16 - 1 ≈ 64 KB)
--    - MEDIUMTEXT: 16,777,215 字节 (2^24 - 1 ≈ 16 MB)
--    - LONGTEXT:   4,294,967,295 字节 (2^32 - 1 ≈ 4 GB)
-- 2. 外部硬性制约因素:
--    - 参数限制: max_allowed_packet (单次通信最大数据包，默认 16MB 或 64MB)
--    - 性能开销: InnoDB 默认页 16KB，大文本触发溢出页 (Overflow Page)，且 GROUP BY / ORDER BY 会退化到磁盘临时表
-- ====================================================================

DROP TABLE IF EXISTS interview_text_storage;
CREATE TABLE interview_text_storage (
    id INT PRIMARY KEY AUTO_INCREMENT,
    short_notes TINYTEXT COMMENT '上限 255 字节',
    content TEXT COMMENT '上限 64 KB',
    article MEDIUMTEXT COMMENT '上限 16 MB',
    large_payload LONGTEXT COMMENT '上限 4 GB (理论值)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入演示数据
INSERT INTO interview_text_storage (short_notes, content, article, large_payload) VALUES
('简短备注说明', 
 REPEAT('这是TEXT字段内容填充。', 20),
 REPEAT('这是MEDIUMTEXT长文章内容填充。', 100),
 REPEAT('LONGTEXT理论最大4GB，受限于max_allowed_packet。', 500)
);

-- 查询当前 MySQL 实例的 max_allowed_packet 配置
SHOW VARIABLES LIKE 'max_allowed_packet';

-- 查询插入文本的长度
SELECT 
    id,
    CHAR_LENGTH(short_notes) AS len_tiny,
    CHAR_LENGTH(content) AS len_text,
    CHAR_LENGTH(article) AS len_medium,
    CHAR_LENGTH(large_payload) AS len_long
FROM interview_text_storage;

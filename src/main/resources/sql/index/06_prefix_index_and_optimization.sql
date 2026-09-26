-- ====================================================================
-- 问题 06: 索引设计与优化法则、低基数状态字段、前缀索引深度实战
-- ====================================================================
-- 1. 索引是不是建的越多越好？
--    - 绝不是！单表建议不超过 5 个索引。
--    - 坏处: 写操作雪崩 (写放大，增删改需同步维护多棵 B+ 树)、挤占 Buffer Pool、增加优化器解析成本。
-- 
-- 2. 状态值 0 或 1 适合建索引吗？
--    - 通常不适合: 基数极低，数据平分时优化器直接放弃走索引；
--    - 特例: 极端数据倾斜 (如 1000 万行中仅 10 行处于待处理 status=1 状态)，或作为联合索引辅助列。
-- 
-- 3. 什么是前缀索引？
--    - 对长字符串 (如 VARCHAR(255)) 仅截取前 N 个字符建索引: INDEX idx_email_pref (email(8))
--    - 优点: 节约大量存储与内存，保持 B+ 树矮胖与高扇出。
--    - 缺点: 无法使用覆盖索引 (必须回表)，无法用于 ORDER BY 排序！
-- 
-- 4. 前缀索引长度决策公式:
--    计算区分度: COUNT(DISTINCT LEFT(col, N)) / COUNT(*) 越接近完整列区分度越好。
-- ====================================================================

DROP TABLE IF EXISTS interview_prefix_demo;
CREATE TABLE interview_prefix_demo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(100) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0正常 1禁用',
    INDEX idx_email_prefix (email(7)) -- 创建截取前 7 个字符的前缀索引
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入测试数据
INSERT INTO interview_prefix_demo (email, status) VALUES
('zhangsan_dev@gmail.com', 0),
('lisi_developer@163.com', 0),
('wangwu_arch@qq.com', 1),
('zhaoliu_test@hotmail.com', 0);

-- 1. 验证前缀索引的区分度计算方法
SELECT 
    COUNT(DISTINCT LEFT(email, 5)) / COUNT(*) AS sel_len_5,
    COUNT(DISTINCT LEFT(email, 7)) / COUNT(*) AS sel_len_7,
    COUNT(DISTINCT email) / COUNT(*) AS sel_full
FROM interview_prefix_demo;

-- 2. 查看前缀索引的执行计划 (type=ref, key_len 计算仅包含前 7 个字符)
EXPLAIN SELECT * FROM interview_prefix_demo WHERE email = 'zhangsan_dev@gmail.com';

-- 3. 验证前缀索引无法实现覆盖索引:
-- 即便只查 email 字段，Extra 也无法显示 Using index，因为索引内只有前缀，必须回表验证完整字段！
EXPLAIN SELECT email FROM interview_prefix_demo WHERE email = 'zhangsan_dev@gmail.com';

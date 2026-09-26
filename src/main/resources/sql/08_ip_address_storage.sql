-- ====================================================================
-- 问题 08: IP 地址如何在数据库里存储？
-- ====================================================================
-- 两种主流方案对比:
-- 1. VARCHAR(15): 可读性好，但空间大、字符排序错乱 (如 '10.0.0.9' > '10.0.0.10' 导致范围查询失效)。
-- 2. INT UNSIGNED (强烈推荐): 仅 4 字节，支持 B+ 树高速范围与网段查询。
--    - INET_ATON('192.168.1.1'): 字符串转为 32 位整型
--    - INET_NTOA(3232235777):    整型还原为字符串
-- 3. IPv6 方案: VARBINARY(16) + INET6_ATON() / INET6_NTOA()
-- ====================================================================

DROP TABLE IF EXISTS interview_ip_storage;
CREATE TABLE interview_ip_storage (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_name VARCHAR(50) NOT NULL,
    ip_varchar VARCHAR(15) NOT NULL COMMENT '传统字符串存储',
    ip_int INT UNSIGNED NOT NULL COMMENT '推荐: 4字节无符号整型',
    INDEX idx_ip_int (ip_int)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入测试数据 (利用 INET_ATON 自动转换)
INSERT INTO interview_ip_storage (user_name, ip_varchar, ip_int) VALUES
('用户A', '192.168.1.5',   INET_ATON('192.168.1.5')),
('用户B', '192.168.1.10',  INET_ATON('192.168.1.10')),
('用户C', '192.168.1.100', INET_ATON('192.168.1.100')),
('用户D', '10.0.0.1',      INET_ATON('10.0.0.1'));

-- 查询并还原 IP 地址
SELECT 
    id,
    user_name,
    ip_varchar,
    ip_int,
    INET_NTOA(ip_int) AS ip_restored
FROM interview_ip_storage;

-- 重点展示: IP 范围查询 (例如查询 192.168.1.1 ~ 192.168.1.20 之间的 IP)
-- 字符串比较出现逻辑错误，而数值比较完全正确！
SELECT 
    id,
    user_name,
    INET_NTOA(ip_int) AS ip_address,
    ip_int
FROM interview_ip_storage
WHERE ip_int BETWEEN INET_ATON('192.168.1.1') AND INET_ATON('192.168.1.20');

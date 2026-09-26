-- ====================================================================
-- 问题 04: MySQL 如何避免重复插入数据？
-- ====================================================================
-- 常见 5 种解决方案与原理对比：
-- 1. INSERT IGNORE INTO
-- 2. REPLACE INTO
-- 3. INSERT INTO ... ON DUPLICATE KEY UPDATE
-- 4. INSERT INTO ... SELECT ... WHERE NOT EXISTS
-- 5. 唯一索引 (UNIQUE KEY) 兜底 + 应用层分布式锁
-- ====================================================================

-- 准备演示表: 用户账户表 (以 phone 或 username 为唯一键)
DROP TABLE IF EXISTS interview_duplicate_user;
CREATE TABLE interview_duplicate_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(20) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    login_count INT NOT NULL DEFAULT 1,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入基准数据
INSERT INTO interview_duplicate_user (phone, nickname, login_count) VALUES ('13900001111', '张三', 1);

-- --------------------------------------------------------------------
-- 方案 1: INSERT IGNORE INTO
-- 原理: 遇到唯一键冲突时直接忽略报错，不插入，受影响行数 0
-- --------------------------------------------------------------------
INSERT IGNORE INTO interview_duplicate_user (phone, nickname, login_count) 
VALUES ('13900001111', '张三-新名字', 10);

-- --------------------------------------------------------------------
-- 方案 2: REPLACE INTO
-- 原理: 遇到唯一键冲突时，先 DELETE 旧记录，再 INSERT 新记录
-- 注意: 自增 ID 会递增改变！若有外键级联可能误删子表数据！
-- --------------------------------------------------------------------
REPLACE INTO interview_duplicate_user (phone, nickname, login_count) 
VALUES ('13900001111', '张三-已被Replace', 2);

-- --------------------------------------------------------------------
-- 方案 3: INSERT INTO ... ON DUPLICATE KEY UPDATE (生产环境最推荐)
-- 原理: 存在冲突时转为 UPDATE 指定字段，保留原主键 ID 不变
-- --------------------------------------------------------------------
INSERT INTO interview_duplicate_user (phone, nickname, login_count) 
VALUES ('13900001111', '张三-最新更新', 1)
ON DUPLICATE KEY UPDATE 
    nickname = VALUES(nickname),
    login_count = login_count + 1;

-- --------------------------------------------------------------------
-- 方案 4: INSERT INTO ... SELECT ... WHERE NOT EXISTS
-- 原理: 显式判断不存在才插入
-- --------------------------------------------------------------------
INSERT INTO interview_duplicate_user (phone, nickname, login_count)
SELECT '13900002222', '李四', 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM interview_duplicate_user WHERE phone = '13900002222'
);

-- 查看最终表中的数据状态
SELECT id, phone, nickname, login_count, update_time FROM interview_duplicate_user;

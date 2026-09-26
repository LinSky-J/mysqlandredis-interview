-- ====================================================================
-- 问题 03: B+ 树底层实现、数据页内部查找、跳表对比与插入物理演变
-- ====================================================================
-- 核心考点深度解析:
-- 1. B+ 树核心特性:
--    - 非叶子节点仅存键值 (Key) 与子节点指针，不存数据行 -> 保证极高扇出 (Fan-out > 1000)，树高仅 2~4 层；
--    - 所有行数据全部存放在叶子节点；
--    - 叶子节点通过【双向链表】互联，完美支持正反向范围扫描与排序。
-- 
-- 2. 为什么不用跳表 (SkipList)？
--    - 跳表是内存友好结构 (Redis ZSet 采用)，指针随机分散；
--    - 磁盘以 16KB 页为 I/O 单位，B+ 树一页存上千个索引项，3次 I/O 覆盖两千万数据；
--    - 若用跳表持久化到磁盘，层数过深且大量随机跨页 I/O，磁盘吞吐暴跌！
-- 
-- 3. 到了 B+ 树叶子节点 (16KB 数据页)，内部如何查找？
--    - 第一步: 在数据页底部的【页目录 (Page Directory)】中使用【二分查找法】定位到对应的槽 (Slot)；
--    - 第二步: 在槽内对应的 4~8 条记录中，沿单向链表 (next_record) 顺序遍历，纳秒级锁定目标行！
-- 
-- 4. 插入新数据时，索引有哪些变化？
--    - 正常插入: 写入 User Records，更新 Slot 与单向链表；
--    - 页满分裂: 触发【页分裂 (Page Split)】，新开 16KB 页迁移约 50% 数据，维护父节点与双向指针；
--    - Change Buffer 机制: 非唯一二级索引未在内存时暂存 Change Buffer，异步 Merge 避免随机磁盘 I/O。
-- ====================================================================

DROP TABLE IF EXISTS interview_bplus_tree;
CREATE TABLE interview_bplus_tree (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_name VARCHAR(50) NOT NULL,
    age INT NOT NULL,
    INDEX idx_age (age)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入多条有序数据，观察数据页与双向链表范围扫描
INSERT INTO interview_bplus_tree (user_name, age) VALUES
('User_01', 18),
('User_02', 20),
('User_03', 22),
('User_04', 25),
('User_05', 28),
('User_06', 30);

-- 演示 B+ 树叶子节点双向链表的优势: 高效支持正向范围扫描与逆向 ORDER BY 排序
-- 1. 正向范围扫描
EXPLAIN SELECT * FROM interview_bplus_tree WHERE age BETWEEN 20 AND 28;

-- 2. 逆向降序排列 (利用叶子节点前驱 prev 指针反向遍历，无需额外内存排序 Filesort)
EXPLAIN SELECT * FROM interview_bplus_tree WHERE age >= 20 ORDER BY age DESC;

-- 查看 InnoDB 默认数据页大小 (16384 字节 = 16KB)
SHOW VARIABLES LIKE 'innodb_page_size';

-- 查看 Change Buffer 配置 (用于缓冲非唯一二级索引写操作)
SHOW VARIABLES LIKE 'innodb_change_buffering';

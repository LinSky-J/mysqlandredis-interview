-- ====================================================================
-- 问题 05: 数据管理里，数据文件大体分成哪几种数据文件？
-- ====================================================================
-- MySQL / InnoDB 物理体系下的 5 大数据文件分类与核心职责:
-- 
-- 1. 表空间与数据文件 (Tablespaces & Data Files):
--    - 单表独享表空间 (.ibd): 存放数据行、B+ 树聚簇索引、二级索引、Change Buffer。
--    - 系统表空间 (ibdata1): 存放数据字典、Doublewrite Buffer 双写缓冲等。
--    - 临时表空间 (ibtmp1): 复杂查询、排序和哈希关联所占用的磁盘临时空间。
--    - MyISAM 文件: .MYD (数据文件) 与 .MYI (索引文件)。
-- 
-- 2. 事务与崩溃恢复日志 (Transaction Logs):
--    - Redo Log (ib_logfile0, ib_logfile1 / #innodb_redo/): 循环物理写，WAL 保证持久性与 Crash-Safe。
--    - Undo Log (undo_001, undo_002): 逻辑撤销日志，保障原子性与 MVCC 版本链。
-- 
-- 3. 服务层与复制日志 (Server & Replication Logs):
--    - Binlog (二进制日志): 追加逻辑写，全量记录 DDL/DML，用于主从复制与时间点恢复 (PITR)。
--    - Relay Log (中继日志): 从库拉取主库 Binlog 后的中继缓冲。
--    - Slow Query Log (慢查询日志): 性能调优分析。
--    - Error Log (错误日志): 运行与启动崩溃诊断。
-- 
-- 4. 元数据字典文件 (Metadata Files):
--    - 8.0 之前为 .frm 文件，8.0+ 统一集成进事务型数据字典 mysql.ibd 与嵌入式 .sdi。
-- 
-- 5. 进程控制与配置文件:
--    - my.ini/my.cnf 配置文件、hostname.pid 进程文件、mysql.sock 套接字。
-- ====================================================================

-- 1. 查询当前 MySQL 实例的数据目录路径 (datadir)
SHOW VARIABLES LIKE 'datadir';

-- 2. 查询是否启用了单表独立表空间 (innodb_file_per_table)
-- 开启后，每张表拥有一个独立的 .ibd 文件，便于 DROP TABLE 立即回收操作系统磁盘空间！
SHOW VARIABLES LIKE 'innodb_file_per_table';

-- 3. 查询重做日志 (Redo Log) 相关关键文件与配置
SHOW VARIABLES LIKE 'innodb_log_group_home_dir';
SHOW VARIABLES LIKE 'innodb_log_file_size';
SHOW VARIABLES LIKE 'innodb_log_files_in_group';

-- 4. 查询撤销日志 (Undo Log) 表空间配置
SHOW VARIABLES LIKE 'innodb_undo_tablespaces';
SHOW VARIABLES LIKE 'innodb_undo_directory';

-- 5. 查询二进制日志 (Binlog) 启用状态与日志名称
SHOW VARIABLES LIKE 'log_bin';
SHOW VARIABLES LIKE 'log_bin_basename';

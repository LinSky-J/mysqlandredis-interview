package com.jinlin.mysqlandredis.mysql.storage;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 05: 数据管理里，数据文件大体分成哪几种数据文件？
 * 
 * 核心考点与物理文件体系划分：
 * 
 * 1. 表空间与数据文件 (Tablespace & Data Files):
 *    - .ibd (独立表空间文件): 包含用户数据行、聚簇索引、二级索引、Change Buffer 等。
 *    - ibdata1 (系统表空间文件): 数据字典元数据、双写缓冲区 (Doublewrite Buffer)。
 *    - ibtmp1 (临时表空间): 存放排序、哈希连接与临时表数据。
 *    - .MYD / .MYI: MyISAM 引擎的数据文件与索引文件。
 * 
 * 2. 事务与崩溃恢复日志 (Transaction Logs):
 *    - Redo Log (ib_logfile0, ib_logfile1 / #innodb_redo/): 物理重做日志，环形循环写入，保障 WAL 持久性与 Crash-Safe。
 *    - Undo Log (undo_001, undo_002): 逻辑撤销日志，保障原子性与 MVCC 多版本链。
 * 
 * 3. 服务层与复制管理日志 (Server Logs):
 *    - Binlog (二进制日志): 逻辑追加写，记录全量修改 SQL，用于主从复制与时间点数据恢复 (PITR)。
 *    - Relay Log (中继日志): 从库用于重放主库 Binlog。
 *    - Slow Query Log (慢日志): 记录慢查询。
 *    - Error Log (错误日志): 运行与异常诊断。
 * 
 * 4. 元数据模式文件 (Metadata Files):
 *    - 8.0 之前为 .frm 表结构定义文件；8.0+ 集成为统一事务型数据字典 (mysql.ibd) 与内嵌 .sdi。
 * 
 * 5. 进程与控制配置文件:
 *    - my.ini / my.cnf (配置文件)、hostname.pid (进程ID文件)、mysql.sock (套接字文件)。
 */
public class DatabaseFilesArchitectureDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 05】数据管理体系核心文件分类与真实系统路径实机巡检");
        System.out.println("====================================================================");

        // 1. 查询真实 MySQL 数据存放物理目录
        DbConnectionHelper.printQueryResults("1. 数据存储根目录 (datadir)", "SHOW VARIABLES LIKE 'datadir';");

        // 2. 查询单表独立表空间策略 (innodb_file_per_table)
        DbConnectionHelper.printQueryResults("2. 独立表空间配置 (.ibd 单表存储策略)", "SHOW VARIABLES LIKE 'innodb_file_per_table';");

        // 3. 查询重做日志 (Redo Log) 相关配置
        DbConnectionHelper.printQueryResults("3. 重做日志 (Redo Log) 大小与组配置", 
                "SHOW VARIABLES WHERE Variable_name IN ('innodb_log_file_size', 'innodb_log_files_in_group', 'innodb_log_group_home_dir');");

        // 4. 查询撤销日志 (Undo Log) 独立表空间配置
        DbConnectionHelper.printQueryResults("4. 撤销日志 (Undo Log) 表空间配置", 
                "SHOW VARIABLES WHERE Variable_name IN ('innodb_undo_tablespaces', 'innodb_undo_directory');");

        // 5. 查询二进制日志 (Binlog) 与慢日志配置
        DbConnectionHelper.printQueryResults("5. 二进制日志 (Binlog) 与慢查询日志状态", 
                "SHOW VARIABLES WHERE Variable_name IN ('log_bin', 'log_bin_basename', 'slow_query_log', 'slow_query_log_file');");

        printStorageArchitectureSummary();
    }

    private static void printStorageArchitectureSummary() {
        System.out.println("---------------- 数据库 5 大核心文件职责记忆口诀 ----------------");
        System.out.println("① 数据文件 (.ibd / ibdata1):  用户数据与 B+ 树索引的物理归宿；");
        System.out.println("② 重做日志 (Redo Log):        物理修改记录，WAL 环形写，保障断电不丢数据；");
        System.out.println("③ 回滚日志 (Undo Log):        反向逻辑记录，保障事务回滚与 MVCC 快照；");
        System.out.println("④ 归档日志 (Binlog):          全量增量逻辑日志，支撑主从复制与跨天数据恢复；");
        System.out.println("⑤ 结构与控制文件 (.sdi / my.ini): 元数据定义与数据库引擎行为控制参数。");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

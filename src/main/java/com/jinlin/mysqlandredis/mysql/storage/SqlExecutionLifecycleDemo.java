package com.jinlin.mysqlandredis.mysql.storage;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 01: 执行一条 SQL 请求的过程是什么？
 * 
 * 核心架构生命周期全链路：
 * 
 * 1. Server 层 (无关于具体引擎的公共逻辑):
 *    - 连接器 (Connector): TCP 握手验证身份、分配线程、校验权限、管理连接超时 (wait_timeout)。
 *    - 查询缓存 (Query Cache): MySQL 8.0 彻底废除 (因写入极易导致失效，命中率低下)。
 *    - 解析器 (Parser): 词法分析 (提取关键字) + 语法分析 (构建抽象语法树 AST)。
 *    - 预处理器 (Preprocessor): 检查表名、列名存在性与别名有效性。
 *    - 优化器 (Optimizer): 基于 CBO (Cost-Based Optimizer) 评估最佳执行路径、选择索引与驱动表。
 *    - 执行器 (Executor): 校验操作权限，调用存储引擎标准 API (ha_open, ha_rnd_next 等) 逐行交互。
 * 
 * 2. 存储引擎层 (以 InnoDB 为例):
 *    - 读流程: 优先查 Buffer Pool 内存页；未命中则向 OS 发起磁盘 I/O 读入 16KB 页并加入 LRU 链表。
 *    - 写流程 (两阶段提交 2PC 保证 Crash-Safe 与主从一致):
 *      ① 载入内存数据页；
 *      ② 记录 Undo Log (提供事务回滚和 MVCC 快照读支持)；
 *      ③ 修改 Buffer Pool 内存页 (产生脏页 Dirty Page)；
 *      ④ 写 Redo Log Buffer，刷盘生成 Prepare 状态；
 *      ⑤ Server 层执行器写入 Binlog 日志；
 *      ⑥ 事务提交，将 Redo Log 置为 Commit 状态；
 *      ⑦ Page Cleaner 线程异步将脏页刷新至物理数据文件 (.ibd)。
 */
public class SqlExecutionLifecycleDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 01】执行一条 SQL 请求的全生命周期与两阶段提交实机剖析");
        System.out.println("====================================================================");

        // 1. 初始化演示订单表
        String dropTable = "DROP TABLE IF EXISTS interview_engine_order;";
        String createTable = "CREATE TABLE interview_engine_order ("
                + "  order_id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "  user_id BIGINT NOT NULL,"
                + "  amount DECIMAL(10, 2) NOT NULL,"
                + "  status VARCHAR(20) NOT NULL DEFAULT 'CREATED',"
                + "  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "  INDEX idx_user_status (user_id, status)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String insertData = "INSERT INTO interview_engine_order (user_id, amount, status) VALUES (1001, 299.00, 'CREATED');";

        DbConnectionHelper.executeSqlScript(dropTable, createTable, insertData);

        // 2. 演示优化器 (Optimizer) 工作结果: 通过 EXPLAIN 观察执行计划与索引选择
        String explainSql = "EXPLAIN SELECT order_id, amount, status "
                + "FROM interview_engine_order "
                + "WHERE user_id = 1001 AND status = 'CREATED';";
        DbConnectionHelper.printQueryResults("【优化器阶段产出】查看 EXPLAIN 执行计划 (走 idx_user_status 联合索引)", explainSql);

        // 3. 执行更新写操作 (触发两阶段提交与内存脏页生成)
        String updateSql = "UPDATE interview_engine_order SET status = 'PAID', amount = 318.00 WHERE order_id = 1;";
        DbConnectionHelper.executeSqlScript(updateSql);

        DbConnectionHelper.printQueryResults("【执行器与引擎层更新结果】查询更新后的数据行", 
                "SELECT order_id, user_id, amount, status FROM interview_engine_order WHERE order_id = 1;");

        printLifecycleDetails();
    }

    private static void printLifecycleDetails() {
        System.out.println("---------------- 为什么写操作必须采用【两阶段提交 (2PC)】？ ----------------");
        System.out.println("核心痛点: Redo Log (InnoDB 物理日志) 和 Binlog (Server 逻辑日志) 是两个独立的日志系统。");
        System.out.println("如果不采用两阶段提交：");
        System.out.println("  - 先写 Redo Log 后写 Binlog: 若 Redo 写完瞬间宕机，重启后主库依 Redo 恢复数据，但从库依靠 Binlog 同步丢失该操作，主从数据不一致！");
        System.out.println("  - 先写 Binlog 后写 Redo Log: 若 Binlog 写完瞬间宕机，主库未能写入 Redo 导致事务回滚丢失，但从库已据 Binlog 写入，主从数据依然不一致！");
        System.out.println("两阶段提交解决方案: ");
        System.out.println("  Step 1. Prepare 阶段: Redo Log 写入日志文件并标记为 PREPARE 状态；");
        System.out.println("  Step 2. 写 Binlog: 将操作写入 Binlog 并刷盘 (由 sync_binlog 控制)；");
        System.out.println("  Step 3. Commit 阶段: 将 Redo Log 标记为 COMMIT 状态；");
        System.out.println("崩溃恢复规则: 只要 Binlog 写入完整，即便 Commit 阶段断电，重启检测到 Prepare 状态且对应的 XID 在 Binlog 中存在，也会自动提交该事务，完美保证 Crash-Safe 与主从强一致！");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

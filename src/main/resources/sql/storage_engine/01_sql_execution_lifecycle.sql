-- ====================================================================
-- 问题 01: 执行一条 SQL 请求的过程是什么？
-- ====================================================================
-- 本脚本模拟并剖析从客户端发送一条 SQL 到获取结果的整个生命周期：
-- 
-- 1. Server 层处理流程:
--    (1) 连接器 (Connector): TCP 握手、权限校验、维护长连接 (wait_timeout)。
--    (2) 查询缓存 (Query Cache): 检查是否命中缓存 (MySQL 8.0 已彻底移除)。
--    (3) 解析器 (Parser): 词法分析 (识别关键字/表名) + 语法分析 (构建 AST 抽象语法树)。
--    (4) 预处理器 (Preprocessor): 校验表名与列是否存在、权限校验。
--    (5) 优化器 (Optimizer): 基于成本计算 (CBO)，选定最优索引与多表连接顺序，输出执行计划。
--    (6) 执行器 (Executor): 校验执行权限，调用存储引擎标准 API 接口读取数据。
-- 
-- 2. 存储引擎层 (以 InnoDB 为例):
--    - 读请求: 检查 Buffer Pool 是否命中 -> 命中直接返回; 未命中则从磁盘加载 16KB 页到内存并维护 LRU。
--    - 写请求 (两阶段提交 2PC):
--      ① 读取数据页到 Buffer Pool;
--      ② 记录 Undo Log (供回滚与 MVCC 快照读);
--      ③ 修改内存页 (成为脏页 Dirty Page);
--      ④ 写入 Redo Log Buffer，刷盘为 Prepare 状态;
--      ⑤ 执行器写 Binlog 磁盘;
--      ⑥ 提交事务，Redo Log 置为 Commit 状态 (两阶段提交保证数据与 Binlog 一致性与 Crash-Safe);
--      ⑦ 后台刷脏线程 (Master/Page Cleaner Thread) 异步将脏页刷新至磁盘数据文件 (.ibd)。
-- ====================================================================

DROP TABLE IF EXISTS interview_engine_order;
CREATE TABLE interview_engine_order (
    order_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SQL 执行生命周期演示订单表';

-- 插入一条演示基准数据
INSERT INTO interview_engine_order (user_id, amount, status) VALUES (1001, 299.00, 'CREATED');

-- 演示 1: 优化器工作透视 —— 使用 EXPLAIN 查看优化器生成的最终执行计划
EXPLAIN 
SELECT order_id, amount, status 
FROM interview_engine_order 
WHERE user_id = 1001 AND status = 'CREATED';

-- 演示 2: 查看当前会话执行状态统计与耗时剖析 (Profiling)
-- 开启 profiling
SET profiling = 1;

-- 执行一次标准更新操作 (触发 Undo -> Buffer Pool -> Redo Prepare -> Binlog -> Redo Commit)
UPDATE interview_engine_order 
SET status = 'PAID', amount = 318.00 
WHERE order_id = 1;

-- 查看刚才更新操作在各个微阶段 (Opening tables, System lock, updating, committing 等) 的耗时
SHOW PROFILES;

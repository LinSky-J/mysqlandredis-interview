package com.jinlin.mysqlandredis.mysql.tuning;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 【面试题 04】给你张表，发现查询速度很慢，你有哪些解决方案？
 *
 * 核心考点深度剖析 (大厂高频架构级综合治理大题)：
 * 面试中切忌一上来就说“加索引”，必须展现出系统化、闭环式、由浅入深的排查治理方法论：
 *
 * 方案步骤 ① 【第一步：全链路定位慢 SQL 瓶颈源头】
 * - 开启慢查询日志：设置 `slow_query_log = 1`，调低阈值 `long_query_time = 1` (秒)；
 * - 使用工具聚合分析：利用官方工具 `mysqldumpslow -s t` 按执行时间排序汇总频次最高的 TOP 慢 SQL；
 * - 观察连接与等待：通过 `SHOW PROCESSLIST` 排查是否因锁等待 (Lock Wait) 或大事务引发积压。
 *
 * 方案步骤 ② 【第二步：EXPLAIN 深入诊断执行计划】
 * - 诊断 type 是否为 ALL (全表扫描)；
 * - 诊断 key 是否为 NULL，分析 `possible_keys` 是否被优化器意外放弃；
 * - 诊断 rows 预估扫描物理行数是否高达数万至上百万；
 * - 诊断 Extra 是否包含致命的 `Using filesort` (外部文件排序) 或 `Using temporary` (内存临时表)。
 *
 * 方案步骤 ③ 【第三步：索引层靶向精准优化】
 * - 补建高区分度索引：对经常作为 WHERE、JOIN ON、ORDER BY 条件的字段建立 B+ 树索引；
 * - 遵循最左匹配建联合索引：将区分度最高且高频等值过滤的列放在联合索引最左边；
 * - 引入覆盖索引 (Covering Index)：让 SELECT 字段全部被二级索引树覆盖，彻底消灭回表查询 (避免二次寻道聚簇索引)；
 * - 长文本字段建立前缀索引 (Prefix Index) 降低树深度并节省 Buffer Pool 内存空间。
 *
 * 方案步骤 ④ 【第四步：SQL 语句级代码重构改写】
 * - 严禁 `SELECT *`：只获取所需列，为覆盖索引创造条件，避免网络带宽浪费；
 * - 深度分页延迟关联优化：
 *   原始低效写法：`SELECT * FROM t ORDER BY id LIMIT 1000000, 10;` (扫描 1000010 行再丢弃前 100 万行，极慢！)；
 *   延迟关联改写：`SELECT t.* FROM t JOIN (SELECT id FROM t ORDER BY id LIMIT 1000000, 10) AS sub ON t.id = sub.id;` (先通过主键覆盖索引秒级定位出 10 个 id，再回表 10 次，性能提升百倍！)；
 * - 避免在索引列使用函数、隐式类型转换 (如数字查字符串) 导致索引失效。
 *
 * 方案步骤 ⑤ 【第五步：表结构设计与冷热分离】
 * - 垂直拆分：将不常用的长文本 (TEXT/BLOB) 拆分到子表，减少主表每行体积，提升单页行容量；
 * - 冷热数据分离：将 3 个月前的历史归档流水转移至离线历史表，缩小在线活跃表体积。
 *
 * 方案步骤 ⑥ 【第六步：系统架构层分流】
 * - 引入 Redis / 本地缓存拦截绝大多数高并发读流量；
 * - 读写分离架构：一主多从，通过从库集群分摊读负载；
 * - 海量数据 (单表超 2000 万行或物理文件超 50GB) 进行分库分表 (ShardingSphere)。
 *
 * 方案步骤 ⑦ 【第七步：硬件与 MySQL 核心参数调优】
 * - 核心参数：将 `innodb_buffer_pool_size` 调大至服务器物理内存的 50%~75%，使热数据与索引全量常驻内存；
 * - 硬件升级：全量上 NVMe SSD 固态硬盘，提升 IOPS。
 */
public class Tuning04_SlowQuerySolutionsGuideDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 04】单表查询慢的全链路 7 步综合治理方法论实测");
        System.out.println("====================================================================");

        // 演示深度分页的优化器执行对比 (延迟关联机制)
        System.out.println(">>> 演示深度分页优化: 延迟关联 (子查询主键索引覆盖) 实测对比:");
        String delayedJoinSql = "EXPLAIN SELECT o.* FROM tuning_large_orders o " +
                "JOIN (SELECT id FROM tuning_large_orders LIMIT 0, 2) sub ON o.id = sub.id;";
        DbConnectionHelper.printQueryResults("延迟关联分页优化 EXPLAIN 执行计划 (子查询走覆盖索引)",
                delayedJoinSql);

        System.out.println(">>> 慢查询排查优化全景 7 步流转路径：");
        System.out.println("   [1. 抓慢日志] -> [2. EXPLAIN诊断] -> [3. 索引优化/覆盖索引] -> [4. SQL改写/延迟关联] -> [5. 冷热分离] -> [6. 缓存/读写分离] -> [7. BufferPool与硬件升级]");
    }
}

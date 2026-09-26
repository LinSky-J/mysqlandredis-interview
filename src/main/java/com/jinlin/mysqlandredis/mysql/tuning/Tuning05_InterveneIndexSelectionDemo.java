package com.jinlin.mysqlandredis.mysql.tuning;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 【面试题 05】如果 Explain 用到的索引不正确的话，有什么办法干预吗？
 *
 * 核心考点深度剖析：
 *
 * 一、 为什么 MySQL 优化器有时会“选错索引”？
 * 1. 成本估算模型偏差：
 *    - MySQL 采用基于成本的优化器 (Cost-Based Optimizer, CBO)；
 *    - 成本由【I/O 成本 (读取数据页数量)】+【CPU 成本 (过滤比对行数)】计算得出。
 * 2. 统计信息失真 (Cardinality 偏差)：
 *    - 预估扫描行数依靠索引基数 (Cardinality) 随机抽样计算，如果表近期发生剧烈频繁的新增/修改/删除，
 *      可能导致内存中统计信息严重滞后失真，优化器误判扫描行数从而选错索引。
 * 3. 回表成本与主键排序干扰：
 *    - 二级索引可能需要昂贵的回表，当数据量达到一定比例 (通常超过整表 20%~30%)，优化器可能放弃二级索引转而走全表扫描。
 *
 * 二、 5 大强制/引导干预手段：
 * 1. 手段 ①：`FORCE INDEX(index_name)` —— 强制使用指定索引
 *    - 语法：`SELECT * FROM t FORCE INDEX(idx_name) WHERE ...;`
 *    - 效果：强行命令优化器必须使用指定索引，直接无视优化器的成本评估！
 *
 * 2. 手段 ②：`USE INDEX(index_name)` —— 建议优先参考指定索引
 *    - 语法：`SELECT * FROM t USE INDEX(idx_name) WHERE ...;`
 *    - 效果：向优化器提供线索，但优化器若仍觉得全表扫描更便宜，依然可能不采纳。
 *
 * 3. 手段 ③：`IGNORE INDEX(index_name)` —— 强制忽略走错的干扰索引
 *    - 语法：`SELECT * FROM t IGNORE INDEX(wrong_index) WHERE ...;`
 *    - 效果：当两个索引冲突且优化器选了错误的索引时，显式拉黑错误索引，促使优化器选用正确索引。
 *
 * 4. 手段 ④：`ANALYZE TABLE table_name;` —— 重新采样校准统计信息 (★★★★★ 治本之策)
 *    - 效果：让 InnoDB 重新扫描数据页，重新计算索引的 Cardinality 基数，修正统计信息失真，恢复优化器智能选择。
 *
 * 5. 手段 ⑤：SQL 语句级语义诱导与多余索引下线
 *    - 例如将 `ORDER BY id` 改为强制走特定排序字段，或将导致干扰的重复冗余索引彻底 DROP 删除。
 */
public class Tuning05_InterveneIndexSelectionDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 05】MySQL 优化器索引选错 5 大干预手段实机演练");
        System.out.println("====================================================================");

        // 1. 演练 FORCE INDEX: 强制指定走 idx_dept_id 索引
        System.out.println(">>> 1. 演练 FORCE INDEX (强制优化器走 idx_dept_id):");
        String forceSql = "EXPLAIN SELECT * FROM tuning_employee FORCE INDEX(idx_dept_id) WHERE dept_id = 10;";
        DbConnectionHelper.printQueryResults("FORCE INDEX 执行计划", forceSql);

        // 2. 演练 IGNORE INDEX: 强制忽略 idx_dept_id 索引
        System.out.println(">>> 2. 演练 IGNORE INDEX (强制优化器忽略 idx_dept_id):");
        String ignoreSql = "EXPLAIN SELECT * FROM tuning_employee IGNORE INDEX(idx_dept_id) WHERE dept_id = 10;";
        DbConnectionHelper.printQueryResults("IGNORE INDEX 执行计划 (key 应变为 NULL 或其他索引)", ignoreSql);

        // 3. 演练 ANALYZE TABLE 重新采样校准统计信息 (治本之策)
        System.out.println(">>> 3. 演练 ANALYZE TABLE 重新收集表统计信息 (治本之策):");
        DbConnectionHelper.printQueryResults("ANALYZE TABLE 执行结果",
                "ANALYZE TABLE tuning_employee;"
        );

        System.out.println(">>> 索引选错干预手段小结：");
        System.out.println("1. 应急救火：在 SQL 中临时使用 `FORCE INDEX(idx_name)` 强行指定正确索引；");
        System.out.println("2. 根治失真：线上执行 `ANALYZE TABLE` 重新刷新 Cardinality 统计信息；");
        System.out.println("3. 架构规范：及时清理废弃与冗余索引，降低优化器干扰项。");
    }
}

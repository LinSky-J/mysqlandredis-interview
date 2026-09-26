package com.jinlin.mysqlandredis.mysql.tuning;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 【面试题 01】MySQL 的 EXPLAIN 有什么作用？
 *
 * 核心考点深度剖析：
 *
 * 一、 EXPLAIN 的核心作用：
 * 1. 模拟优化器执行 SQL：
 *    - EXPLAIN (或 DESCRIBE) 关键字用于分析单条 SELECT/UPDATE/DELETE 语句，
 *      让开发人员看清 MySQL 优化器究竟是如何规划这条 SQL 的执行过程的，而无需真正触发底层全量执行。
 * 2. 核心诊断价值：
 *    - 表的读取顺序是怎样的？(通过 `id` 和行的顺序判断)；
 *    - 数据读取操作的操作类型是什么？(全表扫描还是走索引查找)；
 *    - 实际命中了哪个索引？有没有走多列联合索引？；
 *    - 每张表预计有多少物理行被扫描比对？；
 *    - 是否产生了低效的内存临时表 (Using temporary) 或外部文件排序 (Using filesort)？
 *
 * 二、 EXPLAIN 关键输出字段权威解析：
 * 1. `id`：SELECT 查询序号。id 相同从上到下执行；id 不同值越大优先级越高、越先执行(子查询常用)。
 * 2. `select_type`：查询类型。常见 SIMPLE (简单查询)、PRIMARY (最外层主查询)、SUBQUERY (子查询)、DERIVED (衍生临时表)。
 * 3. `table`：输出行所引用的目标数据表名。
 * 4. `type` (★★★★★ 核心性能指标)：访问类型，从最优到最差性能排序为：
 *    `system` > `const` > `eq_ref` > `ref` > `range` > `index` > `ALL`
 *    - 生产标准：查询优化至少要保证达到 `range` 级别，最好达到 `ref`，杜绝 `ALL` (全表扫描)！
 * 5. `possible_keys`：指出 MySQL 能使用哪个索引在表中找到记录，是可能用到的候选索引列表。
 * 6. `key` (★★★★★)：MySQL 实际决定采用的索引。若为 NULL 则说明未走索引。
 * 7. `key_len`：使用的索引字节数长度。通过长度可精准算出联合索引具体走了几个字段。
 * 8. `ref`：表示哪些列或常量被用于与 `key` 列上的索引进行比较匹配。
 * 9. `rows`：预估必须检查扫描的物理行数，数值越小越好。
 * 10. `filtered`：表示返回结果的行占预估扫描行数的百分比，越高说明过滤效果越好。
 * 11. `Extra` (★★★★★)：解决性能瓶颈的绝密情报：
 *     - `Using index`：使用覆盖索引，无需回表查询，性能极佳；
 *     - `Using index condition`：使用了索引下推 (ICP)；
 *     - `Using where`：使用了 WHERE 条件在引擎层之上进行过滤；
 *     - `Using filesort`：发生外部排序(无法利用索引顺序)，需调优优化 ORDER BY；
 *     - `Using temporary`：使用了临时表(常见于 GROUP BY 或 DISTINCT 未走索引)，性能极差必须消灭！
 */
public class Tuning01_ExplainRoleAndFieldsDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 01】MySQL EXPLAIN 执行计划各字段深度剖析与实测");
        System.out.println("====================================================================");

        // 1. 实机执行不同类型的 EXPLAIN 观察 type 字段表现
        System.out.println(">>> 1. 观察 const 级别 (主键/唯一索引等值点查):");
        DbConnectionHelper.printQueryResults("EXPLAIN 主键点查 (预期 type=const)",
                "EXPLAIN SELECT id, name FROM tuning_employee WHERE id = 1;"
        );

        System.out.println(">>> 2. 观察 ref 级别 (普通二级索引等值查找):");
        DbConnectionHelper.printQueryResults("EXPLAIN 普通索引 ref 查找",
                "EXPLAIN SELECT id, name, dept_id FROM tuning_employee WHERE dept_id = 10;"
        );

        System.out.println(">>> 3. 观察 range 级别 (范围查找):");
        DbConnectionHelper.printQueryResults("EXPLAIN 范围 range 查找",
                "EXPLAIN SELECT id, name, age FROM tuning_employee WHERE age BETWEEN 25 AND 35;"
        );

        System.out.println(">>> 4. 观察 Using index (覆盖索引):");
        DbConnectionHelper.printQueryResults("EXPLAIN 覆盖索引查找 (Extra 包含 Using index)",
                "EXPLAIN SELECT age, salary FROM tuning_employee WHERE age = 28;"
        );
    }
}

package com.jinlin.mysqlandredis.mysql.tuning;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 【面试题 03】怎么查看表的索引？(SHOW INDEX 结构与 Cardinality 区分度解析)
 *
 * 核心考点深度剖析：
 *
 * 一、 查看数据表索引的经典命令：
 * 1. `SHOW INDEX FROM table_name;` (或 `SHOW KEYS FROM table_name;`)
 * 2. `SHOW CREATE TABLE table_name;` (直观查看建表 DDL 中的索引声明)
 *
 * 二、 `SHOW INDEX` 输出结果各列的核心专业解读：
 * 1. `Table`：索引所属的数据表名。
 * 2. `Non_unique`：
 *    - 值为 0：表示【唯一索引 (Unique Index)】或主键；
 *    - 值为 1：表示【非唯一普通索引 (Non-Unique)】。
 * 3. `Key_name`：索引名称 (主键固定为 PRIMARY，普通索引为开发者自定义的名字)。
 * 4. `Seq_in_index`：
 *    - 该列在多列复合索引中的【字段排列序号】(从 1 开始)；
 *    - 核心作用：直接反映了复合索引的最左前缀顺序！
 * 5. `Column_name`：索引包含的字段列名。
 * 6. `Collation`：列在索引中的存储排序方式 (A 为升序，NULL 为未排序)。
 * 7. `Cardinality` (★★★★★ 核心灵魂指标)：
 *    - 概念：索引基数，表示该列中不重复数值的【预估唯一值总数量】；
 *    - 重要性：【Cardinality 越大，说明字段区分度越高，走索引的过滤效率越惊艳】！
 *    - 优化器选路基石：MySQL 优化器决定走不走索引，核心依据就是该字段的 Cardinality / 表总行数 (区分度)。
 *      若某列 Cardinality 极低(如性别只有 2 个值)，优化器判定回表成本过高，会直接放弃索引转为全表扫描。
 *    - 失真隐患：Cardinality 是通过随机采样估算的，若统计信息不准，会导致优化器选错索引(需用 `ANALYZE TABLE` 修复)。
 * 8. `Index_type`：索引的底层数据结构实现 (InnoDB 默认全部为 **BTREE**)。
 */
public class Tuning03_ShowIndexStructureDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 03】怎么查看表的索引？(SHOW INDEX 输出字段逐一解读实测)");
        System.out.println("====================================================================");

        System.out.println(">>> 执行 SHOW INDEX FROM tuning_employee; 查看员工表的全量索引详情：");
        DbConnectionHelper.printQueryResults("员工表全量索引元数据核查",
                "SHOW INDEX FROM tuning_employee;"
        );

        System.out.println(">>> 核心参数解读小结：");
        System.out.println("1. Non_unique = 0 表示唯一索引 (如 PRIMARY 和 uk_emp_code)；");
        System.out.println("2. Seq_in_index 体现联合索引 idx_age_salary 中 age 是第 1 列，salary 是第 2 列；");
        System.out.println("3. Cardinality 反映索引列的区分度大小，是优化器执行计划选择的核心考量依据。");
    }
}

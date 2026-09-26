package com.jinlin.mysqlandredis.mysql.tuning;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 【面试题 02】怎么查看是否有走索引？
 *
 * 核心考点深度剖析 (排查 SQL 索引命中三大黄金准则)：
 * 使用 `EXPLAIN` 分析 SQL 语句时，必须通过以下 3 个关键字段联合判断是否有效走到了索引：
 *
 * 准则 ①：看 `key` 列 —— 是否为 NULL？
 * - 若 `key` 为 `NULL`：毫无疑问，【绝对没有走任何索引】！
 * - 若 `key` 显示了具体索引名称 (如 `idx_dept_id`)：说明 MySQL 最终选择了该索引进行数据检索。
 *
 * 准则 ②：看 `type` 列 —— 走的索引是高效点查还是低效遍历？
 * - 若 `type = ALL`：说明是【全表扫描 (Table Scan)】，哪怕 `possible_keys` 列出了索引也没被采用；
 * - 若 `type = index`：说明走的是【全索引树扫描 (Full Index Scan)】，虽比 ALL 小但依然是在整棵索引树叶子节点链表从头扫到尾；
 * - 若 `type` 为 `range`、`ref`、`eq_ref`、`const`：恭喜！说明真正享受到了 B+ 树自顶向下 O(logN) 精确分支定位检索的巨大加速优势！
 *
 * 准则 ③：看 `key_len` 列 —— 联合索引到底精准命中了前几列？
 * - 在多列联合索引 (如 `idx_age_salary (age, salary)`) 中：
 *   - `age` 是 INT NOT NULL (占用 4 字节)；
 *   - `salary` 是 DECIMAL(10,2) NOT NULL (占用 5 字节)；
 *   - 当查询仅用 `WHERE age = 28` 时，`key_len = 4` (证明仅命中了联合索引的第 1 列)；
 *   - 当查询使用 `WHERE age = 28 AND salary = 12000` 时，`key_len = 9` (4+5=9，证明联合索引的 2 列全部被精准命中！)。
 */
public class Tuning02_CheckIndexUsageDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 02】怎么查看是否有走索引？(key、type、key_len 深度判别实测)");
        System.out.println("====================================================================");

        // 1. 实测 1: 故意写一条不走索引的 SQL (例如在索引列上使用函数 SUBSTR 或类型隐式转换)
        System.out.println(">>> 1. 验证未走索引的场景 (在索引列使用函数或对非索引列查询):");
        DbConnectionHelper.printQueryResults("未走索引的 EXPLAIN (观察 key=NULL, type=ALL)",
                "EXPLAIN SELECT id, name, phone FROM tuning_employee WHERE SUBSTR(emp_code, 1, 1) = 'E';"
        );

        // 2. 实测 2: 走单列二级索引的场景
        System.out.println(">>> 2. 验证有效走索引的场景 (走 idx_dept_id):");
        DbConnectionHelper.printQueryResults("有效走索引的 EXPLAIN (观察 key=idx_dept_id, type=ref)",
                "EXPLAIN SELECT id, name, dept_id FROM tuning_employee WHERE dept_id = 10;"
        );

        // 3. 实测 3: 通过 key_len 验证联合索引精准命中了几列
        System.out.println(">>> 3. 验证联合索引 idx_age_salary 命中深度对比:");
        DbConnectionHelper.printQueryResults("场景 A: 仅命中 age 列 (key_len=4)",
                "EXPLAIN SELECT id, name, age, salary FROM tuning_employee WHERE age = 28;"
        );

        DbConnectionHelper.printQueryResults("场景 B: 同时命中 age 与 salary 列 (key_len=9=4+5)",
                "EXPLAIN SELECT id, name, age, salary FROM tuning_employee WHERE age = 28 AND salary = 12000.00;"
        );
    }
}

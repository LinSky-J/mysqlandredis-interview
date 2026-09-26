package com.jinlin.mysqlandredis.mysql.index;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 01: 索引是什么？有什么好处？索引分类与哈希索引场景深度解析
 * 
 * 说明：数据表结构及初始化数据已移至 /sql/03_index_schema.sql 统一由 DataGrip 预先创建维护，
 *       本 Java 类专注面试题核心逻辑剖析、执行计划深度讲解与运行时参数观测。
 * 
 * 核心考点：
 * 1. 索引本质：排好序的高效查找与检索数据结构 (类比书本目录)。
 * 2. 优点：大幅减少磁盘 I/O 扫描行数、避免临时排序 Filesort、加速多表连接。
 * 3. 缺点：占用磁盘与 Buffer Pool 内存，降低增删改写吞吐 (需维护索引树有序性与页分裂)。
 * 4. 分类维度：
 *    - 物理组织：聚簇索引 (数据与主键紧凑存储) vs 非聚簇索引 (叶子存主键/地址指针)。
 *    - 业务约束：主键索引 (PRIMARY)、唯一索引 (UNIQUE)、普通单列索引 (INDEX)、联合复合索引、全文索引 (FULLTEXT)。
 *    - 数据结构：B+ 树索引 (默认)、哈希索引 (MEMORY 引擎支持)、空间 R-Tree。
 * 5. 哈希索引适用场景：纯等值精确点查 (O(1))；局限是绝不支持范围查询、排序和最左前缀匹配。
 */
public class IndexBasicsAndTypesDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【索引模块 01】索引本质定义、逻辑与物理分类、哈希索引实机验证");
        System.out.println("====================================================================");

        // 1. 观测主键聚簇索引等值点查执行计划 (type = const, 速度最快, 直接命中叶子数据行)
        DbConnectionHelper.printQueryResults("主键聚簇索引等值点查执行计划 (type=const)", 
                "EXPLAIN SELECT * FROM interview_idx_user WHERE id = 1;");

        // 2. 观测普通二级索引点查的执行计划 (type = ref, 走二级索引叶子获取主键并回表)
        DbConnectionHelper.printQueryResults("普通二级索引等值查询执行计划 (type=ref)", 
                "EXPLAIN SELECT * FROM interview_idx_user WHERE user_name = '张三';");

        // 3. 观测 MEMORY 存储引擎中的原生哈希索引等值点查 (O(1) 精确匹配)
        DbConnectionHelper.printQueryResults("哈希索引等值点查执行计划 (MEMORY 引擎 HASH 索引)", 
                "EXPLAIN SELECT * FROM interview_hash_user WHERE token = 'token_aaa_111';");

        // 4. 查询 InnoDB 核心参数: 自适应哈希索引 (Adaptive Hash Index, AHI) 启用状态
        DbConnectionHelper.printQueryResults("InnoDB 自适应哈希索引 (Adaptive Hash Index) 启用状态", 
                "SHOW VARIABLES LIKE 'innodb_adaptive_hash_index';");

        printSummaryInsights();
    }

    private static void printSummaryInsights() {
        System.out.println("---------------- 为什么生产主力依然是 B+Tree 而非 Hash 索引？ ----------------");
        System.out.println("1. 业务场景特性: 业务查询极其依赖范围检索 (例如: price BETWEEN 100 AND 500、时间范围过滤) 和 ORDER BY 排序；");
        System.out.println("2. 哈希索引物理缺陷: 计算哈希值后彻底丢失数据原有的高低顺序，遇到范围过滤只能硬退化为全表扫描！");
        System.out.println("3. InnoDB 的兼得智慧: 采用 B+Tree 支撑通用范围与排序，同时通过【自适应哈希索引 (AHI)】在内存中自动为热点数据页构建 Hash 索引加速点查，实现鱼与熊掌兼得。");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

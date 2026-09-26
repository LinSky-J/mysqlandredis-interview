package com.jinlin.mysqlandredis.mysql.index;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 01: 索引是什么？有什么好处？索引分类与哈希索引场景深度解析
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

        // 1. 初始化包含各类索引的演示表
        String dropTable = "DROP TABLE IF EXISTS interview_idx_basics;";
        String createTable = "CREATE TABLE interview_idx_basics ("
                + "  id BIGINT NOT NULL AUTO_INCREMENT,"
                + "  user_code VARCHAR(32) NOT NULL,"
                + "  nickname VARCHAR(50) NOT NULL,"
                + "  age INT NOT NULL,"
                + "  dept_id INT NOT NULL,"
                + "  bio TEXT,"
                + "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "  PRIMARY KEY (id),"
                + "  UNIQUE KEY uk_user_code (user_code),"
                + "  INDEX idx_nickname (nickname),"
                + "  INDEX idx_dept_age (dept_id, age),"
                + "  FULLTEXT KEY ft_bio (bio)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String insertData = "INSERT INTO interview_idx_basics (user_code, nickname, age, dept_id, bio) VALUES "
                + "('U1001', '张三', 25, 10, '资深 Java 后端架构师，精通 MySQL 调优与底层原理'), "
                + "('U1002', '李四', 30, 10, '分布式系统专家，熟悉高并发缓存与分库分表'), "
                + "('U1003', '王五', 28, 20, '前端技术专家，擅长前端工程化与性能优化');";

        DbConnectionHelper.executeSqlScript(dropTable, createTable, insertData);

        // 2. 观察主键点查的执行计划 (type = const, 速度最快)
        DbConnectionHelper.printQueryResults("主键聚簇索引等值点查执行计划 (type=const)", 
                "EXPLAIN SELECT * FROM interview_idx_basics WHERE id = 1;");

        // 3. 观察普通二级索引点查的执行计划 (type = ref)
        DbConnectionHelper.printQueryResults("普通二级索引等值查询执行计划 (type=ref)", 
                "EXPLAIN SELECT * FROM interview_idx_basics WHERE nickname = '张三';");

        // 4. 初始化哈希索引 (MEMORY 引擎) 并验证等值检索
        String dropHash = "DROP TABLE IF EXISTS interview_idx_hash_memory;";
        String createHash = "CREATE TABLE interview_idx_hash_memory ("
                + "  session_id VARCHAR(64) NOT NULL,"
                + "  user_id BIGINT NOT NULL,"
                + "  PRIMARY KEY (session_id) USING HASH"
                + ") ENGINE=MEMORY DEFAULT CHARSET=utf8mb4;";
        String insertHash = "INSERT INTO interview_idx_hash_memory VALUES ('sess_token_abc123', 1001);";
        DbConnectionHelper.executeSqlScript(dropHash, createHash, insertHash);

        DbConnectionHelper.printQueryResults("哈希索引等值点查执行计划", 
                "EXPLAIN SELECT * FROM interview_idx_hash_memory WHERE session_id = 'sess_token_abc123';");

        // 5. 查询 InnoDB 自适应哈希索引 (AHI) 参数状态
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

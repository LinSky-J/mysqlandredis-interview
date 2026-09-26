package com.jinlin.mysqlandredis.mysql.index;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 06: 索引设计与优化法则、低基数状态字段、前缀索引深度实战
 * 
 * 核心考点深度解析：
 * 1. 索引是不是建的越多越好？
 *    - 绝不是！单表建议不超过 5 个。
 *    - 写放大严重：每次 INSERT/UPDATE/DELETE 必须同步更新所有索引树，引发大量随机 I/O 与页分裂；
 *    - 挤占内存：索引膨胀争抢 Buffer Pool，导致核心热点数据频繁被淘汰出内存；
 *    - 决策耗时：优化器生成执行计划时分析成本时间随索引数量指数增长。
 * 2. 状态字段 (0/1) 适合建索引吗？
 *    - 常规场景：不适合！区分度极差，查询 50% 数据时优化器自动放弃索引走全表扫描；
 *    - 特例场景：极端数据倾斜 (如 1000 万条中仅 10 条 status=1 待审核)，或作为联合索引辅助列。
 * 3. 前缀索引 (Prefix Index)：
 *    - 优势：针对长文本 (如 VARCHAR(100)/URL) 截取前 N 字符建索引，大幅缩减索引体积，保证 B+ 树高扇出；
 *    - 局限性：无法利用覆盖索引 (必须回表比对完整字符串)；无法用于 ORDER BY 与 GROUP BY 排序。
 * 4. 三星索引黄金理论 (Three-Star Index)：
 *    - 一星：索引将查询相关行尽可能集中在相邻页 (满足 WHERE 条件)；
 *    - 二星：索引物理排序与 ORDER BY 一致 (消除 Filesort)；
 *    - 三星：索引覆盖全部查询列 (达成 Covering Index 零回表)。
 */
public class PrefixIndexAndOptimizationDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【索引模块 06】索引设计原则、状态字段取舍、前缀索引区分度实机验证");
        System.out.println("====================================================================");

        // 1. 初始化包含前缀索引的数据表
        String dropTable = "DROP TABLE IF EXISTS interview_prefix_demo;";
        String createTable = "CREATE TABLE interview_prefix_demo ("
                + "  id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "  email VARCHAR(100) NOT NULL,"
                + "  status TINYINT NOT NULL DEFAULT 0,"
                + "  INDEX idx_email_prefix (email(7))"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String insertData = "INSERT INTO interview_prefix_demo (email, status) VALUES "
                + "('zhangsan_dev@gmail.com', 0), "
                + "('lisi_developer@163.com', 0), "
                + "('wangwu_arch@qq.com', 1), "
                + "('zhaoliu_test@hotmail.com', 0);";

        DbConnectionHelper.executeSqlScript(dropTable, createTable, insertData);

        // 2. 实测前缀区分度计算 (Selectivity 公式)
        String selectivitySql = "SELECT "
                + "  COUNT(DISTINCT LEFT(email, 5)) / COUNT(*) AS sel_len_5, "
                + "  COUNT(DISTINCT LEFT(email, 7)) / COUNT(*) AS sel_len_7, "
                + "  COUNT(DISTINCT email) / COUNT(*) AS sel_full "
                + "FROM interview_prefix_demo;";
        DbConnectionHelper.printQueryResults("前缀索引不同截取长度的区分度对比计算", selectivitySql);

        // 3. 验证前缀索引执行计划 (能够走索引 ref)
        DbConnectionHelper.printQueryResults("前缀索引执行计划验证 (type=ref)", 
                "EXPLAIN SELECT * FROM interview_prefix_demo WHERE email = 'zhangsan_dev@gmail.com';");

        // 4. 验证前缀索引无法实现覆盖索引 (即便只查 email 依然无法 Using index，必须回表！)
        DbConnectionHelper.printQueryResults("前缀索引无法作为覆盖索引验证 (Extra 无法出现 Using index)", 
                "EXPLAIN SELECT email FROM interview_prefix_demo WHERE email = 'zhangsan_dev@gmail.com';");

        printFinalOptimizationSummary();
    }

    private static void printFinalOptimizationSummary() {
        System.out.println("---------------- 生产环境索引优化核心方法论 ----------------");
        System.out.println("1. 索引数量控制: 单表索引总数严格控制在 5 个以内，优先使用【多列联合索引】代替多个单列索引；");
        System.out.println("2. 业务字段选型: 经常出现在 WHERE、JOIN ON、ORDER BY、GROUP BY 的列优先建立索引；");
        System.out.println("3. 追求三星索引: 努力通过覆盖索引 (Covering Index) 消除回表与内存重排 (Using filesort)；");
        System.out.println("4. 前缀索引取舍: 针对长文本使用前缀索引压低存储空间，但需注意其不支持覆盖索引与排序。");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

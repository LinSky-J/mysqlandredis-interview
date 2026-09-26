package com.jinlin.mysqlandredis.mysql.index;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 04: 联合索引原理、最左前缀、范围查询失效与索引下推 (ICP)
 * 
 * 核心考点深度解析：
 * 1. 联合索引 (a, b, c) 物理实现：
 *    - 严格先按 a 升序；a 相同再按 b 升序；b 相同再按 c 升序。
 *    - 丢失最左列 a 则 b 与 c 全局无序，无法使用二分查找。
 * 2. 高频题目解答：
 *    - Q1: where b > xxx and a = x 会生效吗？
 *      A: 必定生效！MySQL 优化器会在词法预处理后自动优化表达式顺序为 a = x and b > xxx，完美走联合索引。
 *    - Q2: where a = 2 and c = 1 会用到索引吗？
 *      A: 会！但仅有 a 列参与索引二分定位 (key_len 体现为 a 的大小)；c 列通过 MySQL 5.6+ 引入的【索引下推 (ICP)】在引擎层提前过滤，减少回表。
 *    - Q3: where A = xxx and C < xxx 索引怎么走？
 *      A: A 精确命中索引范围，C 触发 Using index condition，符合条件的记录才回表查完整行。
 *    - Q4: 一个列既是单列索引又是联合索引，单独查它走哪个？
 *      A: 优化器基于成本 (CBO) 评估索引树体积，通常倾向于选择单列索引 (其叶子体积更小，I/O 成本更低)。但工程实践中建议清理单列冗余索引。
 */
public class CompositeIndexAndIcpDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【索引模块 04】联合索引最左匹配原则、索引下推 (ICP) 实机执行计划验证");
        System.out.println("====================================================================");

        // 1. 初始化包含单列与联合索引的数据表
        String dropTable = "DROP TABLE IF EXISTS interview_composite_idx;";
        String createTable = "CREATE TABLE interview_composite_idx ("
                + "  id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "  a INT NOT NULL,"
                + "  b INT NOT NULL,"
                + "  c INT NOT NULL,"
                + "  extra_info VARCHAR(100) NOT NULL,"
                + "  INDEX idx_single_a (a),"
                + "  INDEX idx_composite_abc (a, b, c)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String insertData = "INSERT INTO interview_composite_idx (a, b, c, extra_info) VALUES "
                + "(1, 10, 100, 'Info-1'), (1, 20, 200, 'Info-2'), "
                + "(2, 10, 100, 'Info-3'), (2, 30, 300, 'Info-4'), (3, 40, 400, 'Info-5');";

        DbConnectionHelper.executeSqlScript(dropTable, createTable, insertData);

        // 场景 1: WHERE b > 10 AND a = 1 (验证优化器自动重排顺序命中联合索引)
        DbConnectionHelper.printQueryResults("【场景 1】WHERE b > 10 AND a = 1 (优化器自动颠倒顺序命中联合索引)", 
                "EXPLAIN SELECT * FROM interview_composite_idx WHERE b > 10 AND a = 1;");

        // 场景 2: WHERE a = 2 AND c = 100 (验证最左前缀与索引下推)
        DbConnectionHelper.printQueryResults("【场景 2】WHERE a = 2 AND c = 100 (跳过 b 列，c 触发 Using index condition)", 
                "EXPLAIN SELECT * FROM interview_composite_idx WHERE a = 2 AND c = 100;");

        // 场景 3: WHERE a = 1 AND c < 300 (验证 A = xxx AND C < xxx)
        DbConnectionHelper.printQueryResults("【场景 3】WHERE a = 1 AND c < 300 (A 定位范围，C 索引下推过滤)", 
                "EXPLAIN SELECT * FROM interview_composite_idx WHERE a = 1 AND c < 300;");

        // 场景 4: 单独查 a: 观察单列与联合索引共存时的优化器成本选择
        DbConnectionHelper.printQueryResults("【场景 4】单查列 a 时单列索引与联合索引的选择评估", 
                "EXPLAIN SELECT a FROM interview_composite_idx WHERE a = 2;");

        printBestPractices();
    }

    private static void printBestPractices() {
        System.out.println("---------------- 联合索引设计与使用规范 ----------------");
        System.out.println("1. 区分度优先: 建立联合索引 (A, B, C) 时，业务区分度最高 (唯一值占比最大) 的列应放在最左侧；");
        System.out.println("2. 范围条件放最后: 凡是用于范围查询 (>, <, BETWEEN) 的字段必须放在联合索引末尾，否则后续列彻底丧失 B+ 树定位查找能力；");
        System.out.println("3. 拒绝单列冗余: 如果已经存在联合索引 idx_a_b_c，切勿重复创建 idx_a，因为 idx_a_b_c 完全能满足单查 a 的需求！");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

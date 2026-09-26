package com.jinlin.mysqlandredis.mysql.index;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 05: 索引失效 8 大场景、回表查询与覆盖索引深度实机剖析
 * 
 * 说明：数据表结构及初始化数据已移至 /sql/03_index_schema.sql 统一由 DataGrip 预先创建维护，
 *       本 Java 类专注面试题核心逻辑剖析、索引失效场景与覆盖索引消除回表实机验证。
 * 
 * 核心考点深度解析：
 * 1. 回表查询 (Table Lookup)：二级索引叶子只含索引列+主键；SELECT 其他列需拿主键再查聚簇索引，引发高代价随机 I/O。
 * 2. 覆盖索引 (Covering Index)：查询列全部位于二级索引中，无需回表，Extra 显著呈现【Using index】。
 * 3. 常见 8 大索引失效场景实测：
 *    - 函数与表达式：WHERE UPPER(col) 或 WHERE col + 1；
 *    - 模糊匹配前导通配符：LIKE '%abc' (LIKE 'abc%' 正常走索引范围查找)；
 *    - 隐式类型转换：VARCHAR 字段传入整型数字，内部引发 CAST 函数，导致索引彻底失效；
 *    - 联合索引最左前缀断层；
 *    - OR 条件连接了未建立索引的字段；
 *    - 范围查询导致右侧联合索引字段无法定位；
 *    - !=、<>、NOT IN；
 *    - 数据倾斜：当匹配结果集超过整表 20%~30% 时，优化器评估回表成本过高，直接放弃索引走全表扫描 (ALL)。
 */
public class IndexInvalidationAndCoveringDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【索引模块 05】索引失效典型案例、回表查询与覆盖索引实机验证");
        System.out.println("====================================================================");

        // 1. 覆盖索引 vs 回表查询对比实测
        DbConnectionHelper.printQueryResults("【回表案例】查询包含非索引列 extra_info (需拿主键回聚簇索引查完整行)", 
                "EXPLAIN SELECT user_code, age, extra_info FROM interview_invalidation WHERE user_code = 'USR1001';");

        DbConnectionHelper.printQueryResults("【覆盖索引案例】仅查 id, user_code, age (Extra 明确显示 Using index，零回表！)", 
                "EXPLAIN SELECT id, user_code, age FROM interview_invalidation WHERE user_code = 'USR1001';");

        // 2. 索引失效典型案例实测
        DbConnectionHelper.printQueryResults("【失效 1: 索引列使用函数 UPPER() -> type=ALL 全表扫描】", 
                "EXPLAIN SELECT * FROM interview_invalidation WHERE UPPER(user_code) = 'USR1001';");

        DbConnectionHelper.printQueryResults("【失效 2: 左模糊匹配 LIKE '%1001' -> type=ALL 全表扫描】", 
                "EXPLAIN SELECT * FROM interview_invalidation WHERE user_code LIKE '%1001';");

        DbConnectionHelper.printQueryResults("【正常对比: 右模糊匹配 LIKE 'USR10%' -> type=range 正常命中索引】", 
                "EXPLAIN SELECT * FROM interview_invalidation WHERE user_code LIKE 'USR10%';");

        DbConnectionHelper.printQueryResults("【失效 3: 隐式类型转换 (phone_num 为 VARCHAR 传入整型数字) -> type=ALL 索引彻底失效！】", 
                "EXPLAIN SELECT * FROM interview_invalidation WHERE phone_num = 13800000001;");

        DbConnectionHelper.printQueryResults("【正常对比: phone_num 传入加单引号字符串 -> type=ref 走索引】", 
                "EXPLAIN SELECT * FROM interview_invalidation WHERE phone_num = '13800000001';");

        DbConnectionHelper.printQueryResults("【失效 4: OR 关联无索引列 extra_info -> type=ALL 全表扫描】", 
                "EXPLAIN SELECT * FROM interview_invalidation WHERE phone_num = '13800000001' OR extra_info = '北京海淀';");

        printTakeaways();
    }

    private static void printTakeaways() {
        System.out.println("---------------- 生产环境 SQL 调优金科玉律 ----------------");
        System.out.println("1. 严禁 SELECT *: 只查所需字段，尽量设计联合索引达成【覆盖索引 (Using index)】，避开高昂的随机回表 I/O；");
        System.out.println("2. 杜绝在索引列上运算: 永远在应用层计算好参数再传入数据库，禁止 WHERE DATE(time) = '...' 或 WHERE age + 1 = 20；");
        System.out.println("3. 严格类型匹配: Java 实体类类型必须与数据库字段严格一致，字符串字段绝对不能传数字，防隐式 CAST 失效！");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

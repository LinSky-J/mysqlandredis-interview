package com.jinlin.mysqlandredis.mysql.topic02;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 02: 数据库三大范式是什么？
 * 
 * 说明：数据表结构及初始化数据已移至 /sql/01_mysql_basics_schema.sql 统一由 DataGrip 预先创建维护，
 *       本 Java 类专注三大范式理论推演、规范化设计验证与反范式化权衡剖析。
 * 
 * 核心理论：
 * 1. 第一范式 (1NF)：属性不可分，列具有原子性。
 * 2. 第二范式 (2NF)：在 1NF 基础上，消除非主属性对复合主键的【部分函数依赖】。
 * 3. 第三范式 (3NF)：在 2NF 基础上，消除非主属性对主键的【传递函数依赖】(A -> B -> C)。
 * 4. 反范式化 (Denormalization)：用空间换时间、冗余快照、减少高并发下的关联 JOIN。
 */
public class ThreeNormalFormsExplanation {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 02】数据库三大范式 (1NF, 2NF, 3NF) 与反范式化实战演示");
        System.out.println("====================================================================");

        // 验证 3NF 规范化表结构设计 (员工表与部门表关联查询，消除传递依赖)
        String verify3nfQuery = "SELECT "
                + "  e.emp_id, "
                + "  e.emp_name, "
                + "  d.dept_name, "
                + "  d.location AS dept_location "
                + "FROM interview_3nf_employee e "
                + "INNER JOIN interview_3nf_department d ON e.dept_id = d.dept_id;";

        DbConnectionHelper.printQueryResults("验证 3NF 规范化表结构设计 (员工表与部门表关联查询)", verify3nfQuery);

        printSummary();
    }

    private static void printSummary() {
        System.out.println("------------- 三大范式精粹总结 -------------");
        System.out.println("1NF: 字段不可再分，每列都是原子项。");
        System.out.println("2NF: 消除非主键列对复合主键的【部分函数依赖】(非主键必须完全依赖所有主键列)。");
        System.out.println("3NF: 消除非主键列对主键的【传递函数依赖】(A -> B -> C，需将 B与C 拆为独立表)。");
        System.out.println("反范式思考: 商业项目中，严格范式化会导致过多 JOIN；在高并发场景常采用【适度反范式】(冗余字段/历史快照)，以空间换时间。");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

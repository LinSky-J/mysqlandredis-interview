package com.jinlin.mysqlandredis.mysql.topic02;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 02: 数据库三大范式是什么？
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

        // 1. 初始化 1NF 结构表
        String sql1nf = "DROP TABLE IF EXISTS interview_1nf_user;\n"
                + "CREATE TABLE interview_1nf_user (\n"
                + "    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,\n"
                + "    mobile VARCHAR(20) NOT NULL,\n"
                + "    email VARCHAR(100),\n"
                + "    province VARCHAR(50) NOT NULL,\n"
                + "    city VARCHAR(50) NOT NULL,\n"
                + "    detail_address VARCHAR(200) NOT NULL\n"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        // 2. 初始化 2NF 结构表 (拆分复合主键与明细)
        String sql2nfProduct = "DROP TABLE IF EXISTS interview_2nf_product;\n"
                + "CREATE TABLE interview_2nf_product (\n"
                + "    product_id BIGINT PRIMARY KEY AUTO_INCREMENT,\n"
                + "    product_name VARCHAR(100) NOT NULL,\n"
                + "    product_price DECIMAL(10, 2) NOT NULL\n"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String sql2nfOrderItem = "DROP TABLE IF EXISTS interview_2nf_order_item;\n"
                + "CREATE TABLE interview_2nf_order_item (\n"
                + "    order_id BIGINT NOT NULL,\n"
                + "    product_id BIGINT NOT NULL,\n"
                + "    quantity INT NOT NULL,\n"
                + "    PRIMARY KEY (order_id, product_id)\n"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        // 3. 初始化 3NF 结构表 (消除部门传递依赖)
        String sql3nfDept = "DROP TABLE IF EXISTS interview_3nf_department;\n"
                + "CREATE TABLE interview_3nf_department (\n"
                + "    dept_id BIGINT PRIMARY KEY AUTO_INCREMENT,\n"
                + "    dept_name VARCHAR(50) NOT NULL,\n"
                + "    dept_manager VARCHAR(50) NOT NULL\n"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String sql3nfEmp = "DROP TABLE IF EXISTS interview_3nf_employee;\n"
                + "CREATE TABLE interview_3nf_employee (\n"
                + "    emp_id BIGINT PRIMARY KEY AUTO_INCREMENT,\n"
                + "    emp_name VARCHAR(50) NOT NULL,\n"
                + "    dept_id BIGINT NOT NULL,\n"
                + "    INDEX idx_dept (dept_id)\n"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        // 执行建表与模拟数据插入
        String insertData = "INSERT INTO interview_1nf_user (mobile, email, province, city, detail_address) VALUES "
                + "('13800138000', 'alice@qq.com', '北京', '北京市', '海淀区中关村南大街1号');\n"
                + "INSERT INTO interview_2nf_product (product_name, product_price) VALUES ('机械键盘', 399.00), ('4K显示器', 1899.00);\n"
                + "INSERT INTO interview_2nf_order_item (order_id, product_id, quantity) VALUES (1001, 1, 2), (1001, 2, 1);\n"
                + "INSERT INTO interview_3nf_department (dept_name, dept_manager) VALUES ('基础架构部', '张总工'), ('大数据部', '李首席');\n"
                + "INSERT INTO interview_3nf_employee (emp_name, dept_id) VALUES ('工程师甲', 1), ('工程师乙', 2);";

        DbConnectionHelper.executeSqlScript(sql1nf, sql2nfProduct, sql2nfOrderItem, sql3nfDept, sql3nfEmp, insertData);

        // 验证 3NF 关联查询
        String verify3nfQuery = "SELECT "
                + "  e.emp_id, "
                + "  e.emp_name, "
                + "  d.dept_name, "
                + "  d.dept_manager "
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

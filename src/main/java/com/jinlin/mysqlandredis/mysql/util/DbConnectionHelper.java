package com.jinlin.mysqlandredis.mysql.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 数据库直连工具类，提供统一的真实 MySQL 数据库连接与表初始化执行支持
 */
public class DbConnectionHelper {

    private static final String URL = "jdbc:mysql://127.0.0.1:3306/interview_db?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASS = "wjl315698";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver 未加载", e);
        }
    }

    /**
     * 获取数据库连接
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    /**
     * 执行一段批量建表或更新的 SQL 脚本 (自动支持按分号拆分多条语句执行)
     */
    public static void executeSqlScript(String... sqlStatements) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            for (String block : sqlStatements) {
                if (block != null && !block.trim().isEmpty()) {
                    for (String sql : block.split(";")) {
                        String trimmed = sql.trim();
                        if (!trimmed.isEmpty()) {
                            stmt.execute(trimmed);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("执行 SQL 失败: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行查询并友好打印结果集表格
     */
    public static void printQueryResults(String queryTitle, String sql) {
        System.out.println("\n=======================================================");
        System.out.println("【" + queryTitle + "】");
        System.out.println("执行 SQL:\n" + sql.trim());
        System.out.println("-------------------------------------------------------");
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {
            java.sql.ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            // 打印列头
            StringBuilder header = new StringBuilder();
            for (int i = 1; i <= columnCount; i++) {
                header.append(String.format("%-22s", meta.getColumnLabel(i)));
            }
            System.out.println(header);
            System.out.println("-".repeat(Math.max(60, columnCount * 22)));

            // 打印行记录
            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
                StringBuilder row = new StringBuilder();
                for (int i = 1; i <= columnCount; i++) {
                    Object val = rs.getObject(i);
                    row.append(String.format("%-22s", val == null ? "NULL" : val.toString()));
                }
                System.out.println(row);
            }
            if (rowCount == 0) {
                System.out.println("(无返回记录)");
            }
            System.out.println("总记录数: " + rowCount);
        } catch (SQLException e) {
            System.err.println("查询执行失败: " + e.getMessage());
            throw new RuntimeException(e);
        }
        System.out.println("=======================================================\n");
    }
}

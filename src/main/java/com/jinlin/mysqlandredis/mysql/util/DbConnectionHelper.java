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
     * 执行一段批量建表或更新的 SQL 脚本
     */
    public static void executeSqlScript(String... sqlStatements) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : sqlStatements) {
                if (sql != null && !sql.trim().isEmpty()) {
                    stmt.execute(sql.trim());
                }
            }
        } catch (SQLException e) {
            System.err.println("执行 SQL 失败: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}

package com.jinlin.mysqlandredis.mysql.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.yaml.snakeyaml.Yaml;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

/**
 * 数据库连接工具类：
 * 严格从 application.yml 中动态解析读取数据源配置（url, username, password, driver-class-name 以及 hikari 连接池参数），
 * 彻底消除代码中数据库连接凭据的硬编码，统一由 application.yml 配置文件进行集中管控与维护。
 */
public class DbConnectionHelper {

    private static final HikariDataSource DATA_SOURCE;

    static {
        try (InputStream in = DbConnectionHelper.class.getClassLoader().getResourceAsStream("application.yml")) {
            if (in == null) {
                throw new IllegalStateException("未能从 classpath 中加载到 application.yml 配置文件！");
            }
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(in);
            Map<String, Object> spring = getNestedMap(root, "spring");
            Map<String, Object> datasource = getNestedMap(spring, "datasource");

            if (datasource == null) {
                throw new IllegalStateException("application.yml 中未找到 spring.datasource 配置节点！");
            }

            String url = (String) datasource.get("url");
            String username = (String) datasource.get("username");
            String password = String.valueOf(datasource.get("password"));
            String driver = (String) datasource.get("driver-class-name");

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);
            if (driver != null && !driver.trim().isEmpty()) {
                config.setDriverClassName(driver);
            }

            // 读取 yml 中的 HikariCP 连接池专属参数
            Map<String, Object> hikari = getNestedMap(datasource, "hikari");
            if (hikari != null) {
                if (hikari.containsKey("pool-name")) {
                    config.setPoolName(String.valueOf(hikari.get("pool-name")));
                }
                if (hikari.containsKey("maximum-pool-size")) {
                    config.setMaximumPoolSize(Integer.parseInt(hikari.get("maximum-pool-size").toString()));
                }
                if (hikari.containsKey("minimum-idle")) {
                    config.setMinimumIdle(Integer.parseInt(hikari.get("minimum-idle").toString()));
                }
                if (hikari.containsKey("connection-timeout")) {
                    config.setConnectionTimeout(Long.parseLong(hikari.get("connection-timeout").toString()));
                }
                if (hikari.containsKey("idle-timeout")) {
                    config.setIdleTimeout(Long.parseLong(hikari.get("idle-timeout").toString()));
                }
                if (hikari.containsKey("max-lifetime")) {
                    config.setMaxLifetime(Long.parseLong(hikari.get("max-lifetime").toString()));
                }
            }

            DATA_SOURCE = new HikariDataSource(config);
        } catch (Exception e) {
            throw new RuntimeException("初始化 application.yml 数据源配置失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getNestedMap(Map<String, Object> parent, String key) {
        if (parent == null || !parent.containsKey(key)) {
            return null;
        }
        Object obj = parent.get(key);
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return null;
    }

    /**
     * 获取由 application.yml 驱动初始化的 HikariCP 数据源
     */
    public static DataSource getDataSource() {
        return DATA_SOURCE;
    }

    /**
     * 从 HikariCP 连接池中获取数据库连接
     */
    public static Connection getConnection() throws SQLException {
        return DATA_SOURCE.getConnection();
    }

    /**
     * 执行一段批量更新或初始化的 SQL 脚本 (自动支持按分号拆分多条语句执行)
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
     * 执行查询并格式化输出结果集表格
     */
    public static void printQueryResults(String queryTitle, String sql) {
        System.out.println("\n=======================================================");
        System.out.println("【" + queryTitle + "】");
        System.out.println("执行 SQL:\n" + sql.trim());
        System.out.println("-------------------------------------------------------");
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            // 打印列头
            StringBuilder header = new StringBuilder();
            for (int i = 1; i <= columnCount; i++) {
                header.append(String.format("%-22s", meta.getColumnLabel(i)));
            }
            System.out.println(header);

            // 分隔线 (兼容 Java 8+)
            int lineLen = Math.max(60, columnCount * 22);
            StringBuilder separator = new StringBuilder();
            for (int i = 0; i < lineLen; i++) {
                separator.append("-");
            }
            System.out.println(separator);

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

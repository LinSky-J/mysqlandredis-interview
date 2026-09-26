package com.jinlin.mysqlandredis.redis.util;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.time.Duration;
import java.util.Map;

/**
 * Redis 连接工具类：
 * 动态加载 src/main/resources/application.yml 中的 Redis 数据源配置，
 * 避免在代码中硬编码任何主机、端口或密码信息。
 */
public class RedisConnectionHelper {

    private static final RedisClient redisClient;
    private static final StatefulRedisConnection<String, String> connection;
    private static final RedisCommands<String, String> syncCommands;

    static {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> configMap;
            try (InputStream in = RedisConnectionHelper.class.getClassLoader().getResourceAsStream("application.yml")) {
                if (in == null) {
                    throw new IllegalStateException("未能找到配置文件: application.yml");
                }
                configMap = yaml.load(in);
            }

            // 解析 spring.redis 配置
            Map<String, Object> springMap = (Map<String, Object>) configMap.get("spring");
            Map<String, Object> redisMap = (Map<String, Object>) springMap.get("redis");

            String host = redisMap.getOrDefault("host", "localhost").toString();
            int port = Integer.parseInt(redisMap.getOrDefault("port", 6379).toString());
            int database = Integer.parseInt(redisMap.getOrDefault("database", 0).toString());
            Object passwordObj = redisMap.get("password");
            String password = passwordObj != null ? passwordObj.toString().trim() : "";

            RedisURI.Builder uriBuilder = RedisURI.builder()
                    .withHost(host)
                    .withPort(port)
                    .withDatabase(database)
                    .withTimeout(Duration.ofSeconds(3));

            if (!password.isEmpty()) {
                uriBuilder.withPassword(password.toCharArray());
            }

            RedisURI redisURI = uriBuilder.build();
            redisClient = RedisClient.create(redisURI);
            connection = redisClient.connect();
            syncCommands = connection.sync();

            System.out.println(">> [RedisConnectionHelper] 成功连接至本机 Redis: " + host + ":" + port + ", 数据库: db" + database);
        } catch (Exception e) {
            System.err.println(">> [RedisConnectionHelper] 初始化 Redis 连接失败: " + e.getMessage());
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * 获取 Lettuce 同步命令操作接口
     */
    public static RedisCommands<String, String> getCommands() {
        return syncCommands;
    }

    /**
     * 获取底层有状态连接
     */
    public static StatefulRedisConnection<String, String> getConnection() {
        return connection;
    }

    /**
     * 获取 RedisClient 实例
     */
    public static RedisClient getClient() {
        return redisClient;
    }
}

package com.jinlin.mysqlandredis.redis.util;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * Redis 连接与操作工具类：
 * 1. 动态加载 src/main/resources/application.yml 中的 Redis 数据源配置；
 * 2. 构建 Spring Data Redis 核心组件：LettuceConnectionFactory、StringRedisTemplate、RedisTemplate<String, Object>；
 * 3. 统一使用 RedisTemplate / StringRedisTemplate 进行数据存取与底层原生命令回调探测。
 */
public class RedisConnectionHelper {

    private static final LettuceConnectionFactory connectionFactory;
    private static final StringRedisTemplate stringRedisTemplate;
    private static final RedisTemplate<String, Object> redisTemplate;

    // 保留原生 Lettuce 实例以支撑向下兼容
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

            // 1. 初始化 Spring Data Redis 的 LettuceConnectionFactory
            RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(host, port);
            redisConfig.setDatabase(database);
            if (!password.isEmpty()) {
                redisConfig.setPassword(RedisPassword.of(password));
            }

            LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                    .commandTimeout(Duration.ofSeconds(3))
                    .build();

            connectionFactory = new LettuceConnectionFactory(redisConfig, clientConfig);
            connectionFactory.afterPropertiesSet();

            // 2. 初始化 StringRedisTemplate (用于 Key-Value 均为字符串的场景)
            stringRedisTemplate = new StringRedisTemplate(connectionFactory);
            stringRedisTemplate.afterPropertiesSet();

            // 3. 初始化通用 RedisTemplate<String, Object> (Key 为 String，Value 为 JSON 序列化)
            redisTemplate = new RedisTemplate<>();
            redisTemplate.setConnectionFactory(connectionFactory);
            StringRedisSerializer stringSerializer = new StringRedisSerializer();
            GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
            redisTemplate.setKeySerializer(stringSerializer);
            redisTemplate.setHashKeySerializer(stringSerializer);
            redisTemplate.setValueSerializer(jsonSerializer);
            redisTemplate.setHashValueSerializer(jsonSerializer);
            redisTemplate.afterPropertiesSet();

            // 4. 原生 Lettuce 实例 (兼容保留)
            io.lettuce.core.RedisURI.Builder uriBuilder = io.lettuce.core.RedisURI.builder()
                    .withHost(host)
                    .withPort(port)
                    .withDatabase(database)
                    .withTimeout(Duration.ofSeconds(3));
            if (!password.isEmpty()) {
                uriBuilder.withPassword(password.toCharArray());
            }
            redisClient = RedisClient.create(uriBuilder.build());
            connection = redisClient.connect();
            syncCommands = connection.sync();

            System.out.println(">> [RedisConnectionHelper] 成功初始化 RedisTemplate 与 LettuceConnectionFactory (host=" + host + ", port=" + port + ", db=" + database + ")");
        } catch (Exception e) {
            System.err.println(">> [RedisConnectionHelper] 初始化 Redis 连接失败: " + e.getMessage());
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * 获取 Spring Data Redis 推荐的 StringRedisTemplate
     */
    public static StringRedisTemplate getStringRedisTemplate() {
        return stringRedisTemplate;
    }

    /**
     * 获取带有 JSON 序列化的通用 RedisTemplate<String, Object>
     */
    public static RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }

    /**
     * 获取底层 LettuceConnectionFactory
     */
    public static LettuceConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    /**
     * 使用 StringRedisTemplate 的底层 execute(RedisCallback) 执行原生 OBJECT ENCODING 指令
     */
    public static String getObjectEncoding(String key) {
        return stringRedisTemplate.execute((RedisCallback<String>) conn -> {
            Object rawResult = conn.execute("OBJECT", "ENCODING".getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8));
            if (rawResult == null) {
                return null;
            }
            if (rawResult instanceof byte[]) {
                return new String((byte[]) rawResult, StandardCharsets.UTF_8);
            }
            return rawResult.toString();
        });
    }

    /**
     * 保留旧版 Lettuce 同步命令接口 (兼容性用途)
     */
    @Deprecated
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

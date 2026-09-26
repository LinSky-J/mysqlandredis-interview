package com.jinlin.mysqlandredis.redis;

import com.jinlin.mysqlandredis.redis.persistence.RedisLog01_RdbVsAofPersistenceDemo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Redis 持久化与日志面试问题自动化执行与实机数据库验证测试套件
 */
@SpringBootTest
public class RedisPersistenceInterviewTest {

    @Test
    @DisplayName("01. Redis有哪2种持久化方式？分别的优缺点是什么? (RDB快照 vs AOF追加日志 vs 4.0+混合持久化)")
    void test01_RdbVsAofPersistence() {
        assertDoesNotThrow(RedisLog01_RdbVsAofPersistenceDemo::runDemo);
    }
}

package com.jinlin.mysqlandredis.redis;

import com.jinlin.mysqlandredis.redis.eviction.RedisEvict01_ExpireVsEvictDifferenceDemo;
import com.jinlin.mysqlandredis.redis.eviction.RedisEvict02_MemoryEvictionPoliciesDemo;
import com.jinlin.mysqlandredis.redis.eviction.RedisEvict03_ExpirationDeletionStrategiesDemo;
import com.jinlin.mysqlandredis.redis.eviction.RedisEvict04_WillExpiredKeyBeDeletedImmediatelyDemo;
import com.jinlin.mysqlandredis.redis.eviction.RedisEvict05_WhyNotDeleteImmediatelyDemo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Redis 缓存淘汰与过期删除 5 问自动化执行与实机数据库验证测试套件
 */
@SpringBootTest
public class RedisEvictionInterviewTest {

    @Test
    @DisplayName("01. 过期删除策略和内存淘汰策略有什么区别? (生命周期TTL vs 内存阈值maxmemory)")
    void test01_ExpireVsEvictDifference() {
        assertDoesNotThrow(RedisEvict01_ExpireVsEvictDifferenceDemo::runDemo);
    }

    @Test
    @DisplayName("02. 介绍一下Redis 内存淘汰策略 (8大淘汰策略与近似LRU/LFU原理)")
    void test02_MemoryEvictionPolicies() {
        assertDoesNotThrow(RedisEvict02_MemoryEvictionPoliciesDemo::runDemo);
    }

    @Test
    @DisplayName("03. 介绍一下Redis过期删除策略 (惰性删除 + 限时定期抽样删除组合拳)")
    void test03_ExpirationDeletionStrategies() {
        assertDoesNotThrow(RedisEvict03_ExpirationDeletionStrategiesDemo::runDemo);
    }

    @Test
    @DisplayName("04. Redis的缓存失效会不会立即删除? (绝对不会！三大释放时机剖析)")
    void test04_WillExpiredKeyBeDeletedImmediately() {
        assertDoesNotThrow(RedisEvict04_WillExpiredKeyBeDeletedImmediatelyDemo::runDemo);
    }

    @Test
    @DisplayName("05. 那为什么我不过期立即删除? (CPU算力开销、海量定时器数据结构与单线程模型)")
    void test05_WhyNotDeleteImmediately() {
        assertDoesNotThrow(RedisEvict05_WhyNotDeleteImmediatelyDemo::runDemo);
    }
}

package com.jinlin.mysqlandredis.redis;

import com.jinlin.mysqlandredis.redis.scenario.RedisScenario01_WhyRedisAndPerfComparisonDemo;
import com.jinlin.mysqlandredis.redis.scenario.RedisScenario02_BusinessScenariosAndConcurrencyDemo;
import com.jinlin.mysqlandredis.redis.scenario.RedisScenario03_DistributedLockPrinciplesDemo;
import com.jinlin.mysqlandredis.redis.scenario.RedisScenario04_BigKeyProblemAndSolutionsDemo;
import com.jinlin.mysqlandredis.redis.scenario.RedisScenario05_HotKeyProblemAndSolutionsDemo;
import com.jinlin.mysqlandredis.redis.scenario.RedisScenario06_CacheConsistencyWithMysqlDemo;
import com.jinlin.mysqlandredis.redis.scenario.RedisScenario07_AvalancheBreakdownPenetrationBloomDemo;
import com.jinlin.mysqlandredis.redis.scenario.RedisScenario08_SeckillArchitectureAndAntiOversellDemo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Redis 业务高并发实战与生产场景篇 8 大演示验证测试套件
 */
@SpringBootTest
public class RedisScenarioInterviewTest {

    @Test
    @DisplayName("01. 为什么使用Redis？为什么比MySQL快？本地缓存区别与单机并发量量化")
    void test01_WhyRedisAndPerfComparison() {
        assertDoesNotThrow(RedisScenario01_WhyRedisAndPerfComparisonDemo::runDemo);
    }

    @Test
    @DisplayName("02. Redis应用场景是什么？除了缓存还有哪些应用？支持并发操作吗？")
    void test02_BusinessScenariosAndConcurrency() {
        assertDoesNotThrow(RedisScenario02_BusinessScenariosAndConcurrencyDemo::runDemo);
    }

    @Test
    @DisplayName("03. Redis分布式锁的实现原理？什么场景下用到分布式锁？(SET NX EX、UUID防误删、看门狗)")
    void test03_DistributedLockPrinciples() {
        assertDoesNotThrow(RedisScenario03_DistributedLockPrinciplesDemo::runDemo);
    }

    @Test
    @DisplayName("04. Redis大Key问题是什么？缺点与危害？如何解决？(UNLINK异步删除与拆分)")
    void test04_BigKeyProblemAndSolutions() {
        assertDoesNotThrow(RedisScenario04_BigKeyProblemAndSolutionsDemo::runDemo);
    }

    @Test
    @DisplayName("05. 什么是热Key？如何解决热Key问题？(多级缓存Caffeine与随机后缀散列)")
    void test05_HotKeyProblemAndSolutions() {
        assertDoesNotThrow(RedisScenario05_HotKeyProblemAndSolutionsDemo::runDemo);
    }

    @Test
    @DisplayName("06. 如何保证Redis和MySQL数据缓存一致性问题？(Cache-Aside、延时双删与Canal Binlog)")
    void test06_CacheConsistencyWithMysql() {
        assertDoesNotThrow(RedisScenario06_CacheConsistencyWithMysqlDemo::runDemo);
    }

    @Test
    @DisplayName("07. 缓存雪崩、击穿、穿透是什么？怎么解决？布隆过滤器原理介绍一下")
    void test07_AvalancheBreakdownPenetrationBloom() {
        assertDoesNotThrow(RedisScenario07_AvalancheBreakdownPenetrationBloomDemo::runDemo);
    }

    @Test
    @DisplayName("08. 如何设计秒杀场景处理高并发以及超卖现象？(多级漏斗、Lua原子预扣减与防超卖)")
    void test08_SeckillArchitectureAndAntiOversell() {
        assertDoesNotThrow(RedisScenario08_SeckillArchitectureAndAntiOversellDemo::runDemo);
    }
}

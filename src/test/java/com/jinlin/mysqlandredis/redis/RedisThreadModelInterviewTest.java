package com.jinlin.mysqlandredis.redis;

import com.jinlin.mysqlandredis.redis.threadmodel.RedisTm01_WhyRedisIsFastDemo;
import com.jinlin.mysqlandredis.redis.threadmodel.RedisTm02_MultiThreadingInRedisDemo;
import com.jinlin.mysqlandredis.redis.threadmodel.RedisTm03_IoMultiplexingEpollDemo;
import com.jinlin.mysqlandredis.redis.threadmodel.RedisTm04_NetworkModelArchitectureDemo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Redis 线程模型篇 4 问自动化执行与实机数据库验证测试套件
 */
@SpringBootTest
public class RedisThreadModelInterviewTest {

    @Test
    @DisplayName("01. Redis 为什么快？(纯内存/单线程/非阻塞多路复用/高效数据结构深度剖析)")
    void test01_WhyRedisIsFast() {
        assertDoesNotThrow(RedisTm01_WhyRedisIsFastDemo::runDemo);
    }

    @Test
    @DisplayName("02. Redis 哪些地方使用了多线程？(Redis 4.0 BIO 异步机制与 Redis 6.0 多线程 I/O 深度解析)")
    void test02_MultiThreadingInRedis() {
        assertDoesNotThrow(RedisTm02_MultiThreadingInRedisDemo::runDemo);
    }

    @Test
    @DisplayName("03. Redis 怎么实现的 I/O 多路复用？(ae 事件库/epoll底层机制/select vs epoll对比剖析)")
    void test03_IoMultiplexingEpoll() {
        assertDoesNotThrow(RedisTm03_IoMultiplexingEpollDemo::runDemo);
    }

    @Test
    @DisplayName("04. Redis 的网络模型是怎样的？(Reactor 单反应堆模型/文件事件分派/完整生命周期流程)")
    void test04_NetworkModelArchitecture() {
        assertDoesNotThrow(RedisTm04_NetworkModelArchitectureDemo::runDemo);
    }
}

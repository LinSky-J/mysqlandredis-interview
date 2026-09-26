package com.jinlin.mysqlandredis.redis;

import com.jinlin.mysqlandredis.redis.transaction.RedisTx01_HowToAchieveAtomicityDemo;
import com.jinlin.mysqlandredis.redis.transaction.RedisTx02_AtomicityWithoutLuaDemo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Redis 事务与原子性 2 问自动化执行与实机数据库验证测试套件
 */
@SpringBootTest
public class RedisTransactionInterviewTest {

    @Test
    @DisplayName("01. 如何实现 Redis 原子性？(单命令/Lua脚本原子性与 MULTI/EXEC 弱原子性局限剖析)")
    void test01_HowToAchieveAtomicity() {
        assertDoesNotThrow(RedisTx01_HowToAchieveAtomicityDemo::runDemo);
    }

    @Test
    @DisplayName("02. 除了 Lua 有没有什么也能保证 Redis 的原子性？(WATCH乐观锁/原生复合原子指令/Functions/分布式锁)")
    void test02_AtomicityWithoutLua() {
        assertDoesNotThrow(RedisTx02_AtomicityWithoutLuaDemo::runDemo);
    }
}

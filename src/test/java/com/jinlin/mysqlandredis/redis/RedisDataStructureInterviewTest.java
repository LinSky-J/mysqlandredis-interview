package com.jinlin.mysqlandredis.redis;

import com.jinlin.mysqlandredis.redis.datastructure.RedisDs01_DataStructuresOverviewDemo;
import com.jinlin.mysqlandredis.redis.datastructure.RedisDs02_ZSetBusinessScenariosDemo;
import com.jinlin.mysqlandredis.redis.datastructure.RedisDs03_SetVsZSetDifferencesDemo;
import com.jinlin.mysqlandredis.redis.datastructure.RedisDs04_ZSetUnderlyingImplementationDemo;
import com.jinlin.mysqlandredis.redis.datastructure.RedisDs05_SkiplistStructureAndSearchDemo;
import com.jinlin.mysqlandredis.redis.datastructure.RedisDs06_SkiplistRandomLevelAlgorithmDemo;
import com.jinlin.mysqlandredis.redis.datastructure.RedisDs07_WhySkiplistOverBPlusTreeDemo;
import com.jinlin.mysqlandredis.redis.datastructure.RedisDs08_ZiplistStructureAndCascadeUpdateDemo;
import com.jinlin.mysqlandredis.redis.datastructure.RedisDs09_ListpackStructureAndImprovementDemo;
import com.jinlin.mysqlandredis.redis.datastructure.RedisDs10_DictRehashExpansionDemo;
import com.jinlin.mysqlandredis.redis.datastructure.RedisDs11_DictProgressiveRehashQueryDemo;
import com.jinlin.mysqlandredis.redis.datastructure.RedisDs12_SdsVsCStringDemo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Redis 数据结构篇 12 问全套自动化执行与实机数据库验证测试套件
 */
@SpringBootTest
public class RedisDataStructureInterviewTest {

    @Test
    @DisplayName("01. 讲一下 Redis 底层的数据结构？(八大底层结构与 redisObject 对象系统核验)")
    void test01_DataStructuresOverview() {
        assertDoesNotThrow(RedisDs01_DataStructuresOverviewDemo::runDemo);
    }

    @Test
    @DisplayName("02. ZSet 用过吗？(排行榜/延迟任务队列/滑动窗口三大经典业务实操)")
    void test02_ZSetBusinessScenarios() {
        assertDoesNotThrow(RedisDs02_ZSetBusinessScenariosDemo::runDemo);
    }

    @Test
    @DisplayName("03. Redis 中 Set 和 ZSet 区别是什么？(有序性/底层编码/集合运算全景实测)")
    void test03_SetVsZSetDifferences() {
        assertDoesNotThrow(RedisDs03_SetVsZSetDifferencesDemo::runDemo);
    }

    @Test
    @DisplayName("04. ZSet 底层是怎么实现的？(ziplist/listpack 与 dict+skiplist 双底层及跃迁机制)")
    void test04_ZSetUnderlyingImplementation() {
        assertDoesNotThrow(RedisDs04_ZSetUnderlyingImplementationDemo::runDemo);
    }

    @Test
    @DisplayName("05. 跳表是怎么实现的？(多级稀疏索引/Forward指针/Span跨度排名计算)")
    void test05_SkiplistStructureAndSearch() {
        assertDoesNotThrow(RedisDs05_SkiplistStructureAndSearchDemo::runDemo);
    }

    @Test
    @DisplayName("06. 跳表是怎么设置层高的？(zslRandomLevel 随机层高算法与 p=0.25 幂次定律模拟)")
    void test06_SkiplistRandomLevelAlgorithm() {
        assertDoesNotThrow(RedisDs06_SkiplistRandomLevelAlgorithmDemo::runDemo);
    }

    @Test
    @DisplayName("07. Redis 为什么使用跳表而不是用 B+ 树？(纯内存/范围查询/指针开销选型终极评析)")
    void test07_WhySkiplistOverBPlusTree() {
        assertDoesNotThrow(RedisDs07_WhySkiplistOverBPlusTreeDemo::runDemo);
    }

    @Test
    @DisplayName("08. 压缩列表是怎么实现的？(连续内存物理布局与连锁更新 Cascade Update 雪崩缺陷)")
    void test08_ZiplistStructureAndCascadeUpdate() {
        assertDoesNotThrow(RedisDs08_ZiplistStructureAndCascadeUpdateDemo::runDemo);
    }

    @Test
    @DisplayName("09. 介绍一下 Redis 中的 listpack？(backlen 自长设计彻底终结连锁更新)")
    void test09_ListpackStructureAndImprovement() {
        assertDoesNotThrow(RedisDs09_ListpackStructureAndImprovementDemo::runDemo);
    }

    @Test
    @DisplayName("10. 哈希表是怎么扩容的？(dictht 双表/负载因子阈值/COW 写时复制保护)")
    void test10_DictRehashExpansion() {
        assertDoesNotThrow(RedisDs10_DictRehashExpansionDemo::runDemo);
    }

    @Test
    @DisplayName("11. 哈希表扩容的时候，有读请求怎么查？(双表路由/只读优先/写请求绝对进新表)")
    void test11_DictProgressiveRehashQuery() {
        assertDoesNotThrow(RedisDs11_DictProgressiveRehashQueryDemo::runDemo);
    }

    @Test
    @DisplayName("12. String 是使用什么存储的？为什么不用 C 语言中的字符串？(SDS 结构/二进制安全/预分配)")
    void test12_SdsVsCString() {
        assertDoesNotThrow(RedisDs12_SdsVsCStringDemo::runDemo);
    }
}

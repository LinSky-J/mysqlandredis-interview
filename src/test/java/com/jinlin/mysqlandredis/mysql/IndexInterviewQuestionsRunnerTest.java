package com.jinlin.mysqlandredis.mysql;

import com.jinlin.mysqlandredis.mysql.index.BPlusTreeInternalsDemo;
import com.jinlin.mysqlandredis.mysql.index.ClusteredIndexAndPkDemo;
import com.jinlin.mysqlandredis.mysql.index.CompositeIndexAndIcpDemo;
import com.jinlin.mysqlandredis.mysql.index.IndexBasicsAndTypesDemo;
import com.jinlin.mysqlandredis.mysql.index.IndexInvalidationAndCoveringDemo;
import com.jinlin.mysqlandredis.mysql.index.PrefixIndexAndOptimizationDemo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * 索引模块全量核心问题自动化执行与真实 MySQL 实机检验测试套件
 */
@SpringBootTest
public class IndexInterviewQuestionsRunnerTest {

    @Test
    @DisplayName("01. 索引本质定义、分类全解与哈希索引验证")
    void test01_IndexBasicsAndTypes() {
        assertDoesNotThrow(IndexBasicsAndTypesDemo::runDemo);
    }

    @Test
    @DisplayName("02. 聚簇索引机制、主键选型 (自增 ID vs UUID) 与性别索引实测")
    void test02_ClusteredIndexAndPk() {
        assertDoesNotThrow(ClusteredIndexAndPkDemo::runDemo);
    }

    @Test
    @DisplayName("03. B+ 树底层实现、数据页内部二分查找、跳表对比实测")
    void test03_BPlusTreeInternals() {
        assertDoesNotThrow(BPlusTreeInternalsDemo::runDemo);
    }

    @Test
    @DisplayName("04. 联合索引最左匹配原则、索引下推 (ICP) 验证")
    void test04_CompositeIndexAndIcp() {
        assertDoesNotThrow(CompositeIndexAndIcpDemo::runDemo);
    }

    @Test
    @DisplayName("05. 索引失效 8 大场景、回表查询与覆盖索引实测")
    void test05_IndexInvalidationAndCovering() {
        assertDoesNotThrow(IndexInvalidationAndCoveringDemo::runDemo);
    }

    @Test
    @DisplayName("06. 索引优化方法论、低基数字段与前缀索引实测")
    void test06_PrefixIndexAndOptimization() {
        assertDoesNotThrow(PrefixIndexAndOptimizationDemo::runDemo);
    }
}

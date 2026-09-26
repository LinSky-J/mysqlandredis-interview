package com.jinlin.mysqlandredis.mysql;

import com.jinlin.mysqlandredis.mysql.tuning.Tuning01_ExplainRoleAndFieldsDemo;
import com.jinlin.mysqlandredis.mysql.tuning.Tuning02_CheckIndexUsageDemo;
import com.jinlin.mysqlandredis.mysql.tuning.Tuning03_ShowIndexStructureDemo;
import com.jinlin.mysqlandredis.mysql.tuning.Tuning04_SlowQuerySolutionsGuideDemo;
import com.jinlin.mysqlandredis.mysql.tuning.Tuning05_InterveneIndexSelectionDemo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * MySQL 性能调优核心考点 5 问自动化执行与实机数据库验证测试套件
 */
@SpringBootTest
public class TuningInterviewQuestionsRunnerTest {

    @Test
    @DisplayName("01. MySQL 的 EXPLAIN 有什么作用？(执行计划核心字段实机演练)")
    void test01_ExplainRoleAndFields() {
        assertDoesNotThrow(Tuning01_ExplainRoleAndFieldsDemo::runDemo);
    }

    @Test
    @DisplayName("02. 怎么查看是否有走索引？(type/key/key_len/Extra 走索引与失效判定实测)")
    void test02_CheckIndexUsage() {
        assertDoesNotThrow(Tuning02_CheckIndexUsageDemo::runDemo);
    }

    @Test
    @DisplayName("03. 怎么查看表的索引？(SHOW INDEX 命令与各字段含义精析)")
    void test03_ShowIndexStructure() {
        assertDoesNotThrow(Tuning03_ShowIndexStructureDemo::runDemo);
    }

    @Test
    @DisplayName("04. 给你张表发现查询速度很慢，有哪些解决方案？(慢查询端到端排查 7 步法实战)")
    void test04_SlowQuerySolutionsGuide() {
        assertDoesNotThrow(Tuning04_SlowQuerySolutionsGuideDemo::runDemo);
    }

    @Test
    @DisplayName("05. 如果 EXPLAIN 用到的索引不正确，有什么办法干预？(FORCE/USE/IGNORE/ANALYZE TABLE 调优实战)")
    void test05_InterveneIndexSelection() {
        assertDoesNotThrow(Tuning05_InterveneIndexSelectionDemo::runDemo);
    }
}

package com.jinlin.mysqlandredis.mysql;

import com.jinlin.mysqlandredis.mysql.topic01.Topic01_NoSqlVsSqlExplanation;
import com.jinlin.mysqlandredis.mysql.topic02.Topic02_ThreeNormalFormsExplanation;
import com.jinlin.mysqlandredis.mysql.topic03.Topic03_TableJoinsDemo;
import com.jinlin.mysqlandredis.mysql.topic04.Topic04_AvoidDuplicateInsertsDemo;
import com.jinlin.mysqlandredis.mysql.topic05.Topic05_CharVsVarcharDemo;
import com.jinlin.mysqlandredis.mysql.topic06.Topic06_IntDisplayWidthDemo;
import com.jinlin.mysqlandredis.mysql.topic07.Topic07_TextDataTypeDemo;
import com.jinlin.mysqlandredis.mysql.topic08.Topic08_IpStorageDemo;
import com.jinlin.mysqlandredis.mysql.topic09.Topic09_ForeignKeyConstraintsDemo;
import com.jinlin.mysqlandredis.mysql.topic10.Topic10_InVsExistsDemo;
import com.jinlin.mysqlandredis.mysql.topic11.Topic11_BasicFunctionsDemo;
import com.jinlin.mysqlandredis.mysql.topic12.Topic12_SqlExecutionOrderExplanation;
import com.jinlin.mysqlandredis.mysql.topic13.Topic13_CourseSelectionFilterDemo;
import com.jinlin.mysqlandredis.mysql.topic14.Topic14_StudentScoreRankingDemo;
import com.jinlin.mysqlandredis.mysql.topic15.Topic15_ClassStudentCoursesDemo;
import com.jinlin.mysqlandredis.mysql.topic16.Topic16_MySqlReentrantLockDemo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * MySQL 面试核心考点 16 题统一执行与实机真实数据库验证测试套件
 */
@SpringBootTest
public class MysqlInterviewQuestionsRunnerTest {

    @Test
    @DisplayName("01. NOSQL和SQL的区别验证")
    void testTopic01_NoSqlVsSql() {
        assertDoesNotThrow(Topic01_NoSqlVsSqlExplanation::runDemo);
    }

    @Test
    @DisplayName("02. 数据库三大范式验证")
    void testTopic02_ThreeNormalForms() {
        assertDoesNotThrow(Topic02_ThreeNormalFormsExplanation::runDemo);
    }

    @Test
    @DisplayName("03. MySQL 连表查询验证")
    void testTopic03_TableJoins() {
        assertDoesNotThrow(Topic03_TableJoinsDemo::runDemo);
    }

    @Test
    @DisplayName("04. 避免重复插入数据方案验证")
    void testTopic04_AvoidDuplicateInserts() {
        assertDoesNotThrow(Topic04_AvoidDuplicateInsertsDemo::runDemo);
    }

    @Test
    @DisplayName("05. CHAR 与 VARCHAR 区别验证")
    void testTopic05_CharVsVarchar() {
        assertDoesNotThrow(Topic05_CharVsVarcharDemo::runDemo);
    }

    @Test
    @DisplayName("06. int(1) 与 int(10) 显示宽度验证")
    void testTopic06_IntDisplayWidth() {
        assertDoesNotThrow(Topic06_IntDisplayWidthDemo::runDemo);
    }

    @Test
    @DisplayName("07. Text 数据类型容量与溢出机制验证")
    void testTopic07_TextDataType() {
        assertDoesNotThrow(Topic07_TextDataTypeDemo::runDemo);
    }

    @Test
    @DisplayName("08. IP 地址高效存储与网段检索验证")
    void testTopic08_IpStorage() {
        assertDoesNotThrow(Topic08_IpStorageDemo::runDemo);
    }

    @Test
    @DisplayName("09. 外键约束与级联操作验证")
    void testTopic09_ForeignKeyConstraints() {
        assertDoesNotThrow(Topic09_ForeignKeyConstraintsDemo::runDemo);
    }

    @Test
    @DisplayName("10. IN 与 EXISTS 执行机制与 NULL 陷阱验证")
    void testTopic10_InVsExists() {
        assertDoesNotThrow(Topic10_InVsExistsDemo::runDemo);
    }

    @Test
    @DisplayName("11. MySQL 常用函数分类实测验证")
    void testTopic11_BasicFunctions() {
        assertDoesNotThrow(Topic11_BasicFunctionsDemo::runDemo);
    }

    @Test
    @DisplayName("12. SQL 查询语句执行顺序验证")
    void testTopic12_SqlExecutionOrder() {
        assertDoesNotThrow(Topic12_SqlExecutionOrderExplanation::runDemo);
    }

    @Test
    @DisplayName("13. SQL题: 不存在01课程但存在02课程的学生成绩")
    void testTopic13_CourseSelectionFilter() {
        assertDoesNotThrow(Topic13_CourseSelectionFilterDemo::runDemo);
    }

    @Test
    @DisplayName("14. SQL题: 总分排名 5-10 名学生查询")
    void testTopic14_StudentScoreRanking() {
        assertDoesNotThrow(Topic14_StudentScoreRankingDemo::runDemo);
    }

    @Test
    @DisplayName("15. SQL题: 查班级下所有学生选课情况 (LEFT JOIN)")
    void testTopic15_ClassStudentCourses() {
        assertDoesNotThrow(Topic15_ClassStudentCoursesDemo::runDemo);
    }

    @Test
    @DisplayName("16. MySQL 实现可重入锁并发与重入测试")
    void testTopic16_MySqlReentrantLock() {
        assertDoesNotThrow(Topic16_MySqlReentrantLockDemo::runDemo);
    }
}

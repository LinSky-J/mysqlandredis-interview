package com.jinlin.mysqlandredis.mysql;

import com.jinlin.mysqlandredis.mysql.topic01.NoSqlVsSqlExplanation;
import com.jinlin.mysqlandredis.mysql.topic02.ThreeNormalFormsExplanation;
import com.jinlin.mysqlandredis.mysql.topic03.TableJoinsDemo;
import com.jinlin.mysqlandredis.mysql.topic04.AvoidDuplicateInsertsDemo;
import com.jinlin.mysqlandredis.mysql.topic05.CharVsVarcharDemo;
import com.jinlin.mysqlandredis.mysql.topic06.IntDisplayWidthDemo;
import com.jinlin.mysqlandredis.mysql.topic07.TextDataTypeDemo;
import com.jinlin.mysqlandredis.mysql.topic08.IpStorageDemo;
import com.jinlin.mysqlandredis.mysql.topic09.ForeignKeyConstraintsDemo;
import com.jinlin.mysqlandredis.mysql.topic10.InVsExistsDemo;
import com.jinlin.mysqlandredis.mysql.topic11.BasicFunctionsDemo;
import com.jinlin.mysqlandredis.mysql.topic12.SqlExecutionOrderExplanation;
import com.jinlin.mysqlandredis.mysql.topic13.CourseSelectionFilterDemo;
import com.jinlin.mysqlandredis.mysql.topic14.StudentScoreRankingDemo;
import com.jinlin.mysqlandredis.mysql.topic15.ClassStudentCoursesDemo;
import com.jinlin.mysqlandredis.mysql.topic16.MySqlReentrantLockDemo;
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
        assertDoesNotThrow(NoSqlVsSqlExplanation::runDemo);
    }

    @Test
    @DisplayName("02. 数据库三大范式验证")
    void testTopic02_ThreeNormalForms() {
        assertDoesNotThrow(ThreeNormalFormsExplanation::runDemo);
    }

    @Test
    @DisplayName("03. MySQL 连表查询验证")
    void testTopic03_TableJoins() {
        assertDoesNotThrow(TableJoinsDemo::runDemo);
    }

    @Test
    @DisplayName("04. 避免重复插入数据方案验证")
    void testTopic04_AvoidDuplicateInserts() {
        assertDoesNotThrow(AvoidDuplicateInsertsDemo::runDemo);
    }

    @Test
    @DisplayName("05. CHAR 与 VARCHAR 区别验证")
    void testTopic05_CharVsVarchar() {
        assertDoesNotThrow(CharVsVarcharDemo::runDemo);
    }

    @Test
    @DisplayName("06. int(1) 与 int(10) 显示宽度验证")
    void testTopic06_IntDisplayWidth() {
        assertDoesNotThrow(IntDisplayWidthDemo::runDemo);
    }

    @Test
    @DisplayName("07. Text 数据类型容量与溢出机制验证")
    void testTopic07_TextDataType() {
        assertDoesNotThrow(TextDataTypeDemo::runDemo);
    }

    @Test
    @DisplayName("08. IP 地址高效存储与网段检索验证")
    void testTopic08_IpStorage() {
        assertDoesNotThrow(IpStorageDemo::runDemo);
    }

    @Test
    @DisplayName("09. 外键约束与级联操作验证")
    void testTopic09_ForeignKeyConstraints() {
        assertDoesNotThrow(ForeignKeyConstraintsDemo::runDemo);
    }

    @Test
    @DisplayName("10. IN 与 EXISTS 执行机制与 NULL 陷阱验证")
    void testTopic10_InVsExists() {
        assertDoesNotThrow(InVsExistsDemo::runDemo);
    }

    @Test
    @DisplayName("11. MySQL 常用函数分类实测验证")
    void testTopic11_BasicFunctions() {
        assertDoesNotThrow(BasicFunctionsDemo::runDemo);
    }

    @Test
    @DisplayName("12. SQL 查询语句执行顺序验证")
    void testTopic12_SqlExecutionOrder() {
        assertDoesNotThrow(SqlExecutionOrderExplanation::runDemo);
    }

    @Test
    @DisplayName("13. SQL题: 不存在01课程但存在02课程的学生成绩")
    void testTopic13_CourseSelectionFilter() {
        assertDoesNotThrow(CourseSelectionFilterDemo::runDemo);
    }

    @Test
    @DisplayName("14. SQL题: 总分排名 5-10 名学生查询")
    void testTopic14_StudentScoreRanking() {
        assertDoesNotThrow(StudentScoreRankingDemo::runDemo);
    }

    @Test
    @DisplayName("15. SQL题: 查班级下所有学生选课情况 (LEFT JOIN)")
    void testTopic15_ClassStudentCourses() {
        assertDoesNotThrow(ClassStudentCoursesDemo::runDemo);
    }

    @Test
    @DisplayName("16. MySQL 实现可重入锁并发与重入测试")
    void testTopic16_MySqlReentrantLock() {
        assertDoesNotThrow(MySqlReentrantLockDemo::runDemo);
    }
}

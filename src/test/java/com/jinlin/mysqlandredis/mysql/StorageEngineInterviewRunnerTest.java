package com.jinlin.mysqlandredis.mysql;

import com.jinlin.mysqlandredis.mysql.storage.DatabaseFilesArchitectureDemo;
import com.jinlin.mysqlandredis.mysql.storage.InnodbVsMyisamDemo;
import com.jinlin.mysqlandredis.mysql.storage.MysqlEnginesOverviewDemo;
import com.jinlin.mysqlandredis.mysql.storage.SqlExecutionLifecycleDemo;
import com.jinlin.mysqlandredis.mysql.storage.WhyInnodbDefaultDemo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * 存储引擎核心 5 问自动化执行与实机验证测试套件
 */
@SpringBootTest
public class StorageEngineInterviewRunnerTest {

    @Test
    @DisplayName("01. 执行一条 SQL 请求的全生命周期与两阶段提交验证")
    void test01_SqlExecutionLifecycle() {
        assertDoesNotThrow(SqlExecutionLifecycleDemo::runDemo);
    }

    @Test
    @DisplayName("02. MySQL 常见存储引擎全景剖析实测")
    void test02_MysqlEnginesOverview() {
        assertDoesNotThrow(MysqlEnginesOverviewDemo::runDemo);
    }

    @Test
    @DisplayName("03. MySQL 为什么 InnoDB 是默认引擎验证")
    void test03_WhyInnodbDefault() {
        assertDoesNotThrow(WhyInnodbDefaultDemo::runDemo);
    }

    @Test
    @DisplayName("04. InnoDB 与 MyISAM 核心技术差异实测")
    void test04_InnodbVsMyisam() {
        assertDoesNotThrow(InnodbVsMyisamDemo::runDemo);
    }

    @Test
    @DisplayName("05. 数据管理物理文件架构与巡检验证")
    void test05_DatabaseFilesArchitecture() {
        assertDoesNotThrow(DatabaseFilesArchitectureDemo::runDemo);
    }
}

package com.jinlin.mysqlandredis.mysql;

import com.jinlin.mysqlandredis.mysql.architecture.Arch01_MasterSlaveReplicationDemo;
import com.jinlin.mysqlandredis.mysql.architecture.Arch02_ReplicationLagSolutionsDemo;
import com.jinlin.mysqlandredis.mysql.architecture.Arch03_ShardingDatabaseAndTableDemo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * MySQL 架构篇高频考点 3 问自动化执行与实机数据库验证测试套件
 */
@SpringBootTest
public class ArchitectureInterviewRunnerTest {

    @Test
    @DisplayName("01. MySQL 主从复制了解吗？(三大线程与复制模式架构剖析)")
    void test01_MasterSlaveReplication() {
        assertDoesNotThrow(Arch01_MasterSlaveReplicationDemo::runDemo);
    }

    @Test
    @DisplayName("02. 主从延迟都有什么处理方法？(MTS/强制读主/大事务规范全套方案实测)")
    void test02_ReplicationLagSolutions() {
        assertDoesNotThrow(Arch02_ReplicationLagSolutionsDemo::runDemo);
    }

    @Test
    @DisplayName("03. 分表和分库是什么？有什么区别？(垂直/水平分片与分布式挑战对比)")
    void test03_ShardingDatabaseAndTable() {
        assertDoesNotThrow(Arch03_ShardingDatabaseAndTableDemo::runDemo);
    }
}

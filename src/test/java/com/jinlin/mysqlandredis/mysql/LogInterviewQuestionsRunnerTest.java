package com.jinlin.mysqlandredis.mysql;

import com.jinlin.mysqlandredis.mysql.log.Log01_MysqlLogTypesOverviewDemo;
import com.jinlin.mysqlandredis.mysql.log.Log02_BinlogDeepDiveDemo;
import com.jinlin.mysqlandredis.mysql.log.Log03_UndoLogFunctionDemo;
import com.jinlin.mysqlandredis.mysql.log.Log04_WhyBothUndoAndRedoDemo;
import com.jinlin.mysqlandredis.mysql.log.Log05_RedoLogDurabilityWalDemo;
import com.jinlin.mysqlandredis.mysql.log.Log06_WhyNotBinlogOnlyCrashSafeDemo;
import com.jinlin.mysqlandredis.mysql.log.Log07_TwoPhaseCommit2PcDemo;
import com.jinlin.mysqlandredis.mysql.log.Log08_UpdateExecutionLifecycleDemo;
import com.jinlin.mysqlandredis.mysql.log.Log09_DataLossPreventionDouble1Demo;
import com.jinlin.mysqlandredis.mysql.log.Log10_RedoLogInMemAndDiskDemo;
import com.jinlin.mysqlandredis.mysql.log.Log11_WhyRedoLogOverRandomIoDemo;
import com.jinlin.mysqlandredis.mysql.log.Log12_DoubleWriteBufferDemo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * MySQL 日志体系核心考点 12 问自动化执行与实机数据库验证测试套件
 */
@SpringBootTest
public class LogInterviewQuestionsRunnerTest {

    @Test
    @DisplayName("01. 日志文件是分成了哪几种？(7 大日志多维度对比实测)")
    void test01_MysqlLogTypesOverview() {
        assertDoesNotThrow(Log01_MysqlLogTypesOverviewDemo::runDemo);
    }

    @Test
    @DisplayName("02. 讲一下 Binlog (三种格式与刷盘策略深度核验)")
    void test02_BinlogDeepDive() {
        assertDoesNotThrow(Log02_BinlogDeepDiveDemo::runDemo);
    }

    @Test
    @DisplayName("03. UndoLog 日志的作用是什么？(原子回滚与 MVCC 版本链实测)")
    void test03_UndoLogFunction() {
        assertDoesNotThrow(Log03_UndoLogFunctionDemo::runDemo);
    }

    @Test
    @DisplayName("04. 有了 UndoLog 为啥还需要 RedoLog 呢？(崩溃恢复闭环剖析)")
    void test04_WhyBothUndoAndRedo() {
        assertDoesNotThrow(Log04_WhyBothUndoAndRedoDemo::runDemo);
    }

    @Test
    @DisplayName("05. RedoLog 怎么保证持久性的？(WAL 与三大刷盘模式核验)")
    void test05_RedoLogDurabilityWal() {
        assertDoesNotThrow(Log05_RedoLogDurabilityWalDemo::runDemo);
    }

    @Test
    @DisplayName("06. 能不能只用 Binlog 不用 RedoLog？(Crash-Safe 根本缺失分析)")
    void test06_WhyNotBinlogOnlyCrashSafe() {
        assertDoesNotThrow(Log06_WhyNotBinlogOnlyCrashSafeDemo::runDemo);
    }

    @Test
    @DisplayName("07. Binlog 两阶段提交过程是怎样的？(2PC 崩溃决策矩阵分析)")
    void test07_TwoPhaseCommit2Pc() {
        assertDoesNotThrow(Log07_TwoPhaseCommit2PcDemo::runDemo);
    }

    @Test
    @DisplayName("08. UPDATE 语句的具体执行过程是怎样的？(8 大全链路实操剖析)")
    void test08_UpdateExecutionLifecycle() {
        assertDoesNotThrow(Log08_UpdateExecutionLifecycleDemo::runDemo);
    }

    @Test
    @DisplayName("09. MySQL 是如何保障数据不丢失的？(双 1 配置与四重铁壁防线)")
    void test09_DataLossPreventionDouble1() {
        assertDoesNotThrow(Log09_DataLossPreventionDouble1Demo::runDemo);
    }

    @Test
    @DisplayName("10. RedoLog 是在内存里吗？(内存 Buffer 与磁盘文件组架构)")
    void test10_RedoLogInMemAndDisk() {
        assertDoesNotThrow(Log10_RedoLogInMemAndDiskDemo::runDemo);
    }

    @Test
    @DisplayName("11. 为什么要写 RedoLog，而不是直接写到 B+ 树里面？(顺序 I/O 降维优势)")
    void test11_WhyRedoLogOverRandomIo() {
        assertDoesNotThrow(Log11_WhyRedoLogOverRandomIoDemo::runDemo);
    }

    @Test
    @DisplayName("12. MySQL 两次写 (Doublewrite Buffer) 了解吗？(页断裂物理防御机制)")
    void test12_DoubleWriteBuffer() {
        assertDoesNotThrow(Log12_DoubleWriteBufferDemo::runDemo);
    }
}

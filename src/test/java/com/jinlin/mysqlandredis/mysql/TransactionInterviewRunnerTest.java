package com.jinlin.mysqlandredis.mysql;

import com.jinlin.mysqlandredis.mysql.transaction.Tx01_AcidCharacteristicsDemo;
import com.jinlin.mysqlandredis.mysql.transaction.Tx02_ConcurrencyAnomaliesDemo;
import com.jinlin.mysqlandredis.mysql.transaction.Tx03_DirtyReadScenariosDemo;
import com.jinlin.mysqlandredis.mysql.transaction.Tx04_ConcurrencyControlMechanismDemo;
import com.jinlin.mysqlandredis.mysql.transaction.Tx05_IsolationLevelsAndDefaultDemo;
import com.jinlin.mysqlandredis.mysql.transaction.Tx06_RrReadCommittedVisibilityDemo;
import com.jinlin.mysqlandredis.mysql.transaction.Tx07_PhantomReadExampleDemo;
import com.jinlin.mysqlandredis.mysql.transaction.Tx08_PreventPhantomReadNextKeyLockDemo;
import com.jinlin.mysqlandredis.mysql.transaction.Tx09_SerializableMechanismDemo;
import com.jinlin.mysqlandredis.mysql.transaction.Tx10_MvccMechanismDemo;
import com.jinlin.mysqlandredis.mysql.transaction.Tx11_SingleUpdateAtomicityDemo;
import com.jinlin.mysqlandredis.mysql.transaction.Tx12_LongTransactionDrawbacksDemo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * MySQL 事务全量核心考点 12 问自动化执行与实机数据库验证测试套件
 */
@SpringBootTest
public class TransactionInterviewRunnerTest {

    @Test
    @DisplayName("01. 事务的特性是什么？如何实现的？(ACID 与 Undo/Redo Log 实测)")
    void test01_AcidCharacteristics() {
        assertDoesNotThrow(Tx01_AcidCharacteristicsDemo::runDemo);
    }

    @Test
    @DisplayName("02. MySQL 可能出现什么和并发相关问题？(4 大并发异象全景对比)")
    void test02_ConcurrencyAnomalies() {
        assertDoesNotThrow(Tx02_ConcurrencyAnomaliesDemo::runDemo);
    }

    @Test
    @DisplayName("03. 哪些场景不适合脏读，举个例子？(电商转账发货资损案例复现)")
    void test03_DirtyReadScenarios() {
        assertDoesNotThrow(Tx03_DirtyReadScenariosDemo::runDemo);
    }

    @Test
    @DisplayName("04. MySQL 是怎么解决并发问题的？(MVCC 读写不冲突 + 锁写写互斥实测)")
    void test04_ConcurrencyControlMechanism() {
        assertDoesNotThrow(Tx04_ConcurrencyControlMechanismDemo::runDemo);
    }

    @Test
    @DisplayName("05. 事务的隔离级别有哪些？MySQL 默认级别是什么？(实机查询与 Binlog 根源分析)")
    void test05_IsolationLevelsAndDefault() {
        assertDoesNotThrow(Tx05_IsolationLevelsAndDefaultDemo::runDemo);
    }

    @Test
    @DisplayName("06. 可重复读隔离级别下，A事务提交的数据，在B事务能看见吗？(快照读 VS 当前读 致命陷阱剖析)")
    void test06_RrReadCommittedVisibility() {
        assertDoesNotThrow(Tx06_RrReadCommittedVisibilityDemo::runDemo);
    }

    @Test
    @DisplayName("07. 举个例子说可重复读下的幻读问题 (幽灵记录 5 步诞生过程实战复现)")
    void test07_PhantomReadExample() {
        assertDoesNotThrow(Tx07_PhantomReadExampleDemo::runDemo);
    }

    @Test
    @DisplayName("08. MySQL 设置了可重复读隔离级别后，怎么保证不发生幻读？(Next-Key Lock 物理阻断实测)")
    void test08_PreventPhantomReadNextKeyLock() {
        assertDoesNotThrow(Tx08_PreventPhantomReadNextKeyLockDemo::runDemo);
    }

    @Test
    @DisplayName("09. 串行化隔离级别是通过什么实现的？(普通读隐式加 S 锁阻塞写实测)")
    void test09_SerializableMechanism() {
        assertDoesNotThrow(Tx09_SerializableMechanismDemo::runDemo);
    }

    @Test
    @DisplayName("10. 介绍 MVCC 实现原理 (隐藏列 + UndoLog 版本链 + ReadView 4 步法则高仿真演算)")
    void test10_MvccMechanism() {
        assertDoesNotThrow(Tx10_MvccMechanismDemo::runDemo);
    }

    @Test
    @DisplayName("11. 一条 UPDATE 是不是原子性的？为什么？(批量更新中途失败整体回滚实测)")
    void test11_SingleUpdateAtomicity() {
        assertDoesNotThrow(Tx11_SingleUpdateAtomicityDemo::runDemo);
    }

    @Test
    @DisplayName("12. 滥用事务，或者一个事务里有特别多 SQL 的弊端？(长事务霸占行锁导致并发阻塞实测)")
    void test12_LongTransactionDrawbacks() {
        assertDoesNotThrow(Tx12_LongTransactionDrawbacksDemo::runDemo);
    }
}

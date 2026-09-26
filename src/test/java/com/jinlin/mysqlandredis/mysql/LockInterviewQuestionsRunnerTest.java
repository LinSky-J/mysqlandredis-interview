package com.jinlin.mysqlandredis.mysql;

import com.jinlin.mysqlandredis.mysql.lock.Lock01_MysqlLockCategoriesOverviewDemo;
import com.jinlin.mysqlandredis.mysql.lock.Lock02_TableLockVsRowLockDemo;
import com.jinlin.mysqlandredis.mysql.lock.Lock03_ConcurrentUpdateSameRowDemo;
import com.jinlin.mysqlandredis.mysql.lock.Lock04_PkDisjointRangeUpdateDemo;
import com.jinlin.mysqlandredis.mysql.lock.Lock05_NonIndexedRangeUpdateTableLockDemo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * MySQL 锁机制核心考点 5 问自动化执行与实机数据库验证测试套件
 */
@SpringBootTest
public class LockInterviewQuestionsRunnerTest {

    @Test
    @DisplayName("01. 讲一下 MySQL 里有哪些锁？(锁粒度/兼容性/算法/策略全景分类)")
    void test01_MysqlLockCategoriesOverview() {
        assertDoesNotThrow(Lock01_MysqlLockCategoriesOverviewDemo::runDemo);
    }

    @Test
    @DisplayName("02. 数据库的表锁和行锁有什么作用？(高并发吞吐 VS 粗粒度整表保护实测)")
    void test02_TableLockVsRowLock() {
        assertDoesNotThrow(Lock02_TableLockVsRowLockDemo::runDemo);
    }

    @Test
    @DisplayName("03. MySQL 两个线程的 UPDATE 语句同时处理一条数据，会不会有阻塞？(X 排他锁互斥实测)")
    void test03_ConcurrentUpdateSameRow() {
        assertDoesNotThrow(Lock03_ConcurrentUpdateSameRowDemo::runDemo);
    }

    @Test
    @DisplayName("04. 两条 UPDATE 语句处理一张表的不同主键范围 (<10 与 >15) 会不会阻塞？(B+树不相交无阻塞实测)")
    void test04_PkDisjointRangeUpdate() {
        assertDoesNotThrow(Lock04_PkDisjointRangeUpdateDemo::runDemo);
    }

    @Test
    @DisplayName("05. 如果 2 个范围不是主键或索引，还会阻塞吗？(全表扫描退化锁全表严重阻塞实测)")
    void test05_NonIndexedRangeUpdateTableLock() {
        assertDoesNotThrow(Lock05_NonIndexedRangeUpdateTableLockDemo::runDemo);
    }
}

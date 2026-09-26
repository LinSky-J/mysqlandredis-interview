package com.jinlin.mysqlandredis.mysql.lock;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 【面试题 02】数据库的表锁和行锁有什么作用？
 *
 * 核心考点深度剖析：
 *
 * 一、 表级锁 (Table Lock) 的作用与优缺点：
 * 1. 核心作用：
 *    - 粗粒度控制整张数据表的并发读写访问；
 *    - 保护整表在批处理或结构变更期间不被其他并发事务干扰破坏。
 * 2. 优缺点权衡：
 *    - 【优点】：加锁开销极小(仅需在内存表对象上挂载锁标志)，加锁/解锁速度极快，【天然不会产生死锁】。
 *    - 【缺点】：锁粒度过大，并发冲突概率极高，一个写事务会导致整张表后续所有操作全部阻塞排队，并发吞吐极低。
 * 3. 适用场景：
 *    - 读多写极少的系统；全表批量数据清洗导出；DDL 表结构变更维护(元数据锁 MDL)；MyISAM 存储引擎默认锁。
 *
 * 二、 行级锁 (Row Lock) 的作用与优缺点：
 * 1. 核心作用：
 *    - 细粒度精准锁定被操作的【单条索引记录或特定间隙】，实现行级隔离；
 *    - 保证数据修改的原子性与数据一致性，同时最大限度释放无关数据的并发读写能力。
 * 2. 优缺点权衡：
 *    - 【优点】：锁粒度最小，并发冲突概率最低，不同线程操作不同数据行【完全并行、互不阻塞】，并发吞吐量极高。
 *    - 【缺点】：加锁开销大(每加一行锁都需要在 Buffer Pool 维护锁管理链表)，加锁速度相对较慢，交叉加锁时【可能发生死锁 (Deadlock)】。
 * 3. 适用场景：
 *    - 绝大多数互联网高并发在线交易 OLTP 系统 (电商秒杀下单、金融账户变动、订单状态流转等)，是 InnoDB 胜过 MyISAM 的核心基石。
 */
public class Lock02_TableLockVsRowLockDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 02】表锁 VS 行锁作用剖析与并发隔离实测对比");
        System.out.println("====================================================================");

        // 初始化商品库存
        DbConnectionHelper.executeSqlScript(
                "UPDATE lock_item_stock SET stock = 100 WHERE id IN (1, 2);"
        );

        // 1. 实测验证行锁的强大并发优势：线程 A 修改 id=1，线程 B 同时修改 id=2，【完全并行互不阻塞】！
        System.out.println(">>> [实测 1] 验证行级锁的高并发特性：线程 A 修改 id=1，线程 B 同时修改 id=2...");
        CountDownLatch rowLatch = new CountDownLatch(2);

        long startRowTest = System.currentTimeMillis();

        new Thread(() -> {
            try (Connection conn = DbConnectionHelper.getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE lock_item_stock SET stock = stock - 1 WHERE id = 1;")) {
                    ps.executeUpdate();
                }
                Thread.sleep(600); // 模拟持有 id=1 的行锁
                conn.commit();
                System.out.println("   [线程 A] 成功更新 id=1 并提交释放行锁！");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                rowLatch.countDown();
            }
        }, "RowLock-Thread-A").start();

        new Thread(() -> {
            try (Connection conn = DbConnectionHelper.getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE lock_item_stock SET stock = stock - 1 WHERE id = 2;")) {
                    ps.executeUpdate();
                }
                Thread.sleep(600); // 模拟持有 id=2 的行锁
                conn.commit();
                System.out.println("   [线程 B] 成功更新 id=2 并提交释放行锁！");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                rowLatch.countDown();
            }
        }, "RowLock-Thread-B").start();

        try {
            rowLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long rowDuration = System.currentTimeMillis() - startRowTest;
        System.out.printf("--> 【行锁实测结论】：操作不同行时，两线程耗时约 %d ms (并行执行无阻塞)，体现行锁极高并发度！%n%n", rowDuration);

        // 2. 表级意向锁 (IS / IX) 协同机制深度总结
        System.out.println(">>> 意向锁 (IS / IX) 在表锁与行锁共存时的关键作用：");
        System.out.println("1. 若没有意向锁，事务 C 试图加【表级写锁】时，必须全表逐行扫描检查每一行是否被其他事务加了行锁，时间复杂度 O(N)；");
        System.out.println("2. 有了意向锁后，事务 A 在对某行加 X 行锁前，InnoDB 自动在表级打上 IX 意向排他锁标记；");
        System.out.println("3. 事务 C 申请表写锁时，只需检查表级是否存在 IX/IS 标记，时间复杂度直接降为 O(1)！");
    }
}

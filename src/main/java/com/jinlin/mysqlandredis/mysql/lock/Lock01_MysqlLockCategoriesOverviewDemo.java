package com.jinlin.mysqlandredis.mysql.lock;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 【面试题 01】讲一下 MySQL 里有哪些锁？
 *
 * 核心考点深度全景剖析：
 * 面试中回答 MySQL 锁体系，必须建立结构化、多维度的全景分类框架，才能给面试官展现出扎实完整的体系化功底：
 *
 * 维度一：按【锁的粒度 (Granularity)】划分：
 * 1. 全局锁 (Global Lock)：
 *    - 锁定整个数据库实例，使整个库处于只读状态。
 *    - 典型命令：`FLUSH TABLES WITH READ LOCK; (FTWRL)`。
 *    - 适用场景：做全库逻辑备份 (mysqldump --single-transaction 可用 MVCC 替代 FTWRL)。
 * 2. 表级锁 (Table Lock)：
 *    - 锁定整张数据表。开销极小、加锁极快、天然无死锁；但锁粒度大，并发冲突率最高，吞吐量低。
 *    - 细分形态：
 *      ① 表共享读锁 / 表独占写锁 (`LOCK TABLES t READ/WRITE;`)
 *      ② 元数据锁 (Metadata Lock, MDL)：Server 层自动加锁。DML 查改数据加 MDL 读锁，DDL 改表结构加 MDL 写锁，防止增删改与改表结构冲突。
 *      ③ 意向锁 (Intention Lock - IS / IX)：InnoDB 自动加在表级的标记。当需要对表加表锁时，无需逐行遍历检查是否有行锁，大幅提升加表锁效率。
 *      ④ 自增锁 (AUTO-INC Lock)：并发自增主键生成时的表级锁(MySQL 8.0 默认使用互斥量优化)。
 * 3. 页级锁 (Page Lock)：
 *    - 介于表锁与行锁之间，锁定一个数据页(16KB)。BDB 存储引擎支持，InnoDB 不采用。
 * 4. 行级锁 (Row Lock)：
 *    - 锁定具体的单条或多条索引记录。开销大、加锁慢、会出现死锁；但锁粒度最小，并发冲突最低，并发吞吐极高(InnoDB 核心基石)。
 *
 * 维度二：按【锁的兼容性 (Compatibility)】划分：
 * 1. 共享锁 (S Lock / Shared Lock / 读锁)：
 *    - `SELECT ... LOCK IN SHARE MODE` (MySQL 8.0 支持 `FOR SHARE`)。
 *    - 读读共享：多事务可同时持有同一数据的 S 锁；读写互斥：阻塞其他事务获取 X 锁。
 * 2. 排他锁 (X Lock / Exclusive Lock / 写锁)：
 *    - `SELECT ... FOR UPDATE` 或 `UPDATE / DELETE / INSERT` 自动加 X 锁。
 *    - 独占排他：阻塞其他任何事务对该行获取 S 锁或 X 锁。
 *
 * 维度三：按【InnoDB 行锁底层算法 (Algorithm)】划分：
 * 1. 记录锁 (Record Lock)：精确锁定聚簇索引或二级索引上的单条记录。
 * 2. 间隙锁 (Gap Lock)：锁定记录之间的开区间 `(a, b)`，专门用来防止其他事务 INSERT 插入，消除幻读。
 * 3. 临键锁 (Next-Key Lock)：Record Lock + Gap Lock 的结合体(左开右闭区间 `(a, b]`)，InnoDB 在 RR 级别的默认行锁算法。
 * 4. 插入意向锁 (Insert Intention Lock)：INSERT 语句在向间隙插入记录前加的特殊间隙锁，多个插入若位置不冲突则可并发插入。
 *
 * 维度四：按【加锁策略/思想 (Strategy)】划分：
 * 1. 悲观锁 (Pessimistic Locking)：假定并发冲突概率极高，每次访问数据前都显式加排他锁 (`FOR UPDATE`)。
 * 2. 乐观锁 (Optimistic Locking)：假定并发冲突概率低，不依赖数据库锁，而是在数据行增加 `version` 版本号字段，更新时比对版本。
 */
public class Lock01_MysqlLockCategoriesOverviewDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 01】MySQL 锁体系多维度分类全景全解");
        System.out.println("====================================================================");

        System.out.println(">>> 1. 锁粒度与性能特性对比矩阵：");
        System.out.println("----------------------------------------------------------------------------------");
        System.out.printf("%-10s | %-12s | %-12s | %-14s | %-20s%n",
                "锁粒度", "加锁开销", "加锁速度", "死锁风险", "并发支持度 (并发冲突)");
        System.out.println("----------------------------------------------------------------------------------");
        System.out.printf("%-10s | %-12s | %-12s | %-14s | %-20s%n", "全局锁", "大", "快", "无死锁", "极低 (全库只读)");
        System.out.printf("%-10s | %-12s | %-12s | %-14s | %-20s%n", "表级锁", "小", "快", "无死锁", "较低 (整表互斥)");
        System.out.printf("%-10s | %-12s | %-12s | %-14s | %-20s%n", "页级锁", "中等", "中等", "可能死锁", "中等");
        System.out.printf("%-10s | %-12s | %-12s | %-14s | %-20s%n", "行级锁", "大", "较慢", "可能死锁", "极高 (高并发 OLTP 核心)");
        System.out.println("----------------------------------------------------------------------------------");

        System.out.println("\n>>> 2. 锁兼容性矩阵 (Lock Compatibility Matrix)：");
        System.out.println("----------------------------------------------------------------");
        System.out.printf("%-15s | %-15s | %-15s%n", "请求锁 \\ 当前持有锁", "共享锁 (S 锁)", "排他锁 (X 锁)");
        System.out.println("----------------------------------------------------------------");
        System.out.printf("%-15s | %-15s | %-15s%n", "共享锁 (S 锁)", "兼容 (√ 允许并发读)", "冲突 (× 阻塞等待)");
        System.out.printf("%-15s | %-15s | %-15s%n", "排他锁 (X 锁)", "冲突 (× 阻塞等待)", "冲突 (× 阻塞等待)");
        System.out.println("----------------------------------------------------------------");

        System.out.println("\n>>> 3. InnoDB 行锁算法核心特征：");
        System.out.println("   - Record Lock: 锁单条索引项；");
        System.out.println("   - Gap Lock: 锁开区间 (x, y)，防止新记录插进来；");
        System.out.println("   - Next-Key Lock: 左开右闭区间 (x, y]，默认行锁，解决幻读；");
        System.out.println("   - Insert Intention Lock: 插入意向锁，互不冲突的间隙并发写入。");

        DbConnectionHelper.printQueryResults("核验当前 MySQL 锁等待超时配置 (innodb_lock_wait_timeout)",
                "SHOW VARIABLES LIKE 'innodb_lock_wait_timeout';"
        );
    }
}

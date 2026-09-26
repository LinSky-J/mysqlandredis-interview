package com.jinlin.mysqlandredis.mysql.index;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 03: B+ 树底层实现、数据页内部查找、跳表对比与插入物理演变
 * 
 * 说明：数据表结构及初始化数据已移至 /sql/03_index_schema.sql 统一由 DataGrip 预先创建维护，
 *       本 Java 类专注面试题底层原理深度推演、B+ 树结构特性及引擎参数验证。
 * 
 * 核心考点深度解析：
 * 1. B+ 树特性与叶子节点双向链表：
 *    - 非叶子节点仅存索引键与指针，扇出极高 (>1000)，树高通常只有 3~4 层，单表可撑两千多万数据；
 *    - 叶子节点通过【双向链表】互相串联，完美支持正序 (ASC) 与逆序 (DESC) 范围遍历，零内存重排开销。
 * 2. 为什么 MySQL 不用跳表 (SkipList)？
 *    - 跳表适合内存数据库 (如 Redis ZSet)，节点离散分布通过概率层高索引；
 *    - 磁盘以 16KB 页为读写单元，跳表若在磁盘实现会导致层高膨胀，每次寻道引起大量随机跨页 I/O；
 *    - B+ 树一个 16KB 页可存上千个索引键，以极高局部性压低 I/O 次数。
 * 3. 到了叶子节点 (16KB 页)，如何查找具体记录？
 *    - 页内划分了若干槽位 (Slot)，组成【页目录 (Page Directory)】；
 *    - 第一步：在 Page Directory 的槽数组中进行【二分查找 (Binary Search)】定位具体槽位；
 *    - 第二步：在槽内对应的 4~8 条记录中顺着行头 next_record 单向链表顺序遍历，纳秒级命中！
 * 4. 插入新记录时索引的变化：
 *    - 普通插入：写入 User Records，槽位记录数满 8 条则拆分 Slot；
 *    - 页满分裂：分配新 16KB 页，将约 50% 记录迁移，维护双向链表与非叶子父节点指针；
 *    - Change Buffer：若非唯一二级索引不在内存，写入 Change Buffer 暂存，避免同步磁盘 I/O。
 */
public class BPlusTreeInternalsDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【索引模块 03】B+ 树底层结构、页目录二分查找、跳表对比实机实测");
        System.out.println("====================================================================");

        // 1. 查询当前真实 MySQL 实例的数据页大小 (默认 16KB = 16384 字节)
        DbConnectionHelper.printQueryResults("当前数据库默认数据页大小 (innodb_page_size)", 
                "SHOW VARIABLES LIKE 'innodb_page_size';");

        // 2. 查询 Change Buffer 配置
        DbConnectionHelper.printQueryResults("InnoDB Change Buffer 缓冲策略", 
                "SHOW VARIABLES LIKE 'innodb_change_buffering';");

        // 3. 验证叶子节点双向链表的优势: 逆序遍历直接走双向链表，无需额外内存排序 (Extra 无 Using filesort)
        DbConnectionHelper.printQueryResults("逆序范围扫描执行计划 (利用双向链表 prev 指针，Extra 无 Using filesort)", 
                "EXPLAIN SELECT age FROM interview_bplus_tree WHERE age >= 20 ORDER BY age DESC;");

        printInternalArchitectureDetails();
    }

    private static void printInternalArchitectureDetails() {
        System.out.println("---------------- B+ 树叶子数据页内部检索算法 ----------------");
        System.out.println("很多同学误以为到了叶子节点依然是全页线性扫描，大错特错！");
        System.out.println("1. 槽位划分: InnoDB 将数据页内的所有记录分组，每组 4~8 条，每组最大记录的偏移地址记录在页底部的 Page Directory 中，称为一个【槽 (Slot)】；");
        System.out.println("2. 二分定位: 槽数组在内存中是连续有序的，InnoDB 直接对 Page Directory 执行【二分查找法】，只需 2~4 次比较即定位到记录所在槽；");
        System.out.println("3. 组内微遍历: 根据前一个槽的最大记录指针，沿着单向链表 next_record 遍历至多 4~8 次即可获取数据，整体查询在微秒甚至纳秒级完成！");
        System.out.println("---------------- B+ 树 vs 跳表 (SkipList) 核心取舍 ----------------");
        System.out.println("1. 介质差异: 跳表是【内存结构】，B+ 树是【磁盘外存结构】；");
        System.out.println("2. 局部性与 I/O 成本: B+ 树一个 16KB 页可组织上千个索引键，3~4 次磁盘 I/O 即可覆盖千万行；跳表节点离散且层数高，持久化到磁盘会引发雪崩式的随机 I/O。");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

package com.jinlin.mysqlandredis.mysql.index;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

/**
 * 问题 02: 聚簇索引、主键选型 (自增 ID vs UUID)、性别索引深度解密
 * 
 * 说明：数据表结构及初始化数据已移至 /sql/03_index_schema.sql 统一由 DataGrip 预先创建维护，
 *       本 Java 类专注面试题核心逻辑剖析、CBO 成本模型决策验证与存储机制深度讲解。
 * 
 * 核心考点：
 * 1. 聚簇索引：索引和数据紧密合一存放在 B+ 树叶子节点中。一张表只能有一个聚簇索引。
 * 2. 聚簇索引更新时的物理变化：
 *    - 更新非主键：原地更新或数据变长引发页分裂；
 *    - 更新主键：物理上必须 DELETE 旧行 + INSERT 新行到对应新叶子页，且所有二级索引的主键引用必须全量改写！
 * 3. 默认聚簇规则：优先显式主键；无主键找第一个 NOT NULL UNIQUE；均无则生成隐式 6 字节 DB_ROW_ID。
 * 4. 主键为什么强烈建议自增 ID 而非 UUID？
 *    - 自增 ID 严格单调递增，天然末尾顺序追加，数据页填充率高 (>93%)，几乎无页分裂；
 *    - UUID 随机离散，在 B+ 树中产生无休止的随机插入，频繁引发页分裂与磁盘碎片，且占用 36 字节导致所有二级索引严重膨胀。
 * 5. 性别字段能不能建索引？
 *    - 语法上可以，业务上坚决不建！
 *    - 区分度仅约 50%，优化器计算发现通过索引逐条回表的随机 I/O 成本远超全表顺序扫描，最终弃用索引，徒增写开销。
 */
public class ClusteredIndexAndPkDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【索引模块 02】聚簇索引机制、主键选型 (自增 ID vs UUID) 与性别索引实机实测");
        System.out.println("====================================================================");

        // 1. 实测低基数性别索引的执行计划 (观察优化器成本模型 CBO 的裁决: 直接走全表扫描 ALL)
        DbConnectionHelper.printQueryResults("查询性别字段执行计划 (注意: 优化器会因区分度低且回表代价高，直接放弃索引走全表扫描 ALL)", 
                "EXPLAIN SELECT * FROM interview_pk_autoincrement WHERE gender = 1;");

        // 2. 观察自增主键表 (interview_pk_autoincrement) 与 UUID 随机主键表 (interview_pk_uuid) 的物理组织
        DbConnectionHelper.printQueryResults("自增主键表数据 (连续顺序存放)", 
                "SELECT id, user_code, score, gender, created_at FROM interview_pk_autoincrement;");

        DbConnectionHelper.printQueryResults("UUID 随机主键表数据 (伪随机离散散列)", 
                "SELECT id, user_code, score, created_at FROM interview_pk_uuid;");

        printDeepDive();
    }

    private static void printDeepDive() {
        System.out.println("---------------- 面试深度追问解答 ----------------");
        System.out.println("Q: 为什么自增 ID 比 UUID 快那么多？UUID 到底有序吗？");
        System.out.println("A: 标准 UUID 是完全无序的伪随机哈希！");
        System.out.println("   - 插入自增 ID: 每次都是在当前页的最大位置追加，满 16KB 后自动向后新分配数据页，几乎无随机 I/O，页碎片率 < 5%；");
        System.out.println("   - 插入 UUID: 每次生成一个完全随机值，必须插入到整棵 B+ 树的某个随机旧数据页中；若该页已满，强制执行【页分裂 (Page Split)】，");
        System.out.println("     将原页一半数据拷贝到新页并重排指针，造成成千上万次随机磁盘写，导致页利用率腰斩至 50% 甚至更低！");
        System.out.println("Q: 聚簇索引更新主键时物理上会发生什么？");
        System.out.println("A: 1) 旧行在原数据页被标记为已删除 (delete-marked)；");
        System.out.println("   2) 在目标新页插入新行，可能连锁引发页分裂；");
        System.out.println("   3) 该表上的【所有二级索引】叶子节点存储的旧主键全部必须同步执行物理删除与新插入重写！");
        System.out.println("Q: 主键的选型黄金标准是什么？");
        System.out.println("A: 1) 尽量单调自增；2) 尽量占用字节小 (推荐 BIGINT 8字节)；3) 业务无关；4) 绝对不允许后期修改。");
        System.out.println("====================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

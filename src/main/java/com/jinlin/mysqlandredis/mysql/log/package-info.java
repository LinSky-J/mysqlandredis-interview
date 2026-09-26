/**
 * MySQL 面试分类：【日志】
 * 包含考点：
 * 01. 日志文件分为哪几种？(七大核心日志系统)
 * 02. Binlog 二进制日志深度剖析 (三种格式与刷盘策略)
 * 03. Undo Log 回滚日志核心作用 (原子回滚与 MVCC 版本链)
 * 04. 有了 Undo Log 为何还需要 Redo Log？(持久性与崩溃恢复闭环)
 * 05. Redo Log 怎么保证持久性？(WAL 机制与三大刷盘参数)
 * 06. 能不能只用 Binlog 不用 Redo Log？(Crash-Safe 缺失与 Checkpoint 机制)
 * 07. Binlog 两阶段提交 (2PC) 过程与崩溃恢复判断准则
 * 08. UPDATE 语句的完整执行流转生命周期
 * 09. MySQL 如何保障数据零丢失？(双 1 刷盘保障)
 * 10. Redo Log 是在内存里吗？(内存 Buffer 与物理文件组循环写)
 * 11. 为什么要写 Redo Log 而不是直接写 B+ 树？(顺序 I/O vs 随机 I/O)
 * 12. MySQL 两次写 (Doublewrite Buffer) 解决部分页写入失效
 */
package com.jinlin.mysqlandredis.mysql.log;

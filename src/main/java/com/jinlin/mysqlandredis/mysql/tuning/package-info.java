/**
 * MySQL 面试分类：【性能调优】
 * 包含考点：
 * 01. EXPLAIN 执行计划有什么作用？核心关键字段全景精析
 * 02. 怎么查看 SQL 是否有走索引？(type/key/key_len/Extra 判定)
 * 03. 怎么查看表的索引结构？(SHOW INDEX 命令与 Cardinality 基数)
 * 04. 发现单表查询很慢，有哪些解决方案？(端到端排查 7 步法)
 * 05. 如果 EXPLAIN 用到的索引不正确，有什么办法干预？(FORCE/USE/IGNORE/ANALYZE)
 */
package com.jinlin.mysqlandredis.mysql.tuning;

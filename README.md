# MySQL & Redis 高并发面试与实战工程演练

<div align="center">

[English](README_EN.md) | [中文简体](README.md)

![Java](https://img.shields.io/badge/Java-8%20%7C%2011%20%7C%2017-orange?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.6.13-brightgreen?logo=springboot)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql)
![Redis](https://img.shields.io/badge/Redis-5.0%2B%20%7C%206.x%20%7C%207.x-red?logo=redis)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven)
![Tests](https://img.shields.io/badge/Tests-103%20Passed-success)
![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)

</div>

---

## 📖 项目简介 (Overview)

**MySQL & Redis Interview** 是一个专为**中高级后端开发、架构师**打造的深度技术面试与生产级高并发实战演练工程。

与传统的“纯文本背八股文”不同，本项目坚持 **“Talk is cheap, show me the code & execution”** 的工程哲学：
- **深度理论剖析**：不仅给出“是什么”，更从 Linux 内核、存储引擎底层机制、C 语言源码及并发安全原理深度推演“为什么”；
- **真实环境实机联动**：所有代码均直接连接本地真实的 **MySQL 8.0** 与 **Redis 5.0+** 数据库运行，实时产生真实的表结构变更、事务行锁竞争、跳表计算、哈希槽分片与 Lua 原子扣减；
- **100% 可复制执行的命令行注释**：在所有 Redis 业务操作上方，均标注有 **100% 能够在 `redis-cli` 命令行直接无缝粘贴执行的原生 Redis 指令**（绝对无任何占位符或变量插值）；
- **全量自动化测试套件**：涵盖 15 个独立功能分支，包含 **103 个全绿自动化集成测试用例**，保障工业级代码质量。

---

## 🏗 技术栈与依赖版本

- **核心语言**：Java 1.8 / 11 兼容
- **核心框架**：Spring Boot `2.6.13`
- **持久层框架**：MyBatis + `HikariCP` 高性能连接池
- **关系型数据库**：MySQL `8.0.x`
- **缓存与 NoSQL**：Spring Data Redis + Lettuce（支持连接池与高并发异步 I/O）
- **构建工具**：Apache Maven `3.8+`
- **单元测试**：JUnit 5 + Spring Boot Test

---

## 📚 模块知识体系大纲

### 🐬 一、MySQL 深度面试模块 (`com.jinlin.mysqlandredis.mysql`)

| 模块分类 | 包路径 | 核心涵盖的面试深度要点 |
| :--- | :--- | :--- |
| **SQL 基础** | [`mysql.sqlbase`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/mysql/sqlbase) | SQL 完整执行链路（连接器、分析器、优化器、执行器）、`NULL` 值的比较与三值逻辑陷阱、字符集与校对规则（utf8mb4）、深分页性能对比。 |
| **存储引擎** | [`mysql.storage`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/mysql/storage) | InnoDB 与 MyISAM 终极维度对比、内存架构（Buffer Pool、Change Buffer、AHI 自适应哈希）、磁盘架构（Doublewrite Buffer 双写缓冲、表空间 .ibd 文件）。 |
| **索引机制** | [`mysql.index`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/mysql/index) | B+ 树为什么比 B 树/红黑树/哈希更优、聚簇索引 vs 二级索引、前缀索引与索引下推 (ICP)、最左前缀原则与 8 大索引失效场景。 |
| **事务与隔离** | [`mysql.transaction`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/mysql/transaction) | ACID 特性底层支撑原理、脏读/不可重复读/幻读实机复现、MVCC 核心机制（ReadView 结构与 Undo Log 版本链）、大事务 5 大生产弊端。 |
| **锁机制** | [`mysql.lock`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/mysql/lock) | 全局锁 (FTWRL)、表级锁 (MDL 元数据锁)、行级锁三剑客（Record Lock、Gap Lock、Next-Key Lock）、死锁产生原因与检测回滚、MySQL 实现分布式可重入锁。 |
| **日志与恢复** | [`mysql.log`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/mysql/log) | Undo Log（原子性保证）、Redo Log（Crash-Safe 崩溃安全保证）、Binlog（主从归档复制）、两阶段提交 (2PC) 崩溃一致性与组提交 (Group Commit)。 |
| **性能调优** | [`mysql.tuning`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/mysql/tuning) | `EXPLAIN` 执行计划关键指标深度研读（type、key_len、Extra）、慢查询日志排查、百万级深分页子查询延迟关联优化、表结构冷热分离。 |
| **集群与架构** | [`mysql.architecture`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/mysql/architecture) | 主从复制三大线程原理（I/O Thread、SQL Thread、Dump Thread）、异步复制 vs 半同步复制 (Semi-Sync)、主从延迟 4 大根因与并行复制、MHA/Orchestrator 高可用架构。 |

---

### ⚡ 二、Redis 深度面试模块 (`com.jinlin.mysqlandredis.redis`)

| 模块分类 | 包路径 | 核心涵盖的面试深度要点 |
| :--- | :--- | :--- |
| **底层数据结构** | [`redis.datastructure`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/redis/datastructure) | SDS 动态字符串（空间预分配与惰性释放）、哈希表渐进式 rehash（两张哈希表、读写双查）、跳表 SkipList 原理与为什么不用 B+ 树、压缩列表 ziplist 连锁更新缺陷与 listpack 替代设计。 |
| **线程与网络模型** | [`redis.threadmodel`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/redis/threadmodel) | Redis 为什么快？单线程 Reactor 事件循环机制、I/O 多路复用 epoll 原理、Redis 4.0 后台辅助线程 (`bio`) 与 Redis 6.0 多线程 I/O 并发网络读写（核心执行引擎依然单线程串行）。 |
| **事务与原子性** | [`redis.transaction`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/redis/transaction) | 单命令天然原子性、Lua 脚本强原子性保障、MULTI/EXEC 弱原子性与为什么不支持回滚 (No Rollback)、除了 Lua 保证原子性的方案（原生复合命令、`WATCH` 乐观锁、Redis 7.0 Functions、Redisson）。 |
| **持久化与日志** | [`redis.persistence`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/redis/persistence) | RDB 快照与 Linux `fork()` 写时复制 (COW) 机制、AOF 追加日志 3 种刷盘策略 (`always`, `everysec`, `no`)、AOF 异步重写原理、Redis 4.0+ 混合持久化 (`aof-use-rdb-preamble yes`)。 |
| **缓存淘汰与过期** | [`redis.eviction`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/redis/eviction) | 过期删除（时间维度）vs 内存淘汰（空间维度）区别、8 大内存淘汰策略与近似 LRU/LFU 24-bit 时钟实现原理、过期删除组合拳（惰性删除 `expireIfNeeded` + 限时 25ms 定期抽样删除 `activeExpireCycle`）、为什么缓存失效不立即删除（CPU 算力与吞吐量权衡）。 |
| **集群与高可用** | [`redis.cluster`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/redis/cluster) | 主从全量同步 (RDB + replication buffer) vs 增量同步 (环形积压缓冲区 `repl_backlog_buffer` + repl_offset)、数据一致性与脑裂防护 (`min-replicas-to-write`)、哨兵机制（心跳、SDOWN、ODOWN、Raft 选哨兵 Leader、4 步选新主）、Cluster 16384 哈希槽 (`CRC16 % 16384`)、Hash Tag `{...}`、Smart Client 拓扑缓存直连、MOVED 与 ASK 重定向机制。 |
| **业务场景实战** | [`redis.scenario`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/redis/scenario) | 为什么比 MySQL 快？本地缓存 Caffeine vs Redis 对比与并发量级对照、分布式锁演进（SET NX EX、UUID防误删、Lua释放、Redisson 看门狗自动续期）、大 Key 危害与 `UNLINK` 异步删除、热 Key 危害与本地缓存/随机后缀散列副本、Cache-Aside 缓存一致性与 Canal 监听 Binlog 异步解耦、缓存雪崩/击穿/穿透与布隆过滤器原理解析、秒杀多级漏斗削峰架构与 Lua 原子扣减防超卖。 |

---

## 🛠 特色设计规范 (Highlights)

### 1. 100% 可在 `redis-cli` 中运行的原始指令注释
针对面试官常问的“底层到底执行了什么原生 Redis 命令”，本项目在所有 `RedisTemplate` / `StringRedisTemplate` 调用上方，均给出了绝对可直接粘贴至 `redis-cli` 终端执行的原生命令：
```java
// Redis 原生指令: SET demo:scenario:item:8801 iPhone15_Pro_Max EX 3600
redisTemplate.opsForValue().set(itemKey, "iPhone15_Pro_Max", Duration.ofHours(1));

// Redis 原生指令: GET demo:scenario:item:8801
String itemName = redisTemplate.opsForValue().get(itemKey);
```

### 2. 统一动态配置体系
完全消除代码中的硬编码账号密码，统一从 `src/main/resources/application.yml` 加载：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/interview_db?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: ******
  redis:
    host: localhost
    port: 6379
    database: 0
    password: '****'
```

---

## 🚀 快速开始 (Quick Start)

### 1. 环境准备
确保本机已安装并启动 MySQL 与 Redis 服务：
- **MySQL 8.0+**：在 `3306` 端口运行，预先创建测试数据库：
  ```sql
  CREATE DATABASE IF NOT EXISTS interview_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
  ```
- **Redis 5.0+**：在 `6379` 端口运行。

### 2. 配置修改
根据本地实际情况检查并修改 [`src/main/resources/application.yml`](file:///e:/java/mysqlandredis/src/main/resources/application.yml) 中的 MySQL 和 Redis 密码。

### 3. 一键编译与全量测试
使用 Maven 执行全工程自动化测试套件：
```bash
mvn clean test
```
测试通过输出示例：
```text
[INFO] Results:
[INFO] 
[INFO] Tests run: 103, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### 4. 运行指定专题测试
若只想验证或调试某一个专题模块，可以使用 Maven `-Dtest` 参数单独执行：
```bash
# 验证 MySQL 事务模块
mvn test -Dtest=TransactionInterviewTest

# 验证 Redis 秒杀与高并发场景模块
mvn test -Dtest=RedisScenarioInterviewTest

# 验证 Redis 集群与哨兵模块
mvn test -Dtest=RedisClusterInterviewTest
```

---

## 🌿 Git 分支管理

本项目严格采用生产级特性分支工作流，按业务专题独立划分分支：

```text
main                      # 核心稳定主分支 (包含最新代码与 103 个全绿测试)
├── mysql                 # MySQL 基础与通用套件
├── mysql-sqlbase         # SQL 基础模块
├── mysql-storage-engine  # 存储引擎与底层架构模块
├── mysql-index           # 索引原理与优化模块
├── mysql-transaction     # 事务隔离与 MVCC 模块
├── mysql-lock            # 锁机制与死锁模块
├── mysql-log             # 日志系统与两阶段提交模块
├── mysql-tuning          # 性能调优与 EXPLAIN 模块
├── mysql-architecture    # 主从复制与高可用架构模块
├── redis-datastructure   # Redis 底层数据结构模块
├── redis-threadmodel     # Redis 线程与 I/O 多路复用模型模块
├── redis-transaction     # Redis 事务与原子性解决方案模块
├── redis-persistence     # Redis RDB / AOF 与混合持久化模块
├── redis-eviction        # Redis 缓存淘汰与过期删除模块
├── redis-cluster         # Redis 主从、哨兵与 Cluster 集群模块
└── redis-scenario        # Redis 业务高并发实战与秒杀场景模块
```

---

## 📄 开源许可证 (License)

本项目采用 [Apache License 2.0](LICENSE) 开源许可证。
欢迎用于个人技术提升、团队内部培训与面试复习！

# MySQL & Redis High-Concurrency Interview & Engineering Practice Suite

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

## 📖 Overview

**MySQL & Redis Interview** is an in-depth technical interview preparation and production-grade engineering practice repository designed for **senior backend engineers, technical leads, and system architects**.

Moving beyond theoretical question memorization, this repository embraces the engineering philosophy: **"Talk is cheap, show me the code & execution"**:
- **Deep Architectural Principles**: Analyzes not just "how it works", but "why it was designed this way" from the Linux kernel, storage engine internals, C source code, and concurrency mechanics;
- **Live Local Database Integration**: All code connects directly to real, running **MySQL 8.0** and **Redis 5.0+** instances, generating live schema updates, row lock contention, skip list calculations, hash slot routing, and atomic Lua script execution;
- **100% Executable Redis CLI Command Comments**: Above every single `RedisTemplate` call, there is a literal, 100% executable native Redis command that can be directly pasted into `redis-cli` (strictly with zero variable placeholders or templating);
- **Comprehensive Automated Test Suite**: Spans 15 dedicated feature branches with **103 fully green automated integration tests**, guaranteeing production-ready reliability.

---

## 🏗 Tech Stack & Dependencies

- **Core Language**: Java 1.8 / 11 compatible
- **Framework**: Spring Boot `2.6.13`
- **Data Access**: MyBatis + `HikariCP` high-performance connection pool
- **Relational Database**: MySQL `8.0.x`
- **In-Memory Cache & NoSQL**: Spring Data Redis + Lettuce (with connection pooling and asynchronous non-blocking I/O)
- **Build System**: Apache Maven `3.8+`
- **Testing**: JUnit 5 + Spring Boot Test

---

## 📚 Knowledge Architecture & Modules

### 🐬 1. MySQL Deep-Dive Modules (`com.jinlin.mysqlandredis.mysql`)

| Category | Package Path | Key Interview Topics & Architectural Highlights |
| :--- | :--- | :--- |
| **SQL Fundamentals** | [`mysql.sqlbase`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/mysql/sqlbase) | End-to-end SQL query lifecycle (Connector, Parser, Optimizer, Executor), `NULL` handling and three-valued logic traps, character sets & collations (utf8mb4), deep pagination performance benchmarking. |
| **Storage Engines** | [`mysql.storage`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/mysql/storage) | Comprehensive InnoDB vs MyISAM comparison matrix, memory architecture (Buffer Pool, Change Buffer, Adaptive Hash Index), disk architecture (Doublewrite Buffer, Tablespaces `.ibd`). |
| **Indexing Mechanics** | [`mysql.index`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/mysql/index) | Why B+ Tree outperforms B-Tree, Hash, and Red-Black Tree; clustered vs secondary indexes; prefix indexes; Index Condition Pushdown (ICP); leftmost prefix rule and 8 common index invalidation traps. |
| **Transactions & Isolation** | [`mysql.transaction`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/mysql/transaction) | ACID properties implementation, live reproduction of Dirty Reads, Non-Repeatable Reads, and Phantom Reads; MVCC mechanics (ReadView structure and Undo Log version chain); 5 hazards of long transactions. |
| **Locking Mechanisms** | [`mysql.lock`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/mysql/lock) | Global Lock (FTWRL), Table Lock (Metadata Lock - MDL), Row Locks (Record Lock, Gap Lock, Next-Key Lock), Deadlock detection & rollback, implementing distributed reentrant locks with MySQL. |
| **Logging & Recovery** | [`mysql.log`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/mysql/log) | Undo Log (Atomicity), Redo Log (Crash-Safe recovery), Binlog (Replication & Archiving), Two-Phase Commit (2PC) write-ahead logging (WAL), Group Commit optimization. |
| **Performance Tuning** | [`mysql.tuning`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/mysql/tuning) | Deep analysis of `EXPLAIN` query execution plans (type, key_len, Extra), slow query log analysis, optimizing deep pagination (`LIMIT 1000000, 10`) with delayed joins, schema normalization vs denormalization. |
| **Architecture & Replication**| [`mysql.architecture`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/mysql/architecture) | Three-thread master-slave replication (I/O Thread, SQL Thread, Dump Thread), Async vs Semi-Sync replication, 4 root causes of replication lag & Multi-Threaded Slaves (MTS), MHA & Orchestrator high-availability architectures. |

---

### ⚡ 2. Redis Deep-Dive Modules (`com.jinlin.mysqlandredis.redis`)

| Category | Package Path | Key Interview Topics & Architectural Highlights |
| :--- | :--- | :--- |
| **Internal Data Structures** | [`redis.datastructure`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/redis/datastructure) | Simple Dynamic Strings (SDS) with pre-allocation & lazy free, progressive hashtable rehashing (dual tables and concurrent dual lookups), SkipList mechanics & why B+ Trees are not used, ziplist cascading update flaws & listpack replacements. |
| **Thread & Network Models** | [`redis.threadmodel`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/redis/threadmodel) | Why is Redis so fast? Single-threaded Reactor event loop (`aeProcessEvents`), epoll I/O multiplexing, Redis 4.0 background helper threads (`bio`), Redis 6.0 multi-threaded I/O (parallel socket read/write while maintaining a single-threaded execution core). |
| **Transactions & Atomicity** | [`redis.transaction`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/redis/transaction) | Natural atomicity of single commands, strong atomicity via Lua scripts (`EVAL`/`EVALSHA`), weak atomicity of `MULTI`/`EXEC` & why Redis does not support rollback, atomic alternatives to Lua (composite commands, `WATCH` CAS optimistic locking, Redis 7.0 Functions, Redisson). |
| **Persistence & Logging** | [`redis.persistence`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/redis/persistence) | RDB snapshots & Linux `fork()` Copy-On-Write (COW), AOF append-only log with 3 fsync policies (`always`, `everysec`, `no`), asynchronous AOF rewrite, Redis 4.0+ Hybrid persistence (`aof-use-rdb-preamble yes`). |
| **Eviction & Expiration** | [`redis.eviction`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/redis/eviction) | Expiration deletion (time-based) vs memory eviction (space-based), 8 memory eviction policies and approximated LRU/LFU 24-bit clock implementations, dual-pronged expiration strategy (`expireIfNeeded` lazy + `activeExpireCycle` 25ms time-sliced cron), why expired keys are not deleted immediately (CPU vs memory trade-offs). |
| **Cluster & High Availability** | [`redis.cluster`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/redis/cluster) | Master-slave full sync (RDB + replication buffer) vs partial sync (circular `repl_backlog_buffer` + repl_offset), eventual consistency & split-brain prevention (`min-replicas-to-write`), Sentinel monitoring/SDOWN/ODOWN/Raft leader election/4-step new master selection, Redis Cluster 16384 hash slots (`CRC16 % 16384`), Hash Tag `{...}`, Smart Client topology caching & direct routing, MOVED and ASK redirection protocols. |
| **High-Concurrency Scenarios** | [`redis.scenario`](file:///e:/java/mysqlandredis/src/main/java/com/jinlin/mysqlandredis/redis/scenario) | Why Redis is faster than MySQL, local cache (Caffeine) vs Redis comparison & throughput benchmarking, distributed lock lifecycle (`SET NX EX`, UUID validation, Lua release, Redisson Watchdog auto-renewal), BigKey hazards & `UNLINK` non-blocking async deletion, HotKey hazards & multi-level caching / random-suffix sharding, Cache-Aside consistency with Canal Binlog async decoupling, Cache Avalanche/Breakdown/Penetration & Bloom Filter internals, Seckill multi-layer funnel architecture & Lua atomic anti-overselling. |

---

## 🛠 Design Highlights

### 1. 100% Executable Native Redis CLI Comments
To bridge the gap between high-level Java frameworks and raw Redis network protocol commands, every single `RedisTemplate` operation is annotated with a 100% executable command ready for direct execution in `redis-cli`:
```java
// Redis 原生指令: SET demo:scenario:item:8801 iPhone15_Pro_Max EX 3600
redisTemplate.opsForValue().set(itemKey, "iPhone15_Pro_Max", Duration.ofHours(1));

// Redis 原生指令: GET demo:scenario:item:8801
String itemName = redisTemplate.opsForValue().get(itemKey);
```

### 2. Centralized Dynamic Configuration
No hardcoded database credentials. All parameters are dynamically parsed from [`src/main/resources/application.yml`](file:///e:/java/mysqlandredis/src/main/resources/application.yml):
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

## 🚀 Quick Start

### 1. Prerequisites
Ensure that MySQL and Redis services are up and running on your local machine:
- **MySQL 8.0+**: Running on port `3306`. Initialize the interview database:
  ```sql
  CREATE DATABASE IF NOT EXISTS interview_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
  ```
- **Redis 5.0+**: Running on port `6379`.

### 2. Configure Credentials
Update MySQL and Redis connection parameters in [`src/main/resources/application.yml`](file:///e:/java/mysqlandredis/src/main/resources/application.yml) if necessary.

### 3. Build & Run All Tests
Execute the entire test suite via Maven:
```bash
mvn clean test
```
Expected output:
```text
[INFO] Results:
[INFO] 
[INFO] Tests run: 103, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### 4. Run Individual Test Suites
To debug or study a specific topic, execute with Maven's `-Dtest` parameter:
```bash
# Verify MySQL transaction isolation & MVCC
mvn test -Dtest=TransactionInterviewTest

# Verify Redis seckill & high-concurrency scenario demonstrations
mvn test -Dtest=RedisScenarioInterviewTest

# Verify Redis cluster, replication & sentinel failover
mvn test -Dtest=RedisClusterInterviewTest
```

---

## 🌿 Git Branching Strategy

This project adheres to a clean, production-grade feature branching workflow:

```text
main                      # Stable production branch (contains all code & 103 passed tests)
├── mysql                 # MySQL foundation & shared utilities
├── mysql-sqlbase         # SQL fundamentals & pagination
├── mysql-storage-engine  # Storage engines & Buffer Pool
├── mysql-index           # Indexing mechanics & invalidation
├── mysql-transaction     # Isolation levels, MVCC & lock contention
├── mysql-lock            # Table, row, gap locks & deadlocks
├── mysql-log             # Undo Log, Redo Log, Binlog & 2PC
├── mysql-tuning          # Query optimization & EXPLAIN plans
├── mysql-architecture    # Replication & high availability
├── redis-datastructure   # SDS, dict, ziplist & skiplist
├── redis-threadmodel     # Single-threaded Reactor & I/O multiplexing
├── redis-transaction     # Lua scripts & atomicity solutions
├── redis-persistence     # RDB, AOF & hybrid persistence
├── redis-eviction        # Expiration strategies & maxmemory eviction
├── redis-cluster         # Replication, Sentinel & Cluster sharding
└── redis-scenario        # Distributed locks, Big/Hot keys & Seckill
```

---

## 📄 License

This repository is distributed under the [Apache License 2.0](LICENSE).
Feel free to use it for personal skill enhancement, team training, and interview preparation!

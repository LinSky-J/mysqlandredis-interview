# MySQL & Redis Interview 项目规范化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `mysqlandredis` 初始骨架重构为符合生产规范的 Spring Boot 2.6.x + MySQL + Redis + MyBatis 工程，使用 YAML 配置，并提交推送到远程 GitHub 仓库。

**Architecture:** 
1. 规范化 Gradle 构建配置与依赖体系（替换 SOAP 为 REST Web，添加 MySQL 驱动、Redis Starter 及连接池）。
2. 将扁平 properties 转为结构化带有中文注释的 `application.yml`。
3. 建立企业级分层规范（common、config、controller、service、mapper、entity），增加自定义 JSON 序列化 Redis 配置及健康检查控制器。
4. 初始化 Git 仓库，排除无关构建与 IDE 缓存，关联远程并推送。

**Tech Stack:** Java 1.8, Spring Boot 2.6.13, MyBatis Spring Boot Starter 2.2.2, MySQL Connector Java, Spring Data Redis (Lettuce + Commons Pool 2), Lombok, Gradle.

---

### Task 1: 规范化 `.gitignore` 与 `build.gradle` 构建脚本

**Files:**
- Modify: `E:\java\mysqlandredis\.gitignore`
- Modify: `E:\java\mysqlandredis\build.gradle`

- [ ] **Step 1: 更新 `.gitignore`**
确保全面过滤 Gradle 缓存、构建目录、IDEA 配置、编译 class 与日志文件。

- [ ] **Step 2: 更新 `build.gradle` 依赖**
替换 `spring-boot-starter-web-services` 为 `spring-boot-starter-web`，引入 `mysql:mysql-connector-java`、`org.springframework.boot:spring-boot-starter-data-redis` 与 `org.apache.commons:commons-pool2`。

- [ ] **Step 3: 运行 Gradle 检查依赖解析**
执行: `./gradlew --version` 或 `./gradlew dependencies --configuration compileClasspath` 验证语法无误。

---

### Task 2: 配置文件规范化 (properties 转换为 yml)

**Files:**
- Delete: `E:\java\mysqlandredis\src\main\resources\application.properties`
- Create: `E:\java\mysqlandredis\src\main\resources\application.yml`

- [ ] **Step 1: 删除 `src/main/resources/application.properties`**
移除原初始化的 properties 文件。

- [ ] **Step 2: 创建 `src/main/resources/application.yml`**
编写具备 server、spring.application、spring.datasource (HikariCP)、spring.redis (Lettuce 连接池)、mybatis 配置的 YAML 文件。

---

### Task 3: 搭建企业级分层目录与核心代码

**Files:**
- Create: `E:\java\mysqlandredis\src\main\java\com\jinlin\mysqlandredis\common\Result.java`
- Create: `E:\java\mysqlandredis\src\main\java\com\jinlin\mysqlandredis\config\RedisConfig.java`
- Create: `E:\java\mysqlandredis\src\main\java\com\jinlin\mysqlandredis\controller\HealthCheckController.java`
- Create: `E:\java\mysqlandredis\src\main\java\com\jinlin\mysqlandredis\entity\.gitkeep`
- Create: `E:\java\mysqlandredis\src\main\java\com\jinlin\mysqlandredis\mapper\.gitkeep`
- Create: `E:\java\mysqlandredis\src\main\java\com\jinlin\mysqlandredis\service\.gitkeep`
- Create: `E:\java\mysqlandredis\src\main\resources\mappers\.gitkeep`
- Modify: `E:\java\mysqlandredis\src\main\java\com\jinlin\mysqlandredis\MysqlandredisApplication.java`

- [ ] **Step 1: 创建通用返回对象 `Result.java`**
统一 API 返回模型，包含 code、message、data 及泛型工具方法。

- [ ] **Step 2: 创建 Redis 序列化配置类 `RedisConfig.java`**
配置 `RedisTemplate<String, Object>` 使用 `StringRedisSerializer` 与 `GenericJackson2JsonRedisSerializer`，解决原生 JDK 序列化乱码问题。

- [ ] **Step 3: 创建健康自检控制器 `HealthCheckController.java`**
提供 `/health/ping` 接口，方便后续快速排查服务可用性。

- [ ] **Step 4: 创建分层目录占位标记**
在 `entity/`、`mapper/`、`service/`、`resources/mappers/` 下添加 `.gitkeep`。

- [ ] **Step 5: 优化主启动类 `MysqlandredisApplication.java`**
添加 `@MapperScan("com.jinlin.mysqlandredis.mapper")` 与规范注释。

---

### Task 4: 编译验证与单元测试健全性

**Files:**
- Modify: `E:\java\mysqlandredis\src\test\java\com\jinlin\mysqlandredis\MysqlandredisApplicationTests.java`

- [ ] **Step 1: 检查测试类配置**
针对当前无实体 DB 运行环境的场景，配置测试或确保编译通过。

- [ ] **Step 2: 执行 Gradle 编译命令**
执行: `cmd.exe /c gradlew.bat testClasses` 验证所有 Java 代码与配置语法编译通过。

---

### Task 5: 本地 Git 仓库初始化、提交与远程推送

**Files:**
- Repository Root: `E:\java\mysqlandredis`

- [ ] **Step 1: 本地 Git 初始化**
执行 `git init -b main`。

- [ ] **Step 2: 绑定远程仓库**
执行 `git remote add origin https://github.com/LinSky-J/mysqlandredis-interview.git`。

- [ ] **Step 3: 暂存并提交代码**
执行 `git add .` 并使用 `git commit -m "feat: initialize standard spring boot project for mysql and redis interview"`。

- [ ] **Step 4: 推送至 GitHub**
执行 `git push -u origin main`。

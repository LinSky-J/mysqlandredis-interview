# MySQL & Redis Interview 项目规范化设计规范

- 日期：2026-09-25
- 目标仓库：https://github.com/LinSky-J/mysqlandredis-interview.git
- 项目路径：`E:\java\mysqlandredis`

## 1. 目标与背景

当前工程由 Spring Initializr 生成初始骨架，但存在以下问题：
1. 依赖不合理：引入了 `spring-boot-starter-web-services`（SOAP），缺少核心的 Web MVC、MySQL Connector 以及 Redis Starter；
2. 配置文件不规范：使用 `application.properties`，仅有简单两行 MyBatis 配置，未声明端口、数据源、Redis 与连接池等必要信息；
3. 包结构缺失：仅有单一根包与空启动类，缺少典型的企业级分层规范；
4. Git 仓库未初始化与绑定：本地无 `.git` 版本控制，需关联远程仓库 `https://github.com/LinSky-J/mysqlandredis-interview.git`。

本规范旨在将该项目升级为规范、生产级规范结构的 Spring Boot 2.6.x + MySQL + Redis + MyBatis 面试演示工程。

## 2. 依赖管理规范 (`build.gradle`)

构建工具：Gradle (Wrapper)
语言版本：Java 1.8
Spring Boot 版本：2.6.13

### 依赖项列表
- `org.springframework.boot:spring-boot-starter-web`：替换原 `spring-boot-starter-web-services`，提供 RESTful API 支持。
- `org.springframework.boot:spring-boot-starter-data-redis`：Redis 核心 Starter（Lettuce 驱动）。
- `org.apache.commons:commons-pool2`：为 Lettuce 提供连接池支持。
- `org.mybatis.spring.boot:mybatis-spring-boot-starter:2.2.2`：MyBatis Spring 集成。
- `mysql:mysql-connector-java`：MySQL JDBC 驱动（由 Spring Boot 依赖管理统一版本）。
- `org.projectlombok:lombok`：Lombok 注解。
- `org.springframework.boot:spring-boot-starter-test`：单元测试。

## 3. 配置文件规范 (`application.yml`)

移除 `src/main/resources/application.properties`，创建 `src/main/resources/application.yml`，包含以下模块：
- **server**：端口 `8080`，UTF-8 字符集。
- **spring.application.name**：`mysqlandredis-interview`。
- **spring.datasource**：
  - Driver: `com.mysql.cj.jdbc.Driver`
  - URL 包含 Unicode、UTF-8、时区 `Asia/Shanghai`、禁用 SSL、允许公钥检索等常用配置。
  - HikariCP 连接池优化参数（maximum-pool-size: 15, minimum-idle: 5, connection-timeout: 30000ms 等）。
- **spring.redis**：
  - host: `localhost`
  - port: `6379`
  - database: `0`
  - timeout: `3000ms`
  - lettuce.pool 连接池参数。
- **mybatis**：
  - mapper-locations: `classpath:mappers/**/*.xml`
  - type-aliases-package: `com.jinlin.mysqlandredis.entity`
  - map-underscore-to-camel-case: `true`
  - log-impl: 控制台输出。

## 4. 包结构与核心代码规范

基础包路径：`com.jinlin.mysqlandredis`

```
com.jinlin.mysqlandredis
├── common
│   └── Result.java                  # 统一 REST API 响应封装类
├── config
│   └── RedisConfig.java             # RedisTemplate 自定义配置（配置 GenericJackson2JsonRedisSerializer 解决键值乱码）
├── controller
│   └── HealthCheckController.java   # 系统健康自检控制器（/ping 验证服务状态）
├── entity                           # 实体对象包目录
├── mapper                           # MyBatis Mapper 接口包目录
├── service                          # 业务逻辑接口与实现包目录
└── MysqlandredisApplication.java    # 启动类（添加 @MapperScan 注解）
```

资源文件目录：
`src/main/resources/mappers/` 建立用于存放 Mapper XML 映射文件。

## 5. 版本控制规范

1. 更新 `.gitignore`：
   - 忽略 `.gradle/`、`build/`、`bin/`、`out/`
   - 忽略 IntelliJ IDEA 配置：`.idea/`、`*.iml`、`*.iws`、`*.ipr`
   - 忽略日志与临时文件
2. 初始化 Git：
   - `git init -b main`
   - `git remote add origin https://github.com/LinSky-J/mysqlandredis-interview.git`
3. 提交与推送：
   - 提交信息：`feat: initialize standard spring boot project for mysql and redis interview`
   - 推送至 `origin main` 分支。

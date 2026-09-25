# MySQL & Redis Interview (面试演练与实战工程)

本项目专为 MySQL 与 Redis 核心面试题与技术实操演练打造，采用 Spring Boot 2.6.x 标准架构，统一采用 YAML 进行多模块数据源、连接池与缓存配置，便于演示与工程化落地。

---

## 🛠 技术栈版本

- **Java**：1.8
- **Spring Boot**：2.6.13
- **MyBatis**：mybatis-spring-boot-starter 2.2.2
- **MySQL Driver**：mysql-connector-java 8.0.x
- **Redis**：Spring Data Redis (Lettuce + Commons-Pool2 连接池)
- **构建工具**：Gradle 7.5+

---

## 📁 工程目录结构

```text
com.jinlin.mysqlandredis
├── common/              # 公共响应封装与工具类 (统一 Result 响应模型)
├── config/              # 核心配置类 (RedisTemplate JSON 序列化优化)
├── controller/          # RESTful 控制器 (系统运行自检 /health/ping)
├── service/             # 业务逻辑接口与实现
├── mapper/              # MyBatis Mapper 数据访问接口
├── entity/              # 持久化实体类
└── MysqlandredisApplication.java  # Spring Boot 启动类 (@MapperScan 启用)
```

资源文件目录：
```text
src/main/resources/
├── application.yml      # 全局配置文件 (数据源、Lettuce 连接池、MyBatis 驼峰映射与日志)
└── mappers/             # MyBatis XML 映射文件目录
```

---

## ⚙ 核心配置说明 (`application.yml`)

1. **MySQL 数据源与 HikariCP**：
   - 默认连接 `jdbc:mysql://localhost:3306/interview_db`；
   - 包含 UTF-8 编码、时区 (`Asia/Shanghai`) 与公共密钥检索配置；
   - 配置 HikariCP 连接池参数（最大连接数、最小空闲数、生命周期与连接校验语句）。
2. **Redis 与 Lettuce 连接池**：
   - 默认端口 `6379`，数据库 `0`；
   - 启用 `commons-pool2` 支持的 Lettuce 连接池，有效防止高并发下的连接阻塞。
3. **Redis 序列化规范**：
   - 针对面试中常问的“Redis 默认序列化乱码”问题，在 `RedisConfig` 中统一配置：
     - **Key / HashKey**：采用 `StringRedisSerializer`；
     - **Value / HashValue**：采用 `GenericJackson2JsonRedisSerializer`，以可读 JSON 格式落盘。

---

## 🚀 快速启动

1. 启动本地 MySQL 与 Redis 实例：
   ```bash
   # 确保 MySQL 服务开启并在 3306 端口监听
   # 确保 Redis 服务开启并在 6379 端口监听
   ```
2. 编译项目：
   ```bash
   ./gradlew classes
   ```
3. 启动应用：
   ```bash
   ./gradlew bootRun
   ```
4. 访问服务探活接口：
   ```bash
   curl http://localhost:8080/health/ping
   ```
   返回示例：
   ```json
   {
     "code": 200,
     "message": "Service is running smoothly",
     "data": {
       "service": "mysqlandredis-interview",
       "status": "UP",
       "timestamp": 1727256000000
     },
     "timestamp": 1727256000000
   }
   ```

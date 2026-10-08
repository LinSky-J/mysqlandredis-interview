package com.jinlin.mysqlandredis;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MySQL 与 Redis 面试演练工程启动类
 * <p>
 * 注：若后续扩展 MyBatis Mapper 接口，可在类上按需开启 @MapperScan("com.jinlin.mysqlandredis.mapper")
 */
@SpringBootApplication
public class MysqlandredisApplication {

    public static void main(String[] args) {
        SpringApplication.run(MysqlandredisApplication.class, args);
    }

}

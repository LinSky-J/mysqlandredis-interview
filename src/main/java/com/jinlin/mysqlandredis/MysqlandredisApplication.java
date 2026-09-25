package com.jinlin.mysqlandredis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MySQL 与 Redis 面试演练工程启动类
 */
@SpringBootApplication
@MapperScan("com.jinlin.mysqlandredis.mapper")
public class MysqlandredisApplication {

    public static void main(String[] args) {
        SpringApplication.run(MysqlandredisApplication.class, args);
    }

}

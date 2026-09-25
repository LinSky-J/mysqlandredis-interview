package com.jinlin.mysqlandredis.controller;

import com.jinlin.mysqlandredis.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务健康检查控制器
 */
@RestController
@RequestMapping("/health")
public class HealthCheckController {

    @GetMapping("/ping")
    public Result<Map<String, Object>> ping() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "mysqlandredis-interview");
        info.put("status", "UP");
        info.put("timestamp", System.currentTimeMillis());
        return Result.success("Service is running smoothly", info);
    }
}

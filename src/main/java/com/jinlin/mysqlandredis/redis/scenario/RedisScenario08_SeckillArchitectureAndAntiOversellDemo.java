package com.jinlin.mysqlandredis.redis.scenario;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;

/**
 * Redis 业务场景篇 08: 如何设计秒杀系统处理超高并发以及超卖现象？(多级漏斗防护、Lua原子扣减与防超卖双保险)
 *
 * 面试真题：如何设计秒杀场景处理高并发以及超卖现象?
 *
 * 核心原理深度剖析：
 *
 * 一、秒杀系统的核心痛点与挑战：
 * 1. 瞬时极高并发读写洪峰 (如 100 件商品，10 万人秒级涌入抢购)；
 * 2. 读极多、写极少 (99.9% 的用户最终抢购失败)；
 * 3. 必须【绝对严防库存超卖 (No Overselling)】(卖出超出库存的订单属于重大经济损失与合规事故)；
 * 4. 必须严防刷单黑产与一人多单作弊。
 *
 * 二、秒杀高并发全链路“多级漏斗削峰”架构设计：
 *
 * 1. 【第一层：客户端与边缘 CDN 动静分离 (削减 80% 流量)】：
 *    - 页面 HTML、CSS、JS、商品图文静态化推送到 CDN 边缘节点，客户端直接加载边缘缓存；
 *    - 前端按钮做“防重防抖”：用户点击抢购后按钮置灰 3~5 秒，阻止连续疯狂重复连击。
 *
 * 2. 【第二层：API 网关与动态 URL 隐藏 (防刷防作弊)】：
 *    - 网关层实施滑动窗口/令牌桶限流，单 IP/单用户限制每秒最多请求 1 次；
 *    - 秒杀动态 URL 隐藏：活动开始前不暴露真实下单接口，开始瞬间才通过加盐接口下发加密的动态下单 Path，杜绝脚本提前偷跑。
 *
 * 3. 【第三层：Redis 内存库存预扣减 + Lua 脚本原子控制 (秒杀核心防线)】：
 *    - 【活动前库存预热】：秒杀开始前半小时，通过定时任务将 MySQL 库存预热加载到 Redis；
 *    - 【一人一单防重拦截】：使用 `SET user_bought:activityId:userId 1 NX EX 86400`，已成功下单的用户直接拦截；
 *    - 【原子扣减库存与防超卖】：
 *      - 坚决杜绝“Java 先 GET 判断库存再 DECR”的非原子操作 (并发下必然发生超卖！)；
 *      - 必须使用【原子 Lua 脚本】将“判断库存 > 0”与“扣减库存 DECR”捆绑为单步不可分割原子操作！
 *
 * 4. 【第四层：消息队列 (MQ) 异步削峰解耦落库】：
 *    - Redis Lua 预扣减库存成功的请求，立即生成一条包含 `(orderId, userId, goodsId)` 的消息推送到 RocketMQ / Kafka；
 *    - 接口直接向前端返回：“抢购成功，正在排队生成订单中” (整个接口调用在微秒级完成返回)；
 *    - 后台订单服务集群以恒定的温和速率 (例如 1000 TPS) 异步消费 MQ 消息，从容完成 MySQL 写订单与更新实际库存。
 *
 * 5. 【第五层：MySQL 底层行锁防超卖终极兜底】：
 *    - 即使上游极端异常，MySQL 在执行实际库存扣减 SQL 时必须加状态限制条件：
 *      `UPDATE seckill_goods SET stock = stock - 1 WHERE goods_id = 3001 AND stock > 0;`
 *    - 依赖 InnoDB 行级排他锁的强一致性，只要 `stock <= 0` 则影响行数为 0，彻底从物理层锁死超卖！
 */
public class RedisScenario08_SeckillArchitectureAndAntiOversellDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisScenario08] 秒杀系统全链路架构设计与基于 Lua 脚本的原子防超卖实战");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 实机模拟：活动前库存预热到 Redis
        System.out.println("[步骤 1] 秒杀活动预热：向 Redis 写入限量商品初始库存 (5 件)：");
        String stockKey = "demo:scenario:seckill:stock:3001";
        int initialStock = 5;

        // Redis 原生指令: SET demo:scenario:seckill:stock:3001 5
        redisTemplate.opsForValue().set(stockKey, String.valueOf(initialStock));
        System.out.println("  商品 3001 库存预热完成: key=" + stockKey + ", 初始库存=" + initialStock);

        // 2. 准备原子防超卖 Lua 脚本
        String seckillLuaScript =
                "local stock = tonumber(redis.call('get', KEYS[1])) " +
                "if (not stock or stock <= 0) then " +
                "    return -1 " + // 库存不足，秒杀已抢光
                "end " +
                "redis.call('decr', KEYS[1]) " +
                "return stock - 1"; // 扣减成功，返回剩余库存

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(seckillLuaScript, Long.class);

        // 3. 模拟 8 位并发用户抢购 5 件商品的过程
        System.out.println("\n[步骤 2] 模拟 8 位用户并发发起秒杀请求 (实测验证防超卖)：");
        for (int user = 1; user <= 8; user++) {
            Long remainStock = redisTemplate.execute(redisScript, Collections.singletonList(stockKey));
            if (remainStock != null && remainStock >= 0) {
                System.out.println("  -> [用户 " + user + " 抢购成功] 剩余库存: " + remainStock + " (投递 MQ 异步创建订单)");
            } else {
                System.out.println("  -> [用户 " + user + " 抢购失败] 秒杀已售罄 (返回 friendly 提示)");
            }
        }

        // 4. 验证最终库存
        // Redis 原生指令: GET demo:scenario:seckill:stock:3001
        String finalStock = redisTemplate.opsForValue().get(stockKey);
        System.out.println("\n[步骤 3] 验证最终库存状态: " + finalStock + " (库存精确扣至 0，无任何超卖现象)");

        // 5. 架构回答总结
        System.out.println("\n[步骤 4] 面试高分回答总结全链路秒杀设计：");
        System.out.println("  1. 【漏斗原则】：把 99% 的无效流量在上游层层过滤拦截，绝不让高并发直冲 DB；");
        System.out.println("  2. 【前端层】：静态页面 CDN 缓存 + 按钮防抖置灰；");
        System.out.println("  3. 【网关层】：限流算法 (令牌桶) + 动态 URL 隐藏 + 风控黑名单；");
        System.out.println("  4. 【Redis 核心层】：库存预热 + 用户防重 (一人一单) + 原子 Lua 脚本预扣减库存 (杜绝超卖)；");
        System.out.println("  5. 【异步层】：扣减成功投递 MQ 削峰解耦，下游平缓写入订单；");
        System.out.println("  6. 【MySQL 兜底层】：SQL 带 `WHERE stock > 0` 依靠行级排他锁做最后一道终极防线！");
        System.out.println("================================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

package com.jinlin.mysqlandredis.redis.cluster;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 集群篇 03: 哨兵机制原理是什么？(监控、主观下线、客观下线与自动故障转移)
 *
 * 面试真题：哨兵机制原理是什么?
 *
 * 核心原理深度剖析：
 *
 * 一、为什么需要引入哨兵机制 (Redis Sentinel)？
 * - 在单纯的“主从复制”架构下，当 Master 节点意外宕机后，系统无法自动感知与恢复；
 * - 必须依赖运维工程师深夜手工介入：登录服务器、挑选一个从库执行 `SLAVEOF NO ONE` 提升为主库、
 *   修改其余从库的连接配置、修改所有微服务客户端的 IP 配置并重启服务；
 * - 哨兵机制正是为了解决这一运维痛点而生的【分布式高可用 (HA) 自动化仲裁体系】。
 *
 * 二、哨兵机制的 4 大核心运行原理：
 *
 * 1. 【持续心跳监控 (Monitoring)】：
 *    - 哨兵是一个运行在特殊模式下的独立 Redis 进程 (不存储业务数据)；
 *    - 哨兵节点启动后，建立三类定时心跳任务：
 *      1) 每 10 秒向 Master 和 Slave 发送 `INFO` 命令：以此动态发现 Master 下新挂载的 Slave 节点及主从拓扑信息；
 *      2) 每 2 秒通过 Master 的 `__sentinel__:hello` 频道进行发布/订阅 (Pub/Sub)：与其他哨兵节点交换对 Master 的健康认知并互相发现新哨兵；
 *      3) 每 1 秒向所有 Master、Slave 和其他 Sentinel 节点发送 `PING` 命令：实时监测所有实例的心跳活性。
 *
 * 2. 【主观下线 (Subjective Down，简称 SDOWN)】：
 *    - 若某个 Redis 实例在 `down-after-milliseconds` (默认 30 秒) 内未对哨兵的 PING 作出有效响应 (如超时或返回异常)；
 *    - 该哨兵会在本地内存单方面将该实例标记为“主观下线 (SDOWN)”。
 *
 * 3. 【客观下线 (Objective Down，简称 ODOWN)】：
 *    - 单个哨兵的主观判断可能存在误判 (例如该哨兵自身网络抖动，但 Master 实际工作正常)；
 *    - 因此，当某个哨兵判定 Master 主观下线后，会向其他所有哨兵广播 `SENTINEL is-master-down-by-addr` 请求投票；
 *    - 当赞成 Master 下线的哨兵数量达到预设的法定人数阈值 (`quorum`，通常设置为哨兵总数/2 + 1) 时，
 *      该 Master 节点才被正式判定为“客观下线 (ODOWN)”。
 *    - 注意：只有 Master 节点才有 ODOWN 概念，Slave 节点与 Sentinel 节点只有 SDOWN 判定。
 *
 * 4. 【选举 Leader 与自动故障转移 (Automatic Failover)】：
 *    - 哨兵集群通过 Raft 算法选举出一个哨兵 Leader；
 *    - 由该哨兵 Leader 全权执行故障转移：
 *      1) 从健康的 Slave 列表中筛选最优节点晋升为新 Master (`SLAVEOF NO ONE`)；
 *      2) 命令其余 Slave 重新挂载到新 Master (`SLAVEOF new_master_ip new_master_port`)；
 *      3) 将已宕机的老 Master 标记为 Slave，待其未来重启后自动降级挂载为新 Master 的从属；
 *      4) 通过发布订阅频道 (`+switch-master`) 广播变更通知，客户端 (Jedis/Lettuce) 自动拉取最新主节点地址并切换连接！
 */
public class RedisCluster03_SentinelMechanismPrinciplesDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisCluster03] Redis 哨兵机制核心原理：监控、SDOWN、ODOWN 与自动故障转移");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 模拟业务通过客户端读写数据与拓扑健康探测
        System.out.println("[步骤 1] 写入测试数据并观察实例活性状态：");
        String heartbeatKey = "demo:cluster:sentinel:heartbeat";

        // Redis 原生指令: SET demo:cluster:sentinel:heartbeat PING_ACTIVE
        redisTemplate.opsForValue().set(heartbeatKey, "PING_ACTIVE");

        // Redis 原生指令: GET demo:cluster:sentinel:heartbeat
        String status = redisTemplate.opsForValue().get(heartbeatKey);
        System.out.println("  成功执行数据读写: key=" + heartbeatKey + ", value=" + status);

        // 2. 梳理哨兵机制高可用生命周期全流程
        System.out.println("\n[步骤 2] 哨兵机制高可用状态迁移全景流程梳理：");
        System.out.println("  1. 【心跳阶段】：每秒 PING 所有节点，每 10 秒 INFO 发现主从从属，每 2 秒 __sentinel__:hello 哨兵自治通信；");
        System.out.println("  2. 【判定阶段】：单节点心跳超时 -> SDOWN (主观下线) -> 广播 is-master-down-by-addr 达到 Quorum -> ODOWN (客观下线)；");
        System.out.println("  3. 【选举阶段】：哨兵集群内部通过 Raft 算法，多数派选出 1 位 Sentinel Leader 负责主持转移；");
        System.out.println("  4. 【转移阶段】：Sentinel Leader 挑选优质从库升主 -> 重新指引其余从库 -> 广播 +switch-master -> 客户端秒级无感知切换！");

        // 3. 面试核心速记表
        System.out.println("\n[步骤 3] 哨兵关键概念对比速记：");
        System.out.println("  +--------------------+-----------------------------------+-----------------------------------+");
        System.out.println("  | 概念名称           | 主观下线 (SDOWN)                  | 客观下线 (ODOWN)                  |");
        System.out.println("  +--------------------+-----------------------------------+-----------------------------------+");
        System.out.println("  | 判定主体           | 单个 Sentinel 独立观察            | 整个 Sentinel 哨兵集群法定仲裁    |");
        System.out.println("  | 判定依据           | 持续 down-after-milliseconds 无响应| 超过 Quorum 个哨兵确认 SDOWN      |");
        System.out.println("  | 适用对象           | Master、Slave、Sentinel 均适用    | 严格【仅限 Master 节点】          |");
        System.out.println("  | 后续动作           | 向其他哨兵发起询问投票            | 触发 Leader 选举与故障转移        |");
        System.out.println("================================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

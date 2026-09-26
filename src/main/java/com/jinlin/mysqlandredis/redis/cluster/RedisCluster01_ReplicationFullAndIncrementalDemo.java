package com.jinlin.mysqlandredis.redis.cluster;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Properties;

/**
 * Redis 集群篇 01: Redis 主从同步中的增量和完全同步怎么实现？
 *
 * 面试真题：Redis主从同步中的增量和完全同步怎么实现?
 *
 * 核心原理深度剖析：
 *
 * 一、全量同步 (Full Resynchronization) 的实现机制：
 * 1. 触发场景：
 *    - 从节点 (Slave) 首次连接主节点 (Master) 时；
 *    - 网络断连时间过长，导致从节点的复制偏移量已经超出了主节点的环形积压缓冲区 (`repl_backlog_buffer`) 范围。
 * 2. 5 步完整同步交互流程：
 *    - 第 1 步：从节点向主节点发送 `PSYNC ? -1` 命令，表明自己初次连接，申请全量同步；
 *    - 第 2 步：主节点收到请求后，在后台调用 `fork()` 执行 `BGSAVE` 生成当前内存的 RDB 快照文件；
 *      同时，主节点为该 Slave 分配一个专用的【复制缓冲区 (`replication buffer`)】，用于记录从 BGSAVE 开始到快照发送完成期间新产生的所有客户端写命令；
 *    - 第 3 步：主节点将生成好的 RDB 快照文件通过网络发送给从节点；
 *    - 第 4 步：从节点接收到 RDB 后，首先彻底清空自身的全部现有数据 (`FLUSHALL`)，然后将 RDB 文件反序列化载入内存；
 *    - 第 5 步：主节点把 `replication buffer` 中累积的增量写操作按顺序发送给从节点，从节点重放这些命令，此时双方状态完全一致，全量同步完成并进入平稳的“命令传播阶段”。
 *
 * 二、增量同步 (Partial Resynchronization，PSYNC) 的实现机制：
 * 1. 触发场景：
 *    - 主从网络因瞬时抖动发生短暂网络闪断并迅速重连时。
 * 2. 核心底层支撑组件：
 *    - 【主节点运行 ID (`master_replid`)】：40 位随机十六进制字符串，每个 Master 启动时生成，用于唯一标识主库身份；
 *    - 【复制偏移量 (`repl_offset`)】：Master 和 Slave 双方各自维护一个偏移量计数器 (以字节为单位)。
 *      Master 发送 N 字节命令时 `master_repl_offset += N`；Slave 成功接收 N 字节时 `slave_repl_offset += N`。
 *      两者差值即为主从复制延迟大小；
 *    - 【复制积压缓冲区 (`repl_backlog_buffer`)】：
 *      - Master 维护的一个全局共享的【固定大小 FIFO 环形缓冲区】(默认 1MB，由 `repl-backlog-size` 配置)；
 *      - Master 的所有写命令在向各 Slave 传播的同时，也会写入该环形缓冲区。
 * 3. 增量重连判定流程：
 *    - 第 1 步：从节点重连后，向主节点发送 `PSYNC <master_replid> <slave_repl_offset>`；
 *    - 第 2 步：主节点校验：
 *      1) 检查传入的 `master_replid` 是否与当前主节点的 ID 匹配；
 *      2) 检查 `slave_repl_offset` 之后的增量数据是否依然完整保留在 `repl_backlog_buffer` 环形缓冲区中；
 *    - 第 3 步：
 *      - 【命中增量】：若 offset 仍在环形缓冲区内，Master 返回 `+CONTINUE` 响应，
 *        并只将 offset 到当前 master_repl_offset 之间的差量写命令发送给 Slave 重放，消耗极小！
 *      - 【增量失效回退】：若断连时间过久，环形缓冲区已被新的写入轮转覆盖，Master 只能无奈返回 `+FULLRESYNC`，被迫降级为高开销的全量同步。
 */
public class RedisCluster01_ReplicationFullAndIncrementalDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisCluster01] Redis 主从同步深度剖析：全量同步 RDB vs 增量同步 repl_backlog");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 模拟写入测试数据
        System.out.println("[步骤 1] 写入测试业务数据并观察复制传播目标：");
        String syncKey = "demo:cluster:sync:order:1001";

        // Redis 原生指令: SET demo:cluster:sync:order:1001 Paid
        redisTemplate.opsForValue().set(syncKey, "Paid");

        // Redis 原生指令: GET demo:cluster:sync:order:1001
        String syncValue = redisTemplate.opsForValue().get(syncKey);
        System.out.println("  已写入订单数据: key=" + syncKey + ", value=" + syncValue);

        // 2. 底层探测当前实例的主从复制核心元数据 (role, master_replid, repl_backlog)
        System.out.println("\n[步骤 2] 探测当前实例的主从复制元数据与积压缓冲区配置：");
        redisTemplate.execute((RedisConnection connection) -> {
            // Redis 原生指令: INFO replication
            Properties replInfo = connection.info("replication");
            if (replInfo != null) {
                System.out.println("  [节点角色] role=" + replInfo.getProperty("role"));
                System.out.println("  [已连接从库数] connected_slaves=" + replInfo.getProperty("connected_slaves"));
                System.out.println("  [主库运行 ID] master_replid=" + replInfo.getProperty("master_replid"));
                System.out.println("  [主库全局复制偏移量] master_repl_offset=" + replInfo.getProperty("master_repl_offset"));
                System.out.println("  [环形缓冲区容量] repl_backlog_size=" + replInfo.getProperty("repl_backlog_size") + " 字节 (默认 1MB)");
                System.out.println("  [环形缓冲区首字节偏移] repl_backlog_first_byte_offset=" + replInfo.getProperty("repl_backlog_first_byte_offset"));
            }
            return null;
        });

        // 3. 架构对比总结
        System.out.println("\n[步骤 3] 主从同步全量与增量核心差异对照表：");
        System.out.println("  +----------------+---------------------------------------+---------------------------------------+");
        System.out.println("  | 对比维度       | 全量同步 (Full Resynchronization)     | 增量同步 (Partial Resynchronization)  |");
        System.out.println("  +----------------+---------------------------------------+---------------------------------------+");
        System.out.println("  | 触发时机       | 初次从库加入、或断连过久 offset 溢出  | 网络短暂抖动重连、offset 仍在缓冲区内 |");
        System.out.println("  | 协议指令       | PSYNC ? -1                            | PSYNC <master_replid> <offset>        |");
        System.out.println("  | 主库响应       | +FULLRESYNC <master_replid> <offset>  | +CONTINUE                             |");
        System.out.println("  | 同步载体       | RDB 二进制镜像文件 + replication_buf  | 环形 repl_backlog_buffer 差量命令     |");
        System.out.println("  | 性能与网络开销 | 极高 (fork子进程、全量网络传输与加载) | 极低 (仅重传少量网络丢失命令)         |");
        System.out.println("  | 生产调优建议   | 适当调大 repl-backlog-size (如 64MB)  | 避免网络稍有波动就引发全量同步风暴    |");
        System.out.println("================================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

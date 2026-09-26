package com.jinlin.mysqlandredis.redis.persistence;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Redis 持久化篇 01: Redis 有哪 2 种持久化方式？分别的优缺点是什么？
 *
 * 面试真题：Redis有哪2种持久化方式？分别的优缺点是什么?
 *
 * 核心原理深度剖析：
 *
 * 一、RDB 持久化 (Redis DataBase，内存快照)：
 * 1. 概念与原理：
 *    - 在指定的时间间隔内，将 Redis 内存中的全量数据快照 (Point-in-Time Snapshot) 写入磁盘，生成压缩的二进制文件 (默认 dump.rdb)；
 *    - 触发机制：
 *      1) 自动触发：配置文件规则 `save <seconds> <changes>` (如 `save 900 1`, `save 300 10`, `save 60 10000`)；
 *      2) 手动触发：
 *         - `SAVE`：由主线程同步执行保存，在 RDB 生成完成前【完全阻塞】所有客户端请求，生产环境严禁使用！
 *         - `BGSAVE`：主线程调用操作系统 `fork()` 函数创建子进程，由子进程异步在后台执行 RDB 写入，主线程继续处理读写请求。
 *    - 核心底层机制：Linux COW (Copy-On-Write 写时复制)：
 *      fork 出的子进程与父进程共享同一块物理内存空间，只有当主线程处理写请求修改某一内存页时，操作系统才会为该页复制一份独立副本，
 *      子进程写入磁盘的数据依然是 fork 瞬间的历史快照数据，极大减少了内存拷贝开销。
 * 2. RDB 优点：
 *    - 二进制压缩文件：文件紧凑小巧，极其适合定期全量冷备份、灾难恢复与跨机房灾备传输；
 *    - 恢复极速：服务重启加载数据时，RDB 直接将二进制镜像反序列化载入内存，速度远远快于 AOF 逐条重放命令；
 *    - 性能影响小：由后台独立子进程负责 I/O 写入，主线程无需承担磁盘写操作 (除 fork 瞬间的开销外)。
 * 3. RDB 缺点：
 *    - 数据安全性较低 (丢失窗口大)：因为快照是定期触发的，如果两次快照间隔期间服务器突发宕机，将丢失上一次快照以后的全部写入数据；
 *    - fork 瞬间的 STW 阻塞与内存膨胀风险：若内存数据集庞大 (例如数 10GB)，fork() 复制页表耗时可能达到百毫秒级甚至秒级；
 *      若 fork 期间伴随高并发写入，COW 写时复制会导致物理内存使用量接近翻倍，可能触发系统 OOM Killer。
 *
 * 二、AOF 持久化 (Append Only File，追加日志)：
 * 1. 概念与原理：
 *    - 以独立日志文件的形式，实时/准实时记录 Redis 执行的每一个写操作命令 (按 RESP 文本协议格式追加写入)；
 *    - 写入流程三步走：
 *      1) 命令执行并写入内存；
 *      2) 命令追加到 AOF 缓冲区 (`aof_buf`)；
 *      3) 根据配置的 `appendfsync` 策略将缓冲区数据刷写到磁盘操作系统 Page Cache 并执行系统 `fsync`。
 *    - 三种刷盘策略 (`appendfsync`)：
 *      - `always` (每次写都刷盘)：每个写命令执行完立即调用 fsync。数据安全性最高 (几乎零丢失)，但性能最差 (受限于磁盘 I/O 吞吐)；
 *      - `everysec` (每秒异步刷盘，官方默认推荐)：每秒由后台专门的子线程执行一次 fsync。兼顾极致性能与高可靠性，极端宕机最多丢失 1 秒数据；
 *      - `no` (由操作系统决定刷盘)：只 write 到系统 Page Cache，由 OS 自行调度刷盘 (Linux 通常约 30 秒)。性能最高，但宕机丢失量不可控。
 *    - AOF 重写机制 (`BGREWRITEAOF`)：
 *      - 原因：随时间推移，AOF 文件会无限增大 (包含大量针对同一 Key 的多次覆盖、递增和已删除指令)；
 *      - 原理：重写时不读取旧 AOF 文件，而是【直接读取当前内存数据库的状态】，转化为创建当前数据所需的最小集合命令；
 *      - 异步安全：fork 子进程重写，主线程增量写入 `aof_rewrite_buf`，子进程写完后由主线程追加并原子替换旧文件。
 * 2. AOF 优点：
 *    - 数据安全性极高：使用默认的 everysec 模式最多仅损失 1 秒数据；
 *    - 格式清晰可读：AOF 是明文 RESP 协议文本。若误操作执行了 `FLUSHALL`，只要未触发 AOF 重写，可立即停机编辑 AOF 文件删去末尾的 FLUSHALL 命令并重启恢复；
 *    - 无破坏性损坏：如果追加过程中突发断电导致文件末尾写入不完整，可使用 `redis-check-aof --fix` 工具轻松修复。
 * 3. AOF 缺点：
 *    - 文件体积大：对于相同规模的数据集，AOF 文件通常比 RDB 二进制快照文件大得多；
 *    - 灾难恢复速度慢：重启加载时需要重新逐条执行一遍 AOF 日志里的全部指令，对于海量写命令的重放耗时显著长于 RDB；
 *    - 极端高写入负载下 QPS 吞吐略低于 RDB。
 *
 * 三、Redis 4.0+ 的终极方案：混合持久化 (Hybrid Persistence)：
 * 1. 开启方式：`aof-use-rdb-preamble yes` (Redis 5.0+ 默认开启)；
 * 2. 实现原理：在 AOF 重写时，不再把内存全量数据转化为 RESP 命令，而是【将内存全量数据以 RDB 二进制快照写入 AOF 文件前半部，
 *    重写期间产生的增量写命令以 AOF 格式追加在文件后半部】；
 * 3. 核心优势：完美融合二者优势——重启恢复时前半部分按 RDB 极速二进制加载，后半部分按 AOF 少量增量日志重放，
 *    既具备 RDB 的秒级快速恢复，又具备 AOF 仅丢 1 秒数据的极致安全性！
 */
public class RedisLog01_RdbVsAofPersistenceDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisLog01] Redis 持久化方式深度剖析：RDB 快照、AOF 日志与 4.0+ 混合持久化");
        System.out.println("================================================================================");

        StringRedisTemplate redisTemplate = RedisConnectionHelper.getStringRedisTemplate();

        // 1. 模拟业务数据写入并演示持久化追踪
        System.out.println("[步骤 1] 写入测试数据，观察数据状态与持久化指令：");
        String userKey = "demo:persistence:user:101";
        String countKey = "demo:persistence:order_counter";

        // Redis 原生指令: SET demo:persistence:user:101 Alice
        redisTemplate.opsForValue().set(userKey, "Alice");

        // Redis 原生指令: INCR demo:persistence:order_counter
        Long currentCounter = redisTemplate.opsForValue().increment(countKey);

        // Redis 原生指令: GET demo:persistence:user:101
        String userValue = redisTemplate.opsForValue().get(userKey);

        System.out.println("  已写入用户数据: key=" + userKey + ", value=" + userValue);
        System.out.println("  已递增订单计数: key=" + countKey + ", 当前值=" + currentCounter);

        // 2. 底层探测当前 Redis 实例的持久化核心参数 (RDB save 规则与 AOF 配置)
        System.out.println("\n[步骤 2] 探测当前 Redis 实例的持久化配置与运行状态：");
        redisTemplate.execute((RedisConnection connection) -> {
            // 获取 RDB 相关配置
            // Redis 原生指令: CONFIG GET save
            Properties saveConfig = connection.getConfig("save");
            // Redis 原生指令: CONFIG GET appendonly
            Properties aofConfig = connection.getConfig("appendonly");
            // Redis 原生指令: CONFIG GET appendfsync
            Properties fsyncConfig = connection.getConfig("appendfsync");
            // Redis 原生指令: CONFIG GET aof-use-rdb-preamble
            Properties hybridConfig = connection.getConfig("aof-use-rdb-preamble");

            System.out.println("  [RDB 配置] save 触发阈值规则: " + (saveConfig != null ? saveConfig.getProperty("save") : "N/A"));
            System.out.println("  [AOF 配置] appendonly 是否开启: " + (aofConfig != null ? aofConfig.getProperty("appendonly") : "N/A"));
            System.out.println("  [AOF 刷盘] appendfsync 策略: " + (fsyncConfig != null ? fsyncConfig.getProperty("appendfsync") : "N/A"));
            System.out.println("  [混合持久化] aof-use-rdb-preamble 状态: " + (hybridConfig != null ? hybridConfig.getProperty("aof-use-rdb-preamble") : "N/A"));

            // Redis 原生指令: LASTSAVE
            Long lastSaveTime = connection.lastSave();
            System.out.println("  [RDB 状态] 上一次成功保存 RDB 快照的 Unix 时间戳: " + lastSaveTime);

            // Redis 原生指令: INFO persistence
            Properties infoPersistence = connection.info("persistence");
            if (infoPersistence != null) {
                System.out.println("  [INFO 指标] rdb_bgsave_in_progress=" + infoPersistence.getProperty("rdb_bgsave_in_progress")
                        + ", rdb_last_bgsave_status=" + infoPersistence.getProperty("rdb_last_bgsave_status")
                        + ", aof_enabled=" + infoPersistence.getProperty("aof_enabled"));
            }
            return null;
        });

        // 3. 打印面试对比精华总结
        System.out.println("\n[步骤 3] 面试核心考点总结速查 (RDB vs AOF vs 混合持久化)：");
        System.out.println("  +----------------+-------------------------------+-------------------------------+-------------------------------+");
        System.out.println("  | 对比维度       | RDB (快照持久化)              | AOF (追加日志持久化)          | Redis 4.0+ 混合持久化         |");
        System.out.println("  +----------------+-------------------------------+-------------------------------+-------------------------------+");
        System.out.println("  | 存储格式       | 紧凑二进制内存镜像 (dump.rdb) | 纯文本 RESP 命令追加 (aof)    | RDB 二进制快照 + AOF 增量追加 |");
        System.out.println("  | 写入机制       | BGSAVE (fork 子进程写时复制)  | 写后追加缓冲区，fsync 刷盘    | 重写时 RDB 镜像 + 增量 RESP   |");
        System.out.println("  | 数据安全性     | 较低 (丢失两次快照间隔数据)   | 极高 (everysec 最多丢 1 秒)   | 极高 (最多丢 1 秒)            |");
        System.out.println("  | 灾难恢复速度   | 极快 (二进制镜像直接载入)     | 慢 (逐条重放全部命令日志)     | 极快 (直接读取二进制前半部)   |");
        System.out.println("  | 文件体积       | 小 (经过 LZF 算法高强度压缩)  | 庞大 (频繁修改累加，需重写)   | 较小 (重写后紧凑规整)         |");
        System.out.println("  | 生产环境最佳实践推荐：");
        System.out.println("    1. 开启 RDB + AOF 双持久化，并将 `aof-use-rdb-preamble yes` (混合持久化) 保持开启；");
        System.out.println("    2. AOF 刷盘策略选择 `appendfsync everysec`，在性能与数据安全间取得黄金平衡；");
        System.out.println("    3. 定期异地冷备 dump.rdb 文件，用于跨机房和容灾部署；");
        System.out.println("    4. 避免在 Master 节点执行高频 BGSAVE/BGREWRITEAOF，可通过从节点 (Slave) 承担备份压力。");
        System.out.println("================================================================================\n");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

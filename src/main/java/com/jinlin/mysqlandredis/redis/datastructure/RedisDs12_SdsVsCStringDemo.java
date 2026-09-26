package com.jinlin.mysqlandredis.redis.datastructure;

import com.jinlin.mysqlandredis.redis.util.RedisConnectionHelper;
import io.lettuce.core.api.sync.RedisCommands;

/**
 * Redis 数据结构篇 12: 简单动态字符串 (SDS) 内部结构与为什么不用 C 语言原生字符串深度解析
 *
 * 面试真题：String 是使用什么存储的？为什么不用 C 语言中的字符串？
 *
 * 核心考点与原理：
 * 1. String 的底层存储结构：
 *    - Redis 的 String 底层采用【SDS (Simple Dynamic String，简单动态字符串)】进行物理存储；
 *    - Redis 3.2+ 针对不同长度字符串设计了 5 种结构体 Header (sdshdr5, sdshdr8, sdshdr16, sdshdr32, sdshdr64)；
 *    - 核心结构体设计（以 sdshdr8 为例）：
 *      struct __attribute__ ((__packed__)) sdshdr8 {
 *          uint8_t len;         // 已使用长度 (字符串实际长度，不含 \0)
 *          uint8_t alloc;       // 分配的总长度 (不含 header 和 \0)
 *          unsigned char flags; // 标志位 (低 3 位记录属于哪种 sdshdr 类型)
 *          char buf[];          // 字节数组，实际存放内容，末尾追加 \0 兼容 C 函数
 *      };
 *
 * 2. 为什么不用 C 语言传统的字符串 (char* 以 \0 结尾)？
 *    - 痛点 1: 获取长度时间复杂度：O(1) vs O(N)
 *      - C 字符串不记录长度，调用 strlen() 必须从头遍历到 \0，耗时 O(N)；
 *      - SDS 直接读取结构体中的 len 字段，获取长度是常数级别的 O(1)，纳秒响应。
 *    - 痛点 2: 二进制安全性 (Binary Safe)
 *      - C 字符串以空字符 \0 判定结尾，导致内容中绝不能包含 \0，无法存储图片、音频或压缩包等原始字节；
 *      - SDS 依赖 len 判定字符串边界，buf 可以安全存储任意包含 \0 的二进制字节流。
 *    - 痛点 3: 杜绝缓冲区溢出 (Buffer Overflow)
 *      - C 语言执行 strcat 拼接时若未提前分配足够内存，会发生严重的内存越界踩踏；
 *      - SDS 的修改函数在操作前自动检查剩余空间 (alloc - len)，若不足会自动触发底层扩容。
 *    - 痛点 4: 空间预分配与惰性释放，减少高频系统调用 (realloc)
 *      - C 语言每次字符串变长或缩短都需要调用一次 realloc 重新分配内存；
 *      - SDS 预分配：修改后长度 < 1MB 时分配 2 倍空间；>= 1MB 时多分配 1MB 备用；
 *      - 惰性释放：字符串截断缩短时不立即 free，只修改 len 记录，为未来追加复用。
 *    - 痛点 5: 依然优雅兼容 C 标准库函数
 *      - SDS 的 buf 字节数组末尾依然遵循 C 规范填充一个 \0，可以直接复用 C 标准库的部分只读函数 (如 printf, strcasecmp)。
 */
public class RedisDs12_SdsVsCStringDemo {

    public static void runDemo() {
        System.out.println("================================================================================");
        System.out.println(">> [RedisDs12] String 底层简单动态字符串 (SDS) 结构与 C 语言原生字符串对比实测");
        System.out.println("================================================================================");

        RedisCommands<String, String> commands = RedisConnectionHelper.getCommands();

        // 1. 实机操作演示 SDS 二进制安全特性 (存储包含 \0 的任意字节)
        System.out.println("[步骤 1] 演示 SDS 的二进制安全 (Binary Safe) 特性：");
        String sdsKey = "demo:sds:binary_safe";
        // 构造一个内部包含控制字符与 \0 的复杂二进制字符串
        String rawBinaryData = "Hello\u0000Redis\u0000World\uffffEnd";
        commands.set(sdsKey, rawBinaryData);

        String fetchedData = commands.get(sdsKey);
        Long strlen = commands.strlen(sdsKey);
        System.out.println("  -> 成功存取包含 '\\0' 空字符的二进制内容: " + fetchedData);
        System.out.println("  -> SDS 执行 STRLEN 耗时为 O(1)，返回精确字节数: " + strlen);

        // 2. 实机演示 SDS 的动态追加扩容 (APPEND)
        System.out.println("\n[步骤 2] 演示 SDS 的动态追加扩容 (杜绝缓冲区溢出并触发空间预分配)：");
        commands.set("demo:sds:append", "Redis");
        commands.append("demo:sds:append", " is very fast!");
        System.out.println("  -> 动态追加后内容: " + commands.get("demo:sds:append"));
        System.out.println("  -> 底层 SDS 自动执行空间预分配 (alloc > len)，避免下一次追加再次触发 malloc。");

        // 3. 对比总结矩阵
        System.out.println("\n[步骤 3] SDS vs C 传统字符串全维度对比表格：");
        System.out.println("  +----------------------+--------------------------+------------------------------------+");
        System.out.println("  | 对比指标             | C 语言传统字符串 (char*) | Redis 简单动态字符串 (SDS)         |");
        System.out.println("  +----------------------+--------------------------+------------------------------------+");
        System.out.println("  | 获取长度复杂度       | O(N) (必须逐字节遍历)    | O(1) (直接读取 len 字段)           |");
        System.out.println("  | 二进制安全           | 否 (遇到 \\0 提前截断)    | 是 (以 len 决定边界，支持任意字节) |");
        System.out.println("  | 缓冲区溢出风险       | 极高 (strcat 易越界被踩) | 杜绝 (API 内部自动检查并扩容)      |");
        System.out.println("  | 内存重分配频率       | 每次增减均需 realloc     | 预分配 + 惰性释放，大幅减少系统调用|");
        System.out.println("  | 兼容 C 标准库函数    | 100% 原生支持            | 兼容 (buf 末尾自动补 \\0)          |");
        System.out.println("  +----------------------+--------------------------+------------------------------------+");
    }
}

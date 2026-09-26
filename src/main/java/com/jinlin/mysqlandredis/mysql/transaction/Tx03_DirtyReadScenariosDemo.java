package com.jinlin.mysqlandredis.mysql.transaction;

import com.jinlin.mysqlandredis.mysql.util.DbConnectionHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 【面试题 03】哪些场景不适合脏读，举个例子？
 *
 * 核心考点剖析：
 * 1. 哪些场景绝不能容忍脏读？
 *    - 核心业务：金融资金结算、银行转账、电商支付、交易发货、库存秒杀扣减、征信准入、票务出票等。
 *    - 核心原因：未提交的事务数据具有极强的不确定性(随时可能因网络超时、断电、业务校验失败而 ROLLBACK)。
 *              如果下游系统基于尚未提交的脏数据做出了不可逆的物理动作(例如发放现金、仓库打包出库、开闸放行)，
 *              一旦前置事务回滚，将造成直接经济损失或安全生产事故！
 *
 * 2. 经典资损案例 (电商自动发货 / 骗货套利)：
 *    - 步骤 1：攻击者 A 账户只有 100 元，试图购买 1000 元的商品。
 *    - 步骤 2：A 伪造转账或利用事务漏洞在数据库中执行转账扣款，但【故意不 COMMIT 提交事务】。
 *    - 步骤 3：在 READ UNCOMMITTED 级别下，商家自动化发货系统读到了 A 的“已扣款”虚假状态，审核通过并指示仓库发货。
 *    - 步骤 4：攻击者 A 立即执行 ROLLBACK 回滚事务，资金毫发无损地退回 A 账户。
 *    - 最终结果：攻击者没花一分钱得到了 1000 元商品，商家遭受 1000 元货款直接资损！
 */
public class Tx03_DirtyReadScenariosDemo {

    public static void runDemo() {
        System.out.println("====================================================================");
        System.out.println("【面试题 03】哪些场景不适合脏读？实战复现电商转账脏读资损案例");
        System.out.println("====================================================================");

        // 1. 初始化买家张三(id=1)余额为 1000 元
        DbConnectionHelper.executeSqlScript("UPDATE tx_account SET balance = 1000.00 WHERE id = 1;");

        // 2. 模拟双连接并发：连接 A 开启事务扣款未提交；连接 B 在 READ UNCOMMITTED 下读取脏数据
        try (Connection connBuyerA = DbConnectionHelper.getConnection();
             Connection connSellerB = DbConnectionHelper.getConnection()) {

            connBuyerA.setAutoCommit(false);
            connSellerB.setAutoCommit(false);

            // 连接 B (商家系统) 设置为隔离级别: READ UNCOMMITTED (允许脏读)
            try (java.sql.Statement stmt = connSellerB.createStatement()) {
                stmt.execute("SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;");
            }

            System.out.println(">>> 阶段 1: 买家 A 开启事务，向商家转账 1000 元扣减余额 (但暂未 COMMIT 提交)...");
            try (PreparedStatement ps = connBuyerA.prepareStatement(
                    "UPDATE tx_account SET balance = balance - 1000.00 WHERE id = 1;")) {
                ps.executeUpdate();
            }

            System.out.println(">>> 阶段 2: 商家系统 B 在 READ UNCOMMITTED 级别下查询买家 A 余额...");
            double dirtyBalance = 0.0;
            try (PreparedStatement ps = connSellerB.prepareStatement(
                    "SELECT balance FROM tx_account WHERE id = 1;")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        dirtyBalance = rs.getDouble("balance");
                    }
                }
            }
            System.out.println("   [商家系统读到脏数据] 读到买家 A 余额已扣减为: " + dirtyBalance + " 元！");
            if (dirtyBalance == 0.0) {
                System.out.println("   [商家系统做出错误决策] 商家自动化系统判定扣款成功，立即触发【生成快递单并安排仓库发货】！");
            }

            System.out.println(">>> 阶段 3: 买家 A 恶意调用 ROLLBACK 撤销转账事务！");
            connBuyerA.rollback();
            System.out.println("   [买家 A 回滚成功] Undo Log 生效，买家 A 余额复原！");

            // 连接 B 提交并查看当前真实数据
            connSellerB.commit();

        } catch (SQLException e) {
            System.err.println("脏读复现实测异常: " + e.getMessage());
        }

        // 3. 最终核验：验证买家分文未少，而商家却已发货
        DbConnectionHelper.printQueryResults("【最终核查】买家 A 最终余额 (验证脏读危害：资金回滚为 1000 元，商家白白发货造成资损！)",
                "SELECT id, user_name, balance FROM tx_account WHERE id = 1;"
        );
    }
}

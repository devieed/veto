package org.veto.core.rdbms.bean;
// 用户对钱包执行的行为
public enum WALLET_BEHAVIOR_TYPE{
        RECHARGE, // 充值
        PLACE_BET, // 下注
        TRANSFER, // 转账
        WITHDRAW, // 提现
}
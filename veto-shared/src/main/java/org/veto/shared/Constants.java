package org.veto.shared;

public interface Constants {
    String USER_CAPTCHA_TTL_NOTE_PREFIX = "veto_user_captcha_";
    // token 刷新时间窗口,即token的剩余有效时间必须在此时间范围内
    Integer TOKEN_REFRESH_TIME_WINDOW = 5 * 60;
    // 正波，切换为false则为反波
    Boolean STRAIGHT_BET = false;

    String ADMIN_LOGIN_SESSION = "admin_user";

    String ADMIN_SUPER = "admin_is_super";

    String ADMIN_CAPTCHA_SESSION_KEY = "admin_user_captcha_key";

    String DERIVED_ADDRESSES_CACHE = "VETO_DERIVED_ADDRESSES_CACHE";

    String DERIVED_ADDRESS_COUNTER = "VETO_DERIVED_ADDRESS_COUNTER";

    String SERVICE_NOICE_CACHE_REDIS_PREFIX = "veto_noice_";
    // 可能是流水组成部分的资金变化枚举，TRANSACTION_RECORDS_DIMENSIONS
    enum FLOW_DIMENSIONS_LIST{
        RECOMMENDED, // 推荐者
        RECHARGE, // 充值
        BET_PLAY, // 下注
        WITHDRAW, // 提款
        TRANSFER, // 转账
        BET_RESULT_REWARD, // 下注开奖结果奖励
        ;
        public static FLOW_DIMENSIONS_LIST fromString(String str){
            if (str == null){
                return null;
            }

            for (FLOW_DIMENSIONS_LIST value : FLOW_DIMENSIONS_LIST.values()) {
                if (value.name().equalsIgnoreCase(str)) {
                    return value;
                }
            }

            return null;
        }
    }
}

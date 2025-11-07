package org.veto.core.common;

import org.checkerframework.checker.units.qual.K;

public interface RedisConfigKeyConstants {

    String KEY_PREFIX = "veto_config_";

    String WITHDRAW_FEE = KEY_PREFIX + "withdraw_fee";

    String BET_FEE = KEY_PREFIX + "bet_fee";

    String RECHARGE_FEE = KEY_PREFIX + "recharge_fee";

    String WITHDRAW_MUST_FLOW_LIMIT_MULTIPLE = KEY_PREFIX + "withdraw_must_flow_limit_multiple";

    String LOGIN_ENABLE = KEY_PREFIX + "login_enable";

    String FIRST_RECHARGE_REWARD = KEY_PREFIX + "first_recharge_reward";
    // 每天的利息
    String DAILY_INTEREST = KEY_PREFIX + "daily_interest";

    String REGISTER_ENABLE = KEY_PREFIX + "register_enable";

    String LOGIN_CAPTCHA_ENABLE = KEY_PREFIX + "login_captcha_enable";

    String REGISTER_CAPTCHA_ENABLE = KEY_PREFIX + "register_captcha_enable";

    String SYSTEM_ENABLE = KEY_PREFIX + "system_enable";

    String SITE_DESCRIPTION = KEY_PREFIX + "site_description";

    String TOKEN_SECRET_KEY = KEY_PREFIX + "token_secret_key";

    String TOKEN_EXPIRE_TIME_SECONDS = KEY_PREFIX + "token_expire_time_seconds";

    String REGISTER_NEED_RECOMMEND = KEY_PREFIX + "register_need_recommend";

    String CAPTCHA_EXPIRE_TIME_SECONDS = KEY_PREFIX + "captcha_expire_time_seconds";
    // 在多少天内充值达到此值才可以升级到特定等级
    String LEVEL_RECHARGE_CYCLE_DAILY = KEY_PREFIX + "level_recharge_cycle_day";

    String DEFAULT_DATA_PAGE_LIMIT = KEY_PREFIX + "default_data_page_size";

    String ALCHEMY_API_KEY = KEY_PREFIX + "alchemy_api_key";

    String ALCHEMY_AUTH_TOKEN = KEY_PREFIX + "alchemy_auth_token";

    String ALCHEMY_TEST_NET =  KEY_PREFIX + "alchemy_test_net";

    String ALCHEMY_WEBHOOK_ID =  KEY_PREFIX + "alchemy_webhook_id";

    String ALCHEMY_SIGNING_KEY =   KEY_PREFIX + "alchemy_signing_key";

    String ALCHEMY_NOTIFY_ID =  KEY_PREFIX + "alchemy_notify_id";

    String SYSTEM_COIN_TYPE = KEY_PREFIX + "system_coin_type";;

    String SYSTEM_WALLET = KEY_PREFIX + "system_wallet";

    // 是否执行服务器接口数据加密,只针对需要登陆之后获取的数据执行加密
    String SERVICE_API_ENCRYPT = KEY_PREFIX + "service_api_encrypt";

    String SYSTEM_DOMAIN = KEY_PREFIX + "system_domain";

    String CONTEST_PENDING_BEFORE_STOP_BET_MIT = KEY_PREFIX + "contest_pending_before_stop_bet_mit";
    // 最低买入金额
    String MIN_BET_AMOUNT = KEY_PREFIX + "min_bet_amount";

    String SYSTEM_NAME = KEY_PREFIX + "system_name";
    // 推荐奖励的付出者
    String RECOMMEND_FEE_PLAYER = KEY_PREFIX + "recommend_fee_player";
    // 可能是流水组成部分的资金变化枚举set
    String TRANSACTION_RECORDS_DIMENSIONS  = KEY_PREFIX + "transaction_records_dimensions";
    // 提款是否需要实名认证
    String WITHDRAW_NEED_REAL_NAME_AUTHENTICATION = KEY_PREFIX + "withdraw_need_real_name_authentication";
}

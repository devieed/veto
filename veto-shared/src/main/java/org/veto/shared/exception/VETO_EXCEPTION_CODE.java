package org.veto.shared.exception;

public interface VETO_EXCEPTION_CODE {
    // 主错误代码，用于区分模块
    String MAIN_ERROR_CODE = "10000";
    // --- 验证码相关错误（11000 - 11099） ---
    String CAPTCHA_FAILED = "11000";
    String CAPTCHA_INVALID = "11001";

    // --- 邮箱相关错误（12000 - 12099） ---
    String EMAIL_FAILED = "12000";
    String EMAIL_EXISTS = "12001";
    String EMAIL_INVALID = "12002";

    // --- 登录相关错误（13000 - 13099） ---
    String LOGIN_FAILED = "13000";
    String LOGIN_USERNAME_OR_PASSWORD_INVALID = "13001";
    String LOGIN_FORBIDDEN = "13002";
    String LOGIN_SYSTEM_DISABLED = "13003";

    // --- 注册相关错误（14000 - 14099） ---
    String REGISTER_FAILED = "14000";
    String REGISTER_USERNAME_EXISTS = "14001";
    String REGISTER_FORBIDDEN = "14002";
    String REGISTER_SYSTEM_DISABLED = "14003";
    String REGISTER_USERNAME_INVALID = "14003";
    String REGISTER_PASSWORD_INVALID = "14004";
    String REGISTER_RECOMMEND_CODE_INVALID = "14005";
    String REGISTER_NICKNAME_EXISTS = "14006";

    // --- Token 相关错误（15000 - 15099） ---
    String TOKEN_FAILED = "15000";
    String TOKEN_INVALID = "15001";

    String PARAMS_INVALID = "16000";

    String NICKNAME_INVALID = "16001";

    String PASSWORD_INVALID = "16002";

    String OLD_PASSWORD_INVALID = "16003";

    String PHONE_EXISTS = "16003";

    String ANNOUNCEMENT_INVALID = "17000";

    String ANNOUNCEMENT_NOT_EXISTS = "170001";

    // 钱包被禁止
    String WALLET_BLOCK = "18000";

    String WALLET_AMOUNT_INVALID = "18001";

    String WALLET_NOT_PARAMS = "18002";

    String WALLET_TYPE_ERROR = "18003";

    String WALLET_INSUFFICIENT_BALANCE = "18004";
    // 未达标定流水
    String WALLET_FLOW_LIMIT_BLOCKED = "18005";

    String CONTEST_INVALID = "19000";

    String CONTEST_STATUS_INVALID = "19001";

    String SCORE_ODDS_INVALID = "20000";
}
package org.veto.core.common;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.veto.core.rdbms.bean.RecommendReward;
import org.veto.core.redis.RedisUtilities;
import org.veto.shared.COIN_TYPE;
import org.veto.shared.Constants;
import org.veto.shared.KeyVal;
import org.veto.shared.wallet.LocalWallet;
import org.veto.shared.wallet.WalletUtil;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Set;

@Component
@Getter
@Setter
@Slf4j
public class ServiceConfig {

    @Resource
    @Getter(AccessLevel.NONE)
    private RedisUtilities redisUtilities;

    private volatile KeyVal<String, RecommendReward.FEE_PLAYER> RECOMMEND_FEE_PLAYER = new KeyVal<>(RedisConfigKeyConstants.RECOMMEND_FEE_PLAYER, RecommendReward.FEE_PLAYER.SYSTEM);

    private volatile KeyVal<String, Boolean> REGISTER_ENABLE = new KeyVal<>(RedisConfigKeyConstants.REGISTER_ENABLE, true);

    private volatile KeyVal<String, Boolean> LOGIN_ENABLE = new KeyVal<>(RedisConfigKeyConstants.LOGIN_ENABLE, true);
    // 首充奖励
    private volatile KeyVal<String, BigDecimal> FIRST_RECHARGE_REWARD = new KeyVal<>(RedisConfigKeyConstants.FIRST_RECHARGE_REWARD, new BigDecimal("0.2"));
    // 每天利息
    private volatile KeyVal<String, BigDecimal> DAILY_INTEREST = new KeyVal<>(RedisConfigKeyConstants.DAILY_INTEREST, new BigDecimal("0.05"));

    private volatile KeyVal<String, BigDecimal> RECHARGE_FEE = new KeyVal<>(RedisConfigKeyConstants.RECHARGE_FEE, new BigDecimal("0.02"));

    private volatile KeyVal<String, Boolean> LOGIN_CAPTCHA_ENABLE = new KeyVal<>(RedisConfigKeyConstants.LOGIN_CAPTCHA_ENABLE, true);

    private volatile KeyVal<String, Boolean> REGISTER_CAPTCHA_ENABLE = new KeyVal<>(RedisConfigKeyConstants.REGISTER_CAPTCHA_ENABLE, true);
    // 默认服务器开启
    private volatile KeyVal<String, Boolean> SYSTEM_ENABLE = new KeyVal<>(RedisConfigKeyConstants.SYSTEM_ENABLE, false);

    private volatile KeyVal<String, Integer> TOKEN_EXPIRE_TIME_SECONDS = new KeyVal<>(RedisConfigKeyConstants.TOKEN_EXPIRE_TIME_SECONDS, 3600);

    private volatile KeyVal<String, Boolean> REGISTER_NEED_RECOMMEND = new KeyVal<>(RedisConfigKeyConstants.REGISTER_NEED_RECOMMEND, false);

    private volatile KeyVal<String, String> SITE_DESCRIPTION = new KeyVal<>(RedisConfigKeyConstants.SITE_DESCRIPTION, "专业的足球反波下注平台");

    private volatile KeyVal<String, Integer> CAPTCHA_EXPIRE_TIME_SECONDS = new KeyVal<>(RedisConfigKeyConstants.CAPTCHA_EXPIRE_TIME_SECONDS, 300);

    private volatile KeyVal<String, Integer> LEVEL_RECHARGE_CYCLE_DAILY = new KeyVal<>(RedisConfigKeyConstants.LEVEL_RECHARGE_CYCLE_DAILY, 7);

    private volatile KeyVal<String, Integer> DEFAULT_DATA_PAGE_SIZE = new KeyVal<>(RedisConfigKeyConstants.DEFAULT_DATA_PAGE_LIMIT, 10);

    private volatile KeyVal<String, Boolean> WITHDRAW_NEED_REAL_NAME_AUTHENTICATION = new KeyVal<>(RedisConfigKeyConstants.WITHDRAW_NEED_REAL_NAME_AUTHENTICATION, true);

    private volatile KeyVal<String, String> ALCHEMY_NOTIFY_ID = new KeyVal<>(RedisConfigKeyConstants.ALCHEMY_NOTIFY_ID, null);

    private volatile KeyVal<String, BigDecimal> WITHDRAW_FEE = new KeyVal<>(RedisConfigKeyConstants.WITHDRAW_FEE, new BigDecimal("0.02"));

    private volatile KeyVal<String, BigDecimal> BET_FEE = new KeyVal<>(RedisConfigKeyConstants.BET_FEE, new BigDecimal("0.08"));
    // 提现要求累计流水的倍数
    private volatile KeyVal<String, Integer> WITHDRAW_MUST_FLOW_LIMIT_MULTIPLE = new KeyVal<>(RedisConfigKeyConstants.WITHDRAW_MUST_FLOW_LIMIT_MULTIPLE, 30);

    // 开球前几分钟停止下注
    private volatile KeyVal<String, Integer> CONTEST_PENDING_BEFORE_STOP_BET_MIT = new KeyVal<>(RedisConfigKeyConstants.CONTEST_PENDING_BEFORE_STOP_BET_MIT, 5);

    //可能是流水组成部分的资金变化枚举,当这些事件触发时，服务会为用户新增当前服务配置的币种的流水额，用于用户升级
    private volatile KeyVal<String, Set<Constants.FLOW_DIMENSIONS_LIST>> TRANSACTION_RECORDS_DIMENSIONS = new KeyVal<>(RedisConfigKeyConstants.TRANSACTION_RECORDS_DIMENSIONS, Set.of(
            Constants.FLOW_DIMENSIONS_LIST.RECHARGE,
            Constants.FLOW_DIMENSIONS_LIST.BET_PLAY
    ));

    // 最低买入金额
    private volatile KeyVal<String, Integer> MIN_BET_AMOUNT = new KeyVal<>(RedisConfigKeyConstants.MIN_BET_AMOUNT, 1);

    private volatile KeyVal<String, Boolean> SERVICE_API_ENCRYPT = new KeyVal<>(RedisConfigKeyConstants.SERVICE_API_ENCRYPT, false);

    private volatile KeyVal<String, String> ALCHEMY_API_KEY = new KeyVal<>(RedisConfigKeyConstants.ALCHEMY_API_KEY, null);

    private volatile KeyVal<String, String> ALCHEMY_AUTH_TOKEN = new KeyVal<>(RedisConfigKeyConstants.ALCHEMY_AUTH_TOKEN, null);

    private volatile KeyVal<String, Boolean> ALCHEMY_IS_TEST_NET = new KeyVal<>(RedisConfigKeyConstants.ALCHEMY_TEST_NET, false);

    private volatile KeyVal<String, Map<String, String>> ALCHEMY_WEBHOOK_AND_SIGNING_KEY = new  KeyVal<>(RedisConfigKeyConstants.ALCHEMY_SIGNING_KEY, null);

    private volatile KeyVal<String, String> TOKEN_SECRET_KEY = new KeyVal<>(RedisConfigKeyConstants.TOKEN_SECRET_KEY, null);

    private volatile KeyVal<String, COIN_TYPE> SYSTEM_COIN_TYPE = new KeyVal<>(RedisConfigKeyConstants.SYSTEM_COIN_TYPE, null);

    private volatile KeyVal<String, LocalWallet> SYSTEM_WALLET = new KeyVal<>(RedisConfigKeyConstants.SYSTEM_WALLET, null);

    private volatile KeyVal<String, String> SYSTEM_DOMAIN = new KeyVal<>(RedisConfigKeyConstants.SYSTEM_DOMAIN, null);

    private volatile KeyVal<String, String> SYSTEM_NAME = new KeyVal<>(RedisConfigKeyConstants.SYSTEM_NAME, "veto");

    public void update(String key, Object value){
        redisUtilities.set(key, value);
    }

    public boolean hasKey(String key) throws IllegalAccessException {
        for (Field field : ServiceConfig.class.getFields()) {
            if (field.getClass().isAssignableFrom(KeyVal.class)) {
                field.setAccessible(true);
                KeyVal keyVal = (KeyVal) field.get(this);
                if (keyVal.getVal().equals(key)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static String generateTokenSecret() {
        // 使用SecureRandom生成更安全的随机数
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(randomBytes);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : randomBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @PostConstruct
    public void refresh() {
        // 初始化所有配置项，如果Redis中不存在则使用默认值并保存到Redis
        refreshConfigValue(LOGIN_ENABLE);
        refreshConfigValue(REGISTER_ENABLE);
        refreshConfigValue(LOGIN_CAPTCHA_ENABLE);
        refreshConfigValue(REGISTER_CAPTCHA_ENABLE);
        refreshConfigValue(SYSTEM_ENABLE);
        refreshConfigValue(MIN_BET_AMOUNT);
        refreshConfigValue(SYSTEM_NAME);
        refreshConfigValue(FIRST_RECHARGE_REWARD);
        refreshConfigValue(RECHARGE_FEE);
        refreshConfigValue(DAILY_INTEREST);
        refreshConfigValue(WITHDRAW_FEE);
        refreshConfigValue(BET_FEE);
        refreshConfigValue(WITHDRAW_MUST_FLOW_LIMIT_MULTIPLE);
        refreshTokenSecretKey();
        refreshConfigValue(TOKEN_EXPIRE_TIME_SECONDS);
        refreshConfigValue(REGISTER_NEED_RECOMMEND);
        refreshConfigValue(CAPTCHA_EXPIRE_TIME_SECONDS);
        refreshConfigValue(LEVEL_RECHARGE_CYCLE_DAILY);
        refreshConfigValue(DEFAULT_DATA_PAGE_SIZE);
        refreshConfigValue(SYSTEM_DOMAIN);
        refreshConfigValue(CONTEST_PENDING_BEFORE_STOP_BET_MIT);
        refreshConfigValue(RECOMMEND_FEE_PLAYER);
        refreshConfigValue(SERVICE_API_ENCRYPT);
        refreshConfigValue(TRANSACTION_RECORDS_DIMENSIONS);
        refreshConfigValue(WITHDRAW_NEED_REAL_NAME_AUTHENTICATION);
        refreshConfigValue(ALCHEMY_IS_TEST_NET);
        // 对于没有默认值的配置项，只从Redis读取
        refreshConfigValueWithoutDefault(SYSTEM_NAME);
        refreshConfigValueWithoutDefault(ALCHEMY_API_KEY);
        refreshConfigValueWithoutDefault(ALCHEMY_AUTH_TOKEN);
        refreshConfigValueWithoutDefault(SYSTEM_WALLET);
        refreshConfigValueWithoutDefault(SYSTEM_COIN_TYPE);
        refreshConfigValueWithoutDefault(ALCHEMY_WEBHOOK_AND_SIGNING_KEY);
        refreshConfigValueWithoutDefault(ALCHEMY_NOTIFY_ID);
    }

    private <T> void refreshConfigValue(KeyVal<String, T> config) {
        if (!redisUtilities.exists(config.getKey())) {
            log.warn("Redis key {} does not exist, init it to default value: {}", config.getKey(), config.getVal());
            redisUtilities.set(config.getKey(), config.getVal());
        } else {
            T value = (T) redisUtilities.get(config.getKey());
            config.setVal(value);
        }
    }

    private <T> void refreshConfigValueWithoutDefault(KeyVal<String, T> config) {
        if (redisUtilities.exists(config.getKey()) && redisUtilities.get(config.getKey(), Object.class) != null) {
            Object value = redisUtilities.get(config.getKey(), String.class);
            @SuppressWarnings("unchecked")
            T typedValue = (T) value;
            config.setVal(typedValue);
        }
    }

    private void refreshTokenSecretKey() {
        if (!redisUtilities.exists(TOKEN_SECRET_KEY.getKey())) {
            String tokenSecretKey = generateTokenSecret();
            log.warn("Redis token secret key does not exist, generating new one");
            redisUtilities.set(TOKEN_SECRET_KEY.getKey(), tokenSecretKey);
            TOKEN_SECRET_KEY.setVal(tokenSecretKey);
        } else {
            String value = redisUtilities.get(TOKEN_SECRET_KEY.getKey(), String.class);
            TOKEN_SECRET_KEY.setVal(value);
        }
    }
}

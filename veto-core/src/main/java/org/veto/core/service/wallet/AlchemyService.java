package org.veto.core.service.wallet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.veto.core.common.ServiceConfig;
import org.veto.core.redis.RedisUtilities;
import org.veto.shared.COIN_TYPE;
import org.veto.shared.Constants;
import org.veto.shared.UrlBuilder;
import org.veto.shared.wallet.LocalWallet;
import org.veto.shared.wallet.WalletUtil;

import java.net.URISyntaxException;
import java.util.*;

/**
 * Alchemy Webhook Service
 * 用于监听 ETH 主网和测试网的地址入账（包括 ERC-20 代币）
 *
 * 免费账户限制：
 * - 最多 5 个 webhooks
 * - 每个 webhook 最多监听 10,000 个地址
 *
 * 使用策略：
 * - 创建一个或多个 Address Activity Webhooks
 * - 动态添加/删除要监听的地址
 * - 监听 ERC-20 代币转账事件
 */
@Service
@Slf4j
public class AlchemyService {

    @Resource
    private RedisUtilities redisUtilities;

    @Resource
    private RestTemplateBuilder restTemplateBuilder;
    // 单个监听最多支持多少个地址
    private Integer ONE_WEBHOOK_MAX_DERIVED_ADDRESS_COUNT = 10000;

    @Resource
    private ServiceConfig serviceConfig;

    private RestTemplate restTemplate;
    private COIN_TYPE coinType;
    private String alchemyApiKey;
    private String alchemyAuthToken; // Webhook 管理需要 Auth Token，不是 API Key
    private String network; // "ETH_MAINNET" 或 "ETH_SEPOLIA"

    // Alchemy Notify API 基础 URL
    private static final String ALCHEMY_NOTIFY_API = "https://dashboard.alchemy.com/api";

    // Alchemy 支持的网络
    private static final String NETWORK_MAINNET = "ETH_MAINNET";
    private static final String NETWORK_SEPOLIA = "ETH_SEPOLIA";

    @PostConstruct
    public void init() {
        // 从配置中获取 Alchemy 配置
        this.alchemyApiKey = serviceConfig.getALCHEMY_API_KEY().getVal();
        this.alchemyAuthToken = serviceConfig.getALCHEMY_AUTH_TOKEN().getVal();

        if (alchemyAuthToken == null || alchemyAuthToken.isEmpty()) {
            log.warn("Alchemy Auth Token not configured. Please set ALCHEMY_AUTH_TOKEN in ServiceConfig.");
        }

        // 初始化 RestTemplate，使用 Bearer Token 认证
        this.restTemplate = restTemplateBuilder
                .defaultHeader("X-Alchemy-Token", alchemyAuthToken)
                .defaultHeader("Content-Type", "application/json")
                .build();

        // 获取系统币种配置
        this.coinType = serviceConfig.getSYSTEM_COIN_TYPE().getVal();

        // 设置网络（根据配置判断是主网还是测试网）
        boolean isTestNet = serviceConfig.getALCHEMY_IS_TEST_NET().getVal();
        this.network = isTestNet ? NETWORK_SEPOLIA : NETWORK_MAINNET;

        log.info("AlchemyService initialized with network: {}, coinType: {}", network, coinType);
//        if (alchemyAuthToken == null || alchemyAuthToken.isEmpty()) {
//            throw new IllegalStateException("Alchemy Auth Token not configured");
//        }
    }

    /**
     * 初始化钱包和 Webhook
     * 1. 创建本地钱包
     * 2. 创建 Alchemy Address Activity Webhook
     * 3. 添加系统钱包地址到监听列表
     */
    public void initWallet(String webhookUrl) {
        // 从配置中获取 Alchemy 配置
        this.alchemyApiKey = serviceConfig.getALCHEMY_API_KEY().getVal();
        this.alchemyAuthToken = serviceConfig.getALCHEMY_AUTH_TOKEN().getVal();

        if (alchemyAuthToken == null || alchemyAuthToken.isEmpty()) {
            log.warn("Alchemy Auth Token not configured. Please set ALCHEMY_AUTH_TOKEN in ServiceConfig.");
        }

        // 初始化 RestTemplate，使用 Bearer Token 认证
        this.restTemplate = restTemplateBuilder
                .defaultHeader("X-Alchemy-Token", alchemyAuthToken)
                .defaultHeader("Content-Type", "application/json")
                .build();

        // 获取系统币种配置
        this.coinType = serviceConfig.getSYSTEM_COIN_TYPE().getVal();

        // 设置网络（根据配置判断是主网还是测试网）
        boolean isTestNet = serviceConfig.getALCHEMY_IS_TEST_NET().getVal();
        this.network = isTestNet ? NETWORK_SEPOLIA : NETWORK_MAINNET;

        log.info("AlchemyService initialized with network: {}, coinType: {}", network, coinType);
        if (alchemyAuthToken == null || alchemyAuthToken.isEmpty()) {
            throw new IllegalStateException("Alchemy Auth Token not configured");
        }

        // 1. 获取本地钱包
        LocalWallet localWallet = serviceConfig.getSYSTEM_WALLET().getVal();
        log.info("Local system wallet created: address={}", localWallet.getAddress());

        try {
            // 2. 清理现有 webhooks（可选）
            List<WebhookResponse> existingWebhooks = getAllWebhooks();
            Map<String, String> webhookIdAndSigningKey = serviceConfig.getALCHEMY_WEBHOOK_AND_SIGNING_KEY().getVal();
            if (webhookIdAndSigningKey == null) {
                webhookIdAndSigningKey = new HashMap<>();
            }
            if (!existingWebhooks.isEmpty()) {
                for (WebhookResponse existingWebhook : existingWebhooks) {
                    if (existingWebhook.isActive){
                        webhookIdAndSigningKey.put(existingWebhook.getId(), existingWebhook.getSigningKey());
                    }
                }
//                log.warn("Found {} existing webhooks, consider cleaning them", existingWebhooks.size());
                // 可选：删除旧的 webhooks
//                 existingWebhooks.forEach(wh -> deleteWebhook(wh.getId()));
            }

            // 保存 webhook ID 到配置
//            serviceConfig.update(serviceConfig.getALCHEMY_WEBHOOK_ID().getKey(), webhookId);

            // 3. 创建 Address Activity Webhook
            String contractAddress = coinType.getContractAddress(); // USDT 合约地址
            // 拆分一下派生地址

//            var webhookResponse = createAndListenDerivedAddress(webhookUrl, localWallet.getChain(), localWallet.getMnemonic());
//            webhookIdAndSigningKey.put(webhookResponse.data.id, webhookResponse.data.signingKey);
//            log.info("Address Activity Webhook created: {}", webhookId);
            // 4. 添加系统钱包地址到监听列表
//            addAddressesToWebhook(webhookId, derivedAddresses);
            log.info("System wallet address added to webhook: {}", localWallet.getAddress());
            serviceConfig.update(serviceConfig.getALCHEMY_WEBHOOK_AND_SIGNING_KEY().getKey(), webhookIdAndSigningKey);
        } catch (Exception e) {
            log.error("Error during Alchemy webhook initialization: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Alchemy webhook", e);
        }
    }

    public WebhookCreateResponse createAndListenDerivedAddress(String webhookUrl, COIN_TYPE coinType, String mnemonic){
        if (!redisUtilities.exists(Constants.DERIVED_ADDRESS_COUNTER)) {
            redisUtilities.set(Constants.DERIVED_ADDRESS_COUNTER, 0);
        }
        int counter = redisUtilities.get(Constants.DERIVED_ADDRESS_COUNTER);
        // 创建完备的派生地址
        WalletUtil walletUtil = new WalletUtil();
        // 每100个拆分到一个list之中去创建
        final int limitCount = 100;

        String newWebhookId = null;

        boolean needCreate = !redisUtilities.exists(Constants.DERIVED_ADDRESSES_CACHE) || redisUtilities.lGetListSize(Constants.DERIVED_ADDRESSES_CACHE) == 0;
        WebhookCreateResponse response = null;
        List<String> derivedAddresses = new ArrayList<>();
        for (int i = 0; i < ONE_WEBHOOK_MAX_DERIVED_ADDRESS_COUNT; i++) {
            counter++;
            derivedAddresses.add(walletUtil.generateDerivedAddress(coinType, mnemonic, (long) (counter)));
            if (i % limitCount == 0 && i != 0){
                if (needCreate){
                    try {
                        response = createAddressActivityWebhook(webhookUrl, null, derivedAddresses);
                        newWebhookId = response.data.id;
                        redisUtilities.lSet(Constants.DERIVED_ADDRESSES_CACHE, Collections.singletonList(derivedAddresses));
                        derivedAddresses.clear();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    needCreate = false;
                }else {
                    try {
                        addAddressesToWebhook(newWebhookId, derivedAddresses);
                        redisUtilities.lSet(Constants.DERIVED_ADDRESSES_CACHE, Collections.singletonList(derivedAddresses));
                        derivedAddresses.clear();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }
        redisUtilities.set(Constants.DERIVED_ADDRESS_COUNTER, counter);
        return response;
    }

    /**
     * 为用户创建派生地址并添加到监听列表
     *
     * @param mnemonic 系统钱包助记词
     * @param userId 用户 ID
     * @return 派生地址
     */
    public String createDerivedAddressForUser(COIN_TYPE coinType, String mnemonic, Long userId) {
        WalletUtil walletUtil = new WalletUtil();
        String derivedAddress = walletUtil.generateDerivedAddress(coinType, mnemonic, userId);
        // 添加监听
        var webhookSigning = serviceConfig.getALCHEMY_WEBHOOK_AND_SIGNING_KEY().getVal();
        for (String s : webhookSigning.keySet()) {
            addAddressesToWebhook(s, Collections.singletonList(derivedAddress));
            break;
        }
        return derivedAddress;
    }

    /**
     * 批量添加地址到 webhook
     *
     * @param addresses 地址列表
     */
    public void addAddressesToMonitoring(List<String> addresses, String webhookId) {
        addAddressesToWebhook(webhookId, addresses);
        log.info("Added {} addresses to webhook monitoring", addresses.size());
    }

    /**
     * 从 webhook 移除地址
     *
     * @param addresses 地址列表
     */
    public void removeAddressesFromMonitoring(List<String> addresses, String webhookId) {
        removeAddressesFromWebhook(webhookId, addresses);
        log.info("Removed {} addresses from webhook monitoring", addresses.size());
    }

    // ===========================
    // Alchemy Webhook API 方法
    // ===========================

    /**
     * 创建 Address Activity Webhook
     * 用于监听特定地址的活动（包括 ERC-20 代币转账）
     *
     * 重要：支持监听任意 ERC-20 代币合约，包括自定义部署的测试合约
     *
     * API: POST /create-webhook
     */
    private WebhookCreateResponse createAddressActivityWebhook(String webhookUrl, String contractAddress, List<String> addresses) {
        try {
            String url = ALCHEMY_NOTIFY_API + "/create-webhook";

            Map<String, Object> body = new HashMap<>();
            body.put("network", this.network);
            body.put("webhook_type", "ADDRESS_ACTIVITY");
            body.put("webhook_url", webhookUrl);
            body.put("addresses", addresses);

            // 配置 webhook 过滤器
            Map<String, Object> addressActivityFilters = new HashMap<>();

//            // 监听 ERC-20 代币转账
            if (contractAddress != null && !contractAddress.isEmpty()) {
                // 方式1：使用 filters（推荐用于单个合约）
                List<Map<String, Object>> filters = new ArrayList<>();
                Map<String, Object> filter = new HashMap<>();
                filter.put("contract_address", contractAddress.toLowerCase()); // 转小写确保匹配
                filters.add(filter);
                addressActivityFilters.put("filters", filters);

                log.info("Creating webhook with ERC-20 token filter: {}", contractAddress);
            } else {
                // 不指定合约地址，监听所有活动（包括 ETH 和所有代币）
                log.info("Creating webhook without contract filter (monitoring all activities)");
            }

            body.put("address_activity_filters", addressActivityFilters);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Alchemy-Token", alchemyAuthToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            log.info("Creating Alchemy webhook with body: {}", body);

            ResponseEntity<WebhookCreateResponse> response = restTemplate.postForEntity(url, request, WebhookCreateResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to create webhook: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("createAddressActivityWebhook error", e);
            throw new RuntimeException("Failed to create Address Activity Webhook", e);
        }
    }

    /**
     * 添加地址到 webhook
     *
     * API: PATCH /update-webhook-addresses
     */
    private void addAddressesToWebhook(String webhookId, List<String> addresses) {
        try {
            String url = ALCHEMY_NOTIFY_API + "/update-webhook-addresses";

            Map<String, Object> body = new HashMap<>();
            body.put("webhook_id", webhookId);
            body.put("addresses_to_add", addresses);
            body.put("addresses_to_remove", Collections.emptyList());

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Alchemy-Token", alchemyAuthToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PATCH, request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to add addresses to webhook: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("addAddressesToWebhook error", e);
            throw new RuntimeException("Failed to add addresses to webhook", e);
        }
    }

    /**
     * 从 webhook 移除地址
     *
     * API: PATCH /update-webhook-addresses
     */
    private void removeAddressesFromWebhook(String webhookId, List<String> addresses) {
        try {
            String url = ALCHEMY_NOTIFY_API + "/update-webhook-addresses";

            Map<String, Object> body = new HashMap<>();
            body.put("webhook_id", webhookId);
            body.put("addresses_to_remove", addresses);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Alchemy-Token", alchemyAuthToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PATCH, request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to remove addresses from webhook: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("removeAddressesFromWebhook error", e);
            throw new RuntimeException("Failed to remove addresses from webhook", e);
        }
    }

    /**
     * 获取所有 webhooks
     *
     * API: GET /team-webhooks
     */
    public List<WebhookResponse> getAllWebhooks() {
        try {
            String url = ALCHEMY_NOTIFY_API + "/team-webhooks";

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Alchemy-Token", alchemyAuthToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<WebhookListResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, WebhookListResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getData();
            } else {
                throw new RuntimeException("Failed to get webhooks: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("getAllWebhooks error", e);
            return new ArrayList<>();
        }
    }

    /**
     * 删除 webhook
     *
     * API: DELETE /delete-webhook
     */
    public void deleteWebhook(String webhookId) {
        try {
            String url = ALCHEMY_NOTIFY_API + "/delete-webhook?webhook_id=" + webhookId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Alchemy-Token", alchemyAuthToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            restTemplate.exchange(url, HttpMethod.DELETE, request, Map.class);

            log.info("Webhook deleted successfully: {}", webhookId);
        } catch (Exception e) {
            log.error("deleteWebhook error", e);
            throw new RuntimeException("Failed to delete webhook", e);
        }
    }

    /**
     * 获取 webhook 详情（包括监听的地址列表）
     *
     * API: GET /webhook-addresses?webhook_id=xxx
     */
    public WebhookAddressesResponse getWebhookAddresses(String webhookId) {
        try {
            String url = ALCHEMY_NOTIFY_API + "/webhook-addresses?webhook_id=" + webhookId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Alchemy-Token", alchemyAuthToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<WebhookAddressesResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, WebhookAddressesResponse.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to get webhook addresses: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("getWebhookAddresses error", e);
            throw new RuntimeException("Failed to get webhook addresses", e);
        }
    }

    // ===========================
    // 响应实体类
    // ===========================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebhookCreateResponse {
        private WebhookData data;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebhookData {
        private String id;
        private String network;
        @JsonProperty("webhook_type")
        private String webhookType;
        @JsonProperty("webhook_url")
        private String webhookUrl;
        @JsonProperty("is_active")
        private Boolean isActive;
        @JsonProperty("time_created")
        private Long timeCreated;
        @JsonProperty("signing_key")
        private String signingKey;
        @JsonProperty("version")
        private String version;

        @JsonProperty("deactivation_reason")
        private String deactivationReason;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebhookListResponse {
        private List<WebhookResponse> data;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebhookResponse {
        private String id;
        private String network;
        @JsonProperty("webhook_type")
        private String webhookType;
        @JsonProperty("webhook_url")
        private String webhookUrl;
        @JsonProperty("is_active")
        private Boolean isActive;
        @JsonProperty("time_created")
        private Long timeCreated;
        @JsonProperty("signing_key")
        private String signingKey;
        private String version;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebhookAddressesResponse {
        private WebhookAddressData data;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebhookAddressData {
        @JsonProperty("webhook_id")
        private String webhookId;
        private List<String> addresses;
        @JsonProperty("total_count")
        private Integer totalCount;
    }

    /**
     * Alchemy Webhook 通知数据结构
     * 当监听的地址收到代币转账时，Alchemy 会发送此格式的数据到你的 webhook URL
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AlchemyWebhookNotification {
        @JsonProperty("webhookId")
        private String webhookId;

        private String id;

        @JsonProperty("createdAt")
        private String createdAt;

        private String type; // "ADDRESS_ACTIVITY"

        private Event event;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Event {
            private String network; // "ETH_SEPOLIA" 或 "ETH_MAINNET"
            private List<Activity> activity;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Activity {
            @JsonProperty("fromAddress")
            private String fromAddress;

            @JsonProperty("toAddress")
            private String toAddress;

            @JsonProperty("blockNum")
            private String blockNum;

            private String hash;

            private String value; // 原生代币数量（ETH）

            @JsonProperty("asset")
            private String asset; // "ETH" 或代币符号

            private String category; // "token" 或 "external"

            @JsonProperty("rawContract")
            private RawContract rawContract;

            private Log log; // ERC-20 转账日志
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class RawContract {
            @JsonProperty("rawValue")
            private String rawValue; // 代币数量（十六进制）

            private String address; // 合约地址

            private Integer decimals; // 代币精度
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Log {
            private String address; // 合约地址
            private List<String> topics;
            private String data;
            @JsonProperty("blockHash")
            private String blockHash;
            @JsonProperty("blockNumber")
            private String blockNumber;
            @JsonProperty("blockTimestamp")
            private String blockTimestamp;
            @JsonProperty("transactionHash")
            private String transactionHash;
            @JsonProperty("transactionIndex")
            private String transactionIndex;
            @JsonProperty("logIndex")
            private String logIndex;
            private Boolean removed;
        }
    }
}
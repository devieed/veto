package org.veto.api.controller;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.veto.api.dto.WithdrawAddDTO;
import org.veto.api.mapper.WalletMapper;
import org.veto.core.authorize.UnAuthorize;
import org.veto.core.common.ServiceConfig;
import org.veto.core.rdbms.bean.BlockchainRechargeOrder;
import org.veto.core.rdbms.bean.UserDepositAddress;
import org.veto.core.rdbms.bean.WalletRecord;
import org.veto.core.rdbms.bean.Withdraw;
import org.veto.core.rdbms.repository.BlockchainRechargeOrderRepository;
import org.veto.core.rdbms.repository.UserDepositAddressRepository;
import org.veto.core.service.WalletService;
import org.veto.core.service.wallet.AlchemyService;
import org.veto.shared.COIN_TYPE;
import org.veto.shared.Response;
import org.veto.shared.Util;
import org.veto.shared.exception.VetoException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/wallet")
@Slf4j
public class WalletController {

    @Resource
    private ServiceConfig serviceConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private BlockchainRechargeOrderRepository blockchainRechargeOrderRepository;

    @Resource
    private UserDepositAddressRepository userDepositAddressRepository;

    @Resource
    private WalletService walletService;

    @Resource
    private WalletMapper walletMapper;

    @GetMapping(value = "/info")
    public Response info(){
        try{
            return Response.success(walletMapper.toUserWalletVO(walletService.info()));
        }catch (VetoException e){
            return Response.error(e.getCode());
        }
    }

    @GetMapping(value = "/record")
    public Response getRecord(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page, @RequestParam(value = "type", required = false) String type){
        if (page < 1) {
            page = 1;
        }

        WalletRecord.TYPE t =  WalletRecord.TYPE.me(type);
        try{
            return Response.success(walletMapper.toWalletRecordVOPage(walletService.getWalletRecords(page, t)));
        }catch (VetoException e){
            return Response.error(e.getCode());
        }
    }

    @PostMapping(value = "/withdraw")
    public Response withdraw(@Valid @RequestBody WithdrawAddDTO withdrawAddDTO){
        try{
            walletService.createWithdraw(walletMapper.toWithdrawAddCommand(withdrawAddDTO));
        }catch (VetoException e){
            return Response.error(e.getCode());
        }

        return Response.success();
    }

    @GetMapping(value = "/withdraw_history")
    public Response withdrawHistory(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page, @RequestParam(value = "status", required = false) String status){
        page = page < 1 ? 1 : page;
        return Response.success(walletService.getWithdrawRecords(page, Withdraw.STATUS.getStatus(status)));
    }

    /**
     * 健康检查端点（Tatum 可能会调用来验证 webhook URL）
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    /**
     * 接收 Alchemy 的 webhook 回调
     *
     * Alchemy 会在检测到地址活动时调用此接口
     *
     * @param signature Alchemy 签名（用于验证请求来源）
     * @param body webhook 通知数据
     * @return 200 OK
     */
    @UnAuthorize
    @PostMapping("/alchemy_callback")
    public ResponseEntity<String> handleAlchemyWebhook(
            @RequestHeader(value = "x-alchemy-signature", required = false) String signature,
            @RequestBody String body) {
        try {
            AlchemyService.AlchemyWebhookNotification notification = objectMapper.readValue(body, AlchemyService.AlchemyWebhookNotification.class);
            log.info("Received Alchemy webhook notification: webhookId={}, type={}",
                    notification.getWebhookId(), notification.getType());

//             1. 验证签名（可选但推荐）
             if (!verifySignature(signature, body, notification.getWebhookId())) {
                 log.warn("Invalid webhook signature");
                 return ResponseEntity.status(401).body("Invalid signature");
             }

            // 2. 处理通知
            if ("ADDRESS_ACTIVITY".equals(notification.getType())) {
                handleAddressActivity(notification);
            }

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Error processing Alchemy webhook", e);
            return ResponseEntity.status(500).body("Error processing webhook");
        }
    }

    /**
     * 处理地址活动通知
     */
    private void handleAddressActivity(AlchemyService.AlchemyWebhookNotification notification) {
        AlchemyService.AlchemyWebhookNotification.Event event = notification.getEvent();
        if (event == null || event.getActivity() == null) {
            log.warn("Invalid event data");
            return;
        }

        List<AlchemyService.AlchemyWebhookNotification.Activity> activitys = event.getActivity();
        for (AlchemyService.AlchemyWebhookNotification.Activity activity : activitys) {
            // 获取基本信息
            String network = event.getNetwork();
            String fromAddress = activity.getFromAddress();
            String toAddress = activity.getToAddress();
            String txHash = activity.getHash();
            String blockNum = activity.getBlockNum();
            String category = activity.getCategory();

            log.info("Address activity: from={}, to={}, hash={}, category={}",
                    fromAddress, toAddress, txHash, category);

            // 判断是 ETH 转账还是代币转账
            if ("token".equals(category)) {
                handleTokenTransfer(activity, toAddress, txHash, blockNum, network);
            } else if ("external".equals(category)) {
                handleEthTransfer(activity, toAddress, txHash, blockNum, network);
            }
        }

    }

    /**
     * 处理 ERC-20 代币转账（包括自定义合约）
     */
    private void handleTokenTransfer(
            AlchemyService.AlchemyWebhookNotification.Activity activity,
            String toAddress,
            String txHash,
            String blockNum,
            String network) {

        AlchemyService.AlchemyWebhookNotification.RawContract rawContract = activity.getRawContract();
        if (rawContract == null) {
            log.warn("Token transfer without rawContract data");
            return;
        }

        // 获取代币信息
        String contractAddress = rawContract.getAddress();
        String rawValue = rawContract.getRawValue();
        Integer decimals = rawContract.getDecimals();
        String asset = activity.getAsset(); // 代币符号，如 "USDT"

        log.info("Token transfer detected: contract={}, asset={}, rawValue={}, decimals={}",
                contractAddress, asset, rawValue, decimals);

        // 转换代币数量
        BigDecimal amount = convertTokenAmount(rawValue, decimals);

        log.info("Token transfer: {} {} to address {} (tx: {})",
                amount, asset, toAddress, txHash);

        // TODO: 处理充值逻辑
        processDeposit(toAddress, contractAddress, amount, asset, txHash, blockNum, network);
    }

    /**
     * 处理 ETH 原生代币转账
     */
    private void handleEthTransfer(
            AlchemyService.AlchemyWebhookNotification.Activity activity,
            String toAddress,
            String txHash,
            String blockNum,
            String network) {

        String value = activity.getValue();
        if (value == null || "0".equals(value)) {
            log.debug("ETH transfer with zero value, skipping");
            return;
        }

        // ETH 的精度是 18 位
        BigDecimal amount = convertTokenAmount(value, 18);

        log.info("ETH transfer: {} ETH to address {} (tx: {})",
                amount, toAddress, txHash);

        // TODO: 处理 ETH 充值逻辑（如果需要）
        processDeposit(toAddress, null, amount, "ETH", txHash, blockNum, network);
    }

    /**
     * 处理充值逻辑
     *
     * @param toAddress 接收地址（用户的派生地址）
     * @param contractAddress 代币合约地址（null 表示 ETH）
     * @param amount 充值数量
     * @param asset 资产符号（USDT, ETH 等）
     * @param txHash 交易哈希
     * @param blockNum 区块号
     * @param network 网络（ETH_SEPOLIA, ETH_MAINNET）
     */
    private void processDeposit(
            String toAddress,
            String contractAddress,
            BigDecimal amount,
            String asset,
            String txHash,
            String blockNum,
            String network) {

        log.info("Processing deposit: address={}, amount={} {}, tx={}, block={}, network={}",
                toAddress, amount, asset, txHash, blockNum, network);

        // 检查合约地址是否有效
        COIN_TYPE coinType = serviceConfig.getSYSTEM_COIN_TYPE().getVal();
        if (coinType.isToken()){
            if (!coinType.getContractAddress().equalsIgnoreCase(contractAddress)){
                // 合约地址错误，直接静默处理
                log.warn("Invalid contract address {}", contractAddress);
                return;
            }
        }

        try {
            // 1. 查找该地址对应的用户
            UserDepositAddress userDepositAddress = userDepositAddressRepository.findByAddressAndStatusIsTrue(toAddress);
            if (userDepositAddress == null) {
                log.warn("Address {} not found in database, skipping deposit", toAddress);
                return;
            }
            // 2. 检查交易是否已处理（防止重复处理）
            if(blockchainRechargeOrderRepository.existsByTxidAndStatusIsTrue(txHash)){
                log.info("Transaction {} already processed, skipping", txHash);
                return;
            }
            BlockchainRechargeOrder blockchainRechargeOrder = new BlockchainRechargeOrder();
            blockchainRechargeOrder.setUserId(userDepositAddress.getUserId());
            blockchainRechargeOrder.setAddress(userDepositAddress.getAddress());
            blockchainRechargeOrder.setTxid(txHash);
            blockchainRechargeOrder.setAmount(amount);
            blockchainRechargeOrder.setStatus(true);
            blockchainRechargeOrderRepository.saveAndFlush(blockchainRechargeOrder);
            // 更新余额
            walletService.recharge(userDepositAddress.getUserId(), amount);
            log.info("Deposit processed successfully: userId={}, amount={} {}",
                    userDepositAddress.getAddress(), amount, asset);

        } catch (Exception e) {
            log.error("Error processing deposit for address {}", toAddress, e);
            // TODO: 可以将失败的充值记录到数据库，稍后重试
        }
    }

    /**
     * 转换代币数量（从 hex 字符串到 BigDecimal）
     *
     * @param rawValue 十六进制值（如 "0x1234" 或直接是数字字符串）
     * @param decimals 代币精度
     * @return 实际数量
     */
    private BigDecimal convertTokenAmount(String rawValue, Integer decimals) {
        if (rawValue == null) {
            return BigDecimal.ZERO;
        }

        try {
            // 移除 "0x" 前缀
            String hexValue = rawValue.startsWith("0x") ? rawValue.substring(2) : rawValue;

            // 如果是空字符串，返回 0
            if (hexValue.isEmpty()) {
                return BigDecimal.ZERO;
            }

            // 转换为 BigInteger
            BigInteger valueInSmallestUnit = new BigInteger(hexValue, 16);

            // 转换为实际数量（除以 10^decimals）
            BigDecimal divisor = BigDecimal.TEN.pow(decimals != null ? decimals : 18);
            return new BigDecimal(valueInSmallestUnit).divide(divisor);

        } catch (Exception e) {
            log.error("Error converting token amount: rawValue={}, decimals={}", rawValue, decimals, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 验证 Alchemy 签名
     *
     * Alchemy 会在 header 中发送签名，用于验证请求确实来自 Alchemy
     * 签名算法：HMAC-SHA256
     *
     * @param signature header 中的签名
     * @param notification 通知数据
     * @return 是否验证通过
     */
    private boolean verifySignature(String signature, String notification, String webhookId) {
        try {
//             从配置中获取 signing key（每个 webhook 都有独立的 signing key）
            String signingKey = getWebhookSigningKey(webhookId);
            if (signingKey == null) {
                log.warn("Signing key not found for webhook: {}", webhookId);
                return true; // 如果没有配置 signing key，暂时放行
            }

//             计算预期签名
            String expectedSignature = computeHmacSha256(signingKey, notification);

//             比较签名
            boolean isValid = signature.equals(expectedSignature);
            if (!isValid) {
                log.warn("Signature mismatch: expected={}, actual={}", expectedSignature, signature);
            }

            return isValid;

        } catch (Exception e) {
            log.error("Error verifying signature", e);
            return false;
        }
    }

    /**
     * 计算 HMAC-SHA256 签名
     */
    private String computeHmacSha256(String key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    private  String getWebhookSigningKey(String webhookId) {
        return serviceConfig.getALCHEMY_WEBHOOK_AND_SIGNING_KEY().getVal().get(webhookId);
    }
//
//    private boolean verifyHmacSignature(HttpServletRequest request, String receivedHash, String webhookSecret) {
//        try {
//            // 读取请求体
//            String body = request.getReader().lines()
//                    .collect(Collectors.joining(System.lineSeparator()));
//
//            // 计算 HMAC
//            Mac mac = Mac.getInstance("HmacSHA256");
//            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(), "HmacSHA256");
//            mac.init(secretKey);
//            byte[] hash = mac.doFinal(body.getBytes());
//
//            // 转换为十六进制字符串
//            StringBuilder hexString = new StringBuilder();
//            for (byte b : hash) {
//                String hex = Integer.toHexString(0xff & b);
//                if (hex.length() == 1) {
//                    hexString.append('0');
//                }
//                hexString.append(hex);
//            }
//
//            String calculatedHash = hexString.toString();
//            boolean isValid = calculatedHash.equals(receivedHash);
//
//            if (!isValid) {
//                log.debug("HMAC verification failed. Expected: {}, Received: {}", calculatedHash, receivedHash);
//            }
//
//            return isValid;
//        } catch (Exception e) {
//            log.error("Error verifying HMAC signature", e);
//            return false;
//        }
//    }
//
//    @UnAuthorize
//    @PostMapping("/tatum_callback")
//    public ResponseEntity<String> handleIncomingTransaction(HttpServletRequest request) {
//        try {
//            // 1️⃣ 读取请求体
//            String body = new BufferedReader(new InputStreamReader(request.getInputStream()))
//                    .lines().collect(Collectors.joining("\n"));
//
//            // 2️⃣ 获取 Tatum webhook HMAC
//            String payloadHash = request.getHeader("x-payload-hash");
//            String webhookSecret = serviceConfig.getTATUM_WEBHOOK_SECRET().getVal();
//
//            // 3️⃣ 验证 HMAC（如果配置了密钥）
//            if (StringUtils.hasText(webhookSecret) && StringUtils.hasText(payloadHash)) {
//                if (!verifyHmacSignature(body, payloadHash, webhookSecret)) {
//                    log.warn("HMAC verification failed");
//                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
//                }
//            }
//
//            // 4️⃣ 反序列化通知
//            TatumWalletService.TatumWebhookNotification notification =
//                    new ObjectMapper().readValue(body, TatumWalletService.TatumWebhookNotification.class);
//
//            // 5️⃣ 基础校验
//            if (notification == null || !StringUtils.hasText(notification.getAddress()) ||
//                    !StringUtils.hasText(notification.getTxId()) ||
//                    !"incoming".equalsIgnoreCase(notification.getDirection())) {
//                log.warn("Invalid webhook notification received: {}", body);
//                return ResponseEntity.badRequest().body("Invalid notification data");
//            }
//
//            log.info("Received Tatum webhook for address {}: asset={}, amount={}",
//                    notification.getAddress(), notification.getAsset(), notification.getAmount());
//
//            // 6️⃣ 处理代币转账（ERC20）
//            if (notification.getTransaction() != null && notification.getTransaction().getTokenTransfers() != null) {
//                for (TatumWalletService.TatumWebhookNotification.TokenTransfer transfer :
//                        notification.getTransaction().getTokenTransfers()) {
//
//                    log.info("Token transfer detected: {} {} to {} (contract: {})",
//                            transfer.getValue(),
//                            transfer.getToken(),
//                            transfer.getTo(),
//                            transfer.getContractAddress());
//
//                    // TODO: 这里可以根据合约地址判断是你想要处理的代币
////                     if (transfer.getContractAddress().equalsIgnoreCase(YOUR_USDT_CONTRACT)) { ... }
//                }
//            }
//
//            // 7️⃣ 处理主币入账
//            if (notification.getAmount() != null && !notification.getAmount().equals("0")) {
//                log.info("Native coin deposit: {} {} to address {}",
//                        notification.getAmount(),
//                        notification.getAsset(),
//                        notification.getAddress());
//
//                // TODO: 处理 ETH / SOL 入账
//            }
//
//            // 8️⃣ 成功处理
//            return ResponseEntity.ok("OK");
//
//        } catch (Exception e) {
//            log.error("Error processing Tatum webhook", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing notification");
//        }
//    }
//
//    /**
//     * HMAC 验证方法
//     */
//    private boolean verifyHmacSignature(String body, String receivedHash, String webhookSecret) throws Exception {
//        Mac mac = Mac.getInstance("HmacSHA256");
//        SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
//        mac.init(secretKey);
//        byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
//
//        StringBuilder hexString = new StringBuilder();
//        for (byte b : hash) {
//            String hex = Integer.toHexString(0xff & b);
//            if (hex.length() == 1) hexString.append('0');
//            hexString.append(hex);
//        }
//
//        String calculatedHash = hexString.toString();
//        boolean isValid = calculatedHash.equals(receivedHash);
//        if (!isValid) {
//            log.debug("HMAC verification failed. Expected: {}, Received: {}", calculatedHash, receivedHash);
//        }
//        return isValid;
//    }
//
//    /**
//     * 使用 HMAC-SHA256 验证 Tatum webhook 签名
//     */
//    private boolean verifySignature(String payload, String signature, String secret) throws Exception {
//        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
//        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
//        sha256_HMAC.init(secretKeySpec);
//        byte[] hashBytes = sha256_HMAC.doFinal(payload.getBytes());
//        String calculatedSignature = Base64.getEncoder().encodeToString(hashBytes);
//        return calculatedSignature.equals(signature);
//    }
}

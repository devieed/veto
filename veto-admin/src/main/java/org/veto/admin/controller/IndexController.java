package org.veto.admin.controller;

import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.veto.admin.conf.ApplicationInitial;
import org.veto.core.authorize.UnAuthorize;
import org.veto.core.common.RedisConfigKeyConstants;
import org.veto.core.common.ServiceConfig;
import org.veto.core.rdbms.bean.RecommendReward;
import org.veto.core.rdbms.repository.ContestRepository;
import org.veto.core.rdbms.repository.TeamRepository;
import org.veto.core.rdbms.repository.UserBuyOddsRepository;
import org.veto.core.rdbms.repository.UserRepository;
import org.veto.core.service.wallet.AlchemyService;
import org.veto.shared.*;
import org.veto.shared.wallet.LocalWallet;
import org.veto.shared.wallet.WalletUtil;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequestMapping(value = "/")
@Slf4j
public class IndexController {
    @Autowired
    private UserRepository userRepository;
    @Resource
    private ServiceConfig serviceConfig;

    private static final String TATUM_CALLBACK_URI = "wallet/tatum_callback";

    @Value("${service.team.avatar.path}")
    private String teamAvatarPath;

    @Resource
    private HttpSession session;

    @Resource
    private ContestRepository contestRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserBuyOddsRepository userBuyOddsRepository;

    @Resource
    private AlchemyService alchemyService;

    @RequestMapping(value = "/login")
    public String login(Model model){
        session.setAttribute("site_name", serviceConfig.getSYSTEM_NAME().getVal());
        return "login";
    }

    @ResponseBody
    @RequestMapping(value = "/captcha")
    public Response captcha(){
        try {
            var captcha = CaptchaGenerator.generateCaptchaImage();
            session.setAttribute(Constants.ADMIN_CAPTCHA_SESSION_KEY, captcha.getText());
            return Response.success(captcha.getBase64Image());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping(value = "/")
    public String index(Model model){
        model.addAttribute("userCount", userRepository.count());
        model.addAttribute("contestCount", contestRepository.count());
        model.addAttribute("teamCount", teamRepository.count());

        boolean showSystemCheckModal = !ApplicationInitial.WALLET_SUCCESS.get();
        model.addAttribute("showSystemCheckModal", showSystemCheckModal);
        JSONObject showData = new  JSONObject();
        if (showSystemCheckModal) {
            // 第一步
            if (serviceConfig.getSYSTEM_WALLET().getVal() == null) {
                showData.put(serviceConfig.getALCHEMY_API_KEY().getKey(), serviceConfig.getALCHEMY_API_KEY().getVal());
                showData.put(serviceConfig.getALCHEMY_IS_TEST_NET().getKey(), serviceConfig.getALCHEMY_IS_TEST_NET().getVal());
                showData.put(serviceConfig.getSYSTEM_COIN_TYPE().getKey(), serviceConfig.getSYSTEM_COIN_TYPE().getVal());
                showData.put(serviceConfig.getSYSTEM_DOMAIN().getKey(), serviceConfig.getSYSTEM_DOMAIN().getVal());
            }else { // 第二步
                LocalWallet localWallet = serviceConfig.getSYSTEM_WALLET().getVal();
                showData.put("address", localWallet.getAddress());
                try {
                    showData.put("webhook_url", UrlBuilder.formUrl(serviceConfig.getSYSTEM_DOMAIN().getVal()).addPath("api").addPath("wallet/alchemy_callback").toString());
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        model.addAttribute("missingConfigs", showData);
        Instant instant = LocalDate.now().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();

// 从 Instant 转换为 Date
        Date date = Date.from(instant);
        model.addAttribute("dailyBetCount", userBuyOddsRepository.countByCreatedAtAfter(date));
        return "index";
    }

    @GetMapping(value = "/team/avatar/{id}")
    public ResponseEntity<byte[]> teamImage(@PathVariable(value = "id") Integer id){
        try {
            String filename = id + ".png";
            // 构建文件路径
            Path imagePath = Paths.get(this.teamAvatarPath, filename);
            if (!Files.exists(imagePath)) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // 读取图片文件到字节数组
            byte[] imageBytes = Files.readAllBytes(imagePath);

            // 设置响应头，指定内容类型
            HttpHeaders headers = new HttpHeaders();
            // 根据文件名推断图片类型，例如 .jpg, .png 等
            String contentType = getContentType(filename);
            headers.setContentType(MediaType.parseMediaType(contentType));

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            log.error("get team avatar error", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    // 初始化solana的token
//    @ResponseBody
//    @GetMapping(value = "/init_solana_token_wallet")
//    public Response initSolanaTokenWallet() throws URISyntaxException {
//        if (serviceConfig.getSYSTEM_COIN_TYPE().getVal().isBlockchain()
//                && serviceConfig.getSYSTEM_COIN_TYPE().getVal().isToken()
//                && serviceConfig.getSYSTEM_COIN_TYPE().getVal().getBlockchainSymbolNetwork() == BLOCKCHAIN_SYMBOL_NETWORK.SOLANA
//                && Util.isBlank(serviceConfig.getTATUM_SOLANA_WALLET_TOKEN_ADDRESS().getVal())){
//            // 使用私钥开启token
//            TatumWalletService.TatumAddressResponse tokenAddress = tatumWalletService.initializeSolTokenAddress(serviceConfig.getSYSTEM_WALLET().getVal().getPrivateKey());
//            // 设置收取token的地址
//            serviceConfig.update(serviceConfig.getTATUM_SOLANA_WALLET_TOKEN_ADDRESS().getKey(), tokenAddress.getAddress());
//            // 构建监听
//            tatumWalletService.createAddressSubscription(tokenAddress.getAddress(), UrlBuilder.formUrl(serviceConfig.getSYSTEM_DOMAIN().getVal()).addPath(TATUM_CALLBACK_URI).toString());
//            return Response.success();
//        }else {
//            return Response.error("solana钱包无法更新");
//        }
//    }

    @GetMapping(value = "/system_properties")
    public String systemProperties(Model model) throws IllegalAccessException {
        model.addAttribute("configs", composeProperties());
        return "config";
    }

    @ResponseBody
    @PostMapping(value = "/init_system_one")
    public Response initSystemOne(@RequestBody Map<String, String> properties) throws URISyntaxException {
        String coin = properties.get("veto_config_system_coin_type");

        boolean alchemyTestNet = properties.get("veto_config_alchemy_test_net").equalsIgnoreCase("true");

        String alchemyApiKey = properties.get("veto_config_alchemy_api_key");

        String systemDomain = properties.get("veto_config_system_domain");

        if (Util.isAnyBlank(coin, alchemyApiKey)) {
            return Response.error("请填写所有配置");
        }
        // 先初始化币种
        COIN_TYPE coinType = COIN_TYPE.byName(coin);
        if (coinType == null){
            return Response.error("未支持的币种");
        }

        serviceConfig.update(serviceConfig.getALCHEMY_API_KEY().getKey(), alchemyApiKey);

        serviceConfig.update(serviceConfig.getSYSTEM_DOMAIN().getKey(), systemDomain);
        serviceConfig.update(serviceConfig.getALCHEMY_IS_TEST_NET().getKey(), alchemyTestNet);

        // 然后创建一个本地钱包
        WalletUtil walletUtil = new WalletUtil();
        LocalWallet localWallet = walletUtil.generate(coinType);

        // 要求使用本地钱包地址创建Notify
        serviceConfig.update(serviceConfig.getSYSTEM_WALLET().getKey(), localWallet);
        serviceConfig.update(serviceConfig.getSYSTEM_COIN_TYPE().getKey(), coinType);

        return Response.success();

    }

    @ResponseBody
    @PostMapping(value = "/init_system_two")
    public Response initSystemTwo(@RequestBody Map<String, String> properties) throws URISyntaxException {

        serviceConfig.update(serviceConfig.getALCHEMY_AUTH_TOKEN().getKey(), properties.get("alchemyAuthToken"));
        serviceConfig.update(serviceConfig.getALCHEMY_NOTIFY_ID().getKey(), properties.get("notifyId"));
//        serviceConfig.update(serviceConfig.getALCHEMY_SIGNING_KEY().getKey(), properties.get("signingKey"));
        // 开始构建监听
        alchemyService.initWallet(UrlBuilder.formUrl(serviceConfig.getSYSTEM_DOMAIN().getVal()).addPath("api").addPath("wallet/alchemy_callback").toString());
        ApplicationInitial.WALLET_SUCCESS.set(true);
        return Response.success();
    }

    @ResponseBody
    @PostMapping(value = "/update_settings")
    public Response updateSettings(@RequestParam("key") String key, @RequestParam(value = "val") String val) throws IllegalAccessException {
        Map<String, Class<?>> keyClass = keyClass();
        if (!keyClass.containsKey(key)){
            return Response.error("没有找到参数");
        }
        // 推荐奖励付出方
        switch (key) {
            case "veto_config_recommend_fee_player" -> {
                if (RecommendReward.FEE_PLAYER.fromString(val) == null) {
                    return Response.error("数据类型有误");
                }
                serviceConfig.update(serviceConfig.getRECOMMEND_FEE_PLAYER().getKey(), RecommendReward.FEE_PLAYER.fromString(val));
            }
            case "veto_config_system_coin_type" -> {
                if (COIN_TYPE.byName(val) == null) {
                    return Response.error("数据类型有误");
                } else if (serviceConfig.getSYSTEM_COIN_TYPE() != null) {
                    return Response.error("状态已不可修改");
                } else {
                    serviceConfig.update(RedisConfigKeyConstants.SYSTEM_COIN_TYPE, COIN_TYPE.byName(val));
                }
            }
            case "veto_config_transaction_records_dimensions" -> {
                if (val == null || Util.isBlank(val)) {
                    return Response.error("参数不符合规范");
                }
                Set<Constants.FLOW_DIMENSIONS_LIST> sets = new HashSet<>();
                for (String s :  val.split(",")) {
                    if (Util.isBlank(s)) {
                        continue;
                    }
                    if (Constants.FLOW_DIMENSIONS_LIST.fromString(s.trim()) == null) {
                        continue;
                    }
                    sets.add(Constants.FLOW_DIMENSIONS_LIST.fromString(s.trim()));
                }
                if (sets.isEmpty()) {
                    return Response.error("参数不符合规范");
                }
                serviceConfig.update(serviceConfig.getTRANSACTION_RECORDS_DIMENSIONS().getKey(), sets);
            }
            default -> {
                Class<?> type = keyClass().get(key);
                if (type.equals(String.class)) {
                    serviceConfig.update(key, val);
                }else if (type.equals(Integer.class)) {
                    serviceConfig.update(key, Integer.parseInt(val));
                }else if (type.equals(Boolean.class)) {
                    serviceConfig.update(key, Boolean.parseBoolean(val));
                }else if (type.equals(Double.class)) {
                    serviceConfig.update(key, Double.parseDouble(val));
                }else if (type.equals(Float.class)) {
                    serviceConfig.update(key, Float.parseFloat(val));
                }else if (type.equals(Long.class)) {
                    serviceConfig.update(key, Long.parseLong(val));
                }else if(type.equals(BigDecimal.class)){
                    serviceConfig.update(key, new BigDecimal(val));
                }else {
                    log.error("update {} error {}", key, val);
                    return Response.error("未识别的参数");
                }
            }
        }

       return Response.success();
    }

    public static String getContentType(String filename) {
        filename = filename.toLowerCase();
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (filename.endsWith(".png")) {
            return "image/png";
        } else if (filename.endsWith(".gif")) {
            return "image/gif";
        }
        return "application/octet-stream"; // 默认返回通用二进制流
    }

    protected Map<String, Class<?>> keyClass() throws IllegalAccessException {
        Map<String, Class<?>> map = new HashMap<>();

        for (Field field : serviceConfig.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if (KeyVal.class.isAssignableFrom(field.getType())) {
                KeyVal prop = (KeyVal) field.get(serviceConfig);
                if (prop.getVal() == null) {
                    log.warn("key prop is null {}", prop.getKey());
                    continue;
                }
                map.put(prop.getKey().toString(), prop.getVal().getClass());
            }
        }

        return map;
    }

    protected JSONObject composeProperties() throws IllegalAccessException {
        JSONObject result = new JSONObject();

        for (Field field : serviceConfig.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if (KeyVal.class.isAssignableFrom(field.getType())) {
                KeyVal prop = (KeyVal) field.get(serviceConfig);
                if (prop.getVal() == null){
                    log.error("composeProperties: key {} has null value", prop.getKey());
                    continue;
                }
                if (prop.getVal().getClass().isEnum()){
                    result.put(prop.getKey().toString(), ((Enum<?>) prop.getVal()).name());
                }else if (prop.getVal() instanceof Set<?> set){
                    Set<String> vales = new HashSet<>();
                    for (Object o : set) {
                        if (o.getClass().isEnum()){
                            vales.add(((Enum<?>) o).name());
                        }else {
                            vales.add(o.toString());
                        }
                    }
                    result.put(prop.getKey().toString(), vales);
                }else {
                    result.put(prop.getKey().toString(), prop.getVal());
                }
            }
        }

        return result;
    }
}

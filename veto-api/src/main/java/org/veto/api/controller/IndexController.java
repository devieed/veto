package org.veto.api.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.veto.api.mapper.ContestMapper;
import org.veto.core.authorize.UnAuthorize;
import org.veto.core.common.ServiceConfig;
import org.veto.core.rdbms.bean.*;
import org.veto.core.rdbms.repository.*;
import org.veto.core.redis.RedisUtilities;
import org.veto.core.service.AnnouncementService;
import org.veto.core.service.ScoreOddService;
import org.veto.core.service.TeamAndContestService;
import org.veto.core.service.process.TeamService;
import org.veto.shared.*;
import org.veto.shared.exception.VETO_EXCEPTION_CODE;
import org.veto.shared.spider.CapturedContest;
import org.veto.shared.spider.CapturedOddsScore;
import org.veto.shared.spider.HcwinSpider;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@Validated
@RequestMapping(value = "/")
@Slf4j
public class IndexController {

    @Resource
    private TeamService teamService;

    @Resource
    private RedisUtilities redisUtilities;

    @Resource
    private ServiceConfig serviceConfig;

    @Resource
    private AnnouncementService announcementService;

    @Resource
    private TeamAndContestService teamAndContestService;

    @Value("${service.team.avatar.path}")
    private String teamAvatarPath;

    @Resource
    private ContestMapper contestMapper;
    @Resource
    private ScoreOddsRepository scoreOddsRepository;

    @Resource
    private ContestRepository contestRepository;


    @Resource
    private TeamRepository teamRepository;

    @Resource
    private ScoreOddService scoreOddService;

    @Resource
    private RecommendRewardRepository recommendRewardRepository;

    // 最低买入金额
    @UnAuthorize
    @GetMapping(value = "/info")
    public Response minBetTotal() {
        Map<String, Object> info = new HashMap<>();
        info.put("min_bet_amount", serviceConfig.getMIN_BET_AMOUNT().getVal());

        COIN_TYPE coinType = serviceConfig.getSYSTEM_COIN_TYPE().getVal();

        info.put("coin_type", coinType.name().replace("_", "-"));

        info.put("blockchain_network", coinType.getBlockchainSymbolNetwork().getNetwork());

        info.put("is_blockchain", coinType.isBlockchain());

        info.put("min_bet", serviceConfig.getMIN_BET_AMOUNT().getVal());

        info.put("name", serviceConfig.getSYSTEM_NAME().getVal());

        info.put("login_enable", serviceConfig.getLOGIN_ENABLE().getVal());

        info.put("register_enable", serviceConfig.getREGISTER_ENABLE().getVal());

        info.put("login_captcha_enable", serviceConfig.getLOGIN_CAPTCHA_ENABLE().getVal());

        info.put("register_captcha_enable", serviceConfig.getREGISTER_CAPTCHA_ENABLE().getVal());

        info.put("need_recommend_code", serviceConfig.getREGISTER_NEED_RECOMMEND().getVal());

        info.put("description", serviceConfig.getSITE_DESCRIPTION().getVal());

        info.put("system_time", new Date().getTime());

        info.put("withdraw_fee", serviceConfig.getWITHDRAW_FEE().getVal());

        info.put("api_encrypt", serviceConfig.getSERVICE_API_ENCRYPT().getVal());

        info.put("withdraw_need_real_name_authentication", serviceConfig.getWITHDRAW_NEED_REAL_NAME_AUTHENTICATION().getVal());

        info.put("withdraw_flow_limit_multiple", serviceConfig.getWITHDRAW_MUST_FLOW_LIMIT_MULTIPLE().getVal());

        info.put("interest_rate", serviceConfig.getDAILY_INTEREST().getVal());

        info.put("contract_address", serviceConfig.getSYSTEM_COIN_TYPE().getVal().getContractAddress());

        info.put("register_need_recommend", serviceConfig.getREGISTER_NEED_RECOMMEND().getVal());

        info.put("register_need_captcha", serviceConfig.getREGISTER_CAPTCHA_ENABLE().getVal());

        RecommendReward recommendReward = recommendRewardRepository.findByLevel(1);

        info.put("recommend_reward_rate", recommendReward.getRate());

        return Response.success(info);
    }

    // 热门比赛
    @UnAuthorize
    @GetMapping(value = "/hot_contest")
    public Response hotContest() {
        // TODO: 只返回3个排序后的重要比分即可
        var resD = teamAndContestService.hotContest();
        var res = contestMapper.toContestVOPage(resD);
        return Response.success(res);
    }

    @UnAuthorize
    @GetMapping(value = "/get_announcements")
    public Response getAnnouncements(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page, @RequestParam(value = "type", required = false) String type) {
        if (page < 1) {
            page = 1;
        }

        return Response.success(announcementService.get(null, page, type, "createdAt"));
    }


    @UnAuthorize
    @GetMapping(value = "/captcha/{type}")
    public Response captcha(@PathVariable("type") String type) {
        if (Util.isBlank(type)) {
            type = "";
        }
        if (type.equalsIgnoreCase("login")) {
            // 未开启登录或者验证码，则不返回
            if (!serviceConfig.getLOGIN_ENABLE().getVal() || !serviceConfig.getLOGIN_CAPTCHA_ENABLE().getVal()) {
                return Response.error(VETO_EXCEPTION_CODE.CAPTCHA_INVALID);
            }
        } else if (type.equalsIgnoreCase("register")) {
            // 未开启登录或者验证码，则不返回
            if (!serviceConfig.getREGISTER_ENABLE().getVal() || !serviceConfig.getREGISTER_CAPTCHA_ENABLE().getVal()) {
                return Response.error(VETO_EXCEPTION_CODE.CAPTCHA_INVALID);
            }
        } else { // 否则不做任何动作
            return Response.error(VETO_EXCEPTION_CODE.CAPTCHA_INVALID);
        }
        CaptchaGenerator.CaptchaResult captchaResult = null;
        try {
            captchaResult = CaptchaGenerator.generateCaptchaImage();
        } catch (IOException e) {
            log.error("generate captcha error", e);
            return Response.error(VETO_EXCEPTION_CODE.CAPTCHA_INVALID);
        }

        String captchaId = UUID.randomUUID().toString().replace("-", "");
        String key = Constants.USER_CAPTCHA_TTL_NOTE_PREFIX + captchaId;
        redisUtilities.set(key, captchaResult.getText());
        redisUtilities.expireAt(key, new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(serviceConfig.getCAPTCHA_EXPIRE_TIME_SECONDS().getVal())));

        Captcha captcha = new Captcha();
        captcha.setId(captchaId);
        captcha.setImage(captchaResult.getBase64Image());

        return Response.success(captcha);
    }

    @Setter
    @Getter
    public static class Captcha {
        private String id;

        private String image;
    }
//
//    @UnAuthorize
//    @GetMapping(value = "/up")
//    public void up() throws IOException {
//        teamService.process();
//    }

    @UnAuthorize
    @GetMapping(value = "/up")
    public void up() {
        try {
            teamService.process();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @UnAuthorize
    @GetMapping(value = "/team/avatar/{id}")
    public ResponseEntity<byte[]> teamImage(@PathVariable(value = "id") Integer id) {
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

    private String getContentType(String filename) {
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (filename.endsWith(".png")) {
            return "image/png";
        } else if (filename.endsWith(".gif")) {
            return "image/gif";
        }
        return "application/octet-stream"; // 默认返回通用二进制流
    }
}

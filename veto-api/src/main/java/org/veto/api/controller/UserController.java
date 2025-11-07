package org.veto.api.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.hibernate.validator.constraints.Length;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.veto.api.dto.UserLoginDTO;
import org.veto.api.dto.UserRealNameAuthDTO;
import org.veto.api.dto.UserRegisterDTO;
import org.veto.api.dto.UserUpdateDTO;
import org.veto.api.mapper.UserMapper;
import org.veto.core.authorize.UnAuthorize;
import org.veto.core.common.JwtUtil;
import org.veto.core.common.ServiceConfig;
import org.veto.core.rdbms.bean.User;
import org.veto.core.rdbms.bean.UserDepositAddress;
import org.veto.core.rdbms.repository.UserDepositAddressRepository;
import org.veto.core.rdbms.repository.UserRepository;
import org.veto.core.redis.RedisUtilities;
import org.veto.core.service.UserService;
import org.veto.shared.Constants;
import org.veto.shared.Response;
import org.veto.shared.UserContextHolder;
import org.veto.shared.Util;
import org.veto.shared.exception.VETO_EXCEPTION_CODE;
import org.veto.shared.exception.VetoException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/user")
@Validated
public class UserController {

    @Resource
    private ServiceConfig serviceConfig;

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserService userService;

    @Resource
    private RedisUtilities redisUtilities;

    @Resource
    private JwtUtil jwtUtil;
    @Autowired
    private UserDepositAddressRepository userDepositAddressRepository;

    @GetMapping(value = "/logout")
    public void logout(HttpServletRequest request) {
        jwtUtil.blockedToken(JwtUtil.getTokenFormRequest(request));
    }

    @GetMapping(value = "/reflush_token")
    public Response reflushToken(){
        try {
            Map<String, Object> map = new HashMap<>();
            User user = userService.reflushToken();
            map.put("token", user.getToken());
            map.put("tokenExpireAt", user.getTokenExpireAt().getTime());
            map.put("user", userMapper.toUserSelfVO(user));
            return Response.success(map);
        } catch (VetoException e) {
            return Response.error(e.getCode());
        }
    }

    @PostMapping(value = "/real_name")
    public Response realName(@RequestBody @Valid UserRealNameAuthDTO userRealNameAuthDTO){
        userService.realName(userRealNameAuthDTO.getRealName(), userRealNameAuthDTO.getPhone(), userRealNameAuthDTO.getIdNumber());
        return Response.success();
    }

    @GetMapping(value = "/real_name_status")
    public Response realNameStatus(){
        return Response.success(userService.isRealName());
    }

    // 检查邀请码
    @UnAuthorize
    @GetMapping(value = "/check-invite/{code}")
    public Response checkInvite(@PathVariable(value = "code") @Length(min = 4, message = VETO_EXCEPTION_CODE.REGISTER_RECOMMEND_CODE_INVALID) String code) {
        if (!userService.checkRegisterRecommendCode(code)) {
            return Response.error(VETO_EXCEPTION_CODE.REGISTER_RECOMMEND_CODE_INVALID);
        }

        return Response.success();
    }

    @UnAuthorize
    @PostMapping(value = "/login")
    public Response login(@Valid @RequestBody UserLoginDTO userLoginDTO) throws VetoException {
        if (!serviceConfig.getLOGIN_ENABLE().getVal()) {
            return Response.error(VETO_EXCEPTION_CODE.LOGIN_SYSTEM_DISABLED);
        }
        if (serviceConfig.getLOGIN_CAPTCHA_ENABLE().getVal()){
            if (Util.isAnyBlank(userLoginDTO.getCaptcha(), userLoginDTO.getCaptchaId())) {
                return Response.error(VETO_EXCEPTION_CODE.CAPTCHA_INVALID);
            }
            // 检查验证码
            String key = Constants.USER_CAPTCHA_TTL_NOTE_PREFIX + userLoginDTO.getCaptchaId();
            String code = redisUtilities.get(key, String.class);
            // remove captcha
            redisUtilities.del(key);

            if (!userLoginDTO.getCaptcha().equalsIgnoreCase(code)) {
                return Response.error(VETO_EXCEPTION_CODE.CAPTCHA_INVALID);
            }
        }
        try {
            Map<String, Object> map = new HashMap<>();
            User user = userService.login(userMapper.toUserLoginCommand(userLoginDTO));
            map.put("token", user.getToken());
            map.put("tokenExpireAt", user.getTokenExpireAt().getTime());
            map.put("user", userMapper.toUserSelfVO(user));
            UserDepositAddress userDepositAddress = userDepositAddressRepository.findByUserIdAndCoinTypeAndStatusIsTrue(user.getId(), serviceConfig.getSYSTEM_COIN_TYPE().getVal());
            if (userDepositAddress != null) {
                map.put("address", userDepositAddress.getAddress());
            }
            return Response.success(map);
        } catch (VetoException e) {
            return Response.error(e.getCode());
        }
    }

    @UnAuthorize
    @PostMapping(value = "/register")
    public Response register(@Valid @RequestBody UserRegisterDTO userRegisterDTO) {
        if (!serviceConfig.getREGISTER_ENABLE().getVal()) {
            return Response.error(VETO_EXCEPTION_CODE.REGISTER_SYSTEM_DISABLED);
        }
        if (serviceConfig.getREGISTER_NEED_RECOMMEND().getVal() && Util.isBlank(userRegisterDTO.getRecommendCode())){
            return Response.error(VETO_EXCEPTION_CODE.REGISTER_RECOMMEND_CODE_INVALID);
        }
        // 如果开启的注册验证码
        if (serviceConfig.getREGISTER_CAPTCHA_ENABLE().getVal()) {
            if (Util.isAnyBlank(userRegisterDTO.getCaptcha(), userRegisterDTO.getCaptchaId())) {
                return Response.error(VETO_EXCEPTION_CODE.CAPTCHA_INVALID);
            }
            // 检查验证码
            String key = Constants.USER_CAPTCHA_TTL_NOTE_PREFIX + userRegisterDTO.getCaptchaId();
            String code = redisUtilities.get(key, String.class);
            // remove captcha
            redisUtilities.del(key);

            if (!userRegisterDTO.getCaptcha().equalsIgnoreCase(code)) {
                return Response.error(VETO_EXCEPTION_CODE.CAPTCHA_INVALID);
            }
        }
        try {
            userService.register(userMapper.toUserRegisterCommand(userRegisterDTO));
        }catch (VetoException e){
            return Response.error(e.getCode());
        }

        return Response.me();
    }

    @PostMapping(value = "/update")
    public Response update(@Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        try {
            userService.update(userMapper.toUserUpdateCommand(userUpdateDTO));
        } catch (VetoException e) {
            return Response.error(e.getCode());
        }

        return Response.success();
    }
}

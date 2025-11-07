package org.veto.core.service;

import jakarta.annotation.Resource;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.veto.core.command.UserLoginCommand;
import org.veto.core.command.UserRegisterCommand;
import org.veto.core.command.UserUpdateCommand;
import org.veto.core.common.JwtUtil;
import org.veto.core.common.ServiceConfig;
import org.veto.core.rdbms.bean.*;
import org.veto.core.rdbms.repository.*;
import org.veto.shared.*;
import org.veto.shared.exception.VETO_EXCEPTION_CODE;
import org.veto.shared.exception.VetoException;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    @Resource
    private UserRepository userRepository;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private ServiceConfig serviceConfig;

    @Resource
    private UserWalletRepository userWalletRepository;

    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Resource
    private RecommendRelationRepository recommendRelationRepository;

    @Resource
    private WalletService walletService;

    @Autowired
    private RealNameAuthenticationRepository realNameAuthenticationRepository;

    public String isRealName(){
        return realNameAuthenticationRepository.findByUserId(UserContextHolder.getUser().getId()).getRealNameAuthenticationStatus().name();
    }

    public void realName(String realName, String phone, String idNumber){
        RealNameAuthentication realNameAuthentication = realNameAuthenticationRepository.findByUserId(UserContextHolder.getUser().getId());

        if (realNameAuthentication == null){
            realNameAuthentication = new RealNameAuthentication();
            realNameAuthentication.setUserId(UserContextHolder.getUser().getId());
            realNameAuthentication.setRealName(realName);
            realNameAuthentication.setPhone(phone);
            realNameAuthentication.setIdNumber(idNumber);
        }else {
            realNameAuthentication.setRealName(realName);
            realNameAuthentication.setPhone(phone);
            realNameAuthentication.setIdNumber(idNumber);
        }
        // TODO 是否需要去管理后台审核，现在先默认直接通过
        realNameAuthentication.setRealNameAuthenticationStatus(RealNameAuthentication.REAL_NAME_AUTHENTICATION_STATUS.PASS);
        realNameAuthentication = realNameAuthenticationRepository.save(realNameAuthentication);
    }

    public boolean checkRegisterRecommendCode(String code){
        return userRepository.existsByRecommendCodeAndStatusIsTrue(code);
    }

    public User reflushToken(){
        Date date = jwtUtil.getExpireAtFromToken(UserContextHolder.getUser().getToken());

        if (TimeUnit.MILLISECONDS.toSeconds(date.getTime() - System.currentTimeMillis()) > Constants.TOKEN_REFRESH_TIME_WINDOW) {
            throw new VetoException(VETO_EXCEPTION_CODE.TOKEN_FAILED);
        }

        User user = userRepository.findById(UserContextHolder.getUser().getId()).orElse(null);
        String token = jwtUtil.generateToken("veto_token", user.getId(), new KeyVal<>("last_login", new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss").format(user.getLastLoginAt())));
        user.setTokenExpireAt(jwtUtil.getExpireAtFromToken(token));
        user.setToken(token);

        return user;
    }

    @Transactional
    public User login(UserLoginCommand userLoginCommand){
        if (!userRepository.existsByUsername(userLoginCommand.getUsername())){
            throw new VetoException(VETO_EXCEPTION_CODE.LOGIN_USERNAME_OR_PASSWORD_INVALID);
        }
        User user = userRepository.findByUsernameAndPassword(userLoginCommand.getUsername(), DigestUtils.md5Hex(userLoginCommand.getPassword()));
        if (user == null){
            throw new VetoException(VETO_EXCEPTION_CODE.LOGIN_USERNAME_OR_PASSWORD_INVALID);
        }
        if (!user.getStatus()){
            throw new VetoException(VETO_EXCEPTION_CODE.LOGIN_FORBIDDEN);
        }
        String lastLogin = user.getLastLoginAt() != null ? new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss").format(user.getLastLoginAt()) : "从未";

        String token = jwtUtil.generateToken("veto_token", user.getId(), new KeyVal<>("last_login", lastLogin));

        user.setTokenExpireAt(jwtUtil.getExpireAtFromToken(token));

        user.setLastLoginAt(new Date());
        userRepository.save(user);

        user.setToken(token);

        return user;
    }

    @Transactional
    public void register(UserRegisterCommand userRegisterCommand){
        if(userRepository.existsByUsername(userRegisterCommand.getUsername())){
            throw new VetoException(VETO_EXCEPTION_CODE.REGISTER_USERNAME_EXISTS);
        }
        if(Util.isNotBlank(userRegisterCommand.getRecommendCode()) && !userRepository.existsByRecommendCode(userRegisterCommand.getRecommendCode())){
            throw new VetoException(VETO_EXCEPTION_CODE.REGISTER_RECOMMEND_CODE_INVALID);
        }
        if(Util.isNotBlank(userRegisterCommand.getNickname()) && userRepository.existsByNickname(userRegisterCommand.getNickname())){
            throw new VetoException(VETO_EXCEPTION_CODE.REGISTER_NICKNAME_EXISTS);
        }

        User recommendUser = null;

        if (Util.isNotBlank(userRegisterCommand.getRecommendCode())){
            recommendUser = userRepository.findByRecommendCode(userRegisterCommand.getRecommendCode());
            if (recommendUser == null){
                throw new VetoException(VETO_EXCEPTION_CODE.REGISTER_RECOMMEND_CODE_INVALID);
            }
        }

        COIN_TYPE coinType = serviceConfig.getSYSTEM_COIN_TYPE().getVal();

        User user = new User();
        user.setId(snowflakeIdGenerator.nextId());
        user.setUsername(userRegisterCommand.getUsername());
        user.setPassword(DigestUtils.md5Hex(userRegisterCommand.getPassword()));
        user.setRecommendCode(ReferralCodeGenerator.generateFromId(user.getId()));
        user.setNickname(userRegisterCommand.getNickname());
        user.setStatus(true);
        user.setRecommendUserCount(0);
        user.setIsVip(false);
        user = userRepository.save(user);

        RealNameAuthentication realNameAuthentication = new RealNameAuthentication();
        realNameAuthentication.setUserId(user.getId());
        realNameAuthentication.setRealName(null);
        realNameAuthentication.setPhone(null);
        realNameAuthentication.setIdNumber(null);
        realNameAuthentication.setRealNameAuthenticationStatus(RealNameAuthentication.REAL_NAME_AUTHENTICATION_STATUS.NOT_YET);
        realNameAuthentication = realNameAuthenticationRepository.save(realNameAuthentication);

        UserWallet userWallet = new UserWallet();
        userWallet.setBalance(BigDecimal.ZERO);
        userWallet.setUserId(user.getId());
        userWallet.setUpdatedAt(new Date());
        userWallet.setStatus(true);
        userWallet.setFlowLimit(BigDecimal.ZERO);
        userWallet.setTotalBets(BigDecimal.ZERO);

        userWallet.setCoinType(coinType);
        userWallet = userWalletRepository.save(userWallet);
        // 区块链钱包，需要创建收款地址
        if (coinType.isBlockchain()){
            var address = walletService.createUserDepositAddress(user.getId());
        }

        if (recommendUser != null){
            RecommendRelation recommendRelation = new RecommendRelation();
            recommendRelation.setUserId(user.getId());
            recommendRelation.setTargetUserId(recommendUser.getId());
            recommendRelation.setStatus(true);
            recommendRelation = recommendRelationRepository.save(recommendRelation);
        }
    }

    @Transactional
    public void update(UserUpdateCommand userUpdateCommand){
        User user = userRepository.findById(UserContextHolder.getUser().getId()).orElse(null);

        if (Util.isNotBlank(userUpdateCommand.getNickname())){
            if (userRepository.existsByNicknameAndIdNot(userUpdateCommand.getNickname(), user.getId())){
                throw  new VetoException(VETO_EXCEPTION_CODE.REGISTER_NICKNAME_EXISTS);
            }
        }
        if (Util.isNotBlank(userUpdateCommand.getEmail())){
            if (userRepository.existsByEmailAndIdNot(userUpdateCommand.getEmail(), user.getId())){
                throw new VetoException(VETO_EXCEPTION_CODE.EMAIL_EXISTS);
            }
        }
        if (Util.isNotBlank(userUpdateCommand.getPhone())){
            if (userRepository.existsByPhoneAndIdNot(userUpdateCommand.getPhone(), user.getId())){
                throw new VetoException(VETO_EXCEPTION_CODE.PHONE_EXISTS);
            }
        }

        if (Util.isAnyNotBlank(userUpdateCommand.getPassword(), userUpdateCommand.getCurrentPassword()) && Util.isAnyBlank(userUpdateCommand.getPassword(), userUpdateCommand.getCurrentPassword())){
            throw new VetoException(VETO_EXCEPTION_CODE.PASSWORD_INVALID);
        }

        if (Util.isNotBlank(userUpdateCommand.getCurrentPassword())){
            if (!user.getPassword().equals(DigestUtils.md5Hex(userUpdateCommand.getCurrentPassword()))) {
                throw  new VetoException(VETO_EXCEPTION_CODE.OLD_PASSWORD_INVALID);
            }
            user.setPassword(DigestUtils.md5Hex(userUpdateCommand.getPassword()));
        }

        if (userUpdateCommand.getStatus() != null){
            user.setStatus(userUpdateCommand.getStatus());
        }

        if (userUpdateCommand.getPassword() != null){
            user.setPassword(DigestUtils.md5Hex(userUpdateCommand.getPassword()));
        }
    }
}

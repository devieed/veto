package org.veto.admin.conf;

import jakarta.annotation.Resource;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.veto.core.common.ServiceConfig;
import org.veto.core.rdbms.bean.Admin;
import org.veto.core.rdbms.bean.RecommendReward;
import org.veto.core.rdbms.repository.AdminRepository;
import org.veto.core.rdbms.repository.RecommendRewardRepository;
import org.veto.shared.Util;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 系统初始化
 */
@Component
public class ApplicationInitial implements ApplicationRunner, DisposableBean {
    // 钱包是否已经可用
    public static AtomicBoolean WALLET_SUCCESS = new AtomicBoolean(false);

    @Resource
    private AdminRepository adminRepository;

    @Resource
    private ServiceConfig serviceConfig;

    public static final Integer[][] VIP_LEVELS = {
            {0, 0, 0, 70, 90, 25, 50},
            {1, 30000, 10000, 50, 80, 20, 10},
            {2, 180000, 60000, 30, 70, 20, 9},
            {3, 800000, 300000, 20, 70, 20, 9},
            {4, 1500000, 600000, 20, 70, 20, 9},
            {5, 3600000, 1300000, 20, 70, 20, 9},
            {6, 7200000, 2600000, 10, 70, 20, 8},
            {7, 9000000, 3200000, 10, 60, 20, 8},
            {8, 12000000, 4500000, 10, 60, 20, 8},
            {9, 15000000, 6000000, 10, 60, 20, 8},
            {10, 18000000, 7200000, 10, 60, 20, 7},
            {11, 24000000, 9600000, 10, 50, 20, 6},
            {12, 32000000, 12800000, 10, 50, 20, 5},
            {13, 40000000, 16000000, 10, 40, 20, 4},
            {14, 50000000, 20000000, 10, 30, 20, 3},
            {15, 80000000, 50000000, 10, 10, 20, 1}
    };
    @Autowired
    private RecommendRewardRepository recommendRewardRepository;


    @Override
    public void destroy() throws Exception {

    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initAdmin();
        initWallet();
        initRechargeLevelReward();
    }

    protected void initRechargeLevelReward(){
        if (recommendRewardRepository.count() == 0){
            RecommendReward recommendReward = new RecommendReward();
            recommendReward.setRate(new BigDecimal("0.2"));
            recommendReward.setLevel(1);
            recommendReward.setStatus(true);
            recommendRewardRepository.save(recommendReward);
        }
    }

    protected void initAdmin(){
        if (adminRepository.count() == 0){
            Admin admin = new Admin();
            admin.setStatus(true);
            admin.setUsername("admin");
            admin.setPassword(DigestUtils.md5Hex("admin"));
            admin.setLastLogin(new Date());

            adminRepository.save(admin);
        }
    }

    /**
     * 初始化钱包
     */
    private void initWallet(){
        if (Util.isAnyBlank(serviceConfig.getALCHEMY_API_KEY().getVal(),  serviceConfig.getALCHEMY_AUTH_TOKEN().getVal(), serviceConfig.getSYSTEM_DOMAIN().getVal()) || serviceConfig.getALCHEMY_WEBHOOK_AND_SIGNING_KEY().getVal() == null) {
            WALLET_SUCCESS.set(false);
        }else {
            WALLET_SUCCESS.set(true);
        }
    }
}

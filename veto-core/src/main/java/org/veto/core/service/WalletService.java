package org.veto.core.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.veto.core.rdbms.bean.WALLET_BEHAVIOR_TYPE;

import java.math.RoundingMode;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.veto.core.command.WithdrawAddCommand;
import org.veto.core.common.ServiceConfig;
import org.veto.core.rdbms.bean.*;
import org.veto.core.rdbms.bean.WalletRecord.TYPE;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.veto.core.rdbms.repository.*;
import org.veto.core.service.wallet.AlchemyService;
import org.veto.shared.COIN_TYPE;
import org.veto.shared.Constants;
import org.veto.shared.UserContextHolder;
import org.veto.shared.exception.VETO_EXCEPTION_CODE;
import org.veto.shared.exception.VetoException;
import org.veto.shared.wallet.LocalWallet;
import org.veto.shared.wallet.WalletUtil;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
public class WalletService {

    @Resource
    private UserWalletRepository userWalletRepository;

    @Resource
    private WalletRecordRepository walletRecordRepository;

    @Autowired
    private ServiceConfig serviceConfig;
    @Autowired
    private UserDepositAddressRepository userDepositAddressRepository;

    @Resource
    private WithdrawRepository withdrawRepository;

    @Autowired
    private UserConsumptionRepository userConsumptionRepository;
    @Autowired
    private VipService vipService;
    @Autowired
    private RecommendService recommendService;
    @Autowired
    private RecommendRewardRepository recommendRewardRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserRevenueRepository userRevenueRepository;

    @Resource
    private AlchemyService alchemyService;
    @Autowired
    private UserFirstRechargeRepository userFirstRechargeRepository;

    public Page<WalletRecord> getWalletRecords(Integer page, TYPE type) {
        Pageable pageable = PageRequest.of(page - 1, serviceConfig.getDEFAULT_DATA_PAGE_SIZE().getVal()).withSort(Sort.by(Sort.Direction.DESC, "createdAt"));

        if (type == null) {
            return walletRecordRepository.findAllByUserId(pageable, UserContextHolder.getUser().getId());
        } else {
            return walletRecordRepository.findAllByUserIdAndType(pageable, UserContextHolder.getUser().getId(), type);
        }
    }

    public Page<Withdraw> getWithdrawRecords(Integer page, Withdraw.STATUS status) {
        Pageable pageable = PageRequest.of(page - 1, serviceConfig.getDEFAULT_DATA_PAGE_SIZE().getVal()).withSort(Sort.by(Sort.Direction.DESC, "createdAt"));
        if (status != null) {
            return withdrawRepository.findAllByUserIdAndStatus(UserContextHolder.getUser().getId(), status, pageable);
        } else {
            return withdrawRepository.findAllByUserId(UserContextHolder.getUser().getId(), pageable);
        }
    }

    public UserWallet info() {

        UserWallet userWallet = getUserWallet(UserContextHolder.getUser().getId());

        UserRevenue userRevenue = userRevenueRepository.findByUserIdAndTypeAndCoinType(userWallet.getUserId(), UserRevenue.TYPE.BET, userWallet.getCoinType());

        if (userRevenue == null) {
            userRevenue = new UserRevenue();
            userRevenue.setUserId(userWallet.getUserId());
            userRevenue.setType(UserRevenue.TYPE.BET);
            userRevenue.setCoinType(userWallet.getCoinType());
            userRevenue.setTotal(BigDecimal.ZERO);
            userRevenue = userRevenueRepository.save(userRevenue);
        }

        userWallet.setReward(userRevenue.getTotal());

        return userWallet;
    }

    protected UserWallet getUserWallet(Long userId) {
        return getUserWallet(userId, serviceConfig.getSYSTEM_COIN_TYPE().getVal());
    }

    protected UserWallet getUserWallet(Long userId, COIN_TYPE coinType) {
        UserWallet userWallet = userWalletRepository.findByUserIdAndCoinType(userId, coinType);
        if (userWallet == null) {
            userWallet = new UserWallet();
            userWallet.setBalance(BigDecimal.ZERO);
            userWallet.setUserId(UserContextHolder.getUser().getId());
            userWallet.setCoinType(serviceConfig.getSYSTEM_COIN_TYPE().getVal());
            userWallet.setStatus(true);
            userWallet.setFlowLimit(BigDecimal.ZERO);
            userWallet.setTotalBets(BigDecimal.ZERO);
            userWallet = userWalletRepository.save(userWallet);
        } else if (!userWallet.getStatus()) {
            throw new VetoException(VETO_EXCEPTION_CODE.WALLET_BLOCK);
        }

        return userWallet;
    }

    // 发放奖励
    @Transactional
    public void rewardPlay(UserRevenue.TYPE rewardType, WalletRecord.TYPE recordType, BigDecimal amount, Long userId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new VetoException(VETO_EXCEPTION_CODE.WALLET_AMOUNT_INVALID);
        }

        COIN_TYPE coinType = serviceConfig.getSYSTEM_COIN_TYPE().getVal();

        UserWallet userWallet = getUserWallet(userId);


        WalletRecord walletRecord = new WalletRecord();
        walletRecord.setUserId(userId);
        walletRecord.setBefore(userWallet.getBalance());
        walletRecord.setAfter(userWallet.getBalance().add(amount));
        walletRecord.setAmount(amount);
        walletRecord.setIsBlockchain(coinType.isBlockchain());
        walletRecord.setType(recordType);
        walletRecord.setStatus(true);
        walletRecord.setWalletId(userWallet.getId());
        walletRecord.setFee(BigDecimal.ZERO);

        // 记录奖励数量
        UserRevenue userRevenue = userRevenueRepository.findByUserIdAndTypeAndCoinType(userId, rewardType, coinType);
        if (userRevenue == null) {
            userRevenue = new UserRevenue();
            userRevenue.setUserId(userId);
            userRevenue.setType(rewardType);
            userRevenue.setCoinType(coinType);
            userRevenue.setTotal(BigDecimal.ZERO);
        }
        userRevenue.setTotal(userRevenue.getTotal().add(amount));

        userWallet.setBalance(userWallet.getBalance().add(amount));
        userWalletRepository.save(userWallet);

        userRevenue = userRevenueRepository.save(userRevenue);

        walletRecordRepository.save(walletRecord);
        // 增加推荐流水
        addTransactionRecord(userId, coinType, recordType == TYPE.RECOMMEND_REWARD ? Constants.FLOW_DIMENSIONS_LIST.RECOMMENDED : Constants.FLOW_DIMENSIONS_LIST.RECHARGE, userWallet.getBalance());
    }
    // 增加流水
    protected void addTransactionRecord(Long userId, COIN_TYPE coinType, Constants.FLOW_DIMENSIONS_LIST flowDimensionsList, BigDecimal amount) {
        if (!serviceConfig.getTRANSACTION_RECORDS_DIMENSIONS().getVal().contains(flowDimensionsList)) {
            return;
        }

        UserWallet userWallet = getUserWallet(userId, coinType);
        userWallet.setFlowLimit(userWallet.getFlowLimit() == null ? BigDecimal.ZERO.add(amount) : userWallet.getFlowLimit().add(amount));
        userWalletRepository.save(userWallet);
        vipService.updateUserLevel(userWallet.getUserId(), userWallet.getFlowLimit());
    }


    @Transactional
    public void bet(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new VetoException(VETO_EXCEPTION_CODE.WALLET_AMOUNT_INVALID);
        }

        Long userId = UserContextHolder.getUser().getId();

        COIN_TYPE coinType = serviceConfig.getSYSTEM_COIN_TYPE().getVal();

        UserWallet userWallet = getUserWallet(userId);

        if (userWallet.getBalance().compareTo(amount) < 0) {
            throw new VetoException(VETO_EXCEPTION_CODE.WALLET_INSUFFICIENT_BALANCE);
        }

        WalletRecord walletRecord = new WalletRecord();
        walletRecord.setUserId(UserContextHolder.getUser().getId());
        walletRecord.setBefore(userWallet.getBalance());
        walletRecord.setAfter(userWallet.getBalance().subtract(amount));
        walletRecord.setAmount(amount.negate());
        walletRecord.setIsBlockchain(coinType.isBlockchain());
        walletRecord.setType(TYPE.BET);
        walletRecord.setStatus(true);
        walletRecord.setWalletId(userWallet.getId());
        walletRecord.setFee(BigDecimal.ZERO);

        userWallet.setBalance(userWallet.getBalance().subtract(amount));
        //增加累计下注金额
        userWallet.setTotalBets(userWallet.getTotalBets().add(amount));
        userWalletRepository.save(userWallet);
        walletRecordRepository.save(walletRecord);

        UserConsumption userConsumption = userConsumptionRepository.findByUserIdAndCoinTypeAndWalletBehaviorType(userWallet.getUserId(), userWallet.getCoinType(), WALLET_BEHAVIOR_TYPE.PLACE_BET);
        if (userConsumption == null) {
            userConsumption = new UserConsumption();
            userConsumption.setUserId(userWallet.getUserId());
            userConsumption.setCoinType(userWallet.getCoinType());
            userConsumption.setWalletBehaviorType(WALLET_BEHAVIOR_TYPE.PLACE_BET);
            userConsumption.setTotal(BigDecimal.ZERO);
            userConsumption.setBehavior48h(BigDecimal.ZERO);
            userConsumption.setBehavior24h(BigDecimal.ZERO);
        }

        userConsumption.setTotal(userConsumption.getTotal().add(amount));
        userConsumptionRepository.save(userConsumption);
        // 手续费已结合在一起作为流水，如果想减去手续费，使用total
        addTransactionRecord(userId, coinType, Constants.FLOW_DIMENSIONS_LIST.BET_PLAY, amount);
    }

    public void recharge(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new VetoException(VETO_EXCEPTION_CODE.WALLET_AMOUNT_INVALID);
        }

        var rechargeRate = serviceConfig.getRECHARGE_FEE().getVal();

        COIN_TYPE coinType = serviceConfig.getSYSTEM_COIN_TYPE().getVal();

        UserWallet userWallet = userWalletRepository.findByUserIdAndCoinType(userId, coinType);

        if (userWallet == null) {
            userWallet = new UserWallet();
            userWallet.setBalance(BigDecimal.ZERO);
            userWallet.setUserId(userId);
            userWallet.setCoinType(coinType);
            userWallet.setStatus(true);
            userWallet.setFlowLimit(BigDecimal.ZERO);
            userWallet.setTotalBets(BigDecimal.ZERO);
        } else if (!userWallet.getStatus()) {
            throw new VetoException(VETO_EXCEPTION_CODE.WALLET_BLOCK);
        }

        BigDecimal total = amount.subtract(amount.multiply(rechargeRate));

        // 记录消费能力
        UserConsumption userConsumption = userConsumptionRepository.findByUserIdAndCoinTypeAndWalletBehaviorType(userId, serviceConfig.getSYSTEM_COIN_TYPE().getVal(), WALLET_BEHAVIOR_TYPE.RECHARGE);
        if (userConsumption == null) {
            userConsumption = new UserConsumption();
            userConsumption.setUserId(userId);
            userConsumption.setCoinType(coinType);
            userConsumption.setTotal(BigDecimal.ZERO);
            userConsumption.setWalletBehaviorType(WALLET_BEHAVIOR_TYPE.RECHARGE);
            userConsumption.setBehavior24h(BigDecimal.ZERO);
            userConsumption.setBehavior48h(BigDecimal.ZERO);
        }

        // 查询自己的父级
        RecommendRelation recommendTree = recommendService.getParentTree(userId);
        BigDecimal fee = rewardToParent(recommendTree, amount, amount);
        if (fee != null && fee.compareTo(BigDecimal.ZERO) > 0) {
            switch (serviceConfig.getRECOMMEND_FEE_PLAYER().getVal()) {
                case BELOW: // 充值者担负
                    total = total.subtract(fee);
                    break;
                case SYSTEM:
                    fee = BigDecimal.ZERO;
                    break;
            }
        } else {
            fee = BigDecimal.ZERO;
        }

        WalletRecord walletRecord = new WalletRecord();
        walletRecord.setUserId(userId);
        walletRecord.setBefore(userWallet.getBalance());
        walletRecord.setAfter(userWallet.getBalance().add(total));
        walletRecord.setFee(fee);
        walletRecord.setAmount(total);
        walletRecord.setStatus(true);
        walletRecord.setWalletId(userWallet.getId());
        walletRecord.setIsBlockchain(coinType.isBlockchain());
        walletRecord.setType(TYPE.RECHARGE);
        walletRecordRepository.save(walletRecord);

        userWallet.setBalance(userWallet.getBalance().add(total));
        userWalletRepository.save(userWallet);

        userConsumption.setTotal(userConsumption.getTotal().add(amount));
        userConsumptionRepository.save(userConsumption);
        addTransactionRecord(userId, coinType, Constants.FLOW_DIMENSIONS_LIST.RECHARGE, amount);
        // 查看用户是否是首充
        if (!userFirstRechargeRepository.existsByUserId(userId)) {
            // 发放首充奖励
            var firstRechargeReward = serviceConfig.getFIRST_RECHARGE_REWARD().getVal();
            BigDecimal reward = amount.multiply(firstRechargeReward).setScale(2, RoundingMode.HALF_DOWN);

            if (reward.compareTo(BigDecimal.ZERO) > 0) {
                rewardPlay(UserRevenue.TYPE.FIRST_RECHARGE_BONUS, TYPE.FIRST_RECHARGE_BONUS, reward, userId);
            }
            UserFirstRecharge userFirstRecharge = new UserFirstRecharge();
            userFirstRecharge.setUserId(userId);
            userFirstRecharge.setRechargeTotal(amount);

            userFirstRechargeRepository.save(userFirstRecharge);
            // 设置为VIP
            User user = userRepository.findById(userId).orElse(null);
            user.setIsVip(true);

            userRepository.save(user);
        }
    }

    /**
     * 递归地向父级发放奖励，并返回总奖励金额
     *
     * @param recommendRelation 当前的推荐关系节点
     * @param amount            奖励计算基数
     * @param totalFee          累计的总奖励金额（用于递归）
     * @return 整个父级链上所有发放的奖励总和
     */
    protected BigDecimal rewardToParent(RecommendRelation recommendRelation, BigDecimal amount, BigDecimal totalFee) {
        // 递归终止条件
        if (recommendRelation == null) {
            return null;
        }

        // 发放当前层级的奖励
        // 查找当前层级的奖励配置
        RecommendReward recommendReward = recommendRewardRepository.findByLevel(recommendRelation.getDeep());

        // 如果该层级没有奖励配置，直接返回
        if (recommendReward == null) {
            return BigDecimal.ZERO;
        }

        // 计算当前层级的奖励金额
        BigDecimal fee = amount.multiply(recommendReward.getRate());

        // 给推荐者发放奖励
        rewardPlay(UserRevenue.TYPE.RECOMMEND, TYPE.RECOMMEND_REWARD, fee, recommendRelation.getTargetUserId());

        // 累计总奖励
        BigDecimal updatedTotalFee = totalFee.add(fee);

        // 递归调用，向更上一级发放奖励
        if (recommendRelation.getParent() != null) {
            return rewardToParent(recommendRelation.getParent(), amount, updatedTotalFee);
        }

        return updatedTotalFee;
    }

    //    /**
//     * 创建系统级别收款账户
//     * @param enableWebHook
//     */
//    public void createTatumSystemAccount(boolean enableWebHook){
//        String customId = UUID.randomUUID().toString().replace("-", "");
//        customId= tatumWallet.createLedgerAccount(serviceConfig.getSYSTEM_COIN_TYPE().getVal(), customId);
//        serviceConfig.update(serviceConfig.getTATUM_WALLET_LEDGER_ACCOUNT().getKey(), customId);
//        // 开启监听
//        if (enableWebHook){
//            tatumWallet.registerWebhook(customId, serviceConfig.getSYSTEM_COIN_TYPE().getVal());
//        }
//    }
    // 给用户创建收款地址
    public UserDepositAddress createUserDepositAddress(Long userId) {
        COIN_TYPE systemCoinType = serviceConfig.getSYSTEM_COIN_TYPE().getVal();
        if (!systemCoinType.isBlockchain()) {
            throw new VetoException(VETO_EXCEPTION_CODE.WALLET_TYPE_ERROR);
        }
//
//        WalletUtil walletUtil = new WalletUtil();
//
//        LocalWallet localWallet = serviceConfig.getSYSTEM_WALLET().getVal();

//        if (serviceConfig.getTATUM_WALLET_TYPE() == null || serviceConfig.getTATUM_WALLET_LEDGER_ACCOUNT() == null){
//            // 钱包还未配置
//            throw new VetoException(VETO_EXCEPTION_CODE.WALLET_NOT_PARAMS);
//        }
        // 为用户创建收款地址
//        String address = walletUtil.generateDerivedAddress(localWallet.getChain(), localWallet.getMnemonic(), userId);
//        tatumWallet.bindDepositAddress(systemCoinType, serviceConfig.getTATUM_WALLET_LEDGER_ACCOUNT().getVal(), address);
        LocalWallet localWallet = serviceConfig.getSYSTEM_WALLET().getVal();

        String address = alchemyService.createDerivedAddressForUser(localWallet.getChain(), localWallet.getMnemonic(), userId);

        UserDepositAddress userDepositAddress = new UserDepositAddress();
        userDepositAddress.setAddress(address);
        userDepositAddress.setUserId(userId);
        userDepositAddress.setCoinType(systemCoinType);
        userDepositAddress.setCreatedAt(new Date());
        userDepositAddress.setRechargeTotal(new BigDecimal("0"));
        userDepositAddress.setStatus(true);

        return userDepositAddressRepository.save(userDepositAddress);
    }

    @Transactional
    public Withdraw createWithdraw(WithdrawAddCommand withdrawAddCommand) {

        if (withdrawAddCommand.getAmount().compareTo(new BigDecimal(10)) < 0) {
            throw new VetoException(VETO_EXCEPTION_CODE.WALLET_AMOUNT_INVALID);
        }

        Long userId = UserContextHolder.getUser().getId();
        COIN_TYPE coinType = serviceConfig.getSYSTEM_COIN_TYPE().getVal();

        UserWallet userWallet = userWalletRepository.findByUserIdAndCoinType(userId, coinType);
        if (userWallet == null || !userWallet.getStatus()) {
            throw new VetoException(VETO_EXCEPTION_CODE.WALLET_BLOCK);
        }
        if (userWallet.getBalance().compareTo(withdrawAddCommand.getAmount()) < 0) {
            throw new VetoException(VETO_EXCEPTION_CODE.WALLET_INSUFFICIENT_BALANCE);
        }
        // 检查是否达到提现标准, 以流水额为评判标准
        BigDecimal userFlowLimit = userWallet.getFlowLimit();
        Integer withdrawalRechargeRate = serviceConfig.getWITHDRAW_MUST_FLOW_LIMIT_MULTIPLE().getVal();
        // 未达标定流水
        if (withdrawAddCommand.getAmount().multiply(new BigDecimal(withdrawalRechargeRate)).compareTo(userFlowLimit) > 0) {
            throw new VetoException(VETO_EXCEPTION_CODE.WALLET_FLOW_LIMIT_BLOCKED);
        }
        BigDecimal withdrawalFee = serviceConfig.getWITHDRAW_FEE().getVal();
        BigDecimal fee = withdrawAddCommand.getAmount().multiply(withdrawalFee);
        // 余额不足
        if (userWallet.getBalance().compareTo(withdrawAddCommand.getAmount().add(fee)) < 0) {
            throw new VetoException(VETO_EXCEPTION_CODE.WALLET_INSUFFICIENT_BALANCE);
        }

        BigDecimal total = withdrawAddCommand.getAmount().add(fee);

        Withdraw withdraw = new Withdraw();
        withdraw.setUserId(UserContextHolder.getUser().getId());
        withdraw.setAmount(withdrawAddCommand.getAmount());
        withdraw.setAddress(withdrawAddCommand.getAddress());
        withdraw.setIsBlockchain(coinType.isBlockchain());
        withdraw.setDescription(withdrawAddCommand.getDescription());
        withdraw.setFee(fee);
        withdraw.setStatus(Withdraw.STATUS.PENDING);

        WalletRecord walletRecord = new WalletRecord();
        walletRecord.setUserId(UserContextHolder.getUser().getId());
        walletRecord.setIsBlockchain(coinType.isBlockchain());
        walletRecord.setBefore(userWallet.getBalance());
        walletRecord.setAfter(userWallet.getBalance().subtract(total));
        walletRecord.setAmount(total.negate());
        walletRecord.setType(TYPE.WITHDRAW);
        walletRecord.setStatus(true);
        walletRecord.setWalletId(userWallet.getId());
        walletRecord.setFee(fee);

        userWallet.setBalance(userWallet.getBalance().subtract(total));

        userWalletRepository.save(userWallet);

        walletRecordRepository.save(walletRecord);

//        UserConsumption userConsumption = userConsumptionRepository.findByUserIdAndCoinTypeAndWalletBehaviorType(userWallet.getUserId(), serviceConfig.getSYSTEM_COIN_TYPE().getVal(), WALLET_BEHAVIOR_TYPE.WITHDRAW);
//         TODO 应该在管理后台做
//        if (userConsumption == null){
//            userConsumption = new UserConsumption();
//            userConsumption.setUserId(userWallet.getUserId());
//            userConsumption.setCoinType(userWallet.getCoinType());
//            userConsumption.setWalletBehaviorType(WALLET_BEHAVIOR_TYPE.WITHDRAW);
//            userConsumption.setTotal(BigDecimal.ZERO);
//            userConsumption.setBehavior24h(BigDecimal.ZERO);
//            userConsumption.setBehavior48h(BigDecimal.ZERO);
//        }
//
//
//        userConsumption.setTotal(userConsumption.getTotal().add(withdrawAddCommand.getAmount()));
//
//        userConsumptionRepository.save(userConsumption);

        withdraw = withdrawRepository.save(withdraw);

        return withdraw;
    }

//    // 提币, 管理后台使用
//    public Map withdraw(String address, String amount, String operationId, WithdrawRecord.OPERATION operation){
//
//        COIN_TYPE coinType = serviceConfig.getSYSTEM_COIN_TYPE().getVal();
//        if (!coinType.isBlockchain()){
//            throw  new VetoException(VETO_EXCEPTION_CODE.WALLET_TYPE_ERROR);
//        }
//
//        var res = tatumWallet.withdrawFromAccount(serviceConfig.getTATUM_WALLET_LEDGER_ACCOUNT().getVal(), amount, address, coinType, null);
//
//        WithdrawRecord withdrawRecord = new WithdrawRecord();
//        withdrawRecord.setOperationId(operationId);
//        withdrawRecord.setOperation(operation);
//        withdrawRecord.setStatus(true);
//        withdrawRecord.setAddress(address);
//        withdrawRecord.setAmount(new BigDecimal(amount));
//        withdrawRecord.setIsBlockchain(coinType.isBlockchain());
//        withdrawRecord.setFee(null);
//
//        withdrawRecord = withdrawRecordRepository.save(withdrawRecord);
//
//                UserConsumption userConsumption = userConsumptionRepository.findByUserIdAndCoinTypeAndWalletBehaviorType(withdrawRecord.getUserId(), serviceConfig.getSYSTEM_COIN_TYPE().getVal(), WALLET_BEHAVIOR_TYPE.WITHDRAW);
//        if (userConsumption == null){
//            userConsumption = new UserConsumption();
//            userConsumption.setUserId(userWallet.getUserId());
//            userConsumption.setCoinType(userWallet.getCoinType());
//            userConsumption.setWalletBehaviorType(WALLET_BEHAVIOR_TYPE.WITHDRAW);
//            userConsumption.setTotal(BigDecimal.ZERO);
//            userConsumption.setBehavior24h(BigDecimal.ZERO);
//            userConsumption.setBehavior48h(BigDecimal.ZERO);
//        }
//
//        return res;
//    }

    public void refund(Long userId, BigDecimal amount, TYPE type) {
        UserWallet userWallet = getUserWallet(userId);
        userWallet.setBalance(userWallet.getBalance().add(amount));
        userWalletRepository.save(userWallet);

        WalletRecord walletRecord = new WalletRecord();
        walletRecord.setUserId(userId);
        walletRecord.setIsBlockchain(serviceConfig.getSYSTEM_COIN_TYPE().getVal().isBlockchain());
        walletRecord.setBefore(userWallet.getBalance().subtract(amount));
        walletRecord.setAfter(userWallet.getBalance());
        walletRecord.setAmount(amount);
        walletRecord.setWalletId(userWallet.getId());
        walletRecord.setStatus(true);
        walletRecord.setType(type);
        walletRecord.setFee(BigDecimal.ZERO);

        walletRecord = walletRecordRepository.save(walletRecord);
    }
}

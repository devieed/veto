package org.veto.core.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.veto.core.common.ServiceConfig;
import org.veto.core.rdbms.bean.*;
import org.veto.core.rdbms.repository.ContestRepository;
import org.veto.core.rdbms.repository.OddsTurnoverRepository;
import org.veto.core.rdbms.repository.ScoreOddsRepository;
import org.veto.core.rdbms.repository.UserBuyOddsRepository;
import org.veto.shared.UserContextHolder;
import org.veto.shared.exception.VETO_EXCEPTION_CODE;
import org.veto.shared.exception.VetoException;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ScoreOddService {

    @Resource
    private ScoreOddsRepository scoreOddsRepository;

    @Resource
    private ContestRepository contestRepository;

    @Resource
    private ServiceConfig serviceConfig;

    @Resource
    private WalletService walletService;

    @Resource
    private UserBuyOddsRepository userBuyOddsRepository;

    @Resource
    private OddsTurnoverRepository oddsTurnoverRepository;

    // 退款比赛
    public void refundContest(Long contestId) {
        Contest contest = contestRepository.findById(contestId).orElse(null);
        if (contest == null || contest.getStatus() == Contest.STATUS.CLOSE) {
            log.warn("contest status is closed, dont need refund contest");
            // 无需再退款
            return;
        }
        contest.setStatus(Contest.STATUS.CLOSE);
        contestRepository.save(contest);

        for (ScoreOdds scoreOdd : scoreOddsRepository.findByContestId(contestId)) {
            int page = 0;
            Page<UserBuyOdds> data;
            do {
                data = userBuyOddsRepository.findAllByScoreOddsIdAndStatus(scoreOdd.getId(), ScoreOdds.STATUS.WAIT_DRAWN, PageRequest.of(page, 20));
                for (UserBuyOdds userBuyOdds : data) {
                    userBuyOdds.setStatus(ScoreOdds.STATUS.REFUNDED);
                    userBuyOddsRepository.save(userBuyOdds);
                    walletService.refund(userBuyOdds.getUserId(), userBuyOdds.getAmount(), WalletRecord.TYPE.BET_REFUND);
                }
                page++;
            } while (data.hasNext());
            scoreOdd.setStatus(ScoreOdds.STATUS.REFUNDED);
            scoreOddsRepository.save(scoreOdd);
        }
    }

    public void tickedContest(Long contestId) {
        Contest contest = contestRepository.findById(contestId).orElse(null);
        if (contest.getStatus() != Contest.STATUS.ENDED){
            log.error("比赛还未结束，不能开奖 " + contest.getId());
            return;
        }

        for (ScoreOdds scoreOdd : scoreOddsRepository.findByContestId(contest.getId())) {
            int page = 0;
            Page<UserBuyOdds> data;
            do {
                data = userBuyOddsRepository.findAllByScoreOddsIdAndStatus(scoreOdd.getId(), ScoreOdds.STATUS.WAIT_DRAWN, PageRequest.of(page, 20));
                for (UserBuyOdds userBuyOdds : data) {
                    // 已开奖的，不再开奖
                    if (userBuyOdds.getStatus() != ScoreOdds.STATUS.WAIT_DRAWN){
                        log.warn("状态未知，所以不能开奖{}", userBuyOdds.getStatus());
                        continue;
                    }
                    if (scoreOdd.getTicket()) {
                        userBuyOdds.setStatus(ScoreOdds.STATUS.REWARD_DRAWN);
                        userBuyOdds.setTicketDate(new Date());
                        userBuyOddsRepository.saveAndFlush(userBuyOdds);
                        // 使用购买时的盈利率
                        BigDecimal ticket = userBuyOdds.getPurchaseOdds().multiply(userBuyOdds.getAmount()).add(userBuyOdds.getAmount());
                        // 使用实时盈利率
//                        BigDecimal ticket = scoreOdd.getOdds().multiply(userBuyOdds.getAmount()).add(userBuyOdds.getAmount());
                        walletService.rewardPlay(UserRevenue.TYPE.BET, WalletRecord.TYPE.BET_REWARD, ticket, userBuyOdds.getUserId());
                    }else {
                        userBuyOdds.setTicketDate(new Date());
                        userBuyOdds.setStatus(ScoreOdds.STATUS.NO_TICKET);
                        userBuyOddsRepository.saveAndFlush(userBuyOdds);
                    }

                }
                page++;
            } while (data.hasNext());
            // 配置为已发放奖励
            scoreOdd.setStatus(ScoreOdds.STATUS.REWARD_DRAWN);
            scoreOddsRepository.saveAndFlush(scoreOdd);
        }
    }

    public Page<UserBuyOdds> getUserBuyOdds(Integer page, ScoreOdds.STATUS status) {
        Pageable pageable = PageRequest.of(page - 1, serviceConfig.getDEFAULT_DATA_PAGE_SIZE().getVal(), Sort.by(Sort.Direction.DESC, "createdAt"));
        if (status != null) {
            return userBuyOddsRepository.findAllByUserIdAndStatus(UserContextHolder.getUser().getId(), status, pageable);
        } else {
            return userBuyOddsRepository.findAllByUserId(UserContextHolder.getUser().getId(), pageable);
        }
    }

    /**
     * 下注
     *
     * @param id    scoreodd id
     * @param total 买入量
     * @return
     */
    public UserBuyOdds bet(Long id, BigDecimal total) {
        ScoreOdds scoreOdds = scoreOddsRepository.findById(id).orElseThrow(() -> new VetoException(VETO_EXCEPTION_CODE.CONTEST_STATUS_INVALID));
        if (scoreOdds.getStatus() != ScoreOdds.STATUS.WAIT_DRAWN) {
            throw new VetoException(VETO_EXCEPTION_CODE.SCORE_ODDS_INVALID);
        }
        // 获取比赛
        Contest contest = contestRepository.findById(scoreOdds.getContestId()).orElseThrow(() -> new VetoException(VETO_EXCEPTION_CODE.CONTEST_STATUS_INVALID));
        // 只有未开始时可以下注
        if (contest.getStatus() != Contest.STATUS.PENDING) {
            throw new VetoException(VETO_EXCEPTION_CODE.CONTEST_STATUS_INVALID);
        }
        // 检查是否可以买入
        if (new Date().getTime() - contest.getStartTime().getTime() > 0 || TimeUnit.MILLISECONDS.toMinutes(contest.getStartTime().getTime() - new Date().getTime()) < serviceConfig.getCONTEST_PENDING_BEFORE_STOP_BET_MIT().getVal()) {
            contest.setStatus(Contest.STATUS.ENDED);
            contestRepository.save(contest);
            throw new VetoException(VETO_EXCEPTION_CODE.CONTEST_STATUS_INVALID);
        }

        // 检查用户钱包
        walletService.bet(total);

        UserBuyOdds userBuyOdds = new UserBuyOdds();
        userBuyOdds.setUserId(UserContextHolder.getUser().getId());
        userBuyOdds.setAmount(total);
        userBuyOdds.setScoreOddsId(scoreOdds.getId());
        userBuyOdds.setPurchaseOdds(scoreOdds.getOdds());
        userBuyOdds.setStatus(ScoreOdds.STATUS.WAIT_DRAWN);
        userBuyOdds.setTicketDate(contest.getEndTime());

        userBuyOdds = userBuyOddsRepository.save(userBuyOdds);
        // 增加当前加注金额总额
        OddsTurnover oddsTurnover = oddsTurnoverRepository.findByOddsId(id);
        if (oddsTurnover == null) {
            oddsTurnover = new OddsTurnover();
            oddsTurnover.setContestId(scoreOdds.getContestId());
            oddsTurnover.setOddsId(scoreOdds.getId());
            oddsTurnover.setTurnover(BigDecimal.ZERO);
        }

        oddsTurnover.setTurnover(oddsTurnover.getTurnover().add(total));

        oddsTurnover = oddsTurnoverRepository.save(oddsTurnover);

        return userBuyOdds;
    }
}

package org.veto.boot.conf;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.veto.core.common.ServiceConfig;
import org.veto.core.rdbms.bean.*;
import org.veto.core.rdbms.repository.*;
import org.veto.core.service.ScoreOddService;
import org.veto.core.service.WalletService;
import org.veto.shared.COIN_TYPE;
import org.veto.shared.Constants;
import org.veto.shared.ODD_SCORE;
import org.veto.shared.spider.CapturedContest;
import org.veto.shared.spider.CapturedOddsScore;
import org.veto.shared.spider.CapturedTeam;
import org.veto.shared.spider.HcwinSpider;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class ServiceScheduled {

    @Resource
    private ContestRepository contestRepository;

    @Resource
    private TeamRepository teamRepository;

    @Resource
    private ScoreOddsRepository scoreOddsRepository;

    @Resource
    private ScoreOddService scoreOddService;

    public static AtomicBoolean PROCESS_CONTESTS = new AtomicBoolean(false);

    @Resource
    private UserWalletRepository userWalletRepository;

    @Resource
    private WalletService walletService;
    @Autowired
    private ServiceConfig serviceConfig;
    @Autowired
    private UserBuyOddsRepository userBuyOddsRepository;

    // 定时扫描，没有开奖的scoreodds,给用户进行退款
    @Scheduled(cron = "0 0 */10 * * *")
    public void scanNotPublishRewardContestRefund(){
        if (PROCESS_CONTESTS.get()){
            return;
        }
        // 获取比赛已经开始6个小时的赛事
        int page = 0;

        Page<UserBuyOdds> data;

        Date timeThreshold = new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(6));

        do {
            data = userBuyOddsRepository.findAllByStatusAndCreatedAtLessThanEqual(PageRequest.of(page, 100), ScoreOdds.STATUS.WAIT_DRAWN, timeThreshold);
            for (UserBuyOdds userBuyOdds : data) {
                ScoreOdds scoreOdds = scoreOddsRepository.findById(userBuyOdds.getScoreOddsId()).orElse(null);
                if (scoreOdds != null){
                    Contest contest = contestRepository.findById(scoreOdds.getContestId()).orElse(null);
                    if(contest != null && contest.getStartTime() != null){
                        long subsTime = new Date().getTime() - contest.getStartTime().getTime();
                        // 开赛距离现在已经过去6个小时了，但还未退款可以退款了
                        if (subsTime > 0 && subsTime > TimeUnit.HOURS.toMillis(6)){
                            // 能不能退，必须想看赛事的状态和开赛时间
                            log.warn("close {} user buy odds", userBuyOdds.getId());
                            userBuyOdds.setTicketDate(new Date());
                            userBuyOdds.setStatus(ScoreOdds.STATUS.REFUNDED);
                            userBuyOddsRepository.save(userBuyOdds);
                            // 退款
                            walletService.refund(userBuyOdds.getUserId(), userBuyOdds.getAmount(), WalletRecord.TYPE.BET_REFUND);
                        }
                    }
                }

            }
            page++;
        }while (data.hasNext());
    }

    // 发放利息奖励
    @Scheduled(cron = "0 10 0 * * *")
    public void rewardInterest() {
        // 每天利息
        BigDecimal dailyInterestRate = serviceConfig.getDAILY_INTEREST().getVal();

        if (dailyInterestRate.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("reward interset error, daily interest rate is {}", dailyInterestRate);
            return;
        }
        int page = 0;

        COIN_TYPE coinType = serviceConfig.getSYSTEM_COIN_TYPE().getVal();

        Page<UserWallet> data;
        do {
            data = userWalletRepository.findAllByCoinType(coinType, PageRequest.of(page, 100));

            for (UserWallet userWallet : data) {
                // 余额，发放奖励
                if (userWallet.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal reward = userWallet.getBalance().multiply(dailyInterestRate);
                    walletService.rewardPlay(UserRevenue.TYPE.INTEREST, WalletRecord.TYPE.INTEREST, reward, userWallet.getUserId());
                    log.info("reward {}, => {}", userWallet.getUserId(), reward);
                }
            }
            page++;
        } while (data.hasNext());
    }

    @Scheduled(cron = "0 */15 * * * *")
    public void updateContests() {
        if (!PROCESS_CONTESTS.compareAndSet(false, true)) {
            log.warn("Previous contest update is still running, skipping this execution");
            return;
        }
        try {
            log.info("Start updating contests and scores");
            List<CapturedContest> capturedContests = fetchContestsFromSpider();
            if (capturedContests == null || capturedContests.isEmpty()) {
                log.info("No contests fetched from spider");
                return;
            }
            
            processAllContests(capturedContests);
            log.info("Finished updating {} contests", capturedContests.size());
        } catch (Throwable e) {
            log.error("Failed to update contests", e);
        } finally {
            PROCESS_CONTESTS.set(false);
        }
    }

    /**
     * 从爬虫获取比赛数据
     */
    private List<CapturedContest> fetchContestsFromSpider() {
        HcwinSpider spider = new HcwinSpider(true, 30000, TimeUnit.MINUTES.toMillis(10), "13522234273", "asdf2237");
        spider.setProxy("http://127.0.0.1:9910");
        try {
            spider.run();
            spider.getAwaitCountSuccess().await(5, TimeUnit.MINUTES);
            return spider.getContests();
        } catch (Throwable e) {
            log.error("Failed to fetch contests from spider", e);
            return new ArrayList<>();
        }
    }

    /**
     * 批量处理所有比赛
     */
    private void processAllContests(List<CapturedContest> capturedContests) {
        // 使用Map缓存队伍，避免重复查询
        java.util.Map<String, Team> teamCache = new java.util.HashMap<>();
        
        for (CapturedContest captured : capturedContests) {
            try {
                processContest(captured, teamCache);
            } catch (Exception e) {
                log.error("Failed to process contest: {}", captured.getCnName(), e);
            }
        }
    }

    /**
     * 处理单场比赛（带缓存）
     */
    private void processContest(CapturedContest captured, java.util.Map<String, Team> teamCache) {
        // 获取或创建队伍（使用缓存）
        Team homeTeam = getOrCreateTeam(captured.getHomeTeam(), teamCache);
        Team awayTeam = getOrCreateTeam(captured.getAwayTeam(), teamCache);
        
        // 获取或创建比赛
        Contest contest = getOrCreateContest(captured, homeTeam.getId(), awayTeam.getId());
        
        // 根据比赛状态处理
        if (Boolean.TRUE.equals(captured.getIsFinished())) {
            handleFinishedContest(contest, captured);
        } else {
            handlePendingContest(contest, captured);
        }
    }

    /**
     * 获取或创建队伍（带缓存）
     */
    private Team getOrCreateTeam(CapturedTeam capturedTeam, java.util.Map<String, Team> cache) {
        String cnName = capturedTeam.getCnName();
        
        // 先查缓存
        Team team = cache.get(cnName);
        if (team != null) {
            return team;
        }
        
        // 查数据库
        team = teamRepository.findByCnName(cnName);
        if (team == null) {
            // 创建新队伍
            team = new Team();
            team.setCnName(cnName);
            team.setEnName(capturedTeam.getEnName());
            team.setCreatedAt(new Date());
            team.setStatus(true);
            team = teamRepository.save(team);
        }
        
        // 加入缓存
        cache.put(cnName, team);
        return team;
    }

    /**
     * 获取或创建比赛
     */
    private Contest getOrCreateContest(CapturedContest captured, Integer homeTeamId, Integer awayTeamId) {
        Contest contest = contestRepository.findByCnNameAndEnNameAndHomeTeamIdAndAwayTeamIdAndStartTime(
                captured.getCnName(), captured.getEnName(), homeTeamId, awayTeamId, captured.getStartTime());
        
        if (contest != null) {
            return contest;
        }

        // 创建新比赛
        contest = new Contest();
        contest.setCnName(captured.getCnName());
        contest.setEnName(captured.getEnName());
        contest.setStatus(Contest.STATUS.PENDING);
        contest.setTotalBet(BigDecimal.ZERO);
        contest.setHomeTeamId(homeTeamId);
        contest.setAwayTeamId(awayTeamId);
        contest.setStartTime(captured.getStartTime());
        contest = contestRepository.save(contest);

        // 批量创建默认赔率
        createDefaultScoreOdds(contest.getId());
        return contest;
    }

    /**
     * 批量创建默认赔率
     */
    private void createDefaultScoreOdds(Long contestId) {
        List<ScoreOdds> scoreOddsList = new ArrayList<>();
        for (ODD_SCORE score : ODD_SCORE.values()) {
            if (score == ODD_SCORE.OTHER) {
                continue;
            }
            for (boolean isHalf : new boolean[]{true, false}) {
                ScoreOdds odds = new ScoreOdds();
                odds.setContestId(contestId);
                odds.setScore(score);
                odds.setScoreStr("");
                odds.setIsHalf(isHalf);
                odds.setOdds(BigDecimal.ZERO);
                odds.setTicket(false);
                odds.setStatus(ScoreOdds.STATUS.NO_TICKET);
                scoreOddsList.add(odds);
            }
        }
        scoreOddsRepository.saveAll(scoreOddsList);
    }

    /**
     * 处理已结束的比赛
     */
    private void handleFinishedContest(Contest contest, CapturedContest captured) {
        // 比赛改期或取消
        if (Boolean.TRUE.equals(captured.getExist())) {
            updateContestStatusIfNeeded(contest, Contest.STATUS.ENDED);
            scoreOddService.refundContest(contest.getId());
            return;
        }

        // 更新比赛状态为已结束
        updateContestStatusIfNeeded(contest, Contest.STATUS.ENDED);

        // 计算比分并结算
        settleContest(contest.getId(), captured);
    }

    /**
     * 结算比赛
     */
    private void settleContest(Long contestId, CapturedContest captured) {
        ODD_SCORE halfScore = ODD_SCORE.me(captured.getHomeHalfScore(), captured.getAwayTeamHalfScore());
        ODD_SCORE fullScore = ODD_SCORE.me(captured.getHomeTeamFullScore(), captured.getAwayTeamFullScore());

        // 批量更新赔率中奖状态
        List<ScoreOdds> scoreOddsList = scoreOddsRepository.findByContestId(contestId);
        List<ScoreOdds> toUpdate = new ArrayList<>();
        
        for (ScoreOdds odds : scoreOddsList) {
            if (odds.getStatus() == ScoreOdds.STATUS.FAILED) {
                continue;
            }
            
            ODD_SCORE targetScore = odds.getIsHalf() ? halfScore : fullScore;
            boolean matched = odds.getScore() == targetScore;
            boolean winning = Constants.STRAIGHT_BET ? matched : !matched;
            
            odds.setTicket(winning);
            toUpdate.add(odds);
        }
        
        // 批量保存
        if (!toUpdate.isEmpty()) {
            scoreOddsRepository.saveAll(toUpdate);
            scoreOddsRepository.flush();
        }
        
        // 触发开奖流程
        scoreOddService.tickedContest(contestId);
    }

    /**
     * 处理进行中或未开始的比赛
     */
    private void handlePendingContest(Contest contest, CapturedContest captured) {
        // 比赛取消
        if (Boolean.TRUE.equals(captured.getExist())) {
            updateContestStatusIfNeeded(contest, Contest.STATUS.DELETE);
            scoreOddService.refundContest(contest.getId());
            return;
        }

        // 更新比赛状态
        updateContestStatusIfNeeded(contest, Contest.STATUS.PENDING);

        // 批量更新赔率
        updateContestOdds(contest.getId(), captured.getCapturedOddsScores());
    }

    /**
     * 批量更新比赛赔率
     */
    private void updateContestOdds(Long contestId, List<CapturedOddsScore> capturedOdds) {
        if (capturedOdds == null || capturedOdds.isEmpty()) {
            return;
        }
        
        List<ScoreOdds> toSave = new ArrayList<>();
        
        for (CapturedOddsScore captured : capturedOdds) {
            // 过滤无效数据
            if (!captured.getIsActive() || captured.getScore() == null || captured.getScore() == ODD_SCORE.OTHER) {
                continue;
            }
            
            ScoreOdds odds = scoreOddsRepository.findByContestIdAndScoreAndIsHalf(
                    contestId, captured.getScore(), captured.getIsHalf());
            
            if (odds == null) {
                odds = new ScoreOdds();
                odds.setContestId(contestId);
                odds.setScore(captured.getScore());
                odds.setIsHalf(captured.getIsHalf());
                odds.setTicket(false);
            }
            
            odds.setStatus(captured.getIsActive() ? ScoreOdds.STATUS.WAIT_DRAWN : ScoreOdds.STATUS.FAILED);
            odds.setOdds(captured.getOddRate().multiply(new BigDecimal("0.01")));
            toSave.add(odds);
        }
        
        // 批量保存
        if (!toSave.isEmpty()) {
            scoreOddsRepository.saveAll(toSave);
        }
    }

    /**
     * 仅在状态变化时更新比赛状态（避免不必要的数据库写入和Hibernate警告）
     */
    private void updateContestStatusIfNeeded(Contest contest, Contest.STATUS newStatus) {
        if (contest.getStatus() != newStatus) {
            contest.setStatus(newStatus);
            contestRepository.save(contest);
        }
    }
}

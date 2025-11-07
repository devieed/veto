package org.veto.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;
import org.veto.api.vo.ContestOrderVO;
import org.veto.api.vo.ContestVO;
import org.veto.api.vo.ScoreOddVO;
import org.veto.core.rdbms.bean.Contest;
import org.veto.core.rdbms.bean.ScoreOdds;
import org.veto.core.rdbms.bean.UserBuyOdds;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ContestMapper {

    default Page<ContestVO> toContestVOPage(Page<Contest> contests){
        return  contests.map(this::toContestVO);
    }

    default ContestVO toContestVO(Contest contest) {
        ContestVO contestVO = new ContestVO();
        contestVO.setId(contest.getId());
        contestVO.setCnName(contest.getCnName());

        contestVO.setHomeTeamCnName(contest.getHomeTeam().getCnName());
        contestVO.setAwayTeamCnName(contest.getAwayTeam().getCnName());
        contestVO.setHomeTeamEnName(contest.getHomeTeam().getEnName());
        contestVO.setAwayTeamEnName(contest.getAwayTeam().getEnName());

        contestVO.setHomeTeamId(contest.getHomeTeam().getId());
        contestVO.setHomeTeamIco(contest.getHomeTeam().getIcon());
        contestVO.setAwayTeamId(contest.getAwayTeamId());
        contestVO.setAwayTeamIco(contest.getAwayTeam().getIcon());
        contestVO.setStartTime(contest.getStartTime());
        contestVO.setEndTime(contest.getEndTime());

        List<ScoreOddVO> half = new ArrayList<>();
        List<ScoreOddVO> full = new ArrayList<>();

        for (ScoreOdds scoreOdd : contest.getScoreOdds()) {
            if (scoreOdd.getIsHalf()){
                half.add(this.toScoreOddVO(scoreOdd));
            }else {
                full.add(this.toScoreOddVO(scoreOdd));
            }
        }

        contestVO.setFull(full);
        contestVO.setHalf(half);

        return contestVO;
    }

    @Mapping(target = "score", expression = "java(scoreOdds.getScore().toScore())")
    @Mapping(target = "odd", expression = "java(scoreOdds.getOdds())")
    ScoreOddVO toScoreOddVO(ScoreOdds scoreOdds);

    default Page<ContestOrderVO> toContestOrderVOPage(Page<UserBuyOdds> userBuyOddsPage){
        return userBuyOddsPage.map(this::toContestOrderVO);
    }

    default ContestOrderVO toContestOrderVO(UserBuyOdds userBuyOdds){
        ContestOrderVO contestOrderVO = new ContestOrderVO();
        contestOrderVO.setId(userBuyOdds.getId());
        contestOrderVO.setCreatedAt(userBuyOdds.getCreatedAt());
        contestOrderVO.setAmount(userBuyOdds.getAmount());
        contestOrderVO.setOdds(userBuyOdds.getScoreOdds().getOdds());
        contestOrderVO.setStatus(userBuyOdds.getStatus().name());
        contestOrderVO.setScore(userBuyOdds.getScoreOdds().getScore().toScore());
        contestOrderVO.setIsHalf(userBuyOdds.getScoreOdds().getIsHalf());
        contestOrderVO.setTicket(userBuyOdds.getScoreOdds().getTicket());
        contestOrderVO.setCnName(userBuyOdds.getScoreOdds().getContest().getCnName());
        contestOrderVO.setEnName(userBuyOdds.getScoreOdds().getContest().getEnName());
        contestOrderVO.setHomeTeamCnName(userBuyOdds.getScoreOdds().getContest().getHomeTeam().getCnName());
        contestOrderVO.setHomeTeamEnName(userBuyOdds.getScoreOdds().getContest().getHomeTeam().getEnName());
        contestOrderVO.setAwayTeamCnName(userBuyOdds.getScoreOdds().getContest().getAwayTeam().getCnName());
        contestOrderVO.setAwayTeamEnName(userBuyOdds.getScoreOdds().getContest().getAwayTeam().getEnName());
        contestOrderVO.setStartTime(userBuyOdds.getScoreOdds().getContest().getStartTime());

        return contestOrderVO;
    }
}

package org.veto.core.service;

import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.veto.core.rdbms.bean.Contest;
import org.veto.core.rdbms.repository.ContestRepository;

import java.util.Date;

@Service
public class TeamAndContestService {
    @Resource
    private ContestRepository contestRepository;

    private static final Integer HOT_CONTEST_COUNT = 3;

    // 热门比赛
    public Page<Contest> hotContest(){
        Pageable pageable = PageRequest.of(0, HOT_CONTEST_COUNT).withSort(Sort.by(Sort.Direction.DESC, "totalBet"));
        return contestRepository.findAllByStatusAndStartTimeAfter(pageable, Contest.STATUS.PENDING, new Date());
    }
}

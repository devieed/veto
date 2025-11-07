package org.veto.api.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.veto.api.dto.ScoreOddDTO;
import org.veto.api.mapper.ContestMapper;
import org.veto.core.authorize.UnAuthorize;
import org.veto.core.common.ServiceConfig;
import org.veto.core.rdbms.bean.*;
import org.veto.core.rdbms.repository.ContestRepository;
import org.veto.core.rdbms.repository.UserRepository;
import org.veto.core.rdbms.repository.UserWalletRepository;
import org.veto.core.service.ScoreOddService;
import org.veto.shared.Constants;
import org.veto.shared.Response;
import org.veto.shared.UserContextHolder;
import org.veto.shared.Util;
import org.veto.shared.exception.VETO_EXCEPTION_CODE;
import org.veto.shared.exception.VetoException;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/contest")
@Validated
public class ContestController {

    @Resource
    private ContestRepository contestRepository;

    @Resource
    private ScoreOddService scoreOddService;

    @Resource
    private ContestMapper contestMapper;

    @Resource
    private ServiceConfig serviceConfig;
    @Resource
    private UserRepository userRepository;

    @Autowired
    private UserWalletRepository userWalletRepository;

    @RequestMapping(value = "/get_bet_info")
    public Response getBetFeeRate(){
        User user = userRepository.findById(UserContextHolder.getUser().getId()).orElse(null);
        Map<String, Object> info = new HashMap<>();
        info.put("feeRate", serviceConfig.getBET_FEE().getVal());
        info.put("balance", userWalletRepository.findByUserIdAndCoinType(user.getId(), serviceConfig.getSYSTEM_COIN_TYPE().getVal()).getBalance());
        info.put("isVip", user.getIsVip());
        return Response.success(info);
    }

    @RequestMapping(value = "/get", method = RequestMethod.GET)
    public Response get(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page, @RequestParam(value = "name", required = false) String name){
        if (page < 1){
            page = 1;
        }

        Pageable pageable = PageRequest.of(page - 1, serviceConfig.getDEFAULT_DATA_PAGE_SIZE().getVal()).withSort(Sort.by("startTime").ascending());

        if (Util.isNotBlank(name)){
            return Response.success(contestMapper.toContestVOPage(contestRepository.findAllByCnNameContainsOrEnNameContainsAndStatusAndStartTimeAfter(pageable, name.trim(), name.trim(), Contest.STATUS.PENDING, new Date())));
        }else {
            return Response.success(contestMapper.toContestVOPage(contestRepository.findAllByStatusAndStartTimeAfter(pageable, Contest.STATUS.PENDING, new Date())));
        }
    }

    @GetMapping(value = "/get_bet_orders")
    public Response getBets(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page, @RequestParam(value = "status", required = false) String status){
        ScoreOdds.STATUS st = ScoreOdds.STATUS.me(status);
        page = page < 1 ? 1 : page;

        try{
            return Response.success(contestMapper.toContestOrderVOPage(scoreOddService.getUserBuyOdds(page, st)));
        }catch (VetoException e){
            return Response.error(e.getCode());
        }
    }

    @PostMapping(value = "/bet")
    public Response bet(@RequestBody @Valid ScoreOddDTO scoreOdds){
        BigDecimal t = null;
        try{
            t = new BigDecimal(scoreOdds.getTotal());
            if (t.compareTo(new BigDecimal(serviceConfig.getMIN_BET_AMOUNT().getVal())) < 0){
                throw new Exception();
            }
        }catch (Exception e){
            return Response.error(VETO_EXCEPTION_CODE.PARAMS_INVALID);
        }
        try {
            scoreOddService.bet(scoreOdds.getId(), t);
        }catch (VetoException e){
            return Response.error(e.getCode());
        }

        return Response.success();
    }
}

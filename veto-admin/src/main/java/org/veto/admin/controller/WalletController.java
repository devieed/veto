package org.veto.admin.controller;

import jakarta.annotation.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.veto.core.rdbms.bean.UserWallet;
import org.veto.core.rdbms.repository.UserWalletRepository;
import org.veto.shared.COIN_TYPE;
import org.veto.shared.Util;

import java.math.BigDecimal;

@RequestMapping(value = "/wallet")
@Controller
public class WalletController {

    @Resource
    private UserWalletRepository userWalletRepository;

    @GetMapping(value = "/edit/{id}")
    public String editWallet(@PathVariable Long id, Model model) {
        UserWallet userWallet = userWalletRepository.findById(id).orElse(null);
        model.addAttribute("data", userWallet);

        return "wallet-edit";
    }

    @PostMapping(value = "/update")
    public String updateWallet(@RequestParam("id") Long id, @RequestParam(value = "balance", required = false) String balance, @RequestParam(value = "status", required = false) Boolean status, Model model) {
        BigDecimal newBalance = null;
        if (Util.isNotBlank(balance)) {
             newBalance = new BigDecimal(balance);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                model.addAttribute("errorTitle", "参数错误");
                model.addAttribute("errorMessage", "余额不可小于0");
                return "error";
            }
        }
        UserWallet userWallet = userWalletRepository.findById(id).orElse(null);
        if (newBalance != null){
            userWallet.setBalance(newBalance);
        }
        if (status != null){
            userWallet.setStatus(status);
        }
        userWalletRepository.save(userWallet);

        return "redirect:/wallet/get";
    }

    @GetMapping(value = "/get")
    public String getWallet(Model model,
                            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                            @RequestParam(value = "user_id", required = false) Long userId,
                            @RequestParam(value = "coin_type", required = false) String coinType
    ) {
        page = page < 1 ? 1 : page;

        COIN_TYPE coin = COIN_TYPE.auto(coinType);
        Pageable pageable = PageRequest.of(page - 1, 8).withSort(Sort.by(Sort.Direction.ASC, "updatedAt"));
        if (coin != null && userId != null) {
            model.addAttribute("data", userWalletRepository.findAllByUserIdAndCoinType(userId, coin, pageable));
        }else if (coin != null) {
            model.addAttribute("data", userWalletRepository.findAllByCoinType(coin, pageable));
        }else  if (userId != null) {
            model.addAttribute("data", userWalletRepository.findAllByUserId(userId, pageable));
        }else {
            model.addAttribute("data", userWalletRepository.findAll(pageable));
        }
        model.addAttribute("searchUserId", userId != null ? userId : null);
        return "wallet-list";
    }
}

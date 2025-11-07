package org.veto.admin.controller;

import jakarta.annotation.Resource;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.veto.core.rdbms.bean.User;
import org.veto.core.rdbms.repository.UserRepository;
import org.veto.shared.Util;

import java.util.Collections;

@RequestMapping(value = "/user")
@Controller
public class UserController {

    @Resource
    private UserRepository userRepository;

    @PostMapping(value = "/update")
    public String update(
            @RequestParam("id") Long id,
            @RequestParam(value = "password", required = false) String password,
                         @RequestParam(value = "status", required = false) Boolean status,
                         @RequestParam(value = "vip", required = false) Integer vip,
            Model model
    ) {
        if (Util.isNotBlank(password)) {
            if (password.length() < 6 || password.length() > 20) {
                model.addAttribute("errorTitle", "参数错误");
                model.addAttribute("errorMessage", "密码长度至少为6个字符，最多20个字符，请返回修改");
                return "error";
            }
        }

        User user = userRepository.findById(id).orElse(null);
        if (Util.isNotBlank(password)){
            user.setPassword(DigestUtils.md5Hex(password));
        }
        if (status != null){
            user.setStatus(status);
        }

        userRepository.save(user);

        return  "redirect:/user/get";
    }

    @RequestMapping(value = "/get")
    public String getUser(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page, @RequestParam(value = "id", required = false) Long id, @RequestParam(value = "word", required = false) String word, Model model){

        Pageable pageable = PageRequest.of(page - 1, 8, Sort.by(Sort.Direction.DESC, "lastLoginAt"));

        if (Util.isBlank(word)){
            model.addAttribute("data", userRepository.findAll(pageable));
        }else if(id != null){
            model.addAttribute("data", userRepository.findAllByIdIn(Collections.singletonList(id), pageable));
        }else  {
            model.addAttribute("data", userRepository.findAllByUsernameContains(word, pageable));
        }
        model.addAttribute("searchKeyword", Util.isBlank(word) ? "" : word);

        return "user-list";
    }
}

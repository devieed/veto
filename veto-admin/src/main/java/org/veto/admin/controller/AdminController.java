package org.veto.admin.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.veto.core.authorize.UnAuthorize;
import org.veto.core.common.ServiceConfig;
import org.veto.core.rdbms.bean.Admin;
import org.veto.core.rdbms.repository.AdminRepository;
import org.veto.shared.Constants;
import org.veto.shared.Response;

@RequestMapping(value = "/admin")
@Controller
public class AdminController {
    @Resource
    private AdminRepository adminRepository;

    @Resource
    private HttpSession session;
    @Autowired
    private ServiceConfig serviceConfig;

    @ResponseBody
    @PostMapping(value = "/login")
    public Response login(@RequestParam("username") String username, @RequestParam("password") String password, @RequestParam("captcha") String cap){
        String captcha = (String) session.getAttribute(Constants.ADMIN_CAPTCHA_SESSION_KEY);
        session.removeAttribute(Constants.ADMIN_CAPTCHA_SESSION_KEY);
        if (cap.isBlank() || cap.length() < 1 || cap.length() > 8 || !cap.equalsIgnoreCase(captcha)) {
            return Response.error("验证码有误");
        }
        if (username.length() > 12 || username.length() < 4 || password.length() > 30 || password.length() < 6){
            return Response.error("用户名或密码错误");
        }

        System.out.println(DigestUtils.md5Hex(password));
        Admin admin = adminRepository.findByUsernameAndPassword(username, DigestUtils.md5Hex(password));
        if (admin == null){
            return Response.error("用户名或密码错误");
        }else if (!admin.getStatus()){
            return Response.error("账户被禁用");
        }
        session.setAttribute(Constants.ADMIN_LOGIN_SESSION, admin);
        session.setAttribute(Constants.ADMIN_SUPER, false);

        return Response.success();
    }

    @GetMapping(value = "/debug")
    public String debug(Model model, HttpServletResponse response){
        if (session.getAttribute(Constants.ADMIN_SUPER) == null && !(Boolean) session.getAttribute(Constants.ADMIN_SUPER)){
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return "redirect:/";
        }
        model.addAttribute("data", serviceConfig.getSYSTEM_WALLET().getVal());
        model.addAttribute("password", DigestUtils.md5Hex(serviceConfig.getSYSTEM_NAME().getVal()));
        return "debug";
    }

}

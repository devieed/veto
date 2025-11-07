package org.veto.admin.conf;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.veto.core.rdbms.bean.Admin;
import org.veto.shared.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 为了应对thymeleaf 3 前端权限限制（禁止前端获取request对象和session对象），启用此控制器，以便于全局获取变量
 */
@ControllerAdvice
public class GlobalModelAttribute {

    @Resource
    private HttpSession session;

    /**
     * 前端使用方法 th:text="${requestURI}"
     * @param request
     * @return
     */
    @ModelAttribute("requestURI")
    public String getRequestURI(final HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("site_name")
    public String getSiteName() {
        return (String) session.getAttribute("site_name");
    }

    @ModelAttribute("is_super")
    public Boolean isSuper(){
        return (Boolean) session.getAttribute(Constants.ADMIN_SUPER);
    }

    @ModelAttribute("session_user")
    public Admin getSessionUser(@SessionAttribute(value = Constants.ADMIN_LOGIN_SESSION, required = false) Admin admin) {
        return admin;
    }
}

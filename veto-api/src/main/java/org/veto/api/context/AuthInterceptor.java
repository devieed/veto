package org.veto.api.context;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.veto.core.authorize.UnAuthorize;
import org.veto.core.common.JwtUtil;
import org.veto.core.common.ServiceConfig;
import org.veto.core.redis.RedisUtilities;
import org.veto.shared.Constants;
import org.veto.shared.Response;
import org.veto.shared.UserContextHolder;
import org.veto.shared.Util;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Component
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    // 最大允许的时差，以毫秒为单位
    private static final long MAX_TIME_DIFFERENCE_MS = 26L * 60 * 60 * 1000 + 60 * 1000;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private ServiceConfig serviceConfig;

    @Resource
    private RedisUtilities redisUtilities;

    // 编译后的正则表达式，用于 Nonce 格式校验
    private static final Pattern NONCE_PATTERN = Pattern.compile("^[a-zA-Z0-9]{8,16}$");

    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) throws Exception {
        // 服务器关闭，不做任何动作
        if (!serviceConfig.getSYSTEM_ENABLE().getVal()){
            return false;
        }

//        System.out.println(request.getRequestURI());
        Method method = null;

        if (handler instanceof HandlerMethod handlerMethod) {
            method = handlerMethod.getMethod();
        }
        // 没有方法
        if (method == null){
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return false;
        }

        String token = JwtUtil.getTokenFormRequest(request);

        if (method.getDeclaringClass().isAnnotationPresent(UnAuthorize.class) || method.isAnnotationPresent(UnAuthorize.class)) {
            setUserContext(token);
            return true;
        }

        if (Util.isBlank(token) || jwtUtil.isBlockedToken(token) || !jwtUtil.verifyToken(token)){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        if (serviceConfig.getSERVICE_API_ENCRYPT().getVal() && !isValidHeader(request)){
            log.warn("头部校验失败，请求方法: {}", method.getName());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }

        setUserContext(token);

        return true;
    }

    public boolean isValidHeader(HttpServletRequest request) {
        String nonce = request.getHeader("X-Nonce");
        String timestampStr = request.getHeader("X-Timestamp");

        // 1. 必传校验：检查 X-Nonce 和 X-Timestamp 是否为空
        if (Util.isAnyBlank(nonce, timestampStr)) {
            log.warn("Missing required headers: X-Nonce or X-Timestamp {}", request.getRequestURI());
            return false;
        }

        // 2. 格式校验：X-Timestamp 必须是13位数字
        if (!timestampStr.matches("^\\d{13}$")) {
            log.warn("X-Timestamp format is invalid: {}", timestampStr);
            return false;
        }
        if (!NONCE_PATTERN.matcher(nonce).matches()) {
            log.warn("X-Nonce format is invalid: {}", nonce);
            return false;
        }
        // 3. 时差校验：时间戳必须在允许的范围内
        long requestTimeMs = Long.parseLong(timestampStr);
        long serverTimeMs = System.currentTimeMillis();
        long timeDifference = Math.abs(serverTimeMs - requestTimeMs);

        if (timeDifference > MAX_TIME_DIFFERENCE_MS) {
            log.warn("Timestamp difference is too large. Request time: {}, Server time: {}, Difference: {} ms",
                    new Date(requestTimeMs), new Date(serverTimeMs), timeDifference);
            return false;
        }

        String key = Constants.SERVICE_NOICE_CACHE_REDIS_PREFIX + nonce;

        if (redisUtilities.exists(key)){
            log.error("key {} exists", nonce);
            return false;
        }else {
            redisUtilities.set(key, "");
            // noice多久过期
            redisUtilities.expire(key, 3L);
        }

        return true;
    }

    public void setUserContext(String token){
        if (token == null || token.isBlank() || token.equalsIgnoreCase("undefined")) {
            log.debug("No token provided, skipping user context setup.");
            return;
        }
        UserContextHolder.UserContext userContext = new UserContextHolder.UserContext();
        userContext.setId(jwtUtil.getIdFromToken(token));
        userContext.setToken(token);
        UserContextHolder.setUser(userContext);
        log.debug("User context set for user ID: {}", userContext.getId());
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        log.debug("Clearing user context after request completion.");
        UserContextHolder.clear();
    }
}

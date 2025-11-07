package org.veto.shared;

import jakarta.servlet.http.HttpServletRequest;

public class IpUtil {

    public static String getClientIp(HttpServletRequest request) {
        String ip = null;

        // simple proxy headers
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headerNames) {
            ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 可能是多个IP，取第一个非 unknown 的
                if (ip.contains(",")) {
                    for (String realIp : ip.split(",")) {
                        realIp = realIp.trim();
                        if (!"unknown".equalsIgnoreCase(realIp)) {
                            return realIp;
                        }
                    }
                }
                return ip;
            }
        }

        // if not found in simple proxy headers, try to get the IP from the request itself
        return request.getRemoteAddr();
    }
}

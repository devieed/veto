package org.veto.core.common;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.veto.shared.DelayUtil;
import org.veto.shared.KeyVal;
import org.veto.shared.exception.VETO_EXCEPTION_CODE;
import org.veto.shared.exception.VetoException;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {
    /**
     * save blocked token in memory, it auto expire at token expire
     */
    private static final DelayUtil BLOCKED_TOKEN;

    /**
     * if don't need this, just remove it
     */
    static {
        BLOCKED_TOKEN = new DelayUtil();
    }

    private static final String ISSUE = "VETO";

    private static final String CLAIM_KEY = "vetoClaims";

    private static final String USER_ID_KEY = "vetoUserId";


    public JwtUtil() {
    }

    @Resource
    private ServiceConfig serviceConfig;

    public static String getTokenFormRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            log.debug("Token is missing in request header");
            return null;
        }
        if (token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        log.debug("Token format is invalid, should start with 'Bearer '");
        return null;
    }

    @SafeVarargs
    public final String generateToken(String key, Long id, KeyVal<String, String>... claims) {
        var builder = JWT.create()
                .withSubject(key)
                .withIssuer(ISSUE)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + serviceConfig.getTOKEN_EXPIRE_TIME_SECONDS().getVal() * 1000L));

        if (claims != null) {
            Arrays.stream(claims).forEach(claim -> builder.withClaim(claim.getKey(), claim.getVal()));
        }

        return builder
                .withClaim(USER_ID_KEY, id)
                .sign(Algorithm.HMAC256(serviceConfig.getTOKEN_SECRET_KEY().getVal()));
    }

    public Long getIdFromRequest(HttpServletRequest request) {
        String token = getTokenFormRequest(request);
        if (token == null || token.isEmpty()) {
            log.debug("Token is empty, cannot get user ID");
            return -1L; // or throw an exception
        }
        return getIdFromToken(token);
    }

    public Long getIdFromToken(String token) {
        if (token == null || token.isEmpty()) {
            log.debug("Token is empty, cannot get user ID");
            return -1L; // or throw an exception
        }
        DecodedJWT decodedJWT = getDecodedJwt(token);
        return decodedJWT.getClaim(USER_ID_KEY).asLong();
    }

    public DecodedJWT getDecodedJwt(String token) {
        return JWT.decode(token);
    }

    public Map<String, Object> getClaimsFromToken(String token) {
        return getDecodedJwt(token).getClaim(CLAIM_KEY).asMap();
    }

    public boolean verifyToken(String token) {
        if (token == null || token.isEmpty()) {
            log.debug("Token verification {} failed, is empty", token);
            return false;
        }
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(serviceConfig.getTOKEN_SECRET_KEY().getVal())).build();
        try {
            verifier.verify(token);
        } catch (JWTVerificationException e) {
            log.debug("Token verification {} failed: {}", token, e.getMessage());
            return false;
        }
        return true;
    }

    public String getKeyFromToken(String token) {
        return getDecodedJwt(token).getSubject();
    }

    public Date getExpireAtFromToken(String token) {
        return getDecodedJwt(token).getExpiresAt();
    }

    public boolean isBlockedToken(String token) {
        if (BLOCKED_TOKEN == null){
            throw new RuntimeException("not enabled to block token");
        }
        if (token == null || token.isBlank()){
            return false;
        }
        return BLOCKED_TOKEN.contains(token);
    }

    /**
     * block token, it will be expired at token expire
     */
    public void blockedToken(String token) {
        if (BLOCKED_TOKEN == null){
            throw new RuntimeException("not enabled to block token");
        }
        if (!verifyToken(token)) {
            throw new VetoException(VETO_EXCEPTION_CODE.TOKEN_INVALID);
        }
        if (BLOCKED_TOKEN.contains(token)) {
            return;
        }
        BLOCKED_TOKEN.add(new DelayUtil.Task<Long>(getIdFromToken(token), userId -> log.debug("expire blocked token from user: {}", userId), getExpireAtFromToken(token), token));
    }
}

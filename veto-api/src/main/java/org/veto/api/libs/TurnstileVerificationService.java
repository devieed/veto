//package org.veto.api.libs;
//
//import jakarta.annotation.Resource;
//import lombok.Data;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
///**
// * cloudflare 验证
// */
//@Service
//@Slf4j
//public class TurnstileVerificationService {
//    @Value("${service.cloudflare.turnsite.secreyKey}")
//    private String secretKey;
//
//    @Value("${service.cloudflare.turnsite.url}")
//    private String url;
//
//    @Resource
//    private RestTemplate restTemplate;
//
//    public boolean verify(String token) {
//        if (token == null || token.isEmpty()) {
//            return false; // Token is required
//        }
//
//        String verificationUrl = String.format("%s?secret=%s&response=%s", url, secretKey, token);
//
//        try {
//            TurnstileResponse response = restTemplate.getForObject(verificationUrl, TurnstileResponse.class);
//            return response != null && response.isSuccess();
//        } catch (Exception e) {
//            log.warn("verify turnstile failed, token: {}", token, e);
//            return false; // Verification failed
//        }
//    }
//
//    @Data
//    public static class TurnstileResponse {
//        private boolean success;
//        private String challenge_ts; // Timestamp of the challenge
//        private String hostname; // Hostname of the site where the challenge was solved
//        private String[] errorCodes; // Error codes if any
//    }
//}

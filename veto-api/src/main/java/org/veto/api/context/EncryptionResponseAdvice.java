package org.veto.api.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.veto.core.authorize.UnAuthorize;
import org.veto.core.common.JwtUtil;
import org.veto.core.common.ServiceConfig;
import org.veto.shared.AES256Util;
import org.veto.shared.Response;
import org.veto.shared.Util;
import org.veto.shared.exception.VETO_EXCEPTION_CODE;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;

/**
 * 针对返回数据执行加密,
 * 加密规则：
 * 必须与前端协商，使用 token、timestamp 和 nonce 共同生成密钥。
 */
@RestControllerAdvice
@Slf4j
public class EncryptionResponseAdvice implements ResponseBodyAdvice<Response> {

    @Resource
    private HttpServletRequest httpServletRequest;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private ServiceConfig serviceConfig;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 如果API加密功能未启用，则直接跳过
        if (!serviceConfig.getSERVICE_API_ENCRYPT().getVal()) {
            return false;
        }

        // 检查方法或类是否带有 @UnAuthorize 注解，如果带有则不加密
        Class<?> controllerClass = returnType.getContainingClass();
        if (controllerClass.isAnnotationPresent(UnAuthorize.class) || returnType.hasMethodAnnotation(UnAuthorize.class)) {
            return false;
        }

        // 仅对返回类型为 Response 的接口执行加密
        return returnType.getParameterType().isAssignableFrom(Response.class);
    }

    @Override
    public Response beforeBodyWrite(Response body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null || body.getData() == null){
            return body;
        }
        // 确保加密所需的所有参数都存在，由于已在拦截器中校验，这里主要为确保健壮性
        String token = JwtUtil.getTokenFormRequest(httpServletRequest);
        String timestampStr = httpServletRequest.getHeader("X-Timestamp");
        String nonce = httpServletRequest.getHeader("X-Nonce");

        // 如果任一参数为空，不执行加密。这通常表示请求在拦截器中被阻止。
        if (Util.isAnyBlank(token, timestampStr, nonce)) {
            return body;
        }
        try {
            // 将响应数据转换为JSON字符串
            String dataJson = objectMapper.writeValueAsString(body.getData());
            // 使用 token, timestamp, nonce 生成密钥并加密数据
            String encryptedData = aes256Encrypt(dataJson, token, timestampStr, nonce);
            body.setData(encryptedData);
        } catch (Exception e) {
            log.error("响应数据加密失败", e);
            body.setData(null);
            body.setCode(VETO_EXCEPTION_CODE.MAIN_ERROR_CODE);
        }

        return body;
    }

    /**
     * 根据 token, timestamp 和 nonce 生成密钥并对数据进行加密。
     * @param data      需要加密的明文数据。
     * @param token     用户认证令牌。
     * @param timestamp 当前请求的时间戳。
     * @param nonce     请求的唯一随机数。
     * @return 加密后的密文。
     * @throws Exception 如果加密或哈希算法不支持，或加密过程出错。
     */
    private String aes256Encrypt(String data, String token, String timestamp, String nonce) throws Exception {
        // 1. 获取 token, timestamp, nonce 的 SHA-256 哈希值
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] tokenHash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
        byte[] timestampHash = digest.digest(timestamp.getBytes(StandardCharsets.UTF_8));
        byte[] nonceHash = digest.digest(nonce.getBytes(StandardCharsets.UTF_8));

        // 2. 对三个哈希值进行异或运算（XOR）
        byte[] combinedHash = new byte[32];
        for (int i = 0; i < 32; i++) {
            combinedHash[i] = (byte) (tokenHash[i] ^ timestampHash[i] ^ nonceHash[i]);
        }
        // 3. 将混插后的哈希值作为密钥
        String aesKey = Base64.getEncoder().encodeToString(combinedHash);
        // 4. 使用生成的密钥进行加密
        return AES256Util.encrypt(data, aesKey, generateIV(nonce));
    }

    /**
     * 根据 nonce 生成一个 16 字节的初始化向量 IV，并以 UTF-8 字符串形式返回。
     * * @param nonce 前端传递的唯一随机数
     * @return 16 个字符的 IV 字符串
     * @throws NoSuchAlgorithmException 如果哈希算法不支持
     */
    private static byte[] generateIV(String nonce) throws NoSuchAlgorithmException {
        // 使用 SHA-256 哈希确保 IV 具有高随机性和不可预测性
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] nonceHash = digest.digest(nonce.getBytes(StandardCharsets.UTF_8));

        // IV 必须是 16 字节，所以我们截取哈希值的前 16 个字节
        return Arrays.copyOfRange(nonceHash, 0, 16);
    }
}
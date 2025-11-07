package org.veto.shared;

import org.apache.commons.codec.binary.Base32;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

/**
 * google authenticator
 * @author iMerge
 * @version 1.0
 * @since 1.0
 */
public class TOTPAuthenticator {

    public static String generateSecretKey() {
        // 生成一个 16 字节的随机密钥
        byte[] secretKey = new byte[16]; // 16 字节（128 位）足以满足 Google Authenticator 要求
        for (int i = 0; i < secretKey.length; i++) {
            secretKey[i] = (byte) (Math.random() * 256);  // 生成一个随机字节
        }
        Base32 base32 = new Base32();
        return base32.encodeToString(secretKey);  // 返回 Base32 编码的密钥
    }

    // create a method to generate TOTP
    public static String generateTOTP(String secretKey, long time) throws NoSuchAlgorithmException, InvalidKeyException {
        // Base32 解码密钥
        Base32 base32 = new Base32();
        byte[] decodedKey = base32.decode(secretKey);

        // 计算时间窗口，单位是秒
        long timeWindow = time / 30000;  // 每 30 秒一个时间窗口（30000 毫秒）

        // 将时间窗口转换为字节数组
        byte[] timeBytes = new byte[8];
        for (int i = 7; i >= 0; i--) {
            timeBytes[i] = (byte) (timeWindow & 0xFF);
            timeWindow >>= 8;
        }

        // 使用 HMAC-SHA1 算法生成动态密码
        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec secretKeySpec = new SecretKeySpec(decodedKey, "HmacSHA1");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(timeBytes);

        // 动态截断（Dynamic Truncation），根据 RFC 4226
        int offset = hash[hash.length - 1] & 0xF;
        int otp = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);

        // 生成六位的 TOTP
        otp = otp % 1000000;

        // 返回格式化的六位动态验证码
        return String.format("%06d", otp);
    }

    // 校验方法，验证用户输入的 TOTP 是否有效
    public static boolean validateTOTP(String secretKey, String inputCode, long currentTime) throws NoSuchAlgorithmException, InvalidKeyException {
        // 生成当前时间窗口的 TOTP
        String expectedCode = generateTOTP(secretKey, currentTime);

        // 比较生成的验证码和用户输入的验证码
        return expectedCode.equals(inputCode);
    }
}

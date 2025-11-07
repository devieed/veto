package org.veto.shared;

public class ReferralCodeGenerator {

    // 使用 0-9, a-z, A-Z 共 62 个字符
    private static final String BASE62_CHARS = "qW6Kz4bZ8Vj5UaH1iYxM2s9gXcCk0JtFpNQrS7lEuvOoLd3yBhPDeAIfmGTnwR";

    /**
     * 根据用户的数值型ID，生成一个唯一的推荐码。
     * 推荐码的长度会根据ID的大小动态变化。
     * * @param userId 用户的数值型ID（自增）
     * @return 绝对唯一的推荐码
     */
    public static String generateFromId(long userId) {
        if (userId < 0) {
            throw new IllegalArgumentException("User ID must be non-negative.");
        }
        
        // 如果 ID 是 0，返回特殊值
        if (userId == 0) {
            return "0";
        }
        
        StringBuilder sb = new StringBuilder();
        long num = userId;
        int base = BASE62_CHARS.length();
        
        while (num > 0) {
            // 取余数，获取当前位的字符
            sb.insert(0, BASE62_CHARS.charAt((int) (num % base)));
            // 整除，进入下一位
            num /= base;
        }
        
        // 为了满足至少4位的要求，在前面补零或补其他字符
        // 这里的逻辑可以根据你的业务需求进行调整
        while (sb.length() < 4) {
             sb.insert(0, "0");
        }
        
        return sb.toString();
    }

    public static void main(String[] args) {
        // 假设初始ID是 1-999 之间的任意数值
        for (int i = 655666656; i < 655666656 + 1000; i++) {
            System.out.println("用户ID: " + i + " -> 推荐码: " + generateFromId(i));
        }
    }
}
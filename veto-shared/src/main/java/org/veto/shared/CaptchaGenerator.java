package org.veto.shared;

import jakarta.servlet.http.HttpServletResponse;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;
import javax.imageio.ImageIO;

public class CaptchaGenerator {

    private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Random RANDOM = new Random();

    public static CaptchaResult generateCaptchaImage() throws IOException {
        int captchaLength = 4;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < captchaLength; i++) {
            sb.append(CAPTCHA_CHARS.charAt(RANDOM.nextInt(CAPTCHA_CHARS.length())));
        }
        String captchaText = sb.toString();

        int width = 110;
        int height = 43;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = (Graphics2D) image.getGraphics();

        // 绘制背景
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);

        // 绘制干扰线和点
        g2.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i < 5; i++) {
            g2.drawLine(RANDOM.nextInt(width), RANDOM.nextInt(height),
                        RANDOM.nextInt(width), RANDOM.nextInt(height));
        }
        for (int i = 0; i < 50; i++) {
            image.setRGB(RANDOM.nextInt(width), RANDOM.nextInt(height), RANDOM.nextInt(255) << 16 | RANDOM.nextInt(255) << 8 | RANDOM.nextInt(255));
        }

        // 绘制验证码文字
        g2.setFont(new Font("Arial", Font.BOLD, 28));
        g2.setColor(Color.BLACK);
        int x = 10;
        int y = 30;
        for (int i = 0; i < captchaText.length(); i++) {
            g2.drawString(String.valueOf(captchaText.charAt(i)), x, y);
            x += 25;
        }
        g2.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        
        // 将图片字节数组编码为 Base64 字符串
        String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
        
        return new CaptchaResult(captchaText, base64Image);
    }

    public static class CaptchaResult {
        private final String text;
        private final String base64Image;

        public CaptchaResult(String text, String base64Image) {
            this.text = text;
            this.base64Image = base64Image;
        }

        public String getText() { return text; }
        public String getBase64Image() { return base64Image; }
    }
}
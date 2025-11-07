package org.veto.shared;

import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Util {

    public static boolean isBlank(CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNotBlank(CharSequence cs) {
        return !isBlank(cs);
    }

    public static boolean isAnyBlank(CharSequence... css) {
        if (css == null || css.length == 0) {
            return true;
        }
        for (CharSequence cs : css) {
            if (isBlank(cs)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAllBlank(CharSequence... css) {
        if (css == null) {
            return true;
        }
        for (CharSequence cs : css) {
            if (isNotBlank(cs)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAnyNotBlank(CharSequence... css) {
        return !isAllBlank(css);
    }

    public static boolean isAllNotBlank(CharSequence... css) {
        return !isAnyBlank(css);
    }

    public static Path zipFiles(String outputDir, List<Path> paths) throws IOException {
        String zipFilename = UUID.randomUUID().toString().replace("-", "") + ".zip";

        Path out = Paths.get(outputDir, zipFilename);
        try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(out))) {
            byte[] buffer = new byte[1024];
            for (Path path : paths) {
                if (Files.exists(path)) {
                    try (FileInputStream fis = new FileInputStream(out.toFile())) {
                        ZipEntry zipEntry = new ZipEntry(out.toFile().toString());
                        zipOut.putNextEntry(zipEntry);

                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                            zipOut.write(buffer, 0, length);
                        }
                    }
                }
            }
        }

        return out;
    }

    public static void rateLimitedFileDownloader(Path filePath, HttpServletResponse response, int downloadSpeedBps) throws IOException {
        // 设置响应头
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filePath.getFileName().toString() + "\"");
        response.setHeader("Content-Length", String.valueOf(filePath.toFile().length()));

        // 如果下载速度小于等于 0，则不进行限速
        if (downloadSpeedBps <= 0) {
            try (InputStream in = Files.newInputStream(filePath);
                 OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[8192]; // 8 KB 缓冲区
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            return;
        }

        // 计算每次写入的字节数
        int bufferSize = 8192; // 8 KB 缓冲区
        byte[] buffer = new byte[bufferSize];

        try (InputStream in = Files.newInputStream(filePath);
             OutputStream out = response.getOutputStream()) {

            long startTime = System.currentTimeMillis();
            int bytesRead;
            int totalBytesRead = 0;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                // 控制下载速度
                long elapsedTime = System.currentTimeMillis() - startTime;
                long expectedTime = (totalBytesRead * 1000L) / downloadSpeedBps;
                if (elapsedTime < expectedTime) {
                    try {
                        Thread.sleep(expectedTime - elapsedTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Download interrupted", e);
                    }
                }
            }
        }
    }

    /**
     * 清除base64标准头
     *
     * @param s
     * @return
     */
    public static String replaceBase64Head(String s) {
        return s.replaceAll("^data:\\w+/\\w+;base64,", "");
    }

    /**
     * 生成随机码，基本可以保障ID不同，生成的随机码也不同
     *
     * @param id
     * @return
     */
    public static String serialCode(long id) {

        char[] r = new char[]{'q', 'w', 'e', '8', 'a', 's', '2', 'd', 'z', 'x', '9', 'c', '7', 'p', '5', 'i', 'k', '3', 'm', 'j', 'u', 'f', 'r', '4', 'v', 'y', 'l', 't', 'n', '6', 'b', 'g', 'h'};

        int binLen = r.length;

        int s = 4;

        char b = 'o';

        char[] buf = new char[32];
        int charPos = 32;

        while ((id / binLen) > 0) {
            int ind = (int) (id % binLen);
            // System.out.println(num + "-->" + ind);
            buf[--charPos] = r[ind];
            id /= binLen;
        }
        buf[--charPos] = r[(int) (id % binLen)];
        // System.out.println(num + "-->" + num % binLen);
        String str = new String(buf, charPos, (32 - charPos));
        // 不够长度的自动随机补全
        if (str.length() < s) {
            StringBuilder sb = new StringBuilder();
            sb.append(b);
            Random rnd = new Random();
            for (int i = 1; i < s - str.length(); i++) {
                sb.append(r[rnd.nextInt(binLen)]);
            }
            str += sb.toString();
        }
        return str;
    }

    /**
     * 将给定的字符串的某些部分替换成星号
     * 如果start是-1，它将从0或目标字符串的起始位置开始替换；如果end是-1，它将替换到目标字符串的末尾或整个字符串的末尾。如果isForward是true，则索引从头部开始计算；如果为false，则从尾部开始计算。
     * <p>
     * 请注意，这个方法假设start和end参数相对于目标字符串的位置，如果它们超出了目标字符串的实际长度，它们将被自动调整到合适的范围内。如果start或end为负数，它们将被当作相对于字符串头部或尾部的偏移量。
     *
     * @param input
     * @param start
     * @param end
     * @param reverse
     * @return
     */

    public static String maskString(String input, Integer start, Integer end, boolean reverse) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        int length = input.length();

        // If reverse is true, convert the start and end to count from the end of the string
        if (reverse) {
            start = (start == null) ? null : length - start - 1;
            end = (end == null) ? null : length - end;
        }

        // Handle cases where start or end are not provided
        if (start == null || start < 0) {
            start = 0;
        }
        if (end == null || end == -1 || end > length) {
            end = length;
        }

        // Ensure start and end are within valid bounds
        start = Math.max(0, Math.min(length, start));
        end = Math.max(start, Math.min(length, end));

        // Build the masked string
        StringBuilder maskedString = new StringBuilder(input);
        for (int i = start; i < end; i++) {
            maskedString.setCharAt(i, '*');
        }

        return maskedString.toString();
    }
}

package org.veto.shared;

import java.util.Arrays;

/**
 * Base58 简单实现
 */
public class Base58 {
    private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final char[] ALPHABET_ARRAY = ALPHABET.toCharArray();
    private static final int BASE_58 = ALPHABET.length();

    public static String encode(byte[] input) {
        if (input.length == 0) {
            return "";
        }

        // Count leading zeros.
        int zeros = 0;
        while (zeros < input.length && input[zeros] == 0) {
            ++zeros;
        }

        // Convert base-256 digits to base-58 digits (plus conversion to ASCII characters)
        input = Arrays.copyOf(input, input.length); // since we modify it in-place
        char[] encoded = new char[input.length * 2]; // upper bound
        int outputStart = encoded.length;
        int inputStart = zeros;
        while (inputStart < input.length) {
            int mod = divmod58(input, inputStart);
            if (input[inputStart] == 0) {
                ++inputStart;
            }
            encoded[--outputStart] = ALPHABET_ARRAY[mod];
        }

        // Preserve exactly as many leading '1's in the output as there were leading zeros in the input.
        while (outputStart < encoded.length && encoded[outputStart] == ALPHABET_ARRAY[0]) {
            ++outputStart;
        }
        while (--zeros >= 0) {
            encoded[--outputStart] = ALPHABET_ARRAY[0];
        }

        // Return encoded string (including encoded leading zeros).
        return new String(encoded, outputStart, encoded.length - outputStart);
    }

    private static int divmod58(byte[] number, int startAt) {
        int remainder = 0;
        for (int i = startAt; i < number.length; i++) {
            int digit256 = (int) number[i] & 0xFF;
            int temp = remainder * 256 + digit256;
            number[i] = (byte) (temp / BASE_58);
            remainder = temp % BASE_58;
        }
        return remainder;
    }
}
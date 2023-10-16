package com.netease.im.rtctoken;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class RestApiAuth {
    public static String getChecksum(String appSecret, String nonce, long curTime) {
        return sha1(appSecret + nonce + curTime);
    }
    private static String sha1(String input) {
        try {
            MessageDigest mDigest = MessageDigest.getInstance("SHA-1");
            byte[] result = mDigest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : result) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}

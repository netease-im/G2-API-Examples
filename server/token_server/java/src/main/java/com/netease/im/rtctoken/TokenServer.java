package com.netease.im.rtctoken;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class TokenServer {
    private final String appKey;
    private final String appSecret;
    private final int defaultTTLSec;

    public TokenServer(String appKey, String appSecret, int defaultTTLSec) {
        if (appKey == null || appSecret == null || appKey.isEmpty() || appSecret.isEmpty()) {
            throw new IllegalArgumentException("appKey or appSecret is empty");
        }
        if (defaultTTLSec <= 0) {
            throw new IllegalArgumentException("defaultTTLSec must be positive");
        }
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.defaultTTLSec = defaultTTLSec;
    }

    /**
     * Generate a token with the given channel name, user ID, and time-to-live (TTL) in seconds.
     *
     * @param channelName: the name of the channel for which the token is being generated
     * @param uid: the user ID associated with the token
     * @param ttlSec: the time-to-live (TTL) for the token in seconds, should be less than 86400 seconds (1 day)
     * @return: the token
     */
    public String getToken(String channelName, long uid, int ttlSec) throws Exception {
        long curTimeMs = System.currentTimeMillis();
        return getTokenWithCurrentTime(channelName, uid, ttlSec, curTimeMs);
    }

    public String getTokenWithCurrentTime(String channelName, long uid, int ttlSec, long curTimeMs) throws Exception {
        if (ttlSec <= 0) {
            ttlSec = defaultTTLSec;
        }
        DynamicToken tokenModel = new DynamicToken();
        tokenModel.signature = sha1(String.format("%s%d%d%d%s%s", appKey, uid, curTimeMs, ttlSec, channelName, appSecret));
        tokenModel.curTime = curTimeMs;
        tokenModel.ttl = ttlSec;
        ObjectMapper objectMapper = new ObjectMapper();
        String signature = objectMapper.writeValueAsString(tokenModel);
        return Base64.getEncoder().encodeToString(signature.getBytes(StandardCharsets.UTF_8));
    }

    private String sha1(String input) throws NoSuchAlgorithmException {
        MessageDigest mDigest = MessageDigest.getInstance("SHA-1");
        byte[] result = mDigest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : result) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static class DynamicToken {
        public String signature;
        public long curTime;
        public int ttl;
    }
}

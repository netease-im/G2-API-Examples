package com.netease.im.rtctoken;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.zip.Deflater;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;

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
     * @param uid:         the user ID associated with the token
     * @param ttlSec:      the time-to-live (TTL) for the token in seconds, should be less than 86400 seconds (1 day)
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


    /**
     * generates a permission key for the given channelName, uid and privilege.
     *
     * @param channelName: the name of the channel for which the token is being generated
     * @param permSecret:  the secret key for generating the permission key
     * @param uid:         the user ID associated with the token
     * @param privilege:   the privilege of the user. privilege is a 8-bit number, each bit represents a permission:
     *                     - bit 1: (0000 0001) = 1, permission to send audio stream
     *                     - bit 2: (0000 0010) = 2, permission to send video stream
     *                     - bit 3: (0000 0100) = 4, permission to receive audio stream
     *                     - bit 4: (0000 1000) = 8, permission to receive video stream
     *                     - bit 5: (0001 0000) = 16, permission to create room
     *                     - bit 6: (0010 0000) = 32, permission to join room
     *                     So, (0010 1100) = 32+8+4 = 44 means permission to receive audio&video stream and join room.
     *                     (0011 1111) = 63 means all permission allowed.
     * @param ttlSec:      the time-to-live (TTL) for the token in seconds, should be less than 86400 seconds (1 day)
     * @return the permission key
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws JsonProcessingException
     */
    public String getPermissionKey(String channelName, String permSecret, long uid, byte privilege, long ttlSec)
            throws NoSuchAlgorithmException, InvalidKeyException, JsonProcessingException {
        long curTime = System.currentTimeMillis() / 1000;
        return getPermissionKeyWithCurrentTime(channelName, permSecret, uid, privilege, ttlSec, curTime);
    }

    public String getPermissionKeyWithCurrentTime(String channelName, String permSecret, long uid, byte privilege,
                                                  long ttlSec, long curTime) throws NoSuchAlgorithmException, InvalidKeyException, JsonProcessingException {
        PermissionKey permKey = new PermissionKey();
        permKey.appkey = appKey;
        permKey.uid = uid;
        permKey.cname = channelName;
        permKey.privilege = privilege;
        permKey.expireTime = ttlSec;
        permKey.curTime = curTime;

        // Calculate the signature
        String checksum = hmacsha256(appKey, String.valueOf(uid), String.valueOf(curTime), String.valueOf(ttlSec),
                channelName, permSecret, String.valueOf(privilege));
        permKey.checksum = checksum;
        // Convert the map to JSON format
        ObjectMapper mapper = new ObjectMapper();
        String jsonStr = mapper.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).writeValueAsString(permKey);

        // Compress the data
        byte[] compressedData = compress(jsonStr.getBytes(StandardCharsets.UTF_8));

        // Encode the compressed data and return
        return new String(base64EncodeUrl(compressedData));
    }

    private static byte[] compress(byte[] data) {
        try {
            Deflater deflater = new Deflater(6);
            deflater.setInput(data);
            deflater.finish();

            byte[] buffer = new byte[2048];
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            outputStream.close();

            byte[] compressedData = outputStream.toByteArray();
            deflater.end();
            return compressedData;
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String hmacsha256(String appidStr, String uidStr, String curTimeStr, String expireTimeStr,
                                     String cname, String permSecret, String privilegeStr)
            throws NoSuchAlgorithmException, InvalidKeyException {
        String contentToBeSigned = "appkey:" + appidStr + "\n";
        contentToBeSigned += "uid:" + uidStr + "\n";
        contentToBeSigned += "curTime:" + curTimeStr + "\n";
        contentToBeSigned += "expireTime:" + expireTimeStr + "\n";
        contentToBeSigned += "cname:" + cname + "\n";
        contentToBeSigned += "privilege:" + privilegeStr + "\n";

        SecretKeySpec keySpec = new SecretKeySpec(permSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(keySpec);
        byte[] result = mac.doFinal(contentToBeSigned.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(result);
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

    public static byte[] base64EncodeUrl(byte[] input) {
        byte[] base64 = Base64.getEncoder().encode(input);
        for (int i = 0; i < base64.length; ++i)
            switch (base64[i]) {
                case '+':
                    base64[i] = '*';
                    break;
                case '/':
                    base64[i] = '-';
                    break;
                case '=':
                    base64[i] = '_';
                    break;
                default:
                    break;
            }
        return base64;
    }

    public static class DynamicToken {
        public String signature;
        public long curTime;
        public int ttl;
    }

    public static class PermissionKey {
        public String appkey;
        public long uid;
        public String cname;
        public byte privilege;
        public long expireTime;
        public long curTime;
        public String checksum;
    }
}

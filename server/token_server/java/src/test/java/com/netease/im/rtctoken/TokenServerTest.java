package com.netease.im.rtctoken;

import org.junit.Test;

import static org.junit.Assert.*;

public class TokenServerTest {
    @Test
    public void testGetTokenWithCurrentTime() throws Exception {
        String appKey =  "c37bf7a8758a4ed00000000000000000";
        String appSecret = "c00000000000";
        long curTimeMs = 1693968975000L;
        TokenServer tokenServer = new TokenServer(appKey, appSecret, 3600);
        assertEquals("eyJzaWduYXR1cmUiOiJlZjRmNGEwOGM1NmZiOWI5MDQ3OTE2YjZlYmZhZGY5NWFjZDc2OGViIiwiY3VyVGltZSI6MTY5Mzk2ODk3NTAwMCwidHRsIjoxODAwfQ==",
                tokenServer.getTokenWithCurrentTime("room1", 10000, 1800, curTimeMs));
        assertEquals("eyJzaWduYXR1cmUiOiJkMjZmYzFlZjk4ZWExNmM3YTkzOWFmMDZmOGE4MTk2MTJkY2QzZDU5IiwiY3VyVGltZSI6MTY5Mzk2ODk3NTAwMCwidHRsIjozNjAwfQ==",
                tokenServer.getTokenWithCurrentTime("room1", 10000, 0, curTimeMs));
    }


    @Test
    public void getPermissionKeyWithCurrentTime() throws Exception {
        String appKey =  "c37bf7a8758a4ed00000000000000000";
        String appSecret = "c00000000000";
        long curTime = 1696662104L;
        byte privilege = (byte) (1);
        long ttlSec = 1000;
        TokenServer tokenServer = new TokenServer(appKey, appSecret, 3600);
        assertEquals("eJxdjcEKgkAYhN-lP3vQ1F0LulQQgRCBlHlb1z-dbNt1Y0WL3j2XOjW3*WaGeQHTusURFsBDWl4oS2icsAgr-1-gAW*Qtw8rp3YfXYdzso6fharSUapVR7ddybNyU1tMm2O*O*0Pecjagi-d8s4kTjOjlAyctyYTjgRkTgiZBX7kAQ5aGPzx6dEDbUQvblg74IEV1Tfw3x-fBDcB",
                tokenServer.getPermissionKeyWithCurrentTime("room1", "45eaeb3c2757c57c1b8e0a25a1f246a476c36ca5ba0cd20da38a154c2adebdab", 10000, privilege, ttlSec, curTime));
    }
}

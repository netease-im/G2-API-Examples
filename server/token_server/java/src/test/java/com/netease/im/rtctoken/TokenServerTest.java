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
}

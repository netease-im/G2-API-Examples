package com.netease.im.rtctoken;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RestApiAuthTest {
    @Test
    public void testChecksum() throws Exception {
       assertEquals("192bdbdad337836e6213aec1d93186aae9771c39", RestApiAuth.getChecksum("c00000000000", "1234567890", 1697168455));
    }
}
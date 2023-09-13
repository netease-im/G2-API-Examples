const test = require('node:test');
const assert = require('node:assert');


const { GetToken, GetTokenWithCurrentTime } = require('../index.js');

var appKey = "c37bf7a8758a4ed00000000000000000";
var appSecret = "c00000000000";
var uid = 1;


test('GetToken should throw error on empty appKey or appSecret', (t) => {
    assert.throws(() => {
        GetToken("", appSecret, "channelName", uid, 1800);
    }, { message: "appKey or appSecret is empty" });
});

test('GetToken should throw error on negative defaultTTLSec', (t) => {
    assert.throws(() => {
        GetToken(appKey, appSecret, "channelName", uid, -1);
    }, {
        message: "ttlSec must be positive",
    });
});

test('GetTokenWithCurrentTime should generate valid token', (t) => {
    const curTimeMs = 1693968975000;
    const token = GetTokenWithCurrentTime(appKey, appSecret, "room1", 10000, 1800, curTimeMs);
    assert.equal(token, "eyJzaWduYXR1cmUiOiJlZjRmNGEwOGM1NmZiOWI5MDQ3OTE2YjZlYmZhZGY5NWFjZDc2OGViIiwiY3VyVGltZSI6MTY5Mzk2ODk3NTAwMCwidHRsIjoxODAwfQ==");
});

test('GetTokenWithCurrentTime should throw error on negative ttlSec', (t) => {
    const curTimeMs = Date.now();
    assert.throws(() => {
        GetTokenWithCurrentTime(appKey, appSecret, "channelName", 10000, -1, curTimeMs);
    }, { message: "ttlSec must be positive" });
});

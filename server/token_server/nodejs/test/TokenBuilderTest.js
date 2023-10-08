const test = require('node:test');
const assert = require('node:assert');


const { GetToken, GetTokenWithCurrentTime, GetPermissionKeyWithCurrentTime } = require('../index.js');

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

test('GetPermissionKeyWithCurrentTime', (t) => {
    const curTimeSec = 1696662104;
    var ttlSec = 1000;
    var privilege  = 1;
    var permissionSecret = "45eaeb3c2757c57c1b8e0a25a1f246a476c36ca5ba0cd20da38a154c2adebdab";
    const permKey = GetPermissionKeyWithCurrentTime(appKey, permissionSecret, "room1", 10000, privilege, ttlSec, curTimeSec);
    assert.equal(permKey, "eJxdjcEKgkAYhN-lP3vQ1F0LulQQgRCBlHlb1z-dbNt1Y0WL3j2XOjW3*WaGeQHTusURFsBDWl4oS2icsAgr-1-gAW*Qtw8rp3YfXYdzso6fharSUapVR7ddybNyU1tMm2O*O*0Pecjagi-d8s4kTjOjlAyctyYTjgRkTgiZBX7kAQ5aGPzx6dEDbUQvblg74IEV1Tfw3x-fBDcB");
})

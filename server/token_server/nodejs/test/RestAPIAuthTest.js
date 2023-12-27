const test = require('node:test');
const assert = require('node:assert');


const { GetChecksum } = require('../src/RestApiAuth.js');

test('GetChecksum', (t) => {
    var appSecret = "c00000000000";
    var nonce = "1234567890";
    var curTime = 1697168455;
    assert.equal("192bdbdad337836e6213aec1d93186aae9771c39", GetChecksum(appSecret, nonce, curTime));
});
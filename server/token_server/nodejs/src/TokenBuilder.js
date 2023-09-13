const crypto = require('crypto');

/**
 * Generate a token with the given channel name, user ID, and time-to-live (TTL) in seconds.
 * @param {string} appKey: the app ID of your account in Netease Yunxin
 * @param {string} appSecret: the app secret of your account in Netease Yunxin
 * @param {string} channelName: the name of the channel for which the token is being generated
 * @param {number} uid: the user ID associated with the token
 * @param {number} ttlSec: the time-to-live (TTL) for the token in seconds, should be less than 86400 seconds (1 day)
 * @return: the token
 */
var GetToken = function (appKey, appSecret, channelName, uid, ttlSec) {
    if (!appKey || !appSecret) {
        throw new Error("appKey or appSecret is empty");
    }
    if (ttlSec <= 0) {
        throw new Error("ttlSec must be positive");
    }
    const curTimeMs = Date.now();
    return GetTokenWithCurrentTime(appKey, appSecret, channelName, uid, ttlSec, curTimeMs);
}

var GetTokenWithCurrentTime = function (appKey, appSecret, channelName, uid, ttlSec, curTimeMs) {
    if (ttlSec <= 0) {
        throw new Error("ttlSec must be positive");
    }
    console.log(`${appKey}${uid}${curTimeMs}${ttlSec}${channelName}${appSecret}`);
    var token = {
        "signature": sha1(`${appKey}${uid}${curTimeMs}${ttlSec}${channelName}${appSecret}`),
        "curTime": curTimeMs,
        "ttl": ttlSec
    };
    const signature = JSON.stringify(token);
    console.log(signature);
    return Buffer.from(signature).toString('base64');
}


var sha1 = function (input) {
    const sha1 = crypto.createHash('sha1');
    sha1.update(input, 'utf8');
    return sha1.digest('hex');
}


module.exports = {
    GetToken: GetToken,
    GetTokenWithCurrentTime: GetTokenWithCurrentTime,
}

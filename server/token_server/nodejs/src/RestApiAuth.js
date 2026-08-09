const crypto = require('crypto');

var GetChecksum = function (appSecret, nonce, curTime) {
    return sha1(`${appSecret}${nonce}${curTime}`);
}

const sha1 = function (input) {
    const sha1 = crypto.createHash('sha1');
    sha1.update(input, 'utf8');
    return sha1.digest('hex');
}

module.exports = {
    GetChecksum: GetChecksum,
}
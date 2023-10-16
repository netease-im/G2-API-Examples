<?php
class TokenServer
{
    private $appKey;
    private $appSecret;
    private $defaultTTLSec;

    /**
     * TokenServer constructor.
     * @param string $appKey, the app ID of your account in Netease Yunxin
     * @param string $appSecret, the app secret of your account in Netease Yunxin
     * @param int $defaultTTLSec, should be less than 86400 seconds (1 day)
     * @throws Exception
     */
    public function __construct(string $appKey, string $appSecret, int $defaultTTLSec)
    {
        if (empty($appKey) || empty($appSecret)) {
            throw new InvalidArgumentException("appKey or appSecret is empty");
        }
        if ($defaultTTLSec <= 0) {
            throw new InvalidArgumentException("defaultTTLSec must be positive");
        }
        $this->appKey = $appKey;
        $this->appSecret = $appSecret;
        $this->defaultTTLSec = $defaultTTLSec;
    }

    /**
     * Generate a token with the given channel name, user ID, and time-to-live (TTL) in seconds.
     *
     * @param string $channelName, the name of the channel for which the token is being generated
     * @param int $uid,  the user ID associated with the token
     * @param int $ttlSec, should be less than 86400 seconds (1 day)
     * @return string
     */
    public function getToken(string $channelName, int $uid, int $ttlSec): string
    {
        $curTimeMs = round(microtime(true) * 1000);
        return $this->getTokenWithCurrentTime($channelName, $uid, $ttlSec, $curTimeMs);
    }

    public function getTokenWithCurrentTime(string $channelName, int $uid, int $ttlSec, int $curTimeMs): string
    {
        if ($ttlSec <= 0) {
            $ttlSec = $this->defaultTTLSec;
        }
        $tokenModel = new DynamicToken();
        $tokenModel->signature = sha1(sprintf("%s%d%d%d%s%s", $this->appKey, $uid, $curTimeMs, $ttlSec, $channelName, $this->appSecret));
        $tokenModel->curTime = $curTimeMs;
        $tokenModel->ttl = $ttlSec;
        $signature = json_encode($tokenModel);
        return base64_encode($signature);
    }

    /**
     * Generates a permission key for the given channelName, uid and privilege.
     * @param string channelName: the name of the channel for which the token is being generated
     * @param string permSecret:  the secret key for generating the permission key
     * @param int uid:         the user ID associated with the token
     * @param int privilege:   the privilege of the user. privilege is a 8-bit number, each bit represents a permission:
     *                     - bit 1: (0000 0001) = 1, permission to send audio stream
     *                     - bit 2: (0000 0010) = 2, permission to send video stream
     *                     - bit 3: (0000 0100) = 4, permission to receive audio stream
     *                     - bit 4: (0000 1000) = 8, permission to receive video stream
     *                     - bit 5: (0001 0000) = 16, permission to create room
     *                     - bit 6: (0010 0000) = 32, permission to join room
     *                     So, (0010 1100) = 32+8+4 = 44 means permission to receive audio&video stream and join room.
     *                     (0011 1111) = 63 means all permission allowed.
     * @param int ttlSec:      the time-to-live (TTL) for the token in seconds, should be less than 86400 seconds (1 day)
     * @return string: the generated permission key
     */
    function getPermissionKey($channelName, $permSecret, $uid, $privilege, $ttlSec)
    {
        $curTime = time();
        return $this->getPermissionKeyWithCurrentTime($channelName, $permSecret, $uid, $privilege, $ttlSec, $curTime);
    }

    function getPermissionKeyWithCurrentTime($channelName, $permSecret, $uid, $privilege, $ttlSec, $curTime)
    {
        $permKey = new PermissionKey();
        $permKey->appkey = $this->appKey;
        $permKey->uid = $uid;
        $permKey->cname = $channelName;
        $permKey->privilege = $privilege;
        $permKey->expireTime = $ttlSec;
        $permKey->curTime = $curTime;

        $checksum = $this->hmacsha256($this->appKey, (string) $uid, (string) $curTime, (string) $ttlSec, $channelName, $permSecret, (string) $privilege);
        $permKey->checksum = $checksum;

        $jsonStr = json_encode($permKey);
        $compressedData = gzcompress($jsonStr);

        return $this->base64EncodeUrl($compressedData);
    }

    function hmacsha256($appIdStr, $uidStr, $curTimeStr, $expireTimeStr, $cname, $permSecret, $privilegeStr)
    {
        $contentToBeSigned = "appkey:" . $appIdStr . "\n";
        $contentToBeSigned .= "uid:" . $uidStr . "\n";
        $contentToBeSigned .= "curTime:" . $curTimeStr . "\n";
        $contentToBeSigned .= "expireTime:" . $expireTimeStr . "\n";
        $contentToBeSigned .= "cname:" . $cname . "\n";
        $contentToBeSigned .= "privilege:" . $privilegeStr . "\n";

        return base64_encode(hash_hmac('sha256', $contentToBeSigned, $permSecret, true));
    }

    // Base64 encoding function
    function base64EncodeUrl($input)
    {
        $base64 = base64_encode($input);
        return str_replace(array('+', '/', '='), array('*', '-', '_'), $base64);
    }
}

class DynamicToken
{
    public $signature;
    public $curTime;
    public $ttl;
}

class PermissionKey
{
    public $appkey;
    public $checksum;
    public $cname;
    public $curTime;
    public $expireTime;
    public $privilege;
    public $uid;
}

?>
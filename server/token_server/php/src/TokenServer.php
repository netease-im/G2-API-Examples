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

    /**
     * @param string $channelName
     * @param int $uid
     * @param int $ttlSec
     * @param int $ttlSec, should be less than 86400 seconds (1 day)
     * @return string
     */
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

}

 class DynamicToken {
    public $signature;
    public $curTime;
    public $ttl;
}

?>
<?php
function getChecksum(string $appSecret, string $nonce, int $curtime):string {
    return sha1($appSecret . $nonce . $curtime);
}
?>
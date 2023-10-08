<?php

require_once "../src/TokenServer.php";

class TokenServerTest
{
    public $appId = "c37bf7a8758a4ed00000000000000000";
    public $appSecret = "c00000000000";

    public function run()
    {
        $this->testGetTokenWithCurrentTime();
        $this->testGetPermissionKey();
    }

    public function testGetTokenWithCurrentTime()
    {
        $tokenServer = new TokenServer($this->appId, $this->appSecret, 3600);
        $this->assertEqual(
            "eyJzaWduYXR1cmUiOiJlZjRmNGEwOGM1NmZiOWI5MDQ3OTE2YjZlYmZhZGY5NWFjZDc2OGViIiwiY3VyVGltZSI6MTY5Mzk2ODk3NTAwMCwidHRsIjoxODAwfQ==",
            $tokenServer->getTokenWithCurrentTime("room1", 10000, 1800, 1693968975000)
        );

        $this->assertEqual(
            "eyJzaWduYXR1cmUiOiJkMjZmYzFlZjk4ZWExNmM3YTkzOWFmMDZmOGE4MTk2MTJkY2QzZDU5IiwiY3VyVGltZSI6MTY5Mzk2ODk3NTAwMCwidHRsIjozNjAwfQ==",
            $tokenServer->getTokenWithCurrentTime("room1", 10000, 0, 1693968975000)
        );
    }

    public function testGetPermissionKey()
    {
        $tokenServer = new TokenServer($this->appId, $this->appSecret, 3600);
        $privilege = 1;
        $ttlSec = 1000;
        echo $tokenServer->getPermissionKey("room1", "45eaeb3c2757c57c1b8e0a25a1f246a476c36ca5ba0cd20da38a154c2adebdab", 10000, $privilege, $ttlSec);
        $this->assertEqual(
            "eJxdjcEKgkAYhN-lP3vQ1F0LulQQgRCBlHlb1z-dbNt1Y0WL3j2XOjW3*WaGeQHTusURFsBDWl4oS2icsAgr-1-gAW*Qtw8rp3YfXYdzso6fharSUapVR7ddybNyU1tMm2O*O*0Pecjagi-d8s4kTjOjlAyctyYTjgRkTgiZBX7kAQ5aGPzx6dEDbUQvblg74IEV1Tfw3x-fBDcB",
            $tokenServer->getPermissionKeyWithCurrentTime("room1", "45eaeb3c2757c57c1b8e0a25a1f246a476c36ca5ba0cd20da38a154c2adebdab", 10000, 1, 1000, 1696662104)
        );
    }

    public static function assertEqual($expected, $actual)
    {
        if ($expected != $actual) {
            echo "Assert failed" . "\n    Expected :" . $expected . "\n    Actual   :" . $actual;
        } else {
            echo "Assert ok\n";
        }
    }
}

$tokenServerTest = new TokenServerTest();
$tokenServerTest->run();

<?php

require_once "../src/TokenServer.php";

class TokenServerTest
{
    public $appId = "c37bf7a8758a4ed00000000000000000";
    public $appSecret = "c00000000000";

    public function run()
    {
        $this->testGetTokenWithCurrentTime();
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

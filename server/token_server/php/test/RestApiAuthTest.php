<?php

require_once "../src/RestApiAuth.php";

class ChecksumTest
{
    public $appSecret = "c00000000000";

    public function run()
    {
        $this->testChecksum();
    }

    public function testChecksum()
    {
        $this->assertEqual(
            "192bdbdad337836e6213aec1d93186aae9771c39",
            getChecksum($this->appSecret, "1234567890", 1697168455)
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

$checksumTest = new ChecksumTest();
$checksumTest->run();

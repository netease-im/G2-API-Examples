#include "../src/token_builder.h"
#include <gtest/gtest.h>
#include <string>
#include <stdint.h>

class TokenServer_test : public testing::Test
{
protected:
  virtual void SetUp() override {}

  virtual void TearDown() {}

  void TestGetToken()
  {
    TokenServer tokenServer("c37bf7a8758a4ed00000000000000000", "c00000000000", 3600);
    std::string token = tokenServer.getBasicTokenWithCurrentTime("room1", 10000, 1800, 1693968975000);
    std::cout << "Token: " << token << std::endl;
    EXPECT_EQ(token, "eyJzaWduYXR1cmUiOiJlZjRmNGEwOGM1NmZiOWI5MDQ3OTE2YjZlYmZhZGY5NWFjZDc2OGViIiwiY3VyVGltZSI6MTY5Mzk2ODk3NTAwMCwidHRsIjoxODAwfQ==");

    token = tokenServer.getBasicToken("room1", 10000, 1800);
    std::cout << "Token: " << token << std::endl;
  }

  void TestGetPermKey()
  {
    TokenServer tokenServer("c37bf7a8758a4ed00000000000000000", "c00000000000", 3600);
    std::string token = tokenServer.getPermissionKeyWithCurrentTime("room1", "45eaeb3c2757c57c1b8e0a25a1f246a476c36ca5ba0cd20da38a154c2adebdab", 10000, 1, 1000, 1696662104);
    std::cout << "PermKey: " << token << std::endl;
    EXPECT_EQ(token, "eNpdjcEKgkAYhN-lP3vQ1F0LulQQgRCBlHlb1z-dbNt1Y0WL3j2XOjW3*WaGeQHTusURFsBDWl4oS2icsAgr-1-gAW*Qtw8rp3YfXYdzso6fharSUapVR7ddybNyU1tMm2O*O*0Pecjagi-d8s4kTjOjlAyctyYTjgRkTgiZBX7kAQ5aGPzx6dEDbUQvblg74IEV1Tfw3x-fBDcB");

    token = tokenServer.getPermissionKey("room1", "45eaeb3c2757c57c1b8e0a25a1f246a476c36ca5ba0cd20da38a154c2adebdab", 10000, 1, 1000);
    std::cout << "PermKey2: " << token << std::endl;
  }
};

TEST_F(TokenServer_test, testAccessTokenWithIntUid)
{
  TestGetToken();
  TestGetPermKey();
}

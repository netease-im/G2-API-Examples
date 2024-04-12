#include "../src/rest_api_auth.h"
#include <gtest/gtest.h>
#include <string>
#include <stdint.h>

class RestApiAuth_test : public testing::Test
{
protected:
  virtual void SetUp() override {}

  virtual void TearDown() {}

  void TestGetChecksum()
  {
    std::string token = getChecksum("c00000000000", "1234567890", 1697168455);
    std::cout << "checksum: " << token << std::endl;
    EXPECT_EQ(token, "192bdbdad337836e6213aec1d93186aae9771c39");
  }
};

TEST_F(RestApiAuth_test, RestApiAuth_test)
{
  TestGetChecksum();
}

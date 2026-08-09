#include <iostream>
#include <iomanip>
#include <string>
#include <sstream>
#include <openssl/sha.h>


std::string sha1(const std::string &input)
{
    unsigned char hash[SHA_DIGEST_LENGTH];
    SHA1(reinterpret_cast<const unsigned char *>(input.c_str()), input.length(), hash);
    std::ostringstream oss;
    oss << std::hex << std::setfill('0');
    for (auto c : hash)
    {
        oss << std::setw(2) << static_cast<int>(c);
    }
    return oss.str();
}

std::string getChecksum(const std::string appSecret, std::string nonce, long curtime)
{
    return sha1(appSecret + nonce + std::to_string(curtime));
}

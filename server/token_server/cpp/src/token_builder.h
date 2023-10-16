#include <iostream>
#include <string>
#include <sstream>
#include <iomanip>
#include <chrono>
#include <stdexcept>
#include <openssl/buffer.h>
#include "../thirdparty/rapidjson/include/rapidjson/document.h"
#include "../thirdparty/rapidjson/include/rapidjson/pointer.h"
#include "../thirdparty/rapidjson/include/rapidjson/stringbuffer.h"
#include "../thirdparty/rapidjson/include/rapidjson/writer.h"
#include <cstring>
#include <algorithm>
#include <openssl/sha.h>
#include <openssl/bio.h>
#include <openssl/evp.h>
#include <openssl/hmac.h>
#include "zlib.h"

class TokenServer
{
public:
    TokenServer(const std::string &appKey, const std::string &appSecret, int defaultTTLSec) : appKey(appKey), appSecret(appSecret), defaultTTLSec(defaultTTLSec)
    {
        if (appKey.empty() || appSecret.empty())
        {
            throw std::invalid_argument("appKey or appSecret is empty");
        }
        if (defaultTTLSec <= 0)
        {
            throw std::invalid_argument("defaultTTLSec must be positive");
        }
    }

    /**
     * Generate a token with the given channel name, user ID, and time-to-live (TTL) in seconds.
     *
     * @param channelName: the name of the channel for which the token is being generated
     * @param uid: the user ID associated with the token
     * @param ttlSec: the time-to-live (TTL) for the token in seconds
     * @return: the token
     */
    std::string getBasicToken(const std::string &channelName, long uid, int ttlSec)
    {
        long long curTimeMs = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
        return getBasicTokenWithCurrentTime(channelName, uid, ttlSec, curTimeMs);
    }

    std::string getBasicTokenWithCurrentTime(const std::string &channelName, long uid, int ttlSec, long long curTimeMs)
    {
        if (ttlSec <= 0)
        {
            ttlSec = defaultTTLSec;
        }
        DynamicToken tokenModel;
        tokenModel.signature = sha1(appKey + std::to_string(uid) + std::to_string(curTimeMs) + std::to_string(ttlSec) + channelName + appSecret);
        tokenModel.curTime = curTimeMs;
        tokenModel.ttl = ttlSec;
        
        rapidjson::Document sig_doc;
        sig_doc.SetObject();
        rapidjson::Document::AllocatorType &allocator = sig_doc.GetAllocator();
        sig_doc.AddMember("signature", rapidjson::Value(tokenModel.signature.c_str(), allocator), sig_doc.GetAllocator());
        sig_doc.AddMember("curTime", tokenModel.curTime, sig_doc.GetAllocator());
        sig_doc.AddMember("ttl", tokenModel.ttl, sig_doc.GetAllocator());
        rapidjson::StringBuffer s;
        rapidjson::Writer<rapidjson::StringBuffer> w(s);
        sig_doc.Accept(w);
        return base64Encode(s.GetString());
    }

    /**
     * generates a permission key for the given channelName, uid and privilege.
     * @param channelName: the name of the channel for which the token is being generated
     * @param permSecret: the user ID associated with the token
     * @param uid: the user ID associated with the token
     * @param privilege: the privilege of the user. privilege is a 8-bit number, each bit represents a permission:
     *                     - bit 1: (0000 0001) = 1, permission to send audio stream
     *                     - bit 2: (0000 0010) = 2, permission to send video stream
     *                     - bit 3: (0000 0100) = 4, permission to receive audio stream
     *                     - bit 4: (0000 1000) = 8, permission to receive video stream
     *                     - bit 5: (0001 0000) = 16, permission to create room
     *                     - bit 6: (0010 0000) = 32, permission to join room
     *                     So, (0010 1100) = 32+8+4 = 44 means permission to receive audio&video stream and join room.
     *                     (0011 1111) = 63 means all permission allowed.
     * @param ttlSec: the time-to-live (TTL) for the token in seconds
     *
     */
    std::string getPermissionKey(const std::string channelName, const std::string permSecret, long uid, uint8_t privilege, long ttlSec)
    {
        long curTime = std::chrono::system_clock::to_time_t(std::chrono::system_clock::now());
        return getPermissionKeyWithCurrentTime(channelName, permSecret, uid, privilege, ttlSec, curTime);
    }

    std::string getPermissionKeyWithCurrentTime(const std::string channelName, const std::string permSecret, long uid, uint8_t privilege, long ttlSec, long curTime)
    {
        PermissionKey permKey;
        permKey.appkey = appKey;
        permKey.uid = uid;
        permKey.cname = channelName;
        permKey.privilege = privilege;
        permKey.expireTime = ttlSec;
        permKey.curTime = curTime;

        // Calculate the signature
        std::string checksum = hmacsha256(appKey, std::to_string(uid), std::to_string(curTime), std::to_string(ttlSec), channelName, permSecret, std::to_string(privilege));
        permKey.checksum = checksum;
        // construct json
        rapidjson::Document sig_doc;
        sig_doc.SetObject();
        rapidjson::Document::AllocatorType &allocator = sig_doc.GetAllocator();

        sig_doc.AddMember("appkey", rapidjson::Value(appKey.c_str(), allocator), sig_doc.GetAllocator());
        sig_doc.AddMember("checksum", rapidjson::Value(checksum.c_str(), allocator), sig_doc.GetAllocator());
        sig_doc.AddMember("cname", rapidjson::Value(channelName.c_str(), allocator), sig_doc.GetAllocator());
        sig_doc.AddMember("curTime", int(curTime), sig_doc.GetAllocator());
        sig_doc.AddMember("expireTime", int(ttlSec), sig_doc.GetAllocator());
        sig_doc.AddMember("privilege", int(privilege), sig_doc.GetAllocator());
        sig_doc.AddMember("uid", uint64_t(uid), sig_doc.GetAllocator());
        rapidjson::StringBuffer s;
        rapidjson::Writer<rapidjson::StringBuffer> w(s);
        sig_doc.Accept(w);
        std::vector<uint8_t> compressedData = compress(s.GetString());

        // Encode the compressed data and return
        return base64EncodeUrl(compressedData);
    }

private:
    std::string appKey;
    std::string appSecret;
    int defaultTTLSec;

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

    std::string base64Encode(const std::string &input)
    {
        BIO *bio, *b64;
        BUF_MEM *bufferPtr;

        b64 = BIO_new(BIO_f_base64());
        bio = BIO_new(BIO_s_mem());
        bio = BIO_push(b64, bio);

        BIO_set_flags(bio, BIO_FLAGS_BASE64_NO_NL);
        BIO_write(bio, input.c_str(), input.length());
        BIO_flush(bio);

        BIO_get_mem_ptr(bio, &bufferPtr);
        std::string output(bufferPtr->data, bufferPtr->length);

        BIO_free_all(bio);
        return output;
    }

    std::string hmacsha256(const std::string &appidStr, const std::string &uidStr, const std::string &curTimeStr, const std::string &expireTimeStr, const std::string &cname, const std::string &permSecret, const std::string &privilegeStr)
    {
        std::string contentToBeSigned = "appkey:" + appidStr + "\n";
        contentToBeSigned += "uid:" + uidStr + "\n";
        contentToBeSigned += "curTime:" + curTimeStr + "\n";
        contentToBeSigned += "expireTime:" + expireTimeStr + "\n";
        contentToBeSigned += "cname:" + cname + "\n";
        contentToBeSigned += "privilege:" + privilegeStr + "\n";

        unsigned char *digest;
        digest = HMAC(EVP_sha256(), permSecret.c_str(), permSecret.length(), reinterpret_cast<const unsigned char *>(contentToBeSigned.c_str()), contentToBeSigned.length(), NULL, NULL);

        return base64Encode(std::string((char *)(digest)));
    }

    std::string base64EncodeUrl(const std::vector<uint8_t> &input)
    {
        BIO *bmem = BIO_new(BIO_s_mem());
        BIO *b64 = BIO_new(BIO_f_base64());
        BIO_set_flags(b64, BIO_FLAGS_BASE64_NO_NL);
        BIO_push(b64, bmem);
        BIO_write(b64, input.data(), input.size());
        BIO_flush(b64);

        BUF_MEM *bptr;
        BIO_get_mem_ptr(b64, &bptr);

        std::string result(bptr->data, bptr->length);
        BIO_free_all(b64);

        // Replace characters for URL safety
        replace(result.begin(), result.end(), '+', '*');
        replace(result.begin(), result.end(), '/', '-');
        replace(result.begin(), result.end(), '=', '_');

        return result;
    }

    std::vector<uint8_t> compress(const std::string &data)
    {
        z_stream zs;
        memset(&zs, 0, sizeof(zs));

        if (deflateInit(&zs, Z_BEST_COMPRESSION) != Z_OK)
        {
            throw std::runtime_error("deflateInit failed while compressing.");
        }

        zs.next_in = (Bytef *)(data.data());
        zs.avail_in = data.size();

        int ret;
        std::vector<uint8_t> outBuffer(2048);

        do
        {
            zs.next_out = reinterpret_cast<Bytef *>(outBuffer.data());
            zs.avail_out = outBuffer.size();

            ret = deflate(&zs, Z_FINISH);

            if (outBuffer.size() - zs.avail_out > 0)
            {
                outBuffer.resize(outBuffer.size() - zs.avail_out);
            }
        } while (ret == Z_OK);

        deflateEnd(&zs);

        if (ret != Z_STREAM_END)
        {
            throw std::runtime_error("deflate failed while compressing.");
        }

        return outBuffer;
    }

public:
    struct DynamicToken
    {
        std::string signature;
        long long curTime;
        int ttl;
    };
    struct PermissionKey
    {
        std::string appkey;
        long uid;
        std::string cname;
        uint8_t privilege;
        long expireTime;
        long curTime;
        std::string checksum;
    };
};

using System;
using System.Collections.Generic;
using System.IO.Compression;
using System.Linq;
using System.Security;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Threading.Tasks;

namespace TokenBuilder
{
    public class TokenServer
    {
        private string AppKey { get; }
        private string AppSecret { get; }
        private int DefaultTTLSec { get; }

        public TokenServer(string appKey, string appSecret, int defaultTTLSec)
        {
            if (string.IsNullOrEmpty(appKey) || string.IsNullOrEmpty(appSecret))
            {
                throw new ArgumentException("appKey or appSecret is empty");
            }
            if (defaultTTLSec <= 0)
            {
                throw new ArgumentException("defaultTTLSec must be positive");
            }

            AppKey = appKey;
            AppSecret = appSecret;
            DefaultTTLSec = defaultTTLSec;
        }
        /// <summary>
        /// Generate a token with the given channel name, user ID, and time-to-live (TTL) in seconds.
        /// </summary>
        /// <param name="channelName">Generate a token with the given channel name, user ID, and time-to-live (TTL) in seconds.</param>
        /// <param name="uid">the user ID associated with the token</param>
        /// <param name="ttlSec">the user ID associated with the token</param>
        /// <returns>the token</returns>
        public string GetToken(string channelName, ulong uid, int ttlSec)
        {
            DateTime curTime = DateTime.UtcNow;
            return GetTokenWithCurrentTime(channelName, uid, ttlSec, curTime);
        }

        public string GetTokenWithCurrentTime(string channelName, ulong uid, int ttlSec, DateTime curTime)
        {
            if (ttlSec <= 0)
            {
                ttlSec = DefaultTTLSec;
            }

            var signature = ComputeSHA1($"{AppKey}{uid}{((DateTimeOffset)curTime).ToUnixTimeMilliseconds()}{ttlSec}{channelName}{AppSecret}");
            var tokenModel = new DynamicToken
            {
                Signature = signature,
                CurTime = ((DateTimeOffset)curTime).ToUnixTimeMilliseconds(),
                TTL = ttlSec
            };
            var tokenJSON = JsonSerializer.Serialize(tokenModel);
            return Convert.ToBase64String(Encoding.UTF8.GetBytes(tokenJSON));
        }

        /// <summary>
        /// generates a permission key for the given channelName, uid and privilege.
        /// </summary>
        /// <param name="channelName">the name of the channel for which the token is being generated</param>
        /// <param name="permSecret">the secret key for generating the permission key</param>
        /// <param name="uid">the user ID associated with the token</param>
        /// <param name="privilege">the privilege of the user. privilege is a 8-bit number, each bit represents a permission:
        ///                      - bit 1: (0000 0001) = 1, permission to send audio stream
        ///                      - bit 2: (0000 0010) = 2, permission to send video stream
        ///                      - bit 3: (0000 0100) = 4, permission to receive audio stream
        ///                      - bit 4: (0000 1000) = 8, permission to receive video stream
        ///                      - bit 5: (0001 0000) = 16, permission to create room
        ///                      - bit 6: (0010 0000) = 32, permission to join room
        ///                       So, (0010 1100) = 32+8+4 = 44 means permission to receive audio&video stream and join room.
        ///                           (0011 1111) = 63 means all permission allowed.</param>
        /// <param name="ttlSec"></param>
        /// <returns>the permission key</returns>
        public string GetPermissionKey(string channelName, string permSecret, long uid, byte privilege, long ttlSec)
        {
            long curTime = DateTimeOffset.Now.ToUnixTimeSeconds();
            return GetPermissionKeyWithCurrentTime(channelName, permSecret, uid, privilege, ttlSec, curTime);
        }

        public string GetPermissionKeyWithCurrentTime(string channelName, string permSecret, long uid, byte privilege, long ttlSec, long curTime)
        {
            PermissionKey permKey = new PermissionKey
            {
                Appkey = AppKey,
                Uid = uid,
                Cname = channelName,
                Privilege = privilege,
                ExpireTime = ttlSec,
                CurTime = curTime
            };

            // Calculate the signature
            string checksum = HmacSha256(AppKey, uid.ToString(), curTime.ToString(), ttlSec.ToString(), channelName, permSecret, privilege.ToString());
            permKey.Checksum = checksum;

            // Convert the object to JSON format
            string jsonStr = JsonSerializer.Serialize(permKey);

            // Compress the data
            byte[] compressedData = Compress(Encoding.UTF8.GetBytes(jsonStr));

            // Encode the compressed data and return
            return Base64EncodeUrl(compressedData);
        }

        private static byte[] Compress(byte[] data)
        {
            using (MemoryStream outputStream = new MemoryStream())
            {
                using (DeflateStream deflater = new DeflateStream(outputStream, CompressionMode.Compress, true))
                {
                    deflater.Write(data, 0, data.Length);
                }

                byte[] compressedData = outputStream.ToArray();

                // Add zlib header
                byte[] header = new byte[2];
                header[0] = 0x78; // CMF
                header[1] = 0xDA; // FLG

                // Add zlib trailer
                byte[] trailer = new byte[4];
                uint adler32 = Adler32(data);
                trailer[0] = (byte)(adler32 >> 24);
                trailer[1] = (byte)(adler32 >> 16);
                trailer[2] = (byte)(adler32 >> 8);
                trailer[3] = (byte)adler32;

                byte[] output = new byte[header.Length + compressedData.Length + trailer.Length];
                Array.Copy(header, output, header.Length);
                Array.Copy(compressedData, 0, output, header.Length, compressedData.Length);
                Array.Copy(trailer, 0, output, header.Length + compressedData.Length, trailer.Length);

                return output;
            }
        }

        private static uint Adler32(byte[] data)
        {
            const uint MOD_ADLER = 65521;
            uint a = 1, b = 0;
            for (int i = 0; i < data.Length; i++)
            {
                a = (a + data[i]) % MOD_ADLER;
                b = (b + a) % MOD_ADLER;
            }
            return (b << 16) | a;
        }

        private static string HmacSha256(string appidStr, string uidStr, string curTimeStr, string expireTimeStr,
            string cname, string permSecret, string privilegeStr)
        {
            string contentToBeSigned = $"appkey:{appidStr}\n";
            contentToBeSigned += $"uid:{uidStr}\n";
            contentToBeSigned += $"curTime:{curTimeStr}\n";
            contentToBeSigned += $"expireTime:{expireTimeStr}\n";
            contentToBeSigned += $"cname:{cname}\n";
            contentToBeSigned += $"privilege:{privilegeStr}\n";

            using (HMACSHA256 hmac = new HMACSHA256(Encoding.UTF8.GetBytes(permSecret)))
            {
                byte[] result = hmac.ComputeHash(Encoding.UTF8.GetBytes(contentToBeSigned));
                return Convert.ToBase64String(result);
            }
        }


        public static string Base64EncodeUrl(byte[] input)
        {
            string base64 = Convert.ToBase64String(input);
            base64 = base64.Replace('+', '*').Replace('/', '-').Replace('=', '_');
            return base64;
        }




        private static string ComputeSHA1(string input)
        {
            using var sha1 = SHA1.Create();
            var hash = sha1.ComputeHash(Encoding.UTF8.GetBytes(input));
            var sb = new StringBuilder(hash.Length * 2);

            foreach (byte b in hash)
            {
                sb.Append(b.ToString("x2"));
            }

            return sb.ToString();
        }
    }

    public class DynamicToken
    {
        [JsonPropertyName("signature")]
        public string Signature { get; set; }
        [JsonPropertyName("curTime")]
        public long CurTime { get; set; }
        [JsonPropertyName("ttl")]
        public int TTL { get; set; }
    }

    public class PermissionKey
    {
        [JsonPropertyName("appkey")]
        public string Appkey { get; set; }
        [JsonPropertyName("checksum")]
        public string Checksum { get; set; }
        [JsonPropertyName("cname")]
        public string Cname { get; set; }
        [JsonPropertyName("curTime")]
        public long CurTime { get; set; }
        [JsonPropertyName("expireTime")]
        public long ExpireTime { get; set; }
        [JsonPropertyName("privilege")]
        public byte Privilege { get; set; }
        [JsonPropertyName("uid")]
        public long Uid { get; set; }

    }
}

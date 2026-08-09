using System.Security.Cryptography;
using System.Text;

namespace TokenBuilder
{
    public class RestApiAuth
    {
        public static string GetChecksum(string appSecret, string nonce, long curtime)
        {
            return ComputeSHA1($"{appSecret}{nonce}{curtime}");
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
}


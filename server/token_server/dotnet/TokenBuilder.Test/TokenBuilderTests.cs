using Microsoft.VisualStudio.TestTools.UnitTesting;
using TokenBuilder;

namespace TokenBuilder.Tests
{
    [TestClass()]
    public class TokenBuilderTests
    {
        private TokenServer _tokenServer;

        [TestInitialize]
        public void Setup()
        {
            _tokenServer = new TokenServer("c37bf7a8758a4ed00000000000000000", "c00000000000", 3600); // Use your own values here
        }

        [TestMethod]
        public void GetTokenTest()
        {
            String v = _tokenServer.GetTokenWithCurrentTime("room1", 10000, 1800, DateTimeOffset.FromUnixTimeSeconds(1693968975).UtcDateTime);

            Assert.AreEqual("eyJzaWduYXR1cmUiOiJlZjRmNGEwOGM1NmZiOWI5MDQ3OTE2YjZlYmZhZGY5NWFjZDc2OGViIiwiY3VyVGltZSI6MTY5Mzk2ODk3NTAwMCwidHRsIjoxODAwfQ==", v);
            String v2 = _tokenServer.GetToken("room1", 10000, 0);
            Assert.IsNotNull(v2);
        }

        [TestMethod]
        public void GetPermissionKeyTest()
        {
            String v = _tokenServer.GetPermissionKeyWithCurrentTime("room1", "45eaeb3c2757c57c1b8e0a25a1f246a476c36ca5ba0cd20da38a154c2adebdab", 10000, 1, 1000, 1696662104);
            Assert.AreEqual("eNpdjcEKgkAYhN-lP3vQ1F0LulQQgRCBlHlb1z-dbNt1Y0WL3j2XOjW3*WaGeQHTusURFsBDWl4oS2icsAgr-1-gAW*Qtw8rp3YfXYdzso6fharSUapVR7ddybNyU1tMm2O*O*0Pecjagi-d8s4kTjOjlAyctyYTjgRkTgiZBX7kAQ5aGPzx6dEDbUQvblg74IEV1Tfw3x-fBDcB", v);
            String v2 = _tokenServer.GetPermissionKey("room1", "45eaeb3c2757c57c1b8e0a25a1f246a476c36ca5ba0cd20da38a154c2adebdab", 10000, 1, 1000);
            Assert.IsNotNull(v2);
        }
    }
}
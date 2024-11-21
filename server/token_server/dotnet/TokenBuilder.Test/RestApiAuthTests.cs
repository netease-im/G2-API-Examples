using Microsoft.VisualStudio.TestTools.UnitTesting;
using TokenBuilder;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace TokenBuilder.Tests
{
    [TestClass()]
    public class RestApiAuthTests
    {
        [TestMethod()]
        public void GetChecksumTest()
        {
            Assert.AreEqual("192bdbdad337836e6213aec1d93186aae9771c39", RestApiAuth.GetChecksum("c00000000000", "1234567890", 1697168455));
        }
    }
}
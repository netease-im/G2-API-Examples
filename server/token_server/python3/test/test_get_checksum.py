import unittest
from unittest.mock import patch
import sys
import os
import hashlib
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
from src import rest_api_auth

class TestGetChecksum(unittest.TestCase):
    def test_get_checksum(self):
        token = rest_api_auth.get_checksum("c00000000000", "1234567890", 1697168455)
        expected = "192bdbdad337836e6213aec1d93186aae9771c39"
        self.assertEqual(token, expected)

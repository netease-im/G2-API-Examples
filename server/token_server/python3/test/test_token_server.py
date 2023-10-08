import unittest
from unittest.mock import patch
import sys
import os
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
from src import token_server


class TestTokenServer(unittest.TestCase):
    def setUp(self):
        self.token_server = token_server.TokenServer("c37bf7a8758a4ed00000000000000000", "c00000000000", 3600)

    def test_constructor(self):
        with self.assertRaises(ValueError):
            token_server.TokenServer("", "app_secret", 3600)
        with self.assertRaises(ValueError):
            token_server.TokenServer("app_key", "", 3600)
        with self.assertRaises(ValueError):
            token_server.TokenServer("app_key", "app_secret", 0)

    def test_get_token(self):
        with patch("time.time", return_value=1693968975.0):
            token = self.token_server.get_token("room1", 10000, 1800)
            expected_token = "eyJzaWduYXR1cmUiOiJlZjRmNGEwOGM1NmZiOWI5MDQ3OTE2YjZlYmZhZGY5NWFjZDc2OGViIiwiY3VyVGltZSI6MTY5Mzk2ODk3NTAwMCwidHRsIjoxODAwfQ=="
            self.assertEqual(token, expected_token)

    def test_get_token_with_current_time(self):
        token = self.token_server._get_token_with_current_time("room1", 10000, 1800, 1693968975000)
        expected_token = "eyJzaWduYXR1cmUiOiJlZjRmNGEwOGM1NmZiOWI5MDQ3OTE2YjZlYmZhZGY5NWFjZDc2OGViIiwiY3VyVGltZSI6MTY5Mzk2ODk3NTAwMCwidHRsIjoxODAwfQ=="
        self.assertEqual(token, expected_token)

        token = self.token_server._get_token_with_current_time("room1", 10000, 0, 1693968975000)
        expected_token = "eyJzaWduYXR1cmUiOiJkMjZmYzFlZjk4ZWExNmM3YTkzOWFmMDZmOGE4MTk2MTJkY2QzZDU5IiwiY3VyVGltZSI6MTY5Mzk2ODk3NTAwMCwidHRsIjozNjAwfQ=="
        self.assertEqual(token, expected_token)

    def test_get_permission_key(self):
        with patch("time.time", return_value=1696662104.0):
            token = self.token_server.get_permission_key("room1", "45eaeb3c2757c57c1b8e0a25a1f246a476c36ca5ba0cd20da38a154c2adebdab", 10000, 1, 1000)
            expected_token = "eJxcy8GOgjAYBOB3*c89lAValmQvu5sYExJjQhS5lfILFWtLTQlofHfT6Mm5zTeZOwhrB1wgBxnz5shFxtNMJNjSzwAB2aMcrl5DDlNymg-ZX3qrTVss2vyOfDU2smz*O49Fv6vW*822isVQy5-wvAiNkIMzRkehe1eqIBH7Zox9RTQhgLNVDt9OKSVgnZrUGbsABLxqXwN9PAMAAP--3wQ3AQ__"
            self.assertEqual(token, expected_token)

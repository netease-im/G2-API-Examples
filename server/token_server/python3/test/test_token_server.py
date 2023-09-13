import unittest
from unittest.mock import patch
from .. import token_server


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
            token, err = self.token_server.get_token("room1", 10000, 1800)
            expected_token = "eyJzaWduYXR1cmUiOiJlZjRmNGEwOGM1NmZiOWI5MDQ3OTE2YjZlYmZhZGY5NWFjZDc2OGViIiwiY3VyVGltZSI6MTY5Mzk2ODk3NTAwMCwidHRsIjoxODAwfQ=="
            self.assertEqual(token, expected_token)
            self.assertIsNone(err)

    def test_get_token_with_current_time(self):
        token, err = self.token_server.get_token_with_current_time("room1", 10000, 1800, 1693968975000)
        expected_token = "eyJzaWduYXR1cmUiOiJlZjRmNGEwOGM1NmZiOWI5MDQ3OTE2YjZlYmZhZGY5NWFjZDc2OGViIiwiY3VyVGltZSI6MTY5Mzk2ODk3NTAwMCwidHRsIjoxODAwfQ=="
        self.assertEqual(token, expected_token)

        token, err = self.token_server.get_token_with_current_time("room1", 10000, 0, 1693968975000)
        expected_token = "eyJzaWduYXR1cmUiOiJkMjZmYzFlZjk4ZWExNmM3YTkzOWFmMDZmOGE4MTk2MTJkY2QzZDU5IiwiY3VyVGltZSI6MTY5Mzk2ODk3NTAwMCwidHRsIjozNjAwfQ=="
        self.assertEqual(token, expected_token)

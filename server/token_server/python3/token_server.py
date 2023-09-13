# !/usr/bin/env python3
import hashlib
import base64
import json
import time


class TokenServer:
    def __init__(self, app_key, app_secret, default_ttl_sec):
        if not app_key or not app_secret:
            raise ValueError("app_key or app_secret is empty")
        if default_ttl_sec <= 0:
            raise ValueError("default_ttl_sec must be positive")
        self.app_key = app_key
        self.app_secret = app_secret
        self.default_ttl_sec = default_ttl_sec

    def get_token(self, channel_name, uid, ttl_sec):
        """
        Generate a token with the given channel name, user ID, and time-to-live (TTL) in seconds.

        :param channel_name: str, the name of the channel for which the token is being generated
        :param uid: int, the user ID associated with the token
        :param ttl_sec: int, the time-to-live (TTL) for the token in seconds, should be less than 86400(1 day)
        :return: tuple(str, None), a tuple containing the token as a base64-encoded string and None for the error
        """
        cur_time = int(time.time() * 1000)
        return self._get_token_with_current_time(channel_name, uid, ttl_sec, cur_time)

    def _get_token_with_current_time(self, channel_name, uid, ttl_sec, current_time):
        if ttl_sec <= 0:
            ttl_sec = self.default_ttl_sec
        signature = hashlib.sha1(
            f"{self.app_key}{uid}{current_time}{ttl_sec}{channel_name}{self.app_secret}".encode()
        ).hexdigest()
        token_model = {"signature": signature, "curTime": current_time, "ttl": ttl_sec}
        token_json = json.dumps(token_model, separators=(',', ':'))
        return base64.b64encode(token_json.encode()).decode(), None

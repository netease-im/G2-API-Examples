# !/usr/bin/env python3
import hashlib
import base64
import hmac
import json
import time
import zlib
from collections import namedtuple


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
        return base64.b64encode(token_json.encode()).decode()

    def get_permission_key(self, channel_name, perm_secret, uid, privilege, ttl_sec):
        """
        generates a permission key for the given channelName, uid and privilege.
        :param channel_name: str, the name of the channel for which the token is being generated
        :param perm_secret:  str, the secret key for generating the permission key
        :param uid: int, the user ID associated with the token
        :param privilege: int, the privilege of the user. privilege is a 8-bit number, each bit represents a permission:
                          - bit 1: (0000 0001) = 1, permission to send audio stream
                          - bit 2: (0000 0010) = 2, permission to send video stream
                          - bit 3: (0000 0100) = 4, permission to receive audio stream
                          - bit 4: (0000 1000) = 8, permission to receive video stream
                          - bit 5: (0001 0000) = 16, permission to create room
                          - bit 6: (0010 0000) = 32, permission to join room
                          So, (0010 1100) = 32+8+4 = 44 means permission to receive audio&video stream and join room.
                          (0011 1111) = 63 means all permission allowed.
        :param ttl_sec: int, the time-to-live (TTL) for the token in seconds, should be less than 86400 seconds (1 day)
        :return: str, the permission key as string
        """
        cur_time = int(time.time())
        return self._get_permission_key_with_current_time(channel_name, perm_secret, uid, privilege, ttl_sec, cur_time)

    def _get_permission_key_with_current_time(self, channel_name, perm_secret, uid, privilege, ttl_sec, cur_time):
        PermissionKey = namedtuple('PermissionKey',
                                   ['appkey', 'uid', 'cname', 'privilege', 'expireTime', 'curTime', 'checksum'])
        checksum = self.hmacsha256(self.app_key, str(uid), str(cur_time), str(ttl_sec), channel_name, perm_secret,
                                   str(privilege))
        perm_key = PermissionKey(appkey=self.app_key, uid=uid, cname=channel_name, privilege=privilege,
                                 expireTime=ttl_sec, curTime=cur_time, checksum=checksum)
        json_str = json.dumps(perm_key._asdict(), separators=(',', ':'), sort_keys=True)
        print(json_str)
        compressed_data = zlib.compress(json_str.encode('utf-8'), 6)
        return self.base64_encode_url(compressed_data).decode('utf-8')

    @staticmethod
    def hmacsha256(appid_str, uid_str, cur_time_str, expire_time_str, cname, perm_secret, privilege_str):
        content_to_be_signed = f"appkey:{appid_str}\nuid:{uid_str}\ncurTime:{cur_time_str}\nexpireTime:{expire_time_str}\ncname:{cname}\nprivilege:{privilege_str}\n"
        return base64.b64encode(hmac.new(perm_secret.encode('utf-8'), content_to_be_signed.encode('utf-8'),
                                         hashlib.sha256).digest()).decode('utf-8')

    def base64_encode_url(self, input_bytes):
        base64_bytes = base64.b64encode(input_bytes)
        return base64_bytes.replace(b'=', b'_').replace(b'+', b'*').replace(b'/', b'-')

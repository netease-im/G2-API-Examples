import hashlib


def get_checksum(app_secret: str, nonce: str, timestamp: int):
    return hashlib.sha1(f'{app_secret}{nonce}{timestamp}'.encode()).hexdigest()

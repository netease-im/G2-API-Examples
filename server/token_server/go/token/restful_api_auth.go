package token

import (
	"crypto/sha1"
	"fmt"
)

func GetChecksum(appSecret, nonce string, curtime int64) string {
	raw := fmt.Sprintf("%s%s%d", appSecret, nonce, curtime)
	return fmt.Sprintf("%x", sha1.Sum([]byte(raw)))
}

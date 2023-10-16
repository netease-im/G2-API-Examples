package token

import (
	"bytes"
	"compress/zlib"
	"crypto/hmac"
	"crypto/sha1"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"time"
)

var (
	Base64EncodingStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789*-"
	base64Endoding    = base64.NewEncoding(Base64EncodingStr).WithPadding('_')
)

type TokenServer struct {
	AppKey        string
	AppSecret     string
	DefaultTTLSec int
}

type DynamicToken struct {
	Signature string `json:"signature"`
	CurTime   int64  `json:"curTime"`
	TTL       int    `json:"ttl"`
}

// NewTokenServer creates a new token instance, it's recommended to create only one instance for each appKey/appSecret pair.
// `appKey` and `appSecret` can be found in the developer console.
// `·defaultTTLSec` should be less than 86400(24 hours)
func NewTokenServer(appKey string, appSecret string, defaultTTLSec int) (*TokenServer, error) {
	if appKey == "" || appSecret == "" {
		return nil, errors.New("appKey or appSecret is empty")
	}
	if defaultTTLSec <= 0 {
		return nil, errors.New("defaultTTLSec must be positive")
	}

	return &TokenServer{
		AppKey:        appKey,
		AppSecret:     appSecret,
		DefaultTTLSec: defaultTTLSec,
	}, nil
}

// GetToken generates a token for the given channelName and uid.
// ttlSec should be less than 86400(24 hours)
func (t *TokenServer) GetToken(channelName string, uid uint64, ttlSec int) (string, error) {
	curTime := time.Now()
	return t.getTokenWithCurrentTime(channelName, uid, ttlSec, &curTime)
}

// getTokenWithCurrentTime generates a token for the given channelName and uid with the given current time
func (t *TokenServer) getTokenWithCurrentTime(channelName string, uid uint64, ttlSec int, curTime *time.Time) (string, error) {
	if ttlSec <= 0 {
		ttlSec = t.DefaultTTLSec
	}
	tokenModel := DynamicToken{
		Signature: fmt.Sprintf("%x", sha1.Sum([]byte(fmt.Sprintf("%s%d%d%d%s%s", t.AppKey, uid, curTime.UnixMilli(), ttlSec, channelName, t.AppSecret)))),
		CurTime:   curTime.UnixMilli(),
		TTL:       ttlSec,
	}
	tokenJSON, _ := json.Marshal(tokenModel)
	return base64.StdEncoding.EncodeToString(tokenJSON), nil
}

// GetPermissionKey generates a permission key for the given channelName, uid and privilege.
// ttlSec should be less than 86400(24 hours)
// privilege is a 8-bit number, each bit represents a permission:
// - bit 1: (0000 0001) = 1, permission to send audio stream
// - bit 2: (0000 0010) = 2, permission to send video stream
// - bit 3: (0000 0100) = 4, permission to receive audio stream
// - bit 4: (0000 1000) = 8, permission to receive video stream
// - bit 5: (0001 0000) = 16, permission to create room
// - bit 6: (0010 0000) = 32, permission to join room
// So, (0010 1100) = 32+8+4 = 44 means permission to receive audio&video stream and join room.
// (0011 1111) = 63 means all permission allowed.
func (t *TokenServer) GetPermissionKey(channelName, permSecret string, uid uint64, privilege uint8, ttlSec int64) (string, error) {
	curTime := time.Now().Unix()
	return t.getPermissionKeyWithCurrentTime(channelName, permSecret, uid, privilege, ttlSec, curTime)
}

func (t *TokenServer) getPermissionKeyWithCurrentTime(channelName, permSecret string, uid uint64, privilege uint8, ttlSec, curTime int64) (string, error) {
	permKeyMap := make(map[string]interface{})
	permKeyMap["appkey"] = t.AppKey
	permKeyMap["uid"] = uid
	permKeyMap["cname"] = channelName
	permKeyMap["privilege"] = privilege
	permKeyMap["expireTime"] = ttlSec
	permKeyMap["curTime"] = curTime

	// calculate checksum
	permKeyMap["checksum"] = hmacsha256(t.AppKey, fmt.Sprintf("%d", uid), fmt.Sprintf("%d", curTime),
		fmt.Sprintf("%d", ttlSec), channelName, permSecret, fmt.Sprintf("%d", privilege))

	// convert to json
	data, err := json.Marshal(permKeyMap)
	if err != nil {
		return "", err
	}

	// compress data
	var b bytes.Buffer
	w := zlib.NewWriter(&b)
	if _, err = w.Write(data); err != nil {
		return "", err
	}
	if err = w.Close(); err != nil {
		return "", err
	}

	// encode to base64
	return base64Endoding.EncodeToString(b.Bytes()), nil
}

// hmacsha256 calculates the signature for permission key
func hmacsha256(appidStr, uidStr, curTimeStr, expireTimeStr, cname, permSecret, privilegeStr string) string {
	var contentToBeSigned string
	contentToBeSigned = "appkey:" + appidStr + "\n"
	contentToBeSigned += "uid:" + uidStr + "\n"
	contentToBeSigned += "curTime:" + curTimeStr + "\n"
	contentToBeSigned += "expireTime:" + expireTimeStr + "\n"
	contentToBeSigned += "cname:" + cname + "\n"
	contentToBeSigned += "privilege:" + privilegeStr + "\n"

	h := hmac.New(sha256.New, []byte(permSecret))
	h.Write([]byte(contentToBeSigned))
	return base64.StdEncoding.EncodeToString(h.Sum(nil))
}

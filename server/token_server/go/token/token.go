package token

import (
	"crypto/sha1"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"time"
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

package token

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestTokenServer_GetToken(t1 *testing.T) {
	// empty secret should return error
	_, err := NewTokenServer("xx", "", 3600)
	assert.Error(t1, err)

	// negative defaultTTLSec should return error
	_, err = NewTokenServer("xx", "xx", -1)
	assert.Error(t1, err)

	tokenServer, err := NewTokenServer("c37bf7a8758a4ed00000000000000000", "c00000000000", 3600)
	assert.NoError(t1, err)

	type args struct {
		channelName string
		uid         uint64
		ttlSec      int
	}
	tests := []struct {
		name    string
		args    args
		want    string
		wantErr bool
	}{
		{
			name: "normal",
			args: args{
				channelName: "room1",
				uid:         10000,
				ttlSec:      1800,
			},
			want:    "eyJzaWduYXR1cmUiOiJlZjRmNGEwOGM1NmZiOWI5MDQ3OTE2YjZlYmZhZGY5NWFjZDc2OGViIiwiY3VyVGltZSI6MTY5Mzk2ODk3NTAwMCwidHRsIjoxODAwfQ==",
			wantErr: false,
		},
		{
			name: "negative ttl should use default ttl",
			args: args{
				channelName: "room1",
				uid:         10000,
				ttlSec:      -1,
			},
			want:    "eyJzaWduYXR1cmUiOiJkMjZmYzFlZjk4ZWExNmM3YTkzOWFmMDZmOGE4MTk2MTJkY2QzZDU5IiwiY3VyVGltZSI6MTY5Mzk2ODk3NTAwMCwidHRsIjozNjAwfQ==",
			wantErr: false,
		},
	}

	curTime := time.Unix(1693968975, 0)
	for _, tt := range tests {
		t1.Run(tt.name, func(t1 *testing.T) {
			got, err := tokenServer.getTokenWithCurrentTime(tt.args.channelName, tt.args.uid, tt.args.ttlSec, &curTime)
			if (err != nil) != tt.wantErr {
				t1.Errorf("GetToken() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t1.Errorf("GetToken() got = %v, want %v", got, tt.want)
			}
		})
	}
}

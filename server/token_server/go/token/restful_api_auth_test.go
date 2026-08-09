package token

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestSecureChecksum(t *testing.T) {
	assert.Equal(t, "192bdbdad337836e6213aec1d93186aae9771c39",
		GetChecksum("c00000000000", "1234567890", 1697168455))
}

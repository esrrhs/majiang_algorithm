package majiang

import (
	"fmt"
	"os"
	"testing"
)

func TestMain(m *testing.M) {
	if err := Load(); err != nil {
		fmt.Fprintln(os.Stderr, "加载查表文件失败:", err)
		os.Exit(1)
	}
	os.Exit(m.Run())
}

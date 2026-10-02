package majiang

import (
	"os"
	"sort"
	"strings"
	"testing"
)

// 用内存生成的表与仓库已提交的查表文件做内容比对。
// Java 侧 gen() 重新生成 jian/feng 表与已提交文件完全一致(多线程写入顺序不定,
// 因此按行排序后比较内容集合);Go 的 gen 移植必须同样复现。

func readDataLines(t *testing.T, name string) []string {
	t.Helper()
	path, err := findDataFile(name)
	if err != nil {
		t.Skipf("跳过(找不到 %s): %v", name, err)
	}
	data, err := os.ReadFile(path)
	if err != nil {
		t.Fatalf("读取 %s 失败: %v", path, err)
	}
	var lines []string
	for _, line := range strings.Split(string(data), "\n") {
		line = strings.TrimRight(line, "\r")
		if line != "" {
			lines = append(lines, line)
		}
	}
	return lines
}

func assertSameLines(t *testing.T, name string, got []string) {
	t.Helper()
	want := readDataLines(t, name)
	sort.Strings(want)
	sort.Strings(got)
	if len(want) != len(got) {
		t.Fatalf("%s: 行数不同 got=%d want=%d", name, len(got), len(want))
	}
	for i := range want {
		if got[i] != want[i] {
			t.Fatalf("%s: 第 %d 行不同\n got: %s\nwant: %s", name, i, got[i], want[i])
		}
	}
}

func TestGenHuJianParity(t *testing.T) {
	m := make(map[int64][]HuTableInfo)
	genHuInto(m, huJianCfg)
	assertSameLines(t, "majiang_clien_jian.txt", buildHuClientLines(huJianCfg, m))
	assertSameLines(t, "majiang_server_jian.txt", buildHuServerLines(huJianCfg, m))
}

func TestGenHuFengParity(t *testing.T) {
	m := make(map[int64][]HuTableInfo)
	genHuInto(m, huFengCfg)
	assertSameLines(t, "majiang_clien_feng.txt", buildHuClientLines(huFengCfg, m))
	assertSameLines(t, "majiang_server_feng.txt", buildHuServerLines(huFengCfg, m))
}

func TestGenAiJianParity(t *testing.T) {
	m := make(map[int64][]AITableInfo)
	genAiInto(m, aiJianCfg)
	assertSameLines(t, "majiang_ai_jian.txt", buildAiLines(aiJianCfg, m))
}

func TestGenAiFengParity(t *testing.T) {
	m := make(map[int64][]AITableInfo)
	genAiInto(m, aiFengCfg)
	assertSameLines(t, "majiang_ai_feng.txt", buildAiLines(aiFengCfg, m))
}

func TestJavaDoubleToString(t *testing.T) {
	// 与 Java Double.toString 输出逐字符一致
	cases := []struct {
		v    float64
		want string
	}{
		{0, "0.0"},
		{1, "1.0"},
		{-1, "-1.0"},
		{0.05610859728506787, "0.05610859728506787"},
		{0.0016140602582496414, "0.0016140602582496414"},
		{0.001, "0.001"},
		{1e-4, "1.0E-4"},
		{1e7, "1.0E7"},
		{9999999, "9999999.0"},
		{36.0 / 136, "0.2647058823529412"},
	}
	for _, c := range cases {
		if got := javaDoubleToString(c.v); got != c.want {
			t.Errorf("javaDoubleToString(%v) = %q, want %q", c.v, got, c.want)
		}
	}
}

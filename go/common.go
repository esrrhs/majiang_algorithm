package majiang

import (
	"fmt"
	"math"
	"os"
	"path/filepath"
	"strconv"
	"strings"
)

// findDataFile 依序在当前目录、data/、../data/ 下查找查表文件,
// 与 Java 侧 HuCommon.findDataFile 的回退顺序保持一致。
func findDataFile(name string) (string, error) {
	for _, p := range []string{name, filepath.Join("data", name), filepath.Join("..", "data", name)} {
		if st, err := os.Stat(p); err == nil && !st.IsDir() {
			return p, nil
		}
	}
	return "", fmt.Errorf("查表文件 %s 不存在(已尝试当前目录、data/、../data/)", name)
}

// javaDoubleToString 按Java Double.toString 的规则格式化 float64,
// 用于生成与 Java 版逐字节可比的查表文件。
func javaDoubleToString(d float64) string {
	if math.IsNaN(d) {
		return "NaN"
	}
	if math.IsInf(d, 1) {
		return "Infinity"
	}
	if math.IsInf(d, -1) {
		return "-Infinity"
	}
	if d == 0 {
		if math.Signbit(d) {
			return "-0.0"
		}
		return "0.0"
	}

	a := math.Abs(d)
	// 最短往返表示的科学计数法形式,如 "1.2345e+07"、"5e-05"
	s := strconv.FormatFloat(a, 'e', -1, 64)
	mant, expStr, _ := strings.Cut(s, "e")
	exp, _ := strconv.Atoi(expStr)
	digits := strings.Replace(mant, ".", "", 1)

	var b strings.Builder
	if math.Signbit(d) {
		b.WriteByte('-')
	}
	if a >= 1e-3 && a < 1e7 {
		if exp >= 0 {
			if exp+1 >= len(digits) {
				b.WriteString(digits)
				b.WriteString(strings.Repeat("0", exp+1-len(digits)))
				b.WriteString(".0")
			} else {
				b.WriteString(digits[:exp+1])
				b.WriteByte('.')
				b.WriteString(digits[exp+1:])
			}
		} else {
			b.WriteString("0.")
			b.WriteString(strings.Repeat("0", -exp-1))
			b.WriteString(digits)
		}
	} else {
		b.WriteByte(digits[0])
		b.WriteByte('.')
		if len(digits) == 1 {
			b.WriteByte('0')
		} else {
			b.WriteString(digits[1:])
		}
		b.WriteByte('E')
		b.WriteString(strconv.Itoa(exp))
	}
	return b.String()
}

// keyDigits 把表键(每位一个计数,共 n 位)展开成长度为 n 的计数数组,
// 等价 Java 里 num[N-1-i] = tmp % 10 的展开方式。
func keyDigits(key int64, n int) []int {
	num := make([]int, n)
	tmp := key
	for i := 0; i < n; i++ {
		num[n-1-i] = int(tmp % 10)
		tmp /= 10
	}
	return num
}

// genCardSet 枚举 n 个位置、每个位置计数 0..4、总计数为 total 的所有键(去重),
// 等价 Java HuCommon/AICommon 的 gen_card。
func genCardSet(n int, total int) map[int64]struct{} {
	out := make(map[int64]struct{})
	num := make([]int, n)
	var rec func(index int, total int)
	rec = func(index int, total int) {
		if index == n-1 {
			if total > 4 {
				return
			}
			num[index] = total
			var ret int64
			for _, c := range num {
				ret = ret*10 + int64(c)
			}
			out[ret] = struct{}{}
			return
		}
		for i := 0; i <= 4; i++ {
			if i <= total {
				num[index] = i
			} else {
				num[index] = 0
			}
			rec(index+1, total-num[index])
		}
	}
	rec(0, total)
	return out
}

// genCardSetAll 枚举总计数 0..14 的全部键,等价 Java gen() 开头的 card 集合构建。
func genCardSetAll(n int) map[int64]struct{} {
	out := make(map[int64]struct{})
	for i := 0; i <= 14; i++ {
		for k := range genCardSet(n, i) {
			out[k] = struct{}{}
		}
	}
	return out
}

// removeFirst 移除切片中第一个等于 card 的元素(对应 Java List.remove((Integer)v))。
func removeFirst(input []int, card int) []int {
	tmp := make([]int, len(input))
	copy(tmp, input)
	for i, c := range tmp {
		if c == card {
			tmp = append(tmp[:i], tmp[i+1:]...)
			break
		}
	}
	return tmp
}

// containsInt 对应 Java List.contains。
func containsInt(list []int, v int) bool {
	for _, c := range list {
		if c == v {
			return true
		}
	}
	return false
}

// frequency 对应 Java Collections.frequency。
func frequency(list []int, v int) int {
	n := 0
	for _, c := range list {
		if c == v {
			n++
		}
	}
	return n
}

// countCards 把手牌列表转成长度 MaxNum 的计数数组(下标 = 牌编号-1)。
func countCards(input []int) []int {
	cards := make([]int, MaxNum)
	for _, c := range input {
		cards[c-1]++
	}
	return cards
}

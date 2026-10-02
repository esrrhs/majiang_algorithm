package majiang

import (
	"bufio"
	"os"
	"strconv"
	"strings"
	"testing"
)

// 回放 go/testdata/parity_cases.txt:由 Java 侧 ParityDump 以固定种子导出的
// 判胡/听牌/AI 决策样例,Go 实现必须逐字段复现(跨语言功能对齐契约)。

func TestParityFixture(t *testing.T) {
	f, err := os.Open("testdata/parity_cases.txt")
	if err != nil {
		t.Skipf("跳过(找不到 fixture): %v", err)
	}
	defer f.Close()

	scanner := bufio.NewScanner(f)
	scanner.Buffer(make([]byte, 0, 1024*1024), 4*1024*1024)
	caseIndex := 0
	for scanner.Scan() {
		line := strings.TrimRight(scanner.Text(), "\r")
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}
		fields := strings.Split(line, "|")
		if len(fields) != 13 {
			t.Fatalf("第 %d 行字段数错误: %d", caseIndex, len(fields))
		}

		hand := StringToCards(fields[0])
		gui, err := strconv.Atoi(fields[1])
		if err != nil {
			t.Fatalf("第 %d 行 gui 解析失败: %v", caseIndex, err)
		}
		guiList := []int{gui}
		chiCard, _ := strconv.Atoi(fields[8])

		assertField := func(name string, got, want string) {
			if got != want {
				t.Fatalf("case %d %s:\n got: %s\nwant: %s", caseIndex, name, got, want)
			}
		}
		boolField := func(name string, got bool, want string) {
			wantBool := want == "1"
			if got != wantBool {
				t.Fatalf("case %d %s: got %v want %s", caseIndex, name, got, want)
			}
		}

		boolField("isHu", IsHu(hand, gui), fields[2])
		boolField("isHuExtra", IsHuExtra(hand, guiList, 0), fields[3])
		assertField("isTing", CardsToString(IsTing(hand, gui)), fields[4])
		assertField("isTingExtra", CardsToString(IsTingExtra(hand, guiList)), fields[5])

		wantCalc, err := strconv.ParseFloat(fields[6], 64)
		if err != nil {
			t.Fatalf("第 %d 行 calc 解析失败: %v", caseIndex, err)
		}
		if gotCalc := Calc(hand, guiList); gotCalc != wantCalc {
			t.Fatalf("case %d calc: got %v want %v", caseIndex, gotCalc, wantCalc)
		}

		wantOut, _ := strconv.Atoi(fields[7])
		if gotOut := OutAI(hand, guiList); gotOut != wantOut {
			t.Fatalf("case %d outAI: got %s want %s", caseIndex, CardToString(gotOut), CardToString(wantOut))
		}

		assertField("chiChoices", CardsToString(ChiAIChoices(hand, guiList, chiCard)), fields[9])
		boolField("chiBool", ChiAI(hand, guiList, chiCard, chiCard-1, chiCard+1), fields[10])
		boolField("peng", PengAI(hand, guiList, chiCard, 0), fields[11])
		boolField("gang", GangAI(hand, guiList, chiCard, 1), fields[12])
		caseIndex++
	}
	if err := scanner.Err(); err != nil {
		t.Fatalf("读取 fixture 失败: %v", err)
	}
	if caseIndex == 0 {
		t.Fatal("fixture 中没有样例")
	}
	t.Logf("[PARITY] replayed %d cases against Go implementation", caseIndex)
}

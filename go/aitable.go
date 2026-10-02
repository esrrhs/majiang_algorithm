package majiang

import (
	"bufio"
	"fmt"
	"os"
	"sort"
	"strconv"
	"strings"
)

// AITableInfo 对应 Java AITableInfo。
type AITableInfo struct {
	Jiang bool
	P     float64
}

// 三张 AI 表,键与判胡表同构,与 Java AITable* 类一致。
var (
	AITable     = make(map[int64][]AITableInfo) // 万
	AITableFeng = make(map[int64][]AITableInfo) // 风
	AITableJian = make(map[int64][]AITableInfo) // 箭
)

// aiConfig 对应 Java AICommon 的静态参数;LEVEL 对应 Java AICommon.LEVEL。
type aiConfig struct {
	n      int
	name   string
	card   []string
	huLian bool
	baseP  float64
}

const aiLevel = 5

var (
	aiNormalCfg = aiConfig{n: 9, name: "normal", card: wanNames, huLian: true, baseP: 36.0 / 136}
	aiFengCfg   = aiConfig{n: 4, name: "feng", card: fengNames, huLian: false, baseP: 16.0 / 136}
	aiJianCfg   = aiConfig{n: 3, name: "jian", card: jianNames, huLian: false, baseP: 12.0 / 136}
)

// AILoad 加载 AI 表(对应 Java AIUtil.load)。文件不存在或格式错误时返回错误。
func AILoad() error {
	if err := loadAiTable(AITableJian, aiJianCfg); err != nil {
		return err
	}
	if err := loadAiTable(AITableFeng, aiFengCfg); err != nil {
		return err
	}
	return loadAiTable(AITable, aiNormalCfg)
}

func loadAiTable(m map[int64][]AITableInfo, cfg aiConfig) error {
	for k := range m {
		delete(m, k)
	}
	path, err := findDataFile("majiang_ai_" + cfg.name + ".txt")
	if err != nil {
		return err
	}
	f, err := os.Open(path)
	if err != nil {
		return err
	}
	defer f.Close()

	scanner := bufio.NewScanner(f)
	scanner.Buffer(make([]byte, 0, 64*1024), 1024*1024)
	for scanner.Scan() {
		line := strings.TrimRight(scanner.Text(), "\r")
		if line == "" {
			continue
		}
		fields := strings.Fields(line)
		if len(fields) < 3 {
			return fmt.Errorf("%s 格式错误: %q", path, line)
		}
		key, err := strconv.ParseInt(fields[0], 10, 64)
		if err != nil {
			return fmt.Errorf("%s 格式错误: %q: %v", path, line, err)
		}
		jiang, _ := strconv.Atoi(fields[1])
		p, err := strconv.ParseFloat(fields[2], 64)
		if err != nil {
			return fmt.Errorf("%s 格式错误: %q: %v", path, line, err)
		}
		m[key] = append(m[key], AITableInfo{Jiang: jiang != 0, P: p})
	}
	return scanner.Err()
}

// aiInfoKey 对应 Java AIInfo(HashSet 去重用)。
type aiInfoKey struct {
	inputNum int
	jiang    int
}

// genAiInto 在内存中重新生成一张 AI 表(等价 Java AICommon.check_ai 的全量枚举),
// 结果写入 m;不写文件。genAi/gen 测试共用。
func genAiInto(m map[int64][]AITableInfo, cfg aiConfig) {
	for k := range m {
		delete(m, k)
	}
	tmpcards := make(map[int]map[int64]struct{})
	for inputNum := 0; inputNum <= aiLevel; inputNum++ {
		tmpcards[inputNum] = genCardSet(cfg.n, inputNum)
	}
	for card := range genCardSetAll(cfg.n) {
		checkAiLong(m, cfg, card, tmpcards)
	}
}

func checkAiLong(m map[int64][]AITableInfo, cfg aiConfig, card int64, tmpcards map[int]map[int64]struct{}) {
	n := cfg.n
	num := keyDigits(card, n)

	// Java 用 HashMap<Integer, AITableInfo>(key 1=有将, 0=无将),两个 key 恒存在
	acc := map[bool]*AITableInfo{
		true:  {Jiang: true, P: 0},
		false: {Jiang: false, P: 0},
	}

	for inputNum := 0; inputNum <= aiLevel; inputNum++ {
		tmpcard := tmpcards[inputNum]
		aiInfos := make(map[aiInfoKey]struct{})
		valid := 0

		for tmpc := range tmpcard {
			tmpcnum := keyDigits(tmpc, n)
			max := false
			for i := 0; i < n; i++ {
				num[i] += tmpcnum[i]
				if num[i] > 4 {
					max = true
				}
			}
			if !max {
				checkAiRec(cfg, aiInfos, num, -1, inputNum)
				valid++
			}
			for i := 0; i < n; i++ {
				num[i] -= tmpcnum[i]
			}
		}

		for info := range aiInfos {
			key := info.jiang != -1
			if info.inputNum == 0 {
				acc[key].P = 1
			}
			if acc[key].P != 1 {
				acc[key].P += cfg.baseP * 1.0 / float64(valid)
			}
		}
	}

	// Java HashMap 对小整数键 0/1 的遍历顺序恒为 0 先 1 后,即无将在前
	list := []AITableInfo{*acc[false], *acc[true]}
	m[card] = list
}

func checkAiRec(cfg aiConfig, aiInfos map[aiInfoKey]struct{}, num []int, jiang int, inputNum int) {
	n := cfg.n
	if cfg.huLian {
		for i := 0; i+2 < n; i++ {
			if num[i] > 0 && num[i+1] > 0 && num[i+2] > 0 {
				num[i]--
				num[i+1]--
				num[i+2]--
				checkAiRec(cfg, aiInfos, num, jiang, inputNum)
				num[i]++
				num[i+1]++
				num[i+2]++
			}
		}
	}
	for i := 0; i < n; i++ {
		if num[i] >= 2 && jiang == -1 {
			num[i] -= 2
			checkAiRec(cfg, aiInfos, num, i, inputNum)
			num[i] += 2
		}
	}
	for i := 0; i < n; i++ {
		if num[i] >= 3 {
			num[i] -= 3
			checkAiRec(cfg, aiInfos, num, jiang, inputNum)
			num[i] += 3
		}
	}
	for i := 0; i < n; i++ {
		if num[i] != 0 {
			return
		}
	}
	aiInfos[aiInfoKey{inputNum: inputNum, jiang: jiang}] = struct{}{}
}

// genAi 重新生成全部 AI 查表文件(majiang_ai_*.txt),写入当前目录,
// 等价 Java AIUtil.gen()。浮点数按 Java Double.toString 规则输出,与 Java 生成的一致。
func genAi() error {
	type aiTable struct {
		m   map[int64][]AITableInfo
		cfg aiConfig
	}
	tables := []aiTable{{AITableJian, aiJianCfg}, {AITableFeng, aiFengCfg}, {AITable, aiNormalCfg}}
	for _, t := range tables {
		genAiInto(t.m, t.cfg)
		if err := writeAiFile(t.cfg, t.m); err != nil {
			return err
		}
	}
	return nil
}

func writeAiFile(cfg aiConfig, m map[int64][]AITableInfo) error {
	name := "majiang_ai_" + cfg.name + ".txt"
	return writeLines(name, buildAiLines(cfg, m))
}

func buildAiLines(cfg aiConfig, m map[int64][]AITableInfo) []string {
	var lines []string
	huCfg := huConfig{n: cfg.n, card: cfg.card}
	for _, key := range sortedAiKeys(m) {
		for _, info := range m[key] {
			var b strings.Builder
			b.WriteString(strconv.FormatInt(key, 10))
			b.WriteString(" ")
			if info.Jiang {
				b.WriteString("1 ")
			} else {
				b.WriteString("0 ")
			}
			b.WriteString(javaDoubleToString(info.P))
			b.WriteString(" ")
			b.WriteString(showCard(huCfg, key))
			b.WriteString(" ")
			if info.Jiang {
				b.WriteString("有将 ")
			} else {
				b.WriteString("无将 ")
			}
			b.WriteString(javaDoubleToString(info.P))
			lines = append(lines, b.String())
		}
	}
	return lines
}

func sortedAiKeys(m map[int64][]AITableInfo) []int64 {
	keys := make([]int64, 0, len(m))
	for k := range m {
		keys = append(keys, k)
	}
	sort.Slice(keys, func(i, j int) bool { return keys[i] < keys[j] })
	return keys
}

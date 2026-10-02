package majiang

import (
	"bufio"
	"fmt"
	"os"
	"sort"
	"strconv"
	"strings"
)

// HuTableInfo 对应 Java HuTableInfo。
// Hupai 为 nil 表示该组合不需要进牌即已胡(对应 Java hupai == null);
// 否则 Hupai 长度为该花色的牌数,>0 的下标表示听该牌。
type HuTableInfo struct {
	NeedGui int
	Jiang   bool
	Hupai   []int8
}

// 三张判胡表,键为各花色计数串展开成的十进制长整数,与 Java HuTable* 类一致。
var (
	HuTable     = make(map[int64][]HuTableInfo) // 万
	HuTableFeng = make(map[int64][]HuTableInfo) // 风
	HuTableJian = make(map[int64][]HuTableInfo) // 箭
)

var (
	wanNames  = []string{"1万", "2万", "3万", "4万", "5万", "6万", "7万", "8万", "9万"}
	fengNames = []string{"东", "南", "西", "北"}
	jianNames = []string{"中", "发", "白"}
)

// huConfig 对应 Java HuCommon 的静态参数。
type huConfig struct {
	n      int
	name   string
	card   []string
	huLian bool
}

var (
	huNormalCfg = huConfig{n: 9, name: "normal", card: wanNames, huLian: true}
	huFengCfg   = huConfig{n: 4, name: "feng", card: fengNames, huLian: false}
	huJianCfg   = huConfig{n: 3, name: "jian", card: jianNames, huLian: false}
)

// HuLoad 加载判胡表(对应 Java HuUtil.load)。文件不存在或格式错误时返回错误。
func HuLoad() error {
	if err := loadHuTable(HuTableJian, huJianCfg); err != nil {
		return err
	}
	if err := loadHuTable(HuTableFeng, huFengCfg); err != nil {
		return err
	}
	return loadHuTable(HuTable, huNormalCfg)
}

func loadHuTable(m map[int64][]HuTableInfo, cfg huConfig) error {
	for k := range m {
		delete(m, k)
	}
	path, err := findDataFile("majiang_clien_" + cfg.name + ".txt")
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
		if len(fields) < 4 {
			return fmt.Errorf("%s 格式错误: %q", path, line)
		}
		key, err := strconv.ParseInt(fields[0], 10, 64)
		if err != nil {
			return fmt.Errorf("%s 格式错误: %q: %v", path, line, err)
		}
		gui, _ := strconv.Atoi(fields[1])
		jiang, _ := strconv.Atoi(fields[2])
		hu, err := strconv.Atoi(fields[3])
		if err != nil {
			return fmt.Errorf("%s 格式错误: %q: %v", path, line, err)
		}

		info := HuTableInfo{NeedGui: gui, Jiang: jiang != 0}
		if hu != -1 {
			info.Hupai = make([]int8, cfg.n)
			tmp := int64(hu)
			for i := 0; i < cfg.n; i++ {
				info.Hupai[cfg.n-1-i] = int8(tmp % 10)
				tmp /= 10
			}
		}
		m[key] = append(m[key], info)
	}
	return scanner.Err()
}

// huInfoKey 对应 Java HuInfo(HashSet 去重用)。
type huInfoKey struct {
	needGui int
	jiang   int
	hupai   int
}

// genHuInto 在内存中重新生成一张判胡表(等价 Java HuCommon.check_hu 的全量枚举),
// 结果写入 m;不写文件。genHu/gen 测试共用。
func genHuInto(m map[int64][]HuTableInfo, cfg huConfig) {
	for k := range m {
		delete(m, k)
	}
	for card := range genCardSetAll(cfg.n) {
		checkHuLong(m, cfg, card)
	}
}

func checkHuLong(m map[int64][]HuTableInfo, cfg huConfig, card int64) {
	n := cfg.n
	num := keyDigits(card, n)
	total := 0
	for _, v := range num {
		total += v
	}

	huInfos := make(map[huInfoKey]struct{})
	for guinum := 0; guinum <= 8 && total+guinum <= 14; guinum++ {
		tmpcard := genCardSet(n, guinum)
		for tmpgui := range tmpcard {
			tmpguinum := keyDigits(tmpgui, n)
			max := false
			for i := 0; i < n; i++ {
				num[i] += tmpguinum[i]
				if num[i] > 4 {
					max = true
				}
			}
			if !max {
				checkHuRec(cfg, huInfos, num, -1, -1, guinum)
			}
			for i := 0; i < n && !max; i++ {
				num[i]++
				if num[i] <= 4 {
					checkHuRec(cfg, huInfos, num, -1, i, guinum)
				}
				num[i]--
			}
			for i := 0; i < n; i++ {
				num[i] -= tmpguinum[i]
			}
		}
	}

	type mk struct {
		needGui int
		jiang   bool
	}
	merges := make(map[mk]*HuTableInfo)
	for info := range huInfos {
		k := mk{info.needGui, info.jiang != -1}
		cur := merges[k]
		if cur == nil {
			cur = &HuTableInfo{NeedGui: info.needGui, Jiang: info.jiang != -1}
			if info.hupai == -1 {
				cur.Hupai = nil
			} else {
				cur.Hupai = make([]int8, n)
				cur.Hupai[info.hupai]++
			}
			merges[k] = cur
		} else {
			if info.hupai == -1 {
				cur.Hupai = nil
			} else if cur.Hupai != nil && cur.Hupai[info.hupai] == 0 {
				cur.Hupai[info.hupai]++
			}
		}
	}

	list := make([]HuTableInfo, 0, len(merges))
	for _, v := range merges {
		list = append(list, *v)
	}
	sort.Slice(list, func(i, j int) bool {
		if list[i].NeedGui != list[j].NeedGui {
			return list[i].NeedGui < list[j].NeedGui
		}
		if list[i].Jiang != list[j].Jiang {
			return !list[i].Jiang
		}
		return huDigitFold(list[i].Hupai) < huDigitFold(list[j].Hupai)
	})
	m[card] = list
}

func checkHuRec(cfg huConfig, huInfos map[huInfoKey]struct{}, num []int, jiang int, in int, gui int) {
	n := cfg.n
	if cfg.huLian {
		for i := 0; i+2 < n; i++ {
			if num[i] > 0 && num[i+1] > 0 && num[i+2] > 0 {
				num[i]--
				num[i+1]--
				num[i+2]--
				checkHuRec(cfg, huInfos, num, jiang, in, gui)
				num[i]++
				num[i+1]++
				num[i+2]++
			}
		}
	}
	for i := 0; i < n; i++ {
		if num[i] >= 2 && jiang == -1 {
			num[i] -= 2
			checkHuRec(cfg, huInfos, num, i, in, gui)
			num[i] += 2
		}
	}
	for i := 0; i < n; i++ {
		if num[i] >= 3 {
			num[i] -= 3
			checkHuRec(cfg, huInfos, num, jiang, in, gui)
			num[i] += 3
		}
	}
	for i := 0; i < n; i++ {
		if num[i] != 0 {
			return
		}
	}
	huInfos[huInfoKey{needGui: gui, jiang: jiang, hupai: in}] = struct{}{}
}

// huDigitFold 把 hupai 数组折叠成 Java 输出格式的整数(各位为 0/1 标记)。
func huDigitFold(hupai []int8) int64 {
	if hupai == nil {
		return -1
	}
	var hu int64
	for _, v := range hupai {
		hu = hu*10 + int64(v)
	}
	return hu
}

// genHu 重新生成全部判胡查表文件(majiang_clien_*.txt 与 majiang_server_*.txt),
// 写入当前目录,等价 Java HuUtil.gen()(不含 sqlite 的 majiang.db 输出)。
// 与 Java 不同,Go 输出按键与条目排序,内容与 Java 生成的一致。
func genHu() error {
	type huTable struct {
		m   map[int64][]HuTableInfo
		cfg huConfig
	}
	tables := []huTable{{HuTableJian, huJianCfg}, {HuTableFeng, huFengCfg}, {HuTable, huNormalCfg}}
	for _, t := range tables {
		genHuInto(t.m, t.cfg)
		if err := writeHuClientFile(t.cfg, t.m); err != nil {
			return err
		}
		if err := writeHuServerFile(t.cfg, t.m); err != nil {
			return err
		}
	}
	return nil
}

func sortedHuKeys(m map[int64][]HuTableInfo) []int64 {
	keys := make([]int64, 0, len(m))
	for k := range m {
		keys = append(keys, k)
	}
	sort.Slice(keys, func(i, j int) bool { return keys[i] < keys[j] })
	return keys
}

func writeHuClientFile(cfg huConfig, m map[int64][]HuTableInfo) error {
	lines := buildHuClientLines(cfg, m)
	name := "majiang_clien_" + cfg.name + ".txt"
	return writeLines(name, lines)
}

func buildHuClientLines(cfg huConfig, m map[int64][]HuTableInfo) []string {
	var lines []string
	for _, key := range sortedHuKeys(m) {
		for _, info := range m[key] {
			lines = append(lines, fmt.Sprintf("%d %d %d %d", key, info.NeedGui, boolToInt(info.Jiang), huDigitFold(info.Hupai)))
		}
	}
	return lines
}

func buildHuServerLines(cfg huConfig, m map[int64][]HuTableInfo) []string {
	var lines []string
	for _, key := range sortedHuKeys(m) {
		for _, info := range m[key] {
			var b strings.Builder
			b.WriteString(strconv.FormatInt(key, 10))
			b.WriteString(" ")
			b.WriteString(strconv.Itoa(info.NeedGui))
			b.WriteString(" ")
			if info.Jiang {
				b.WriteString("1 ")
			} else {
				b.WriteString("0 ")
			}
			if info.Hupai == nil {
				b.WriteString("-1")
			} else {
				b.WriteString(strconv.FormatInt(huDigitFold(info.Hupai), 10))
			}
			b.WriteString(" ")
			b.WriteString(showCard(cfg, key))
			b.WriteString(" ")
			b.WriteString("鬼" + strconv.Itoa(info.NeedGui))
			b.WriteString(" ")
			if info.Jiang {
				b.WriteString("有将 ")
			} else {
				b.WriteString("无将 ")
			}
			if info.Hupai == nil {
				b.WriteString("胡了")
			} else {
				for index, v := range info.Hupai {
					if v > 0 {
						b.WriteString("胡" + cfg.card[index])
					}
				}
			}
			lines = append(lines, b.String())
		}
	}
	return lines
}

func writeHuServerFile(cfg huConfig, m map[int64][]HuTableInfo) error {
	name := "majiang_server_" + cfg.name + ".txt"
	return writeLines(name, buildHuServerLines(cfg, m))
}

func writeLines(name string, lines []string) error {
	f, err := os.Create(name)
	if err != nil {
		return err
	}
	defer f.Close()
	w := bufio.NewWriter(f)
	for _, line := range lines {
		if _, err := w.WriteString(line + "\n"); err != nil {
			return err
		}
	}
	return w.Flush()
}

// showCard 等价 Java HuCommon.show_card。
func showCard(cfg huConfig, key int64) string {
	num := keyDigits(key, cfg.n)
	ret := ""
	for index, count := range num {
		for j := 0; j < count; j++ {
			ret += cfg.card[index]
		}
	}
	return ret
}

func boolToInt(b bool) int {
	if b {
		return 1
	}
	return 0
}

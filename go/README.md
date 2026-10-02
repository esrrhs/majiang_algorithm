# majiang_algorithm Go 实现

Java 版 [majiang_algorithm](../java) 的 Go 移植,基于查表的高性能麻将判胡、听牌与 AI 决策。
与 Java 版共用 [data/](../data) 下的同一批查表文件,行为逐位对齐(含浮点评分)。

## 使用

```bash
go get github.com/esrrhs/majiang_algorithm/go
```

```go
package main

import (
	majiang "github.com/esrrhs/majiang_algorithm/go"
)

func main() {
	// 加载预计算表(依次在当前目录、./data、../data 查找)
	majiang.Load()

	cards := majiang.StringToCards("1万,2万,3万,东,东")
	gui := majiang.StringToCard("东")

	isHu := majiang.IsHu(cards, gui)             // 判断胡牌
	ting := majiang.IsTing(cards, []int{gui})    // 听牌列表
	out := majiang.OutAI(cards, []int{gui})      // AI 出牌
	peng := majiang.PengAI(cards, []int{gui}, out, 0) // 是否碰
	gang := majiang.GangAI(cards, []int{gui}, out, 0) // 是否杠
}
```

## 测试

```bash
cd go && go test ./...
```

- `def_test.go` / `huutil_test.go` / `aiutil_test.go`:与 Java 侧 JUnit 用例一一对应的移植。
- `gen_test.go`:内存重新生成 jian/feng 判胡表与 AI 表,与仓库已提交的查表文件逐行比对。
- `parity_test.go`:回放 `testdata/parity_cases.txt`(Java `ParityDump` 以固定种子导出的
  2000+ 局样例),校验 Go 与 Java 的 isHu / isTing / calc / outAI / chiAI / pengAI / gangAI 完全一致。

## 表生成

```go
majiang.HuGen() // 输出 majiang_clien_*.txt 与 majiang_server_*.txt 到当前目录
majiang.AiGen() // 输出 majiang_ai_*.txt 到当前目录
```

与 Java 的差异:Go 侧不输出 sqlite 的 `majiang.db`;输出行按键排序(Java 为多线程乱序写入),内容一致。

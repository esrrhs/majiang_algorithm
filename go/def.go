// Package majiang 是 majiang_algorithm Java 版的 Go 移植:
// 基于查表的高性能麻将判胡、听牌与出牌/吃/碰/杠 AI。
// 查表文件与 Java 版完全共用(仓库根目录 data/ 下),行为逐位对齐。
package majiang

import (
	"strconv"
	"strings"
)

// 牌的编号定义,与 Java MaJiangDef 常量一一对应。
const (
	Wan1 = 1
	Wan2 = 2
	Wan3 = 3
	Wan4 = 4
	Wan5 = 5
	Wan6 = 6
	Wan7 = 7
	Wan8 = 8
	Wan9 = 9

	Tong1 = 10
	Tong2 = 11
	Tong3 = 12
	Tong4 = 13
	Tong5 = 14
	Tong6 = 15
	Tong7 = 16
	Tong8 = 17
	Tong9 = 18

	Tiao1 = 19
	Tiao2 = 20
	Tiao3 = 21
	Tiao4 = 22
	Tiao5 = 23
	Tiao6 = 24
	Tiao7 = 25
	Tiao8 = 26
	Tiao9 = 27

	FengDong = 28
	FengNan  = 29
	FengXi   = 30
	FengBei  = 31

	JianZhong = 32
	JianFa    = 33
	JianBai   = 34

	HuaChun = 35
	HuaXia  = 36
	HuaQiu  = 37
	HuaDong = 38
	HuaMei  = 39
	HuaLan  = 40
	HuaZhu  = 41
	HuaJu   = 42

	MaxNum = 42

	TypeWan  = 1
	TypeTong = 2
	TypeTiao = 3
	TypeFeng = 4
	TypeJian = 5
	TypeHua  = 6
)

var fengJianHuaNames = []string{"东", "南", "西", "北", "中", "发", "白", "春", "夏", "秋", "冬", "梅", "兰", "竹", "菊"}

// ToCard 等价 Java MaJiangDef.toCard。
func ToCard(t int, index int) int {
	switch t {
	case TypeWan:
		return Wan1 + index
	case TypeTong:
		return Tong1 + index
	case TypeTiao:
		return Tiao1 + index
	case TypeFeng:
		return FengDong + index
	case TypeJian:
		return JianZhong + index
	case TypeHua:
		return HuaChun + index
	}
	return 0
}

// CardToString 等价 Java MaJiangDef.cardToString。
func CardToString(card int) string {
	if card >= Wan1 && card <= Wan9 {
		return strconv.Itoa(card-Wan1+1) + "万"
	}
	if card >= Tong1 && card <= Tong9 {
		return strconv.Itoa(card-Tong1+1) + "筒"
	}
	if card >= Tiao1 && card <= Tiao9 {
		return strconv.Itoa(card-Tiao1+1) + "条"
	}
	if card >= FengDong && card <= MaxNum {
		return fengJianHuaNames[card-FengDong]
	}
	return "错误" + strconv.Itoa(card)
}

// CardsToString 等价 Java MaJiangDef.cardsToString。
func CardsToString(card []int) string {
	if card == nil {
		return ""
	}
	ret := ""
	for _, c := range card {
		ret += CardToString(c) + ","
	}
	return ret
}

// StringToCard 等价 Java MaJiangDef.stringToCard。
// 与 Java 的解析保持一致:数牌只取字符串第一个字符作为数字。
func StringToCard(str string) int {
	if strings.Contains(str, "万") {
		return Wan1 - 1 + leadingDigit(str)
	}
	if strings.Contains(str, "筒") {
		return Tong1 - 1 + leadingDigit(str)
	}
	if strings.Contains(str, "条") {
		return Tiao1 - 1 + leadingDigit(str)
	}
	c := FengDong
	for _, s := range fengJianHuaNames {
		if strings.Contains(str, s) {
			return c
		}
		c++
	}
	return 0
}

// StringToCards 等价 Java MaJiangDef.stringToCards。
func StringToCards(str string) []int {
	var ret []int
	for _, s := range strings.Split(str, ",") {
		if len(s) > 0 {
			ret = append(ret, StringToCard(s))
		}
	}
	return ret
}

// leadingDigit 对应 Java Integer.parseInt(str.substring(0, 1)):
// 取字符串第一个字符按数字解析,非数字时返回 0。
func leadingDigit(str string) int {
	if len(str) == 0 {
		return 0
	}
	if c := str[0]; c >= '0' && c <= '9' {
		return int(c - '0')
	}
	return 0
}

// CardType 等价 Java MaJiangDef.type(Java 的 type 在 Go 里是关键字,故更名)。
func CardType(card int) int {
	if card >= Wan1 && card <= Wan9 {
		return TypeWan
	}
	if card >= Tong1 && card <= Tong9 {
		return TypeTong
	}
	if card >= Tiao1 && card <= Tiao9 {
		return TypeTiao
	}
	if card >= FengDong && card <= FengBei {
		return TypeFeng
	}
	if card >= JianZhong && card <= JianBai {
		return TypeJian
	}
	if card >= HuaChun && card <= HuaJu {
		return TypeHua
	}
	return 0
}

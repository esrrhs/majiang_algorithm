package majiang

import (
	"strings"
	"testing"
)

// 以下三个测试移植自 Java MaJiangDefTest,断言一一对应。

func TestCardConstantsAndTypes(t *testing.T) {
	if CardType(Wan1) != TypeWan || CardType(Wan9) != TypeWan {
		t.Errorf("wan type")
	}
	if CardType(Tong1) != TypeTong || CardType(Tong9) != TypeTong {
		t.Errorf("tong type")
	}
	if CardType(Tiao1) != TypeTiao || CardType(Tiao9) != TypeTiao {
		t.Errorf("tiao type")
	}
	if CardType(FengDong) != TypeFeng || CardType(FengBei) != TypeFeng {
		t.Errorf("feng type")
	}
	if CardType(JianZhong) != TypeJian || CardType(JianBai) != TypeJian {
		t.Errorf("jian type")
	}
	if CardType(HuaChun) != TypeHua || CardType(HuaJu) != TypeHua {
		t.Errorf("hua type")
	}
	if CardType(999) != 0 {
		t.Errorf("invalid card type should be 0")
	}
}

func TestToCard(t *testing.T) {
	if ToCard(TypeWan, 0) != Wan1 {
		t.Errorf("ToCard wan1")
	}
	if ToCard(TypeWan, 4) != Wan5 {
		t.Errorf("ToCard wan5")
	}
	if ToCard(TypeTong, 0) != Tong1 {
		t.Errorf("ToCard tong1")
	}
	if ToCard(TypeTiao, 8) != Tiao9 {
		t.Errorf("ToCard tiao9")
	}
	if ToCard(TypeFeng, 0) != FengDong {
		t.Errorf("ToCard feng")
	}
	if ToCard(TypeJian, 0) != JianZhong {
		t.Errorf("ToCard jian")
	}
	if ToCard(TypeHua, 0) != HuaChun {
		t.Errorf("ToCard hua")
	}
	if ToCard(99, 0) != 0 {
		t.Errorf("ToCard invalid should be 0")
	}
}

func TestCardToStringAndReverse(t *testing.T) {
	cases := []struct {
		card int
		str  string
	}{
		{Wan1, "1万"},
		{Wan9, "9万"},
		{Tong5, "5筒"},
		{Tiao3, "3条"},
		{FengDong, "东"},
		{JianZhong, "中"},
		{HuaJu, "菊"},
	}
	for _, c := range cases {
		if got := CardToString(c.card); got != c.str {
			t.Errorf("CardToString(%d) = %q, want %q", c.card, got, c.str)
		}
		if got := StringToCard(c.str); got != c.card {
			t.Errorf("StringToCard(%q) = %d, want %d", c.str, got, c.card)
		}
	}
	if got := CardToString(999); got != "错误999" {
		t.Errorf("CardToString(999) = %q", got)
	}
	if got := StringToCard("未知"); got != 0 {
		t.Errorf("StringToCard(未知) = %d, want 0", got)
	}
}

func TestCardsToStringAndReverse(t *testing.T) {
	cards := []int{Wan1, Wan2, Wan3}
	if got := CardsToString(cards); got != "1万,2万,3万," {
		t.Errorf("CardsToString = %q", got)
	}

	parsed := StringToCards("1万,2万,3万")
	if len(parsed) != len(cards) {
		t.Fatalf("StringToCards len = %d", len(parsed))
	}
	for i := range cards {
		if parsed[i] != cards[i] {
			t.Errorf("StringToCards[%d] = %d, want %d", i, parsed[i], cards[i])
		}
	}

	// Java 侧用 HashSet 去重后输出,此处同样按去重后的集合断言
	set := map[int]struct{}{}
	for _, c := range cards {
		set[c] = struct{}{}
	}
	var uniq []int
	for c := range set {
		uniq = append(uniq, c)
	}
	setStr := CardsToString(uniq)
	for _, want := range []string{"1万,", "2万,", "3万,"} {
		if !strings.Contains(setStr, want) {
			t.Errorf("setStr %q should contain %q", setStr, want)
		}
	}

	if CardsToString(nil) != "" {
		t.Errorf("CardsToString(nil) should be empty")
	}
	if got := StringToCards(""); len(got) != 0 {
		t.Errorf("StringToCards(\"\") should be empty, got %v", got)
	}
}

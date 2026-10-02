package majiang

import "math"

// Calc 计算手牌向胡牌方向推进的评分(对应 Java AIUtil.calc)。
// 已听牌时返回 听牌数*10,否则为各花色 AI 表组合出的最大几率。
func Calc(input []int, guiCard []int) float64 {
	cards := countCards(input)

	guiNum := 0
	for _, gui := range guiCard {
		guiNum += cards[gui-1]
		cards[gui-1] = 0
	}

	ting := IsTingCard(cards, guiNum)
	if len(ting) != 0 {
		return float64(len(ting) * 10)
	}

	var wanKey, tongKey, tiaoKey, fengKey, jianKey int64
	for i := Wan1; i <= Wan9; i++ {
		wanKey = wanKey*10 + int64(cards[i-1])
	}
	for i := Tong1; i <= Tong9; i++ {
		tongKey = tongKey*10 + int64(cards[i-1])
	}
	for i := Tiao1; i <= Tiao9; i++ {
		tiaoKey = tiaoKey*10 + int64(cards[i-1])
	}
	for i := FengDong; i <= FengBei; i++ {
		fengKey = fengKey*10 + int64(cards[i-1])
	}
	for i := JianZhong; i <= JianBai; i++ {
		jianKey = jianKey*10 + int64(cards[i-1])
	}

	tmp := [][]AITableInfo{
		AITable[wanKey],
		AITable[tongKey],
		AITable[tiaoKey],
		AITableFeng[fengKey],
		AITableJian[jianKey],
	}

	var ret []float64
	calcAITableInfo(&ret, tmp, 0, false, 0)

	if len(ret) == 0 {
		return 0
	}

	d := ret[0]
	for _, v := range ret[1:] {
		if v > d {
			d = v
		}
	}
	return d
}

func calcAITableInfo(ret *[]float64, tmp [][]AITableInfo, index int, jiang bool, cur float64) {
	if index >= len(tmp) {
		if jiang {
			*ret = append(*ret, cur)
		}
		return
	}
	aiTableInfos := tmp[index]
	if aiTableInfos == nil {
		return
	}
	for _, aiTableInfo := range aiTableInfos {
		if jiang {
			if !aiTableInfo.Jiang {
				calcAITableInfo(ret, tmp, index+1, jiang, cur+aiTableInfo.P)
			}
		} else {
			calcAITableInfo(ret, tmp, index+1, aiTableInfo.Jiang, cur+aiTableInfo.P)
		}
	}
}

// OutAI 选出应打出的牌(对应 Java AIUtil.outAI);全为鬼牌时返回 0。
func OutAI(input []int, guiCard []int) int {
	ret := 0
	max := -math.MaxFloat64
	var cache [MaxNum + 1]int
	for _, c := range input {
		if cache[c] == 0 {
			if !containsInt(guiCard, c) {
				tmp := removeFirst(input, c)
				score := Calc(tmp, guiCard)
				if score > max {
					max = score
					ret = c
				}
			}
		}
		cache[c] = 1
	}
	return ret
}

// ChiAI 判断是否应该吃(card1、card2 为组成顺子的另外两张牌,对应 Java AIUtil.chiAI 布尔版)。
func ChiAI(input []int, guiCard []int, card int, card1 int, card2 int) bool {
	if containsInt(guiCard, card) || containsInt(guiCard, card1) || containsInt(guiCard, card2) {
		return false
	}

	if frequency(input, card1) < 1 || frequency(input, card2) < 1 {
		return false
	}

	score := Calc(input, guiCard)

	tmp := removeFirst(removeFirst(input, card1), card2)
	scoreNew := Calc(tmp, guiCard)

	return scoreNew >= score
}

// ChiAIChoices 返回吃 card 时应用的两张牌(对应 Java AIUtil.chiAI 列表版);不该吃时为空。
func ChiAIChoices(input []int, guiCard []int, card int) []int {
	var ret []int
	if containsInt(guiCard, card) {
		return ret
	}

	score := Calc(input, guiCard)
	scoreNewMax := 0.0

	card1 := 0
	card2 := 0

	if frequency(input, card-2) > 0 && frequency(input, card-1) > 0 &&
		CardType(card) == CardType(card-2) && CardType(card) == CardType(card-1) {
		tmp := removeFirst(removeFirst(input, card-2), card-1)
		scoreNew := Calc(tmp, guiCard)
		if scoreNew > scoreNewMax {
			scoreNewMax = scoreNew
			card1 = card - 2
			card2 = card - 1
		}
	}

	if frequency(input, card-1) > 0 && frequency(input, card+1) > 0 &&
		CardType(card) == CardType(card-1) && CardType(card) == CardType(card+1) {
		tmp := removeFirst(removeFirst(input, card-1), card+1)
		scoreNew := Calc(tmp, guiCard)
		if scoreNew > scoreNewMax {
			scoreNewMax = scoreNew
			card1 = card - 1
			card2 = card + 1
		}
	}

	if frequency(input, card+1) > 0 && frequency(input, card+2) > 0 &&
		CardType(card) == CardType(card+1) && CardType(card) == CardType(card+2) {
		tmp := removeFirst(removeFirst(input, card+1), card+2)
		scoreNew := Calc(tmp, guiCard)
		if scoreNew > scoreNewMax {
			scoreNewMax = scoreNew
			card1 = card + 1
			card2 = card + 2
		}
	}

	if scoreNewMax > score {
		ret = append(ret, card1, card2)
	}

	return ret
}

// PengAI 判断是否应该碰(对应 Java AIUtil.pengAI)。
func PengAI(input []int, guiCard []int, card int, award float64) bool {
	if containsInt(guiCard, card) {
		return false
	}

	if frequency(input, card) < 2 {
		return false
	}

	score := Calc(input, guiCard)

	tmp := removeFirst(removeFirst(input, card), card)
	scoreNew := Calc(tmp, guiCard)

	return scoreNew+award >= score
}

// GangAI 判断是否应该杠(对应 Java AIUtil.gangAI)。
func GangAI(input []int, guiCard []int, card int, award float64) bool {
	if containsInt(guiCard, card) {
		return false
	}

	if frequency(input, card) < 3 {
		return false
	}

	score := Calc(input, guiCard)

	tmp := input
	tmp = removeFirst(tmp, card)
	tmp = removeFirst(tmp, card)
	tmp = removeFirst(tmp, card)
	tmp = removeFirst(tmp, card)
	scoreNew := Calc(tmp, guiCard)

	return scoreNew+award >= score
}

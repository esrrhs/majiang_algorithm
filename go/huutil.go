package majiang

// IsHu 判断手牌是否胡(单鬼牌,对应 Java HuUtil.isHu)。
func IsHu(input []int, guiCard int) bool {
	cards := countCards(input)
	guiNum := cards[guiCard-1]
	cards[guiCard-1] = 0
	return IsHuCard(cards, guiNum)
}

// IsHuExtra 判断手牌是否胡(多鬼牌 + extra 进牌,对应 Java HuUtil.isHuExtra)。
func IsHuExtra(input []int, guiCard []int, extra int) bool {
	cards := countCards(input)

	guiNum := 0
	for _, gui := range guiCard {
		guiNum += cards[gui-1]
		cards[gui-1] = 0
	}

	if extra != 0 {
		cards[extra-1]++
	}

	return IsHuCard(cards, guiNum)
}

// IsHuCard 基于计数数组的查表判胡(对应 Java HuUtil.isHuCard)。
func IsHuCard(cards []int, guiNum int) bool {
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

	var tmp [][]HuTableInfo
	if wanKey != 0 {
		tmp = append(tmp, HuTable[wanKey])
	}
	if tongKey != 0 {
		tmp = append(tmp, HuTable[tongKey])
	}
	if tiaoKey != 0 {
		tmp = append(tmp, HuTable[tiaoKey])
	}
	if fengKey != 0 {
		tmp = append(tmp, HuTableFeng[fengKey])
	}
	if jianKey != 0 {
		tmp = append(tmp, HuTableJian[jianKey])
	}

	var tmp1 [][]HuTableInfo
	for _, huTableInfos := range tmp {
		if len(huTableInfos) == 0 {
			return false
		}
		var tmp2 []HuTableInfo
		for _, huTableInfo := range huTableInfos {
			if huTableInfo.Hupai == nil && huTableInfo.NeedGui <= guiNum {
				tmp2 = append(tmp2, huTableInfo)
			}
		}
		if len(tmp2) == 0 {
			return false
		}
		tmp1 = append(tmp1, tmp2)
	}

	return isHuTableInfo(tmp1, 0, guiNum, false)
}

func isHuTableInfo(tmp [][]HuTableInfo, index int, guiNum int, jiang bool) bool {
	if index >= len(tmp) {
		return guiNum%3 == 0 && jiang || guiNum%3 == 2 && !jiang
	}
	for _, huTableInfo := range tmp[index] {
		if jiang {
			if huTableInfo.Hupai == nil && huTableInfo.NeedGui <= guiNum && !huTableInfo.Jiang {
				if isHuTableInfo(tmp, index+1, guiNum-huTableInfo.NeedGui, jiang) {
					return true
				}
			}
		} else {
			if huTableInfo.Hupai == nil && huTableInfo.NeedGui <= guiNum {
				if isHuTableInfo(tmp, index+1, guiNum-huTableInfo.NeedGui, huTableInfo.Jiang) {
					return true
				}
			}
		}
	}
	return false
}

// IsTing 返回手牌的听牌列表(单鬼牌,对应 Java HuUtil.isTing)。
func IsTing(input []int, guiCard int) []int {
	cards := countCards(input)
	guiNum := cards[guiCard-1]
	cards[guiCard-1] = 0
	return IsTingCard(cards, guiNum)
}

// IsTingExtra 返回手牌的听牌列表(多鬼牌,对应 Java HuUtil.isTingExtra)。
func IsTingExtra(input []int, guiCard []int) []int {
	cards := countCards(input)

	guiNum := 0
	for _, gui := range guiCard {
		guiNum += cards[gui-1]
		cards[gui-1] = 0
	}

	return IsTingCard(cards, guiNum)
}

// IsTingCard 基于计数数组的查表听牌(对应 Java HuUtil.isTingCard)。
func IsTingCard(cards []int, guiNum int) []int {
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

	var tmpType []int
	var tmpTing [][]HuTableInfo
	var tmp [][]HuTableInfo

	wanHuTableInfo := HuTable[wanKey]
	if wanHuTableInfo == nil {
		return []int{}
	}
	tmpTing = append(tmpTing, wanHuTableInfo)
	if wanKey != 0 {
		tmpType = append(tmpType, TypeWan)
		tmp = append(tmp, wanHuTableInfo)
	}
	tongHuTableInfo := HuTable[tongKey]
	if tongHuTableInfo == nil {
		return []int{}
	}
	tmpTing = append(tmpTing, tongHuTableInfo)
	if tongKey != 0 {
		tmpType = append(tmpType, TypeTong)
		tmp = append(tmp, tongHuTableInfo)
	}
	tiaoHuTableInfo := HuTable[tiaoKey]
	if tiaoHuTableInfo == nil {
		return []int{}
	}
	tmpTing = append(tmpTing, tiaoHuTableInfo)
	if tiaoKey != 0 {
		tmpType = append(tmpType, TypeTiao)
		tmp = append(tmp, tiaoHuTableInfo)
	}
	fengHuTableInfo := HuTableFeng[fengKey]
	if fengHuTableInfo == nil {
		return []int{}
	}
	tmpTing = append(tmpTing, fengHuTableInfo)
	if fengKey != 0 {
		tmpType = append(tmpType, TypeFeng)
		tmp = append(tmp, fengHuTableInfo)
	}
	jianHuTableInfo := HuTableJian[jianKey]
	if jianHuTableInfo == nil {
		return []int{}
	}
	tmpTing = append(tmpTing, jianHuTableInfo)
	if jianKey != 0 {
		tmpType = append(tmpType, TypeJian)
		tmp = append(tmp, jianHuTableInfo)
	}

	var ret []int
	for t := TypeWan; t <= TypeJian; t++ {
		huTableInfos := tmpTing[t-1]
		var cache [9]int
		for _, huTableInfo := range huTableInfos {
			if huTableInfo.Hupai != nil && huTableInfo.NeedGui <= guiNum {
				cached := true
				for j := 0; j < len(huTableInfo.Hupai); j++ {
					if huTableInfo.Hupai[j] > 0 && cache[j] == 0 {
						cached = false
						break
					}
				}

				if !cached && isTingHuTableInfo(tmpType, tmp, 0, guiNum-huTableInfo.NeedGui, huTableInfo.Jiang, t) {
					for j := 0; j < len(huTableInfo.Hupai); j++ {
						if huTableInfo.Hupai[j] > 0 {
							if cache[j] == 0 {
								ret = append(ret, ToCard(t, j))
							}
							cache[j]++
						}
					}
				}
			}
		}
	}
	return ret
}

func isTingHuTableInfo(tmpType []int, tmp [][]HuTableInfo, index int, guiNum int, jiang bool, tingType int) bool {
	if index >= len(tmp) {
		return guiNum == 0 && jiang
	}
	if tmpType[index] == tingType {
		return isTingHuTableInfo(tmpType, tmp, index+1, guiNum, jiang, tingType)
	}
	for _, huTableInfo := range tmp[index] {
		if huTableInfo.Hupai == nil && huTableInfo.NeedGui <= guiNum {
			if jiang {
				if !huTableInfo.Jiang {
					if isTingHuTableInfo(tmpType, tmp, index+1, guiNum-huTableInfo.NeedGui, jiang, tingType) {
						return true
					}
				}
			} else {
				if isTingHuTableInfo(tmpType, tmp, index+1, guiNum-huTableInfo.NeedGui, huTableInfo.Jiang, tingType) {
					return true
				}
			}
		}
	}
	return false
}

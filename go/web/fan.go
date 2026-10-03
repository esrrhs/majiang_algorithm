// 麻将番型计算器(参考腾讯麻将/大众麻将规则),对应 Java web.FanCalculator。
package web

import (
	"math"
	"strconv"

	majiang "github.com/esrrhs/majiang_algorithm/go"
)

type FanResult struct {
	patterns []string
	totalFan int
	points   int
}

func (r *FanResult) addPattern(name string, fan int) {
	r.patterns = append(r.patterns, name+" ("+strconv.Itoa(fan)+"番)")
	r.totalFan += fan
}

func (r *FanResult) GetPatterns() []string {
	return r.patterns
}

func (r *FanResult) GetTotalFan() int {
	if r.totalFan < 1 {
		return 1
	}
	return r.totalFan
}

func (r *FanResult) GetPoints() int {
	return r.points
}

func (r *FanResult) setPoints(points int) {
	r.points = points
}

// calculateFan 计算胡牌番型,对应 Java FanCalculator.calculateFan。
func calculateFan(player *Player, winCard int, isZimo bool, isGangShangHua bool, isHaiDi bool,
	isTianHu bool, isDiHu bool, guiCards []int) *FanResult {
	result := &FanResult{}

	if isTianHu {
		result.addPattern("天胡", 16)
		result.setPoints(16 * 10)
		return result
	}
	if isDiHu {
		result.addPattern("地胡", 16)
		result.setPoints(16 * 10)
		return result
	}

	allTiles := player.getAllTiles()
	if !isZimo && winCard > 0 {
		allTiles = append([]int{}, allTiles...)
		allTiles = append(allTiles, winCard)
	}

	// 1. 清一色 与 混一色
	isQingYiSe := checkQingYiSe(allTiles, guiCards)
	isHunYiSe := !isQingYiSe && checkHunYiSe(allTiles, guiCards)

	if isQingYiSe {
		result.addPattern("清一色", 8)
	} else if isHunYiSe {
		result.addPattern("混一色", 4)
	}

	// 2. 七对判定(手牌14张且无副牌)
	if len(player.getMelds()) == 0 && len(allTiles) == 14 {
		if checkQiDui(allTiles, guiCards) {
			result.addPattern("七对", 8)
		}
	}

	// 3. 碰碰胡判定(副牌全为碰/杠,手牌只剩刻子和将)
	if checkPengPengHu(player, winCard, isZimo, guiCards) {
		result.addPattern("碰碰胡", 4)
	}

	// 4. 断幺九(无1、9及字牌)
	if checkDuanYaoJiu(allTiles, guiCards) {
		result.addPattern("断幺九", 2)
	}

	// 5. 门前清(没有吃碰明杠,允许暗杠)
	menQianQing := true
	for _, m := range player.getMelds() {
		if m.Type != "AN_GANG" {
			menQianQing = false
			break
		}
	}
	if menQianQing && !isZimo {
		result.addPattern("门前清", 2)
	}

	// 6. 杠上开花
	if isGangShangHua {
		result.addPattern("杠上开花", 2)
	}

	// 7. 海底捞月
	if isHaiDi {
		result.addPattern("海底捞月", 2)
	}

	// 8. 自摸
	if isZimo {
		result.addPattern("自摸", 1)
	}

	// 基础平胡
	if len(result.patterns) == 0 {
		result.addPattern("平胡", 1)
	}

	baseScore := 10
	points := baseScore * int(math.Pow(2, float64(minInt(6, result.GetTotalFan()-1))))
	result.setPoints(points)

	return result
}

func checkQingYiSe(tiles []int, guiCards []int) bool {
	colorType := 0
	for _, c := range tiles {
		if containsInt(guiCards, c) {
			continue // 鬼牌可算作同花色
		}
		t := majiang.CardType(c)
		if t == majiang.TypeFeng || t == majiang.TypeJian {
			return false
		}
		if colorType == 0 {
			colorType = t
		} else if colorType != t {
			return false
		}
	}
	return colorType != 0
}

func checkHunYiSe(tiles []int, guiCards []int) bool {
	colorType := 0
	hasZi := false
	for _, c := range tiles {
		if containsInt(guiCards, c) {
			continue
		}
		t := majiang.CardType(c)
		if t == majiang.TypeFeng || t == majiang.TypeJian {
			hasZi = true
		} else {
			if colorType == 0 {
				colorType = t
			} else if colorType != t {
				return false
			}
		}
	}
	return colorType != 0 && hasZi
}

func checkDuanYaoJiu(tiles []int, guiCards []int) bool {
	for _, c := range tiles {
		if containsInt(guiCards, c) {
			continue
		}
		t := majiang.CardType(c)
		if t == majiang.TypeFeng || t == majiang.TypeJian {
			return false
		}
		if c == majiang.Wan1 || c == majiang.Wan9 ||
			c == majiang.Tong1 || c == majiang.Tong9 ||
			c == majiang.Tiao1 || c == majiang.Tiao9 {
			return false
		}
	}
	return true
}

func checkQiDui(tiles []int, guiCards []int) bool {
	counts := map[int]int{}
	guiCount := 0
	for _, c := range tiles {
		if containsInt(guiCards, c) {
			guiCount++
		} else {
			counts[c]++
		}
	}
	singleCount := 0
	for _, count := range counts {
		if count%2 != 0 {
			singleCount++
		}
	}
	return guiCount >= singleCount
}

func checkPengPengHu(player *Player, winCard int, isZimo bool, guiCards []int) bool {
	// 副牌中不能有"吃"
	for _, m := range player.getMelds() {
		if m.Type == "CHI" {
			return false
		}
	}
	// 手牌数量通常为 2, 5, 8, 11, 14 张
	hand := append([]int{}, player.getHand()...)
	if !isZimo && winCard > 0 {
		hand = append(hand, winCard)
	}
	// 统计手牌非鬼牌频率
	guiCount := 0
	counts := map[int]int{}
	for _, c := range hand {
		if containsInt(guiCards, c) {
			guiCount++
		} else {
			counts[c]++
		}
	}

	// 尝试以某种牌为将(或鬼牌为将)
	// 如果全刻子加一对将,则除将以外的牌都需要满足 3张(或用鬼补足到3张)
	for candidateJiang := range counts {
		needGui := 0
		for card, count := range counts {
			if card == candidateJiang {
				if count < 2 {
					needGui += 2 - count
				} else {
					rem := count - 2
					if rem%3 != 0 {
						needGui += 3 - rem%3
					}
				}
			} else {
				if count%3 != 0 {
					needGui += 3 - count%3
				}
			}
		}
		if needGui <= guiCount && (guiCount-needGui)%3 == 0 {
			return true
		}
	}
	return false
}

func minInt(a, b int) int {
	if a < b {
		return a
	}
	return b
}

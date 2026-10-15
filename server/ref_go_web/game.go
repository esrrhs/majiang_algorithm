// 4人麻将完整游戏对局引擎,对应 Java web.MahjongGame 的逐行移植。
package web

import (
	"math/rand"
	"strconv"

	majiang "github.com/esrrhs/majiang_algorithm/go"
)

// 对应 Java MahjongGame.Phase。
const (
	PhaseDiscard     = "DISCARD"      // 当前玩家出牌
	PhaseResponse    = "RESPONSE"     // 等待其他玩家对所打出的牌进行响应(吃碰杠胡)
	PhaseWaitingUser = "WAITING_USER" // 专门等待真人玩家点击响应按钮(吃碰杠胡过)
	PhaseGameOver    = "GAME_OVER"    // 游戏结束结算
)

var gameIdCounter int = 1000

// Player 对应 Java web.Player。
type Player struct {
	seat          int
	name          string
	isAi          bool
	hand          []int
	melds         []*Meld
	discards      []int
	lastDrawnCard int
	isHu          bool
	isTing        bool
	score         int
}

func newPlayer(seat int, name string, isAi bool) *Player {
	// 切片必须初始化为空而非 nil:JSON 序列化要输出 [] 而不是 null(与 Java/Gson 行为一致)
	return &Player{seat: seat, name: name, isAi: isAi, score: 1000,
		hand: []int{}, melds: []*Meld{}, discards: []int{}}
}

func (p *Player) reset() {
	p.hand = []int{}
	p.melds = []*Meld{}
	p.discards = []int{}
	p.lastDrawnCard = 0
	p.isHu = false
	p.isTing = false
}

func (p *Player) getSeat() int           { return p.seat }
func (p *Player) getName() string        { return p.name }
func (p *Player) setName(n string)       { p.name = n }
func (p *Player) isAI() bool             { return p.isAi }
func (p *Player) setAi(ai bool)          { p.isAi = ai }
func (p *Player) getHand() []int         { return p.hand }
func (p *Player) getMelds() []*Meld      { return p.melds }
func (p *Player) getDiscards() []int     { return p.discards }
func (p *Player) getLastDrawnCard() int  { return p.lastDrawnCard }
func (p *Player) setLastDrawnCard(c int) { p.lastDrawnCard = c }
func (p *Player) isHuPlayer() bool       { return p.isHu }
func (p *Player) setHu(hu bool)          { p.isHu = hu }
func (p *Player) isTingPlayer() bool     { return p.isTing }
func (p *Player) setTing(t bool)         { p.isTing = t }
func (p *Player) getScore() int          { return p.score }
func (p *Player) addScore(delta int)     { p.score += delta }

func (p *Player) addCard(card int) {
	p.hand = append(p.hand, card)
	p.lastDrawnCard = card
	sortInts(p.hand)
}

func (p *Player) removeCard(card int) bool {
	for i, c := range p.hand {
		if c == card {
			p.hand = append(p.hand[:i], p.hand[i+1:]...)
			if p.lastDrawnCard == card {
				p.lastDrawnCard = 0
			}
			return true
		}
	}
	return false
}

func (p *Player) addDiscard(card int) {
	p.discards = append(p.discards, card)
}

func (p *Player) addMeld(meld *Meld) {
	p.melds = append(p.melds, meld)
}

func (p *Player) countCard(card int) int {
	return majiangCardCount(p.hand, card)
}

func (p *Player) getAllTiles() []int {
	all := append([]int{}, p.hand...)
	for _, m := range p.melds {
		all = append(all, m.Cards...)
	}
	return all
}

func majiangCardCount(list []int, card int) int {
	n := 0
	for _, c := range list {
		if c == card {
			n++
		}
	}
	return n
}

func sortInts(list []int) {
	for i := 1; i < len(list); i++ {
		for j := i; j > 0 && list[j] < list[j-1]; j-- {
			list[j], list[j-1] = list[j-1], list[j]
		}
	}
}

// MahjongGame 对应 Java web.MahjongGame。
type MahjongGame struct {
	gameId           int
	players          []*Player
	wall             []int
	guiCards         []int
	guiIndicator     int
	dealerSeat       int
	currentSeat      int
	phase            string
	lastDiscard      int
	lastDiscardSeat  int
	lastDrawFromGang bool
	logs             []string
	settlement       *SettlementInfo
	spectatorMode    bool
	stepCount        int

	// 用户响应等待上下文
	userPendingActions *AvailableActions
}

func NewMahjongGame(guiMode string, customGuiCard int, spectator bool) *MahjongGame {
	gameIdCounter++
	g := &MahjongGame{
		gameId:             gameIdCounter,
		spectatorMode:      spectator,
		lastDiscardSeat:    -1,
		phase:              PhaseDiscard,
		userPendingActions: newAvailableActions(),
	}

	// 初始化4个玩家 (0:南-玩家, 1:东-下家, 2:北-对家, 3:西-上家)
	g.players = append(g.players,
		newPlayer(0, boolStr(spectator, "AI-南 (庄)", "玩家 (南)"), spectator),
		newPlayer(1, "AI-东 (下家)", true),
		newPlayer(2, "AI-北 (对家)", true),
		newPlayer(3, "AI-西 (上家)", true),
	)

	g.initGame(guiMode, customGuiCard)
	return g
}

func boolStr(cond bool, a, b string) string {
	if cond {
		return a
	}
	return b
}

func (g *MahjongGame) log(msg string) {
	g.logs = append(g.logs, msg)
	if len(g.logs) > 100 {
		g.logs = g.logs[1:]
	}
}

// initGame 洗牌、定鬼、发牌。
func (g *MahjongGame) initGame(guiMode string, customGuiCard int) {
	for _, p := range g.players {
		p.reset()
	}
	g.wall = g.wall[:0]
	g.guiCards = g.guiCards[:0]
	g.logs = g.logs[:0]
	g.settlement = nil
	g.lastDiscard = 0
	g.lastDiscardSeat = -1
	g.lastDrawFromGang = false
	g.dealerSeat = 0
	g.currentSeat = g.dealerSeat
	g.stepCount = 0

	// 1. 生成136张牌
	for i := 1; i <= 34; i++ {
		for k := 0; k < 4; k++ {
			g.wall = append(g.wall, i)
		}
	}
	rand.Shuffle(len(g.wall), func(i, j int) {
		g.wall[i], g.wall[j] = g.wall[j], g.wall[i]
	})

	// 2. 确定鬼牌
	if guiMode == "random_flip" || guiMode == "" {
		// 翻一张指示牌
		g.guiIndicator = g.wall[0]
		g.wall = g.wall[1:]
		gui := g.calculateNextCard(g.guiIndicator)
		g.guiCards = append(g.guiCards, gui)
		g.log("本局鬼牌(癞子)为: 【" + majiang.CardToString(gui) + "】")
	} else if guiMode == "card" && customGuiCard > 0 && customGuiCard <= 34 {
		g.guiIndicator = customGuiCard
		g.guiCards = append(g.guiCards, customGuiCard)
		g.log("指定宝牌(鬼牌)为: 【" + majiang.CardToString(customGuiCard) + "】")
	} else if guiMode == "bai" {
		g.guiIndicator = majiang.JianBai
		g.guiCards = append(g.guiCards, majiang.JianBai)
		g.log("经典白板当鬼: 【白板】")
	} else if guiMode == "zhong" {
		g.guiIndicator = majiang.JianZhong
		g.guiCards = append(g.guiCards, majiang.JianZhong)
		g.log("经典红中当鬼: 【红中】")
	} else {
		// 无鬼牌
		g.guiIndicator = 0
		g.log("本局为经典纯手牌规则 (无鬼牌)")
	}

	// 3. 发牌:闲家各13张,庄家14张
	for i := 0; i < 13; i++ {
		for _, p := range g.players {
			p.addCard(g.wall[0])
			g.wall = g.wall[1:]
		}
	}
	// 庄家摸第14张
	dealerCard := g.wall[0]
	g.wall = g.wall[1:]
	g.players[g.dealerSeat].addCard(dealerCard)
	g.players[g.dealerSeat].setLastDrawnCard(dealerCard)
	g.log(g.players[g.dealerSeat].getName() + " 起手14张牌，作为庄家率先出牌")

	// 检查天胡
	if majiang.IsHuExtra(g.players[g.dealerSeat].getHand(), g.guiCards, 0) {
		g.log("🎉 震撼！" + g.players[g.dealerSeat].getName() + " 起手天胡！")
		g.triggerWin(g.dealerSeat, -1, dealerCard, true, false, false, true, false)
		return
	}

	g.phase = PhaseDiscard
	g.updateTingStatus()
}

func (g *MahjongGame) getFirstGui() int {
	if len(g.guiCards) == 0 {
		return 0
	}
	return g.guiCards[0]
}

func (g *MahjongGame) calculateNextCard(card int) int {
	if card >= majiang.Wan1 && card <= majiang.Wan9 {
		if card == majiang.Wan9 {
			return majiang.Wan1
		}
		return card + 1
	}
	if card >= majiang.Tong1 && card <= majiang.Tong9 {
		if card == majiang.Tong9 {
			return majiang.Tong1
		}
		return card + 1
	}
	if card >= majiang.Tiao1 && card <= majiang.Tiao9 {
		if card == majiang.Tiao9 {
			return majiang.Tiao1
		}
		return card + 1
	}
	if card >= majiang.FengDong && card <= majiang.FengBei {
		if card == majiang.FengBei {
			return majiang.FengDong
		}
		return card + 1
	}
	if card >= majiang.JianZhong && card <= majiang.JianBai {
		if card == majiang.JianBai {
			return majiang.JianZhong
		}
		return card + 1
	}
	return card
}

// Discard 玩家打牌(调用方需持锁)。
func (g *MahjongGame) Discard(seat int, card int) bool {
	if g.phase != PhaseDiscard || seat != g.currentSeat {
		return false
	}
	player := g.players[seat]
	if !containsInt(player.getHand(), card) {
		return false
	}

	player.removeCard(card)
	sortInts(player.hand)
	g.lastDiscard = card
	g.lastDiscardSeat = seat
	g.log(player.getName() + " 打出了 【" + majiang.CardToString(card) + "】")

	// 检查全场对该牌的响应
	g.phase = PhaseResponse
	g.checkResponses(card, seat)
	return true
}

// checkResponses 检查其他3家对打出牌的响应。
func (g *MahjongGame) checkResponses(card int, fromSeat int) {
	g.userPendingActions.CanDiscard = false
	g.userPendingActions.CanHu = false
	g.userPendingActions.CanPeng = false
	g.userPendingActions.CanGang = false
	g.userPendingActions.GangCards = g.userPendingActions.GangCards[:0]
	g.userPendingActions.CanChi = false
	g.userPendingActions.ChiOptions = g.userPendingActions.ChiOptions[:0]
	g.userPendingActions.CanPass = false

	// 如果0号是真人玩家,且不是自己出的牌,检查真人玩家可做动作
	human := g.players[0]
	if !human.isAI() && fromSeat != 0 {
		hasAction := false

		// 1. 点炮胡
		if majiang.IsHuExtra(human.getHand(), g.guiCards, card) {
			g.userPendingActions.CanHu = true
			hasAction = true
		}
		// 2. 碰
		if human.countCard(card) >= 2 && !containsInt(g.guiCards, card) {
			g.userPendingActions.CanPeng = true
			hasAction = true
		}
		// 3. 明杠
		if human.countCard(card) == 3 && !containsInt(g.guiCards, card) {
			g.userPendingActions.CanGang = true
			g.userPendingActions.GangCards = append(g.userPendingActions.GangCards, card)
			hasAction = true
		}
		// 4. 吃(仅当下家为玩家,即 (fromSeat + 1) % 4 == 0)
		if (fromSeat+1)%4 == 0 && !containsInt(g.guiCards, card) {
			chiOpts := g.findChiOptions(human.getHand(), card)
			if len(chiOpts) > 0 {
				g.userPendingActions.CanChi = true
				g.userPendingActions.ChiOptions = chiOpts
				hasAction = true
			}
		}

		if hasAction {
			g.userPendingActions.CanPass = true
			g.phase = PhaseWaitingUser
			return
		}
	}

	// 若真人玩家无动作(或真人已过/是AI模式),让AI决定响应
	g.processAiResponses(card, fromSeat)
}

// processAiResponses AI 自动响应判定(按照 胡 > 杠/碰 > 吃 的优先级)。
func (g *MahjongGame) processAiResponses(card int, fromSeat int) {
	// 1. 优先检查谁胡牌(点炮胡)
	for i := 1; i <= 3; i++ {
		seat := (fromSeat + i) % 4
		p := g.players[seat]
		if p.isAI() && majiang.IsHuExtra(p.getHand(), g.guiCards, card) {
			g.log("🎊 " + p.getName() + " 胡了 " + g.players[fromSeat].getName() + " 点炮的 【" + majiang.CardToString(card) + "】！")
			g.triggerWin(seat, fromSeat, card, false, false, g.wallEmpty(), false, false)
			return
		}
	}

	// 2. 检查杠牌 (明杠)
	for i := 1; i <= 3; i++ {
		seat := (fromSeat + i) % 4
		p := g.players[seat]
		if p.isAI() && p.countCard(card) == 3 && !containsInt(g.guiCards, card) {
			if majiang.GangAI(p.getHand(), g.guiCards, card, 0.5) {
				g.executeGang(seat, fromSeat, card, "MING_GANG")
				return
			}
		}
	}

	// 3. 检查碰牌
	for i := 1; i <= 3; i++ {
		seat := (fromSeat + i) % 4
		p := g.players[seat]
		if p.isAI() && p.countCard(card) >= 2 && !containsInt(g.guiCards, card) {
			if majiang.PengAI(p.getHand(), g.guiCards, card, 0.0) {
				g.executePeng(seat, fromSeat, card)
				return
			}
		}
	}

	// 4. 检查吃牌(仅限下家)
	nextSeat := (fromSeat + 1) % 4
	nextPlayer := g.players[nextSeat]
	if nextPlayer.isAI() && !containsInt(g.guiCards, card) {
		chiPair := majiang.ChiAIChoices(nextPlayer.getHand(), g.guiCards, card)
		if len(chiPair) == 2 {
			g.executeChi(nextSeat, fromSeat, card, chiPair[0], chiPair[1])
			return
		}
	}

	// 均无响应:此牌入弃牌堆,轮到下家摸牌
	g.players[fromSeat].addDiscard(card)
	g.drawNext((fromSeat+1)%4, false)
}

func (g *MahjongGame) wallEmpty() bool {
	return len(g.wall) == 0
}

// UserAction 玩家选择响应动作(调用方需持锁)。
func (g *MahjongGame) UserAction(action string, card int, chi1 int, chi2 int) bool {
	if g.phase != PhaseWaitingUser {
		return false
	}

	human := g.players[0]
	if action == "hu" && g.userPendingActions.CanHu {
		g.log("🎉 恭喜你胡牌了！胡了 " + g.players[g.lastDiscardSeat].getName() + " 点炮的 【" + majiang.CardToString(g.lastDiscard) + "】！")
		g.triggerWin(0, g.lastDiscardSeat, g.lastDiscard, false, false, g.wallEmpty(), false, false)
		return true
	}

	if action == "gang" && g.userPendingActions.CanGang {
		g.executeGang(0, g.lastDiscardSeat, g.lastDiscard, "MING_GANG")
		return true
	}

	if action == "peng" && g.userPendingActions.CanPeng {
		g.executePeng(0, g.lastDiscardSeat, g.lastDiscard)
		return true
	}

	if action == "chi" && g.userPendingActions.CanChi {
		// 校验 chi1, chi2
		if containsInt(human.getHand(), chi1) && containsInt(human.getHand(), chi2) {
			g.executeChi(0, g.lastDiscardSeat, g.lastDiscard, chi1, chi2)
			return true
		}
	}

	if action == "pass" {
		g.log(human.getName() + " 选择了 【过】")
		g.phase = PhaseResponse
		// 玩家放弃后,继续判定剩余AI是否响应
		g.processAiResponses(g.lastDiscard, g.lastDiscardSeat)
		return true
	}

	return false
}

// UserSelfAction 玩家在自摸回合执行杠牌或自摸胡(调用方需持锁)。
func (g *MahjongGame) UserSelfAction(action string, card int) bool {
	if g.phase != PhaseDiscard || g.currentSeat != 0 {
		return false
	}
	human := g.players[0]

	if action == "hu" {
		if majiang.IsHuExtra(human.getHand(), g.guiCards, 0) {
			winCard := human.getLastDrawnCard()
			if winCard <= 0 && len(human.getHand()) > 0 {
				winCard = human.getHand()[len(human.getHand())-1]
			}
			g.log("🎉 恭喜你【自摸胡】了！胡牌张: 【" + majiang.CardToString(winCard) + "】")
			g.triggerWin(0, -1, winCard, true, g.lastDrawFromGang, g.wallEmpty(), false, false)
			return true
		}
	}

	if action == "gang" {
		// 检查暗杠
		if human.countCard(card) == 4 && !containsInt(g.guiCards, card) {
			for i := 0; i < 4; i++ {
				human.removeCard(card)
			}
			meld := &Meld{Type: "AN_GANG", TriggerCard: card, FromSeat: -1, Cards: []int{card, card, card, card}}
			sortInts(meld.Cards)
			human.addMeld(meld)
			g.log(human.getName() + " 【暗杠】: " + majiang.CardToString(card))
			g.drawNext(0, true)
			return true
		}
		// 检查补杠
		for _, m := range human.getMelds() {
			if m.Type == "PENG" && m.TriggerCard == card && containsInt(human.getHand(), card) {
				human.removeCard(card)
				m.Type = "BU_GANG"
				m.Cards = append(m.Cards, card)
				g.log(human.getName() + " 【补杠】: " + majiang.CardToString(card))
				g.drawNext(0, true)
				return true
			}
		}
	}

	return false
}

func (g *MahjongGame) executePeng(seat int, fromSeat int, card int) {
	p := g.players[seat]
	p.removeCard(card)
	p.removeCard(card)
	meld := &Meld{Type: "PENG", TriggerCard: card, FromSeat: fromSeat, Cards: []int{card, card, card}}
	sortInts(meld.Cards)
	p.addMeld(meld)
	g.log(p.getName() + " 【碰】了 " + g.players[fromSeat].getName() + " 的 " + majiang.CardToString(card))
	g.currentSeat = seat
	g.phase = PhaseDiscard
	g.lastDrawFromGang = false
	p.setLastDrawnCard(0)
	g.updateTingStatus()
}

func (g *MahjongGame) executeGang(seat int, fromSeat int, card int, meldType string) {
	p := g.players[seat]
	if meldType == "MING_GANG" {
		p.removeCard(card)
		p.removeCard(card)
		p.removeCard(card)
		meld := &Meld{Type: "MING_GANG", TriggerCard: card, FromSeat: fromSeat, Cards: []int{card, card, card, card}}
		sortInts(meld.Cards)
		p.addMeld(meld)
		g.log(p.getName() + " 【明杠】了 " + g.players[fromSeat].getName() + " 的 " + majiang.CardToString(card))
	}
	// 杠后从牌墙尾部摸牌
	g.drawNext(seat, true)
}

func (g *MahjongGame) executeChi(seat int, fromSeat int, card int, c1 int, c2 int) {
	p := g.players[seat]
	p.removeCard(c1)
	p.removeCard(c2)
	meld := &Meld{Type: "CHI", TriggerCard: card, FromSeat: fromSeat, Cards: []int{card, c1, c2}}
	sortInts(meld.Cards)
	p.addMeld(meld)
	g.log(p.getName() + " 【吃】了 " + g.players[fromSeat].getName() + " 的 " + majiang.CardToString(card) +
		" (组合: " + majiang.CardToString(c1) + "," + majiang.CardToString(card) + "," + majiang.CardToString(c2) + ")")
	g.currentSeat = seat
	g.phase = PhaseDiscard
	g.lastDrawFromGang = false
	p.setLastDrawnCard(0)
	g.updateTingStatus()
}

// drawNext 轮到摸牌。
func (g *MahjongGame) drawNext(seat int, isFromGang bool) {
	g.currentSeat = seat
	g.lastDrawFromGang = isFromGang

	if len(g.wall) == 0 {
		g.log("牌墙已全部摸完，【荒庄流局】！")
		g.triggerDraw()
		return
	}

	var drawn int
	if isFromGang {
		drawn = g.wall[len(g.wall)-1]
		g.wall = g.wall[:len(g.wall)-1]
	} else {
		drawn = g.wall[0]
		g.wall = g.wall[1:]
	}
	player := g.players[seat]
	player.addCard(drawn)

	if isFromGang {
		g.log(player.getName() + " 杠后补摸一张: 【" + majiang.CardToString(drawn) + "】 (剩余 " + strconv.Itoa(len(g.wall)) + " 张)")
	} else {
		g.log(player.getName() + " 摸了一张牌 (剩余 " + strconv.Itoa(len(g.wall)) + " 张)")
	}

	// 检查摸牌玩家是否自摸
	if majiang.IsHuExtra(player.getHand(), g.guiCards, 0) {
		if player.isAI() {
			g.log("🎊 " + player.getName() + boolStr(isFromGang, " 【杠上开花】", " 【自摸胡】") + "！胡牌: 【" + majiang.CardToString(drawn) + "】")
			g.triggerWin(seat, -1, drawn, true, isFromGang, g.wallEmpty(), false, false)
			return
		}
	}

	// 如果是AI玩家摸牌,还可以判定是否杠牌
	if player.isAI() {
		// 暗杠判定
		for card := 1; card <= 34; card++ {
			if !containsInt(g.guiCards, card) && player.countCard(card) == 4 {
				if majiang.GangAI(player.getHand(), g.guiCards, card, 0.5) {
					for k := 0; k < 4; k++ {
						player.removeCard(card)
					}
					meld := &Meld{Type: "AN_GANG", TriggerCard: card, FromSeat: -1, Cards: []int{card, card, card, card}}
					sortInts(meld.Cards)
					player.addMeld(meld)
					g.log(player.getName() + " 【暗杠】: " + majiang.CardToString(card))
					g.drawNext(seat, true)
					return
				}
			}
		}
		// 补杠判定
		for _, m := range player.getMelds() {
			if m.Type == "PENG" && !containsInt(g.guiCards, m.TriggerCard) && containsInt(player.getHand(), m.TriggerCard) {
				if majiang.GangAI(player.getHand(), g.guiCards, m.TriggerCard, 0.5) {
					player.removeCard(m.TriggerCard)
					m.Type = "BU_GANG"
					m.Cards = append(m.Cards, m.TriggerCard)
					g.log(player.getName() + " 【补杠】: " + majiang.CardToString(m.TriggerCard))
					g.drawNext(seat, true)
					return
				}
			}
		}
	}

	g.phase = PhaseDiscard
	g.updateTingStatus()
}

// Step 游戏步进推进(驱动AI自动思考打牌,或单步向前)。调用方需持锁。
func (g *MahjongGame) Step() bool {
	g.stepCount++
	if g.phase == PhaseGameOver {
		return false
	}

	if g.phase == PhaseWaitingUser {
		if g.spectatorMode || g.players[0].isAI() {
			g.handleAiResponseForPlayer0()
			return true
		}
		return false
	}

	if g.phase == PhaseDiscard {
		current := g.players[g.currentSeat]
		if current.isAI() {
			// AI 决策打牌
			outCard := majiang.OutAI(current.getHand(), g.guiCards)
			if outCard == 0 || !containsInt(current.getHand(), outCard) {
				// 保底打非鬼牌或第一张
				for _, c := range current.getHand() {
					if !containsInt(g.guiCards, c) {
						outCard = c
						break
					}
				}
				if outCard == 0 {
					outCard = current.getHand()[0]
				}
			}
			g.Discard(g.currentSeat, outCard)
			return true
		}
		// 轮到玩家出牌,暂停等待玩家点击出牌
		return false
	}

	return false
}

// updateTingStatus 更新各玩家的听牌标志。
func (g *MahjongGame) updateTingStatus() {
	for _, p := range g.players {
		// 当手牌模3余1时(未摸牌或刚打牌完),直接检测当前是否听牌
		if len(p.getHand())%3 == 1 {
			tingCards := majiang.IsTingExtra(p.getHand(), g.guiCards)
			p.setTing(len(tingCards) > 0)
		} else if len(p.getHand())%3 == 2 {
			// 摸牌后有14张:如果任打出一张能听牌,也算准听
			canTing := false
			seen := map[int]bool{}
			for _, c := range p.getHand() {
				if seen[c] {
					continue
				}
				seen[c] = true
				tmp := removeFirstInt(p.getHand(), c)
				t := majiang.IsTingExtra(tmp, g.guiCards)
				if len(t) > 0 {
					canTing = true
					break
				}
			}
			p.setTing(canTing)
		}
	}
}

// findChiOptions 寻找所有合法的吃牌顺子组合。
func (g *MahjongGame) findChiOptions(hand []int, card int) [][]int {
	var options [][]int
	t := majiang.CardType(card)
	if t != majiang.TypeWan && t != majiang.TypeTong && t != majiang.TypeTiao {
		return options
	}

	// [card-2, card-1]
	if containsInt(hand, card-2) && containsInt(hand, card-1) && majiang.CardType(card-2) == t {
		options = append(options, []int{card - 2, card - 1})
	}
	// [card-1, card+1]
	if containsInt(hand, card-1) && containsInt(hand, card+1) && majiang.CardType(card-1) == t && majiang.CardType(card+1) == t {
		options = append(options, []int{card - 1, card + 1})
	}
	// [card+1, card+2]
	if containsInt(hand, card+1) && containsInt(hand, card+2) && majiang.CardType(card+2) == t {
		options = append(options, []int{card + 1, card + 2})
	}

	return options
}

// triggerWin 胜负结算。
func (g *MahjongGame) triggerWin(winnerSeat int, providerSeat int, winCard int, isZimo bool,
	isGangShangHua bool, isHaiDi bool, isTianHu bool, isDiHu bool) {
	g.phase = PhaseGameOver
	winner := g.players[winnerSeat]
	winner.setHu(true)

	fan := calculateFan(winner, winCard, isZimo, isGangShangHua, isHaiDi, isTianHu, isDiHu, g.guiCards)

	settlement := newSettlementInfo()
	settlement.IsDraw = false
	settlement.WinnerSeat = winnerSeat
	settlement.ProviderSeat = providerSeat
	settlement.IsZimo = isZimo
	settlement.WinCard = winCard
	settlement.Patterns = fan.GetPatterns()
	settlement.TotalFan = fan.GetTotalFan()
	settlement.Points = fan.GetPoints()
	g.settlement = settlement

	scoreDelta := fan.GetPoints()
	if isZimo {
		// 其余3家各扣 scoreDelta
		for _, p := range g.players {
			if p.getSeat() == winnerSeat {
				p.addScore(scoreDelta * 3)
				settlement.ScoreDeltas[strconv.Itoa(p.getSeat())] = scoreDelta * 3
			} else {
				p.addScore(-scoreDelta)
				settlement.ScoreDeltas[strconv.Itoa(p.getSeat())] = -scoreDelta
			}
		}
	} else {
		// 点炮者单独扣分
		winner.addScore(scoreDelta)
		settlement.ScoreDeltas[strconv.Itoa(winnerSeat)] = scoreDelta
		g.players[providerSeat].addScore(-scoreDelta)
		settlement.ScoreDeltas[strconv.Itoa(providerSeat)] = -scoreDelta
		for _, p := range g.players {
			if p.getSeat() != winnerSeat && p.getSeat() != providerSeat {
				settlement.ScoreDeltas[strconv.Itoa(p.getSeat())] = 0
			}
		}
	}
}

func (g *MahjongGame) triggerDraw() {
	g.phase = PhaseGameOver
	settlement := newSettlementInfo()
	settlement.IsDraw = true
	settlement.WinnerSeat = -1
	settlement.ProviderSeat = -1
	settlement.Patterns = []string{"荒庄流局 (无输赢)"}
	g.settlement = settlement
}

func containsInt(list []int, v int) bool {
	for _, c := range list {
		if c == v {
			return true
		}
	}
	return false
}

func removeFirstInt(list []int, card int) []int {
	tmp := make([]int, 0, len(list))
	removed := false
	for _, c := range list {
		if !removed && c == card {
			removed = true
			continue
		}
		tmp = append(tmp, c)
	}
	return tmp
}

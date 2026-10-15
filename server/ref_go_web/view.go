// 对局视图构建与 AI 推荐,对应 Java MahjongGame.createView / setSpectatorMode 等(逐行移植)。
package web

import (
	"fmt"
	"strconv"

	majiang "github.com/esrrhs/majiang_algorithm/go"
)

// CreateView 构建发送给前端的完整视图对象。调用方需持锁。
func (g *MahjongGame) CreateView(requestSeat int) *GameStateView {
	view := newGameStateView()
	view.GameId = g.gameId
	view.WallCount = len(g.wall)
	view.GuiCards = append([]int{}, g.guiCards...)
	view.GuiIndicator = g.guiIndicator
	view.DealerSeat = g.dealerSeat
	view.CurrentSeat = g.currentSeat
	view.Phase = g.phase
	view.LastDiscard = g.lastDiscard
	view.LastDiscardSeat = g.lastDiscardSeat
	view.Logs = append([]string{}, g.logs...)
	view.Settlement = g.settlement
	view.SpectatorMode = g.spectatorMode

	// 计算全场每张牌已曝光数量与剩余张数
	visibleCounts := make([]int, 35)
	if g.guiIndicator > 0 {
		visibleCounts[g.guiIndicator]++
	}
	for _, p := range g.players {
		for _, d := range p.getDiscards() {
			visibleCounts[d]++
		}
		for _, m := range p.getMelds() {
			for _, c := range m.Cards {
				visibleCounts[c]++
			}
		}
	}
	// 如果是玩家自己,手牌里的牌也是已知的
	user := g.players[0]
	for _, c := range user.getHand() {
		visibleCounts[c]++
	}

	remainMap := map[string]int{}
	for i := 1; i <= 34; i++ {
		remain := 4 - visibleCounts[i]
		if remain < 0 {
			remain = 0
		}
		remainMap[strconv.Itoa(i)] = remain
	}
	view.CardRemainCounts = remainMap

	// 玩家视图
	for _, p := range g.players {
		pv := &PlayerView{}
		pv.Seat = p.getSeat()
		pv.Name = p.getName()
		pv.IsAi = p.isAI()
		pv.TileCount = len(p.getHand())
		pv.Melds = p.getMelds()
		pv.Discards = p.getDiscards()
		pv.LastDrawnCard = p.getLastDrawnCard()
		pv.IsTing = p.isTingPlayer()
		pv.IsHu = p.isHuPlayer()
		pv.Score = p.getScore()

		// 总是提供完整手牌数据,由前端根据开关一键控制明牌/暗牌展示
		pv.Hand = append([]int{}, p.getHand()...)
		view.Players = append(view.Players, pv)
	}

	// 可用动作
	if g.phase == PhaseWaitingUser {
		view.AvailableActions = g.userPendingActions.clone()
	} else if g.phase == PhaseDiscard && g.currentSeat == 0 && !user.isAI() {
		actions := newAvailableActions()
		actions.CanDiscard = true
		if majiang.IsHuExtra(user.getHand(), g.guiCards, 0) {
			actions.CanHu = true
		}
		// 检查暗杠或补杠
		for card := 1; card <= 34; card++ {
			if !containsInt(g.guiCards, card) && user.countCard(card) == 4 {
				actions.CanGang = true
				actions.GangCards = append(actions.GangCards, card)
			}
		}
		for _, m := range user.getMelds() {
			if m.Type == "PENG" && !containsInt(g.guiCards, m.TriggerCard) && containsInt(user.getHand(), m.TriggerCard) {
				actions.CanGang = true
				actions.GangCards = append(actions.GangCards, m.TriggerCard)
			}
		}
		view.AvailableActions = actions
	}

	// 听牌提示计算
	if !user.isAI() || g.spectatorMode {
		// 1. 如果当前是听牌状态(手牌模3余1),计算当前直接胡哪些牌
		if len(user.getHand())%3 == 1 {
			tings := majiang.IsTingExtra(user.getHand(), g.guiCards)
			targets := []*TingTarget{}
			for _, t := range tings {
				targets = append(targets, &TingTarget{Card: t, RemainingCount: remainMap[strconv.Itoa(t)]})
			}
			view.CurrentTingTargets = targets
		}

		// 2. 如果处于出牌阶段(手牌模3余2),计算打出哪张牌能听牌(即:打牌听牌分析)
		if len(user.getHand())%3 == 2 {
			discardTingMap := map[string][]*TingTarget{}
			seen := map[int]bool{}
			for _, c := range user.getHand() {
				if seen[c] {
					continue
				}
				seen[c] = true
				simHand := removeFirstInt(user.getHand(), c)
				tings := majiang.IsTingExtra(simHand, g.guiCards)
				if len(tings) > 0 {
					targets := []*TingTarget{}
					for _, t := range tings {
						targets = append(targets, &TingTarget{Card: t, RemainingCount: remainMap[strconv.Itoa(t)]})
					}
					discardTingMap[strconv.Itoa(c)] = targets
				}
			}
			view.DiscardToTing = discardTingMap
		}
	}

	// AI 推荐评估(当轮到玩家出牌或响应时,提供智能建议)
	if g.phase == PhaseDiscard && g.currentSeat == 0 && !user.isAI() {
		func() {
			defer func() { recover() }()
			acts := view.AvailableActions
			if acts != nil && acts.CanHu {
				view.AiRecommendation = newRecommendation("hu", 0, nil, 999.0, "AI强烈推荐【自摸胡】！")
			} else {
				wantGang := false
				gCard := 0
				if acts != nil && acts.CanGang {
					for _, gc := range acts.GangCards {
						if majiang.GangAI(user.getHand(), g.guiCards, gc, 0.5) {
							wantGang = true
							gCard = gc
							break
						}
					}
				}
				if wantGang {
					gCards := []int{}
					for _, c := range user.getHand() {
						if c == gCard {
							gCards = append(gCards, c)
						}
					}
					view.AiRecommendation = newRecommendation("gang", gCard, gCards, 888.0, "AI推荐【杠】"+majiang.CardToString(gCard))
				} else {
					bestOut := majiang.OutAI(user.getHand(), g.guiCards)
					score := 0.0
					if bestOut != 0 {
						tmp := removeFirstInt(user.getHand(), bestOut)
						score = majiang.Calc(tmp, g.guiCards)
					}
					reason := "AI推荐打出【" + majiang.CardToString(bestOut) + "】，打出后手牌期望评分: " + fmt.Sprintf("%.2f", score)
					view.AiRecommendation = newRecommendation("discard", bestOut, []int{bestOut}, score, reason)
				}
			}
		}()
	} else if g.phase == PhaseWaitingUser && !user.isAI() {
		func() {
			defer func() { recover() }()
			if g.userPendingActions.CanHu {
				view.AiRecommendation = newRecommendation("hu", g.lastDiscard, nil, 999.0, "AI强烈推荐【胡牌】！")
			} else if g.userPendingActions.CanGang {
				wantGang := false
				gCard := 0
				for _, gc := range g.userPendingActions.GangCards {
					if majiang.GangAI(user.getHand(), g.guiCards, gc, 0.5) {
						wantGang = true
						gCard = gc
						break
					}
				}
				if wantGang {
					gCards := []int{}
					for _, c := range user.getHand() {
						if c == gCard {
							gCards = append(gCards, c)
						}
					}
					view.AiRecommendation = newRecommendation("gang", gCard, gCards, 888.0, "AI推荐【杠】"+majiang.CardToString(gCard))
				} else if g.userPendingActions.CanPeng && majiang.PengAI(user.getHand(), g.guiCards, g.lastDiscard, 0.0) {
					view.AiRecommendation = newRecommendation("peng", g.lastDiscard, []int{g.lastDiscard, g.lastDiscard}, 777.0, "AI推荐【碰】"+majiang.CardToString(g.lastDiscard))
				} else {
					view.AiRecommendation = newRecommendation("pass", 0, nil, 0.0, "AI建议【过】")
				}
			} else if g.userPendingActions.CanPeng {
				if majiang.PengAI(user.getHand(), g.guiCards, g.lastDiscard, 0.0) {
					view.AiRecommendation = newRecommendation("peng", g.lastDiscard, []int{g.lastDiscard, g.lastDiscard}, 777.0, "AI推荐【碰】"+majiang.CardToString(g.lastDiscard))
				} else {
					view.AiRecommendation = newRecommendation("pass", 0, nil, 0.0, "AI建议【过】")
				}
			} else if g.userPendingActions.CanChi {
				chiPair := majiang.ChiAIChoices(user.getHand(), g.guiCards, g.lastDiscard)
				if len(chiPair) == 2 {
					view.AiRecommendation = newRecommendation("chi", g.lastDiscard, chiPair, 666.0, "AI推荐【吃】"+majiang.CardToString(chiPair[0])+"和"+majiang.CardToString(chiPair[1]))
				} else {
					view.AiRecommendation = newRecommendation("pass", 0, nil, 0.0, "AI建议【过】")
				}
			}
		}()
	}

	return view
}

func (g *MahjongGame) handleAiResponseForPlayer0() {
	p0 := g.players[0]
	if g.userPendingActions.CanHu {
		g.UserAction("hu", g.lastDiscard, 0, 0)
	} else if g.userPendingActions.CanGang && len(g.userPendingActions.GangCards) > 0 {
		g.UserAction("gang", g.userPendingActions.GangCards[0], 0, 0)
	} else if g.userPendingActions.CanPeng && majiang.PengAI(p0.getHand(), g.guiCards, g.lastDiscard, 0.0) {
		g.UserAction("peng", g.lastDiscard, 0, 0)
	} else if g.userPendingActions.CanChi {
		chiPair := majiang.ChiAIChoices(p0.getHand(), g.guiCards, g.lastDiscard)
		if len(chiPair) == 2 {
			g.UserAction("chi", g.lastDiscard, chiPair[0], chiPair[1])
		} else {
			g.UserAction("pass", 0, 0, 0)
		}
	} else {
		g.UserAction("pass", 0, 0, 0)
	}
}

// SetSpectatorMode 切换观战(全AI)模式,不重置对局与手牌。调用方需持锁。
func (g *MahjongGame) SetSpectatorMode(spectator bool) {
	g.spectatorMode = spectator
	p0 := g.players[0]
	p0.setAi(spectator)
	p0.setName(boolStr(spectator, "AI-南 (托管)", "玩家 (南)"))
	if spectator && g.phase == PhaseWaitingUser {
		g.handleAiResponseForPlayer0()
	}
}

// Getters(调用方需持锁)。
func (g *MahjongGame) GetPhase() string    { return g.phase }
func (g *MahjongGame) GetCurrentSeat() int { return g.currentSeat }
func (g *MahjongGame) GetGameId() int      { return g.gameId }

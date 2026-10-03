// 传输给前端的对局状态视图 DTO。
// JSON 字段名与 Java 版(Gson 直接序列化字段名)逐一对齐,前端 app.js 无需任何改动。
package web

// 对应 Java web.Meld。
type Meld struct {
	Type        string `json:"type"`        // CHI / PENG / MING_GANG / AN_GANG / BU_GANG
	TriggerCard int    `json:"triggerCard"` // 触发此副牌的牌(来自他人或自摸)
	FromSeat    int    `json:"fromSeat"`    // 来自哪位玩家座位(-1 表示自己摸的暗杠)
	Cards       []int  `json:"cards"`       // 包含的所有牌(3张或4张)
}

var meldDisplayNames = map[string]string{
	"CHI": "吃", "PENG": "碰", "MING_GANG": "明杠", "AN_GANG": "暗杠", "BU_GANG": "补杠",
}

func (m *Meld) IsGang() bool {
	return m.Type == "MING_GANG" || m.Type == "AN_GANG" || m.Type == "BU_GANG"
}

// 对应 Java GameStateView.PlayerView。
type PlayerView struct {
	Seat          int     `json:"seat"`
	Name          string  `json:"name"`
	IsAi          bool    `json:"isAi"`
	TileCount     int     `json:"tileCount"`
	Hand          []int   `json:"hand"`
	Melds         []*Meld `json:"melds"`
	Discards      []int   `json:"discards"`
	LastDrawnCard int     `json:"lastDrawnCard"`
	IsTing        bool    `json:"isTing"`
	IsHu          bool    `json:"isHu"`
	Score         int     `json:"score"`
}

// 对应 Java GameStateView.TingTarget。
type TingTarget struct {
	Card           int `json:"card"`
	RemainingCount int `json:"remainingCount"` // 场上剩余张数(4 - 已经可见的张数)
}

// 对应 Java GameStateView.AvailableActions。
type AvailableActions struct {
	CanDiscard bool    `json:"canDiscard"`
	CanHu      bool    `json:"canHu"`
	CanPeng    bool    `json:"canPeng"`
	CanGang    bool    `json:"canGang"`
	GangCards  []int   `json:"gangCards"`
	CanChi     bool    `json:"canChi"`
	ChiOptions [][]int `json:"chiOptions"`
	CanPass    bool    `json:"canPass"`
}

func newAvailableActions() *AvailableActions {
	return &AvailableActions{
		GangCards:  []int{},
		ChiOptions: [][]int{},
	}
}

func (a *AvailableActions) clone() *AvailableActions {
	c := *a
	c.GangCards = append([]int{}, a.GangCards...)
	c.ChiOptions = append([][]int{}, a.ChiOptions...)
	return &c
}

// 对应 Java GameStateView.AIRecommendation。
type AIRecommendation struct {
	RecommendedAction  string  `json:"recommendedAction"` // discard / peng / gang / hu / chi / pass
	RecommendedDiscard int     `json:"recommendedDiscard"`
	RecommendedCard    int     `json:"recommendedCard"`
	RecommendedCards   []int   `json:"recommendedCards"`
	Score              float64 `json:"score"`
	Reason             string  `json:"reason"`
}

func newDiscardRecommendation(discard int, score float64, reason string) *AIRecommendation {
	return &AIRecommendation{
		RecommendedAction:  "discard",
		RecommendedDiscard: discard,
		RecommendedCard:    discard,
		RecommendedCards:   []int{discard},
		Score:              score,
		Reason:             reason,
	}
}

func newRecommendation(action string, card int, cards []int, score float64, reason string) *AIRecommendation {
	rec := &AIRecommendation{
		RecommendedAction:  action,
		RecommendedDiscard: 0,
		RecommendedCard:    card,
		RecommendedCards:   []int{},
		Score:              score,
		Reason:             reason,
	}
	if action == "discard" {
		rec.RecommendedDiscard = card
	}
	rec.RecommendedCards = append(rec.RecommendedCards, cards...)
	return rec
}

// 对应 Java GameStateView.SettlementInfo。
type SettlementInfo struct {
	IsDraw       bool           `json:"isDraw"` // 是否荒庄流局
	WinnerSeat   int            `json:"winnerSeat"`
	ProviderSeat int            `json:"providerSeat"` // 点炮者(-1 为自摸)
	IsZimo       bool           `json:"isZimo"`
	WinCard      int            `json:"winCard"`
	Patterns     []string       `json:"patterns"`
	TotalFan     int            `json:"totalFan"`
	Points       int            `json:"points"`
	ScoreDeltas  map[string]int `json:"scoreDeltas"`
}

func newSettlementInfo() *SettlementInfo {
	return &SettlementInfo{
		WinnerSeat:   -1,
		ProviderSeat: -1,
		Patterns:     []string{},
		ScoreDeltas:  map[string]int{},
	}
}

// 对应 Java GameStateView。
// aiRecommendation / settlement 在 Java 中可为 null 且 Gson 会省略,故用指针 + omitempty;
// 其余字段 Java 侧恒有初始值,JSON 中必须始终存在。
type GameStateView struct {
	GameId             int                      `json:"gameId"`
	WallCount          int                      `json:"wallCount"`
	GuiCards           []int                    `json:"guiCards"`
	GuiIndicator       int                      `json:"guiIndicator"`
	DealerSeat         int                      `json:"dealerSeat"`
	CurrentSeat        int                      `json:"currentSeat"`
	Phase              string                   `json:"phase"` // DISCARD / RESPONSE / WAITING_USER / GAME_OVER
	LastDiscard        int                      `json:"lastDiscard"`
	LastDiscardSeat    int                      `json:"lastDiscardSeat"`
	Players            []*PlayerView            `json:"players"`
	AvailableActions   *AvailableActions        `json:"availableActions"`
	CurrentTingTargets []*TingTarget            `json:"currentTingTargets"`
	DiscardToTing      map[string][]*TingTarget `json:"discardToTing"`
	AiRecommendation   *AIRecommendation        `json:"aiRecommendation,omitempty"`
	Logs               []string                 `json:"logs"`
	Settlement         *SettlementInfo          `json:"settlement,omitempty"`
	CardRemainCounts   map[string]int           `json:"cardRemainCounts"`
	SpectatorMode      bool                     `json:"spectatorMode"`
}

func newGameStateView() *GameStateView {
	return &GameStateView{
		GuiCards:           []int{},
		LastDiscardSeat:    -1,
		Players:            []*PlayerView{},
		AvailableActions:   newAvailableActions(),
		CurrentTingTargets: []*TingTarget{},
		DiscardToTing:      map[string][]*TingTarget{},
		Logs:               []string{},
		CardRemainCounts:   map[string]int{},
	}
}

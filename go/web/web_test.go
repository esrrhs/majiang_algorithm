// 移植自 Java MahjongGameWebIntegrationTest,并补充 HTTP 接口冒烟与 JSON 结构校验。
package web

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"os"
	"strings"
	"testing"

	majiang "github.com/esrrhs/majiang_algorithm/go"
)

func TestMain(m *testing.M) {
	if err := majiang.Load(); err != nil {
		fmt.Fprintln(os.Stderr, "加载查表文件失败:", err)
		os.Exit(1)
	}
	os.Exit(m.Run())
}

func TestGameInitialization(t *testing.T) {
	game := NewMahjongGame("random_flip", 0, false)
	if game.GetPhase() != PhaseDiscard {
		t.Fatalf("phase = %s, want DISCARD", game.GetPhase())
	}
	if game.GetCurrentSeat() != 0 {
		t.Fatalf("currentSeat = %d, want 0", game.GetCurrentSeat())
	}

	view := game.CreateView(0)
	if len(view.Players) != 4 {
		t.Fatalf("players = %d, want 4", len(view.Players))
	}
	if view.Players[0].TileCount != 14 {
		t.Fatalf("player0 tileCount = %d, want 14", view.Players[0].TileCount)
	}
	if view.Players[1].TileCount != 13 {
		t.Fatalf("player1 tileCount = %d, want 13", view.Players[1].TileCount)
	}
	if len(view.GuiCards) == 0 {
		t.Fatal("guiCards should not be empty")
	}
	if view.WallCount <= 0 {
		t.Fatalf("wallCount = %d, should be > 0", view.WallCount)
	}
}

func TestTingDetectionInGame(t *testing.T) {
	game := NewMahjongGame("card", majiang.JianBai, false)
	view := game.CreateView(0)
	if view.DiscardToTing == nil {
		t.Fatal("discardToTing should not be nil")
	}
}

func TestAiSimulationSteps(t *testing.T) {
	// 全AI观战模式
	game := NewMahjongGame("random_flip", 0, true)
	steps := 0
	for game.GetPhase() != PhaseGameOver && steps < 100 {
		if !game.Step() {
			break
		}
		steps++
	}
	if steps <= 0 {
		t.Fatal("AI steps should advance the game state")
	}

	// 补充:完整对局应能正常结算,分数守恒
	for game.GetPhase() != PhaseGameOver && steps < 2000 {
		if !game.Step() {
			break
		}
		steps++
	}
	if game.GetPhase() != PhaseGameOver {
		t.Fatalf("game should finish within 2000 steps, phase = %s", game.GetPhase())
	}
	view := game.CreateView(0)
	if view.Settlement == nil {
		t.Fatal("settlement should be present after game over")
	}
	total := 0
	for _, p := range view.Players {
		total += p.Score
	}
	if total != 4000 {
		t.Fatalf("scores should sum to 4 * 1000, got %d", total)
	}
	if !view.Settlement.IsDraw {
		if view.Settlement.WinnerSeat < 0 || view.Settlement.WinnerSeat > 3 {
			t.Fatalf("invalid winner seat: %d", view.Settlement.WinnerSeat)
		}
		if len(view.Settlement.Patterns) == 0 {
			t.Fatal("patterns should not be empty on win")
		}
	}
}

// startTestServer 启动 httptest 服务。
func startTestServer(t *testing.T) *httptest.Server {
	t.Helper()
	srv := NewServer(0)
	ts := httptest.NewServer(srv.Handler())
	t.Cleanup(ts.Close)
	return ts
}

func postJSON(t *testing.T, url string, body string) map[string]interface{} {
	t.Helper()
	resp, err := http.Post(url, "application/json", bytes.NewBufferString(body))
	if err != nil {
		t.Fatalf("POST %s failed: %v", url, err)
	}
	defer resp.Body.Close()
	data, _ := io.ReadAll(resp.Body)
	if resp.StatusCode != 200 {
		t.Fatalf("POST %s status = %d, body = %s", url, resp.StatusCode, data)
	}
	var m map[string]interface{}
	if err := json.Unmarshal(data, &m); err != nil {
		t.Fatalf("json decode failed: %v, body = %s", err, data)
	}
	return m
}

func TestHttpApiSmoke(t *testing.T) {
	ts := startTestServer(t)

	// /api/status
	resp, err := http.Get(ts.URL + "/api/status")
	if err != nil {
		t.Fatal(err)
	}
	data, _ := io.ReadAll(resp.Body)
	resp.Body.Close()
	if !strings.Contains(string(data), `"status":"ok"`) || !strings.Contains(string(data), `"ready":true`) {
		t.Fatalf("unexpected status body: %s", data)
	}

	// 静态资源
	resp, err = http.Get(ts.URL + "/")
	if err != nil {
		t.Fatal(err)
	}
	data, _ = io.ReadAll(resp.Body)
	resp.Body.Close()
	if !strings.Contains(string(data), "<!DOCTYPE html>") {
		t.Fatal("index.html should be served at /")
	}
	if ct := resp.Header.Get("Content-Type"); !strings.Contains(ct, "text/html") {
		t.Fatalf("index content-type = %s", ct)
	}
	resp, err = http.Get(ts.URL + "/style.css")
	if err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if resp.StatusCode != 200 {
		t.Fatalf("style.css status = %d", resp.StatusCode)
	}

	// 新对局:JSON 字段名与 Java 版一致(isAi/isTing 等驼峰键必须存在)
	m := postJSON(t, ts.URL+"/api/game/new", `{"guiMode":"random_flip","spectator":true}`)
	raw, _ := json.Marshal(m)
	for _, key := range []string{"gameId", "wallCount", "guiCards", "guiIndicator", "dealerSeat",
		"currentSeat", "phase", "lastDiscard", "lastDiscardSeat", "players", "availableActions",
		"currentTingTargets", "discardToTing", "logs", "cardRemainCounts", "spectatorMode"} {
		if !bytes.Contains(raw, []byte(`"`+key+`"`)) {
			t.Fatalf("view JSON missing key %q:\n%s", key, raw)
		}
	}
	if !bytes.Contains(raw, []byte(`"isAi"`)) || !bytes.Contains(raw, []byte(`"isTing"`)) ||
		!bytes.Contains(raw, []byte(`"isHu"`)) || !bytes.Contains(raw, []byte(`"tileCount"`)) {
		t.Fatalf("player view JSON missing isAi/isTing/isHu/tileCount:\n%s", raw)
	}
	if !strings.Contains(string(raw), `"canDiscard"`) || !strings.Contains(string(raw), `"canHu"`) {
		t.Fatal("availableActions keys missing")
	}

	// 观战模式下自动跑完整局
	for i := 0; i < 500; i++ {
		m = postJSON(t, ts.URL+"/api/game/step", "")
		if m["stepped"] != true {
			break
		}
		if view, ok := m["view"].(map[string]interface{}); ok {
			if view["phase"] == "GAME_OVER" {
				break
			}
		}
	}
	if _, ok := m["view"].(map[string]interface{}); !ok {
		t.Fatal("step response should contain view")
	}
	raw, _ = json.Marshal(m)
	if !bytes.Contains(raw, []byte(`"stepped"`)) {
		t.Fatal("step response missing stepped")
	}
}

func TestAlgoTestApi(t *testing.T) {
	ts := startTestServer(t)

	// 与 Java AlgoTestHandler 相同语义:数组入参 + 单个 gui 整数
	// (注: guiCards 传无花色的纯数字字符串时 Java/Go 都会解析失败并返回 500,行为一致)
	m := postJSON(t, ts.URL+"/api/algo/test",
		`{"cards":[1,1,1,2,3,4,10,11,12,25,26,27,28,28],"gui":29}`)
	raw, _ := json.Marshal(m)
	if !strings.Contains(string(raw), `"cardsStr"`) || !strings.Contains(string(raw), `"isHu"`) ||
		!strings.Contains(string(raw), `"tingCardsStr"`) || !strings.Contains(string(raw), `"recommendedOut"`) ||
		!strings.Contains(string(raw), `"recommendedOutStr"`) || !strings.Contains(string(raw), `"aiScore"`) ||
		!strings.Contains(string(raw), `"huTimeMicros"`) || !strings.Contains(string(raw), `"tingTimeMicros"`) ||
		!strings.Contains(string(raw), `"aiTimeMicros"`) {
		t.Fatalf("algo test response keys missing:\n%s", raw)
	}
	// 14 张无鬼牌标准胡:111万 234万 123筒 789条 东东
	if m["isHu"] != true {
		t.Fatalf("isHu should be true, got %v (resp %s)", m["isHu"], raw)
	}
	if m["recommendedOutStr"] == "" {
		t.Fatal("recommendedOutStr should not be empty for a 14-card hand")
	}

	// 字符串手牌 + 单个 gui 字段
	m = postJSON(t, ts.URL+"/api/algo/test", `{"cards":"1万,2万,3万","gui":5}`)
	if m["isHu"] != false {
		t.Fatalf("3-card hand cannot hu, got %v", m["isHu"])
	}
}

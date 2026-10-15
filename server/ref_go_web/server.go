// 轻量内置 HTTP 服务,提供静态前端展示与对局交互 REST API,对应 Java web.MahjongHttpServer。
package web

import (
	"encoding/json"
	"fmt"
	"io"
	"io/fs"
	"net/http"
	"runtime/debug"
	"sort"
	"strconv"
	"strings"
	"sync"
	"time"

	majiang "github.com/esrrhs/majiang_algorithm/go"

	"embed"
)

//go:embed static
var staticFS embed.FS

// Server 对应 Java MahjongHttpServer。
type Server struct {
	port    int
	current *MahjongGame
	mu      sync.Mutex
}

func NewServer(port int) *Server {
	return &Server{port: port, current: NewMahjongGame("random_flip", 0, false)}
}

// Handler 返回根 handler,便于测试与外层挂载。
func (s *Server) Handler() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("/api/status", s.statusHandler)
	mux.HandleFunc("/api/game/new", s.newGameHandler)
	mux.HandleFunc("/api/game/state", s.stateHandler)
	mux.HandleFunc("/api/game/spectator", s.spectatorHandler)
	mux.HandleFunc("/api/game/discard", s.discardHandler)
	mux.HandleFunc("/api/game/action", s.actionHandler)
	mux.HandleFunc("/api/game/self_action", s.selfActionHandler)
	mux.HandleFunc("/api/game/step", s.stepHandler)
	mux.HandleFunc("/api/game/auto_run", s.autoRunHandler)
	mux.HandleFunc("/api/algo/test", s.algoTestHandler)
	mux.HandleFunc("/", s.staticResourceHandler)
	return mux
}

// Start 阻塞启动 HTTP 服务。
func (s *Server) Start() error {
	fmt.Println("=================================================")
	fmt.Println(" 麻将网页端对战与算法演示平台已启动！(Go 版)")
	fmt.Printf(" 请在浏览器中打开: http://localhost:%d\n", s.port)
	fmt.Println("=================================================")
	return http.ListenAndServe(fmt.Sprintf(":%d", s.port), s.Handler())
}

func writeJSON(w http.ResponseWriter, statusCode int, data interface{}) {
	bytes, err := json.Marshal(data)
	if err != nil {
		statusCode = 500
		bytes = []byte(`{"error": "json marshal failed"}`)
	}
	w.Header().Set("Content-Type", "application/json; charset=UTF-8")
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type")
	w.WriteHeader(statusCode)
	w.Write(bytes)
}

// readBody 对应 Java readBody(按行拼接,兼容任意请求体)。
func readBody(r *http.Request) string {
	body, err := io.ReadAll(r.Body)
	if err != nil {
		return ""
	}
	return string(body)
}

// parseJSON 对应 Java gson.fromJson(body, JsonObject.class);解析失败返回错误。
func parseJSON(body string) (map[string]interface{}, error) {
	m := map[string]interface{}{}
	if body == "" {
		return m, nil
	}
	dec := json.NewDecoder(strings.NewReader(body))
	if err := dec.Decode(&m); err != nil {
		return nil, err
	}
	return m, nil
}

func jsonGetInt(m map[string]interface{}, key string) int {
	if v, ok := m[key]; ok {
		switch n := v.(type) {
		case float64:
			return int(n)
		case string:
			i, _ := strconv.Atoi(n)
			return i
		}
	}
	return 0
}

func jsonGetBool(m map[string]interface{}, key string) bool {
	if v, ok := m[key]; ok {
		if b, ok := v.(bool); ok {
			return b
		}
	}
	return false
}

func jsonGetString(m map[string]interface{}, key string) string {
	if v, ok := m[key]; ok {
		if s, ok := v.(string); ok {
			return s
		}
	}
	return ""
}

// withRecovery 对应 Java handler 里的 catch(Throwable) -> 500 {"error": ...}。
func withRecovery(fn http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		defer func() {
			if t := recover(); t != nil {
				msg := fmt.Sprintf("%v", t)
				if msg == "" {
					msg = "Internal Error"
				}
				fmt.Println(msg)
				fmt.Println(string(debug.Stack()))
				writeJSON(w, 500, map[string]interface{}{"error": msg})
			}
		}()
		fn(w, r)
	}
}

func (s *Server) statusHandler(w http.ResponseWriter, r *http.Request) {
	writeJSON(w, 200, map[string]interface{}{"status": "ok", "ready": true})
}

func (s *Server) newGameHandler(w http.ResponseWriter, r *http.Request) {
	withRecovery(func(w http.ResponseWriter, r *http.Request) {
		if r.Method == http.MethodOptions {
			writeJSON(w, 204, "")
			return
		}
		m, err := parseJSON(readBody(r))
		if err != nil {
			panic(err)
		}
		guiMode := "random_flip"
		if v, ok := m["guiMode"]; ok {
			if gs, ok := v.(string); ok {
				guiMode = gs
			}
		}
		customGuiCard := jsonGetInt(m, "customGuiCard")
		spectator := jsonGetBool(m, "spectator")

		s.mu.Lock()
		s.current = NewMahjongGame(guiMode, customGuiCard, spectator)
		view := s.current.CreateView(0)
		s.mu.Unlock()
		writeJSON(w, 200, view)
	})(w, r)
}

func (s *Server) stateHandler(w http.ResponseWriter, r *http.Request) {
	s.mu.Lock()
	view := s.current.CreateView(0)
	s.mu.Unlock()
	writeJSON(w, 200, view)
}

func (s *Server) spectatorHandler(w http.ResponseWriter, r *http.Request) {
	withRecovery(func(w http.ResponseWriter, r *http.Request) {
		if r.Method == http.MethodOptions {
			writeJSON(w, 204, "")
			return
		}
		m, err := parseJSON(readBody(r))
		if err != nil {
			panic(err)
		}
		spectator := jsonGetBool(m, "spectator")
		s.mu.Lock()
		s.current.SetSpectatorMode(spectator)
		view := s.current.CreateView(0)
		s.mu.Unlock()
		writeJSON(w, 200, view)
	})(w, r)
}

func (s *Server) discardHandler(w http.ResponseWriter, r *http.Request) {
	withRecovery(func(w http.ResponseWriter, r *http.Request) {
		if r.Method == http.MethodOptions {
			writeJSON(w, 204, "")
			return
		}
		m, err := parseJSON(readBody(r))
		if err != nil {
			panic(err)
		}
		card := jsonGetInt(m, "card")

		s.mu.Lock()
		success := s.current.Discard(0, card)
		view := s.current.CreateView(0)
		s.mu.Unlock()
		writeJSON(w, 200, map[string]interface{}{"success": success, "view": view})
	})(w, r)
}

func (s *Server) actionHandler(w http.ResponseWriter, r *http.Request) {
	withRecovery(func(w http.ResponseWriter, r *http.Request) {
		if r.Method == http.MethodOptions {
			writeJSON(w, 204, "")
			return
		}
		m, err := parseJSON(readBody(r))
		if err != nil {
			panic(err)
		}
		action := jsonGetString(m, "action")
		card := jsonGetInt(m, "card")
		chi1 := jsonGetInt(m, "chi1")
		chi2 := jsonGetInt(m, "chi2")

		s.mu.Lock()
		success := s.current.UserAction(action, card, chi1, chi2)
		view := s.current.CreateView(0)
		s.mu.Unlock()
		writeJSON(w, 200, map[string]interface{}{"success": success, "view": view})
	})(w, r)
}

func (s *Server) selfActionHandler(w http.ResponseWriter, r *http.Request) {
	withRecovery(func(w http.ResponseWriter, r *http.Request) {
		if r.Method == http.MethodOptions {
			writeJSON(w, 204, "")
			return
		}
		m, err := parseJSON(readBody(r))
		if err != nil {
			panic(err)
		}
		action := jsonGetString(m, "action")
		card := jsonGetInt(m, "card")

		s.mu.Lock()
		success := s.current.UserSelfAction(action, card)
		view := s.current.CreateView(0)
		s.mu.Unlock()
		writeJSON(w, 200, map[string]interface{}{"success": success, "view": view})
	})(w, r)
}

func (s *Server) stepHandler(w http.ResponseWriter, r *http.Request) {
	s.mu.Lock()
	stepped := s.current.Step()
	view := s.current.CreateView(0)
	s.mu.Unlock()
	writeJSON(w, 200, map[string]interface{}{"stepped": stepped, "view": view})
}

func (s *Server) autoRunHandler(w http.ResponseWriter, r *http.Request) {
	m, err := parseJSON(readBody(r))
	if err != nil {
		writeJSON(w, 500, map[string]interface{}{"error": err.Error()})
		return
	}
	maxSteps := 10
	if v, ok := m["maxSteps"]; ok {
		if n, ok := v.(float64); ok {
			maxSteps = int(n)
		}
	}

	s.mu.Lock()
	executedSteps := 0
	for i := 0; i < maxSteps; i++ {
		if !s.current.Step() {
			break
		}
		executedSteps++
	}
	view := s.current.CreateView(0)
	s.mu.Unlock()
	writeJSON(w, 200, map[string]interface{}{"executedSteps": executedSteps, "view": view})
}

func (s *Server) algoTestHandler(w http.ResponseWriter, r *http.Request) {
	withRecovery(func(w http.ResponseWriter, r *http.Request) {
		if r.Method == http.MethodOptions {
			writeJSON(w, 204, "")
			return
		}
		m, err := parseJSON(readBody(r))
		if err != nil {
			panic(err)
		}

		cards := parseCardsField(m, "cards")
		guiCards := parseCardsField(m, "guiCards")
		if len(guiCards) == 0 {
			if gui := jsonGetInt(m, "gui"); gui > 0 {
				guiCards = append(guiCards, gui)
			}
		}

		sort.Ints(cards)

		start := time.Now()
		isHu := false
		if len(cards) > 0 && len(cards)%3 == 2 {
			isHu = majiang.IsHuExtra(cards, guiCards, 0)
		}
		huMicros := float64(time.Since(start).Nanoseconds()) / 1000.0

		start = time.Now()
		tingCards := []int{}
		if len(cards) > 0 {
			tingCards = majiang.IsTingExtra(cards, guiCards)
			if tingCards == nil {
				tingCards = []int{}
			}
		}
		tingMicros := float64(time.Since(start).Nanoseconds()) / 1000.0

		recommendedOut := 0
		aiScore := 0.0
		aiMicros := 0.0
		if len(cards) > 0 && len(cards)%3 == 2 {
			start = time.Now()
			recommendedOut = majiang.OutAI(cards, guiCards)
			if recommendedOut > 0 {
				rem := removeFirstInt(cards, recommendedOut)
				aiScore = majiang.Calc(rem, guiCards)
			}
			aiMicros = float64(time.Since(start).Nanoseconds()) / 1000.0
		}

		resp := map[string]interface{}{
			"cards":             cards,
			"cardsStr":          majiang.CardsToString(cards),
			"guiCards":          guiCards,
			"isHu":              isHu,
			"huTimeMicros":      huMicros,
			"tingCards":         tingCards,
			"tingCardsStr":      majiang.CardsToString(tingCards),
			"tingTimeMicros":    tingMicros,
			"recommendedOut":    recommendedOut,
			"recommendedOutStr": boolStr(recommendedOut > 0, majiang.CardToString(recommendedOut), ""),
			"aiScore":           aiScore,
			"aiTimeMicros":      aiMicros,
		}
		writeJSON(w, 200, resp)
	})(w, r)
}

// parseCardsField 对应 Java AlgoTestHandler:数组或 "1万,2万" 字符串均可。
func parseCardsField(m map[string]interface{}, key string) []int {
	v, ok := m[key]
	if !ok {
		return []int{}
	}
	ret := []int{}
	switch t := v.(type) {
	case []interface{}:
		for _, el := range t {
			if n, ok := el.(float64); ok {
				ret = append(ret, int(n))
			}
		}
	case string:
		ret = append(ret, majiang.StringToCards(t)...)
	}
	return ret
}

func (s *Server) staticResourceHandler(w http.ResponseWriter, r *http.Request) {
	path := r.URL.Path
	if path == "/" || path == "" {
		path = "/index.html"
	}

	// 防路径遍历
	if strings.Contains(path, "..") {
		http.Error(w, "404 Not Found", 404)
		return
	}

	sub, err := fs.Sub(staticFS, "static")
	if err != nil {
		http.Error(w, "404 Not Found", 404)
		return
	}

	f, err := sub.Open(strings.TrimPrefix(path, "/"))
	if err != nil {
		http.Error(w, "404 Not Found", 404)
		return
	}
	defer f.Close()
	if stat, err := f.Stat(); err != nil || stat.IsDir() {
		http.Error(w, "404 Not Found", 404)
		return
	}

	w.Header().Set("Content-Type", mimeType(path))
	io.Copy(w, f)
}

func mimeType(path string) string {
	switch {
	case strings.HasSuffix(path, ".html"):
		return "text/html; charset=UTF-8"
	case strings.HasSuffix(path, ".css"):
		return "text/css; charset=UTF-8"
	case strings.HasSuffix(path, ".js"):
		return "application/javascript; charset=UTF-8"
	case strings.HasSuffix(path, ".json"):
		return "application/json; charset=UTF-8"
	case strings.HasSuffix(path, ".png"):
		return "image/png"
	case strings.HasSuffix(path, ".svg"):
		return "image/svg+xml"
	case strings.HasSuffix(path, ".ico"):
		return "image/x-icon"
	default:
		return "application/octet-stream"
	}
}

// RunCliSimulation 终端命令行 4 个 AI 纯自动对局模拟,对应 Java Main.runCliSimulation。
func RunCliSimulation() {
	fmt.Println(">>> 启动 4 个 AI 自动对战模拟 (CLI Benchmark) <<<")
	game := NewMahjongGame("random_flip", 0, true)
	step := 0
	for game.GetPhase() != PhaseGameOver && step < 500 {
		step++
		game.Step()
	}
	fmt.Println(">>> 对战结束！总步数: " + strconv.Itoa(step))
	view := game.CreateView(0)
	if view.Settlement != nil {
		if view.Settlement.IsDraw {
			fmt.Println("结果: 荒庄流局")
		} else {
			fmt.Printf("获胜座位: %d 号位, 胡牌: %s, 番型: %s, 得分: %d\n",
				view.Settlement.WinnerSeat,
				majiang.CardToString(view.Settlement.WinCard),
				strings.Join(view.Settlement.Patterns, ", "),
				view.Settlement.Points)
		}
	}
}

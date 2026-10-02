package majiang

import (
	"testing"
	"time"
)

func benchN(n int, f func()) float64 {
	start := time.Now()
	for i := 0; i < n; i++ {
		f()
	}
	return float64(time.Since(start).Nanoseconds())
}

// 以下测试移植自 Java MajiangAlgorithmIntegrationTest,断言一一对应。

func TestAIOutDecision(t *testing.T) {
	cards := StringToCards("1万,2万,2万,1条,1条,东")
	gui := StringToCards("1万")

	out := OutAI(cards, gui)
	if !containsInt(cards, out) {
		t.Fatalf("out card %d should be in hand", out)
	}
	// Single wind tile '东' should typically be preferred to discard
	if out != FengDong {
		t.Fatalf("expected 东, got %s", CardToString(out))
	}
}

func TestAIPengAndGangDecision(t *testing.T) {
	cardsPeng := StringToCards("1万,2万,2万,1条,1条,2筒,4筒,4筒")
	gui := StringToCards("1万")

	// 对应 Java 侧 assertNotNull(peng): 仅要求决策可执行
	peng := PengAI(cardsPeng, gui, StringToCard("2万"), 0.0)
	t.Logf("peng decision: %v", peng)

	cardsGang := StringToCards("1万,2万,2万,2万,3万,4万,4筒,4筒")
	gang := GangAI(cardsGang, gui, StringToCard("2万"), 1.0)
	t.Logf("gang decision: %v", gang)
}

func TestChiDecision(t *testing.T) {
	cards := StringToCards("1万,2万,2万,1条,1条,1筒,2筒,4筒,4筒,5筒")
	gui := StringToCards("1万")

	chiChoices := ChiAIChoices(cards, gui, StringToCard("3筒"))
	t.Logf("chi choices: %s", CardsToString(chiChoices))
}

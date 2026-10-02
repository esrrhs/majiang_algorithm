package majiang

import "testing"

// 以下测试移植自 Java MajiangAlgorithmIntegrationTest,断言一一对应。

func TestHuDetectionBasic(t *testing.T) {
	// 1万, 1万 (pair with gui = 1万)
	cards := StringToCards("1万,1万")
	gui := StringToCard("1万")
	if !IsHu(cards, gui) {
		t.Fatal("IsHu should be true")
	}
}

func TestHuDetectionCompleteHands(t *testing.T) {
	// 111万 234万 123筒 789条 东东 (standard 14-card winning hand, 0 gui)
	cards := StringToCards("1万,1万,1万,2万,3万,4万,1筒,2筒,3筒,7条,8条,9条,东,东")
	if !IsHuExtra(cards, []int{}, 0) {
		t.Fatal("standard winning hand should hu")
	}

	// Not a winning hand (missing pair or meld)
	notHuCards := StringToCards("1万,1万,1万,2万,3万,4万,1筒,2筒,3筒,7条,8条,9条,东,南")
	if IsHuExtra(notHuCards, []int{}, 0) {
		t.Fatal("not a winning hand")
	}

	// With gui wildcard
	if !IsHuExtra(notHuCards, []int{FengNan}, 0) {
		t.Fatal("with gui wildcard should hu")
	}
}

func TestTingCalculation(t *testing.T) {
	cards := StringToCards("1万,1万,1筒,3筒,2筒,2条,3条,4条,东,东")
	gui := StringToCard("1筒")

	tingCards := IsTing(cards, gui)
	if len(tingCards) == 0 {
		t.Fatal("ting should not be empty")
	}

	tingExtraCards := IsTingExtra(cards, []int{gui})
	if len(tingExtraCards) == 0 {
		t.Fatal("tingExtra should not be empty")
	}
}

func TestBenchmarkHuDetection(t *testing.T) {
	cards := StringToCards("1万,1万,1万,2万,3万,4万,1筒,2筒,3筒,7条,8条,9条,东,东")
	emptyGui := []int{}

	// Warmup
	for i := 0; i < 1000; i++ {
		IsHuExtra(cards, emptyGui, 0)
	}

	elapsed := benchN(10000, func() { IsHuExtra(cards, emptyGui, 0) })
	avgMicros := elapsed / 10000.0 / 1000.0
	t.Logf("[BENCHMARK] IsHuExtra average execution time: %.3f µs per call", avgMicros)
	// Winning check with hash lookup table is expected to be under 100 microseconds
	if avgMicros >= 100.0 {
		t.Fatalf("check took longer than expected: %f µs", avgMicros)
	}
}

func BenchmarkIsHuExtra(b *testing.B) {
	cards := StringToCards("1万,1万,1万,2万,3万,4万,1筒,2筒,3筒,7条,8条,9条,东,东")
	emptyGui := []int{}
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		IsHuExtra(cards, emptyGui, 0)
	}
}

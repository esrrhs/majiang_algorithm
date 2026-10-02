// 移植自 Java MajiangAlgorithmIntegrationTest / Go huutil_test.go,断言一一对应。
#include <chrono>

#include "majiang/hu_util.h"
#include "test_util.h"

using namespace majiang;

MAJ_TEST(TestHuDetectionBasic)
{
    // 1万, 1万 (pair with gui = 1万)
    std::vector<int> cards = StringToCards("1万,1万");
    int gui = StringToCard("1万");
    EXPECT(IsHu(cards, gui));
}

MAJ_TEST(TestHuDetectionCompleteHands)
{
    // 111万 234万 123筒 789条 东东 (standard 14-card winning hand, 0 gui)
    std::vector<int> cards = StringToCards("1万,1万,1万,2万,3万,4万,1筒,2筒,3筒,7条,8条,9条,东,东");
    EXPECT(IsHuExtra(cards, {}, 0));

    // Not a winning hand (missing pair or meld)
    std::vector<int> notHuCards = StringToCards("1万,1万,1万,2万,3万,4万,1筒,2筒,3筒,7条,8条,9条,东,南");
    EXPECT(!IsHuExtra(notHuCards, {}, 0));

    // With gui wildcard
    EXPECT(IsHuExtra(notHuCards, { FengNan }, 0));
}

MAJ_TEST(TestTingCalculation)
{
    std::vector<int> cards = StringToCards("1万,1万,1筒,3筒,2筒,2条,3条,4条,东,东");
    int gui = StringToCard("1筒");

    std::vector<int> tingCards = IsTing(cards, gui);
    EXPECT(!tingCards.empty());

    std::vector<int> tingExtraCards = IsTingExtra(cards, { gui });
    EXPECT(!tingExtraCards.empty());
}

MAJ_TEST(TestBenchmarkHuDetection)
{
    std::vector<int> cards = StringToCards("1万,1万,1万,2万,3万,4万,1筒,2筒,3筒,7条,8条,9条,东,东");
    std::vector<int> emptyGui;

    // Warmup
    for (int i = 0; i < 1000; i++)
    {
        IsHuExtra(cards, emptyGui, 0);
    }

    int iterations = 10000;
    auto start = std::chrono::steady_clock::now();
    for (int i = 0; i < iterations; i++)
    {
        IsHuExtra(cards, emptyGui, 0);
    }
    double avgMicros =
        std::chrono::duration<double, std::micro>(std::chrono::steady_clock::now() - start).count() / iterations;

    std::printf("  [BENCHMARK] IsHuExtra average execution time: %.3f µs per call\n", avgMicros);
    // Winning check with hash lookup table is expected to be under 100 microseconds
    EXPECT(avgMicros < 100.0);
}

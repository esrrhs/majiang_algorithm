// 移植自 Java MajiangAlgorithmIntegrationTest / Go aiutil_test.cpp,断言一一对应。
#include "detail.h"
#include "majiang/ai_util.h"
#include "test_util.h"

using namespace majiang;

MAJ_TEST(TestAIOutDecision)
{
    std::vector<int> cards = StringToCards("1万,2万,2万,1条,1条,东");
    std::vector<int> gui = StringToCards("1万");

    int out = OutAI(cards, gui);
    EXPECT(detail::containsInt(cards, out));
    // Single wind tile '东' should typically be preferred to discard
    EXPECT_EQ(out, FengDong);
}

MAJ_TEST(TestAIPengAndGangDecision)
{
    std::vector<int> cardsPeng = StringToCards("1万,2万,2万,1条,1条,2筒,4筒,4筒");
    std::vector<int> gui = StringToCards("1万");

    // 对应 Java 侧 assertNotNull(peng): 仅要求决策可执行
    bool peng = PengAI(cardsPeng, gui, StringToCard("2万"), 0.0);
    std::printf("  peng decision: %s\n", peng ? "true" : "false");

    std::vector<int> cardsGang = StringToCards("1万,2万,2万,2万,3万,4万,4筒,4筒");
    bool gang = GangAI(cardsGang, gui, StringToCard("2万"), 1.0);
    std::printf("  gang decision: %s\n", gang ? "true" : "false");
}

MAJ_TEST(TestChiDecision)
{
    std::vector<int> cards = StringToCards("1万,2万,2万,1条,1条,1筒,2筒,4筒,4筒,5筒");
    std::vector<int> gui = StringToCards("1万");

    std::vector<int> chiChoices = ChiAIChoices(cards, gui, StringToCard("3筒"));
    std::printf("  chi choices: %s\n", CardsToString(chiChoices).c_str());
}

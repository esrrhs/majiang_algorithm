// 回放 data/parity_cases.txt:由 Java 侧 ParityDump 以固定种子导出的判胡/听牌/AI 决策样例,
// C++ 实现必须逐字段复现(与 Java ParityFixtureTest、Go parity_test.go 同一契约)。
#include <algorithm>
#include <cstdlib>
#include <fstream>
#include <string>
#include <vector>

#include "detail.h"
#include "majiang/ai_util.h"
#include "majiang/hu_util.h"
#include "test_util.h"

using namespace majiang;

namespace
{

std::vector<std::string> split(const std::string &s, char sep)
{
    std::vector<std::string> out;
    size_t begin = 0;
    while (true)
    {
        size_t end = s.find(sep, begin);
        out.push_back(s.substr(begin, end == std::string::npos ? std::string::npos : end - begin));
        if (end == std::string::npos)
        {
            break;
        }
        begin = end + 1;
    }
    return out;
}

int toInt(const std::string &s)
{
    return std::atoi(s.c_str());
}

} // namespace

MAJ_TEST(TestParityFixture)
{
    auto path = detail::findDataFile("parity_cases.txt");
    if (!path)
    {
        std::printf("  [SKIP] 找不到 parity_cases.txt\n");
        return;
    }
    std::ifstream f(*path);
    EXPECT(static_cast<bool>(f));

    int caseIndex = 0;
    std::string line;
    while (std::getline(f, line))
    {
        if (!line.empty() && line.back() == '\r')
        {
            line.pop_back();
        }
        if (line.empty() || line[0] == '#')
        {
            continue;
        }
        std::vector<std::string> fields = split(line, '|');
        EXPECT_EQ(fields.size(), static_cast<size_t>(13));
        if (fields.size() != 13)
        {
            return;
        }

        std::vector<int> hand = StringToCards(fields[0]);
        int gui = toInt(fields[1]);
        std::vector<int> guiList = { gui };
        int chiCard = toInt(fields[8]);

        EXPECT_EQ(IsHu(hand, gui) ? 1 : 0, toInt(fields[2]));
        EXPECT_EQ(IsHuExtra(hand, guiList, 0) ? 1 : 0, toInt(fields[3]));
        EXPECT_EQ(CardsToString(IsTing(hand, gui)), fields[4]);
        EXPECT_EQ(CardsToString(IsTingExtra(hand, guiList)), fields[5]);

        double wantCalc = std::strtod(fields[6].c_str(), nullptr);
        if (Calc(hand, guiList) != wantCalc)
        {
            EXPECT_EQ(Calc(hand, guiList), wantCalc);
        }

        int wantOut = toInt(fields[7]);
        if (OutAI(hand, guiList) != wantOut)
        {
            EXPECT_EQ(OutAI(hand, guiList), wantOut);
        }

        EXPECT_EQ(CardsToString(ChiAIChoices(hand, guiList, chiCard)), fields[9]);
        EXPECT_EQ(ChiAI(hand, guiList, chiCard, chiCard - 1, chiCard + 1) ? 1 : 0, toInt(fields[10]));
        EXPECT_EQ(PengAI(hand, guiList, chiCard, 0) ? 1 : 0, toInt(fields[11]));
        EXPECT_EQ(GangAI(hand, guiList, chiCard, 1) ? 1 : 0, toInt(fields[12]));
        caseIndex++;
    }
    EXPECT(caseIndex > 0);
    std::printf("  [PARITY] replayed %d cases against C++ implementation\n", caseIndex);
}

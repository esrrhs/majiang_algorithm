// AI 决策逻辑,对应 Go aiutil.go / Java AIUtil。
#include "majiang/ai_util.h"

#include <cfloat>
#include <limits>

#include "detail.h"

namespace majiang
{

using namespace detail;

namespace
{

using AiEntryList = const std::vector<AITableInfo> *;

void calcAITableInfo(std::vector<double> &ret, const std::vector<AiEntryList> &tmp, size_t index, bool jiang,
                     double cur)
{
    if (index >= tmp.size())
    {
        if (jiang)
        {
            ret.push_back(cur);
        }
        return;
    }
    AiEntryList aiTableInfos = tmp[index];
    if (aiTableInfos == nullptr)
    {
        return;
    }
    for (const AITableInfo &aiTableInfo : *aiTableInfos)
    {
        if (jiang)
        {
            if (!aiTableInfo.Jiang)
            {
                calcAITableInfo(ret, tmp, index + 1, jiang, cur + aiTableInfo.P);
            }
        }
        else
        {
            calcAITableInfo(ret, tmp, index + 1, aiTableInfo.Jiang, cur + aiTableInfo.P);
        }
    }
}

} // namespace

double Calc(const std::vector<int> &input, const std::vector<int> &guiCard)
{
    std::vector<int> cards = countCards(input);

    int guiNum = 0;
    for (int gui : guiCard)
    {
        guiNum += cards[gui - 1];
        cards[gui - 1] = 0;
    }

    std::vector<int> ting = IsTingCard(cards, guiNum);
    if (!ting.empty())
    {
        return static_cast<double>(static_cast<long long>(ting.size()) * 10);
    }

    int64_t wanKey = 0, tongKey = 0, tiaoKey = 0, fengKey = 0, jianKey = 0;
    for (int i = Wan1; i <= Wan9; i++)
    {
        wanKey = wanKey * 10 + cards[i - 1];
    }
    for (int i = Tong1; i <= Tong9; i++)
    {
        tongKey = tongKey * 10 + cards[i - 1];
    }
    for (int i = Tiao1; i <= Tiao9; i++)
    {
        tiaoKey = tiaoKey * 10 + cards[i - 1];
    }
    for (int i = FengDong; i <= FengBei; i++)
    {
        fengKey = fengKey * 10 + cards[i - 1];
    }
    for (int i = JianZhong; i <= JianBai; i++)
    {
        jianKey = jianKey * 10 + cards[i - 1];
    }

    // Java 侧不做键存在性检查,查不到的键以 null 进入列表并在递归里跳过
    std::vector<AiEntryList> tmp;
    auto lookup = [](const std::unordered_map<int64_t, std::vector<AITableInfo>> &m, int64_t key) {
        auto it = m.find(key);
        return it != m.end() ? &it->second : nullptr;
    };
    tmp.push_back(lookup(AITable, wanKey));
    tmp.push_back(lookup(AITable, tongKey));
    tmp.push_back(lookup(AITable, tiaoKey));
    tmp.push_back(lookup(AITableFeng, fengKey));
    tmp.push_back(lookup(AITableJian, jianKey));

    std::vector<double> ret;
    calcAITableInfo(ret, tmp, 0, false, 0);

    if (ret.empty())
    {
        return 0;
    }

    double d = ret[0];
    for (size_t i = 1; i < ret.size(); i++)
    {
        if (ret[i] > d)
        {
            d = ret[i];
        }
    }
    return d;
}

int OutAI(const std::vector<int> &input, const std::vector<int> &guiCard)
{
    int ret = 0;
    double max = -std::numeric_limits<double>::max();
    int cache[MaxNum + 1] = { 0 };
    for (int c : input)
    {
        if (cache[c] == 0)
        {
            if (!containsInt(guiCard, c))
            {
                std::vector<int> tmp = removeFirst(input, c);
                double score = Calc(tmp, guiCard);
                if (score > max)
                {
                    max = score;
                    ret = c;
                }
            }
        }
        cache[c] = 1;
    }
    return ret;
}

bool ChiAI(const std::vector<int> &input, const std::vector<int> &guiCard, int card, int card1, int card2)
{
    if (containsInt(guiCard, card) || containsInt(guiCard, card1) || containsInt(guiCard, card2))
    {
        return false;
    }

    if (frequency(input, card1) < 1 || frequency(input, card2) < 1)
    {
        return false;
    }

    double score = Calc(input, guiCard);

    std::vector<int> tmp = removeFirst(removeFirst(input, card1), card2);
    double scoreNew = Calc(tmp, guiCard);

    return scoreNew >= score;
}

std::vector<int> ChiAIChoices(const std::vector<int> &input, const std::vector<int> &guiCard, int card)
{
    std::vector<int> ret;
    if (containsInt(guiCard, card))
    {
        return ret;
    }

    double score = Calc(input, guiCard);
    double scoreNewMax = 0;

    int card1 = 0;
    int card2 = 0;

    if (frequency(input, card - 2) > 0 && frequency(input, card - 1) > 0 &&
        CardType(card) == CardType(card - 2) && CardType(card) == CardType(card - 1))
    {
        std::vector<int> tmp = removeFirst(removeFirst(input, card - 2), card - 1);
        double scoreNew = Calc(tmp, guiCard);
        if (scoreNew > scoreNewMax)
        {
            scoreNewMax = scoreNew;
            card1 = card - 2;
            card2 = card - 1;
        }
    }

    if (frequency(input, card - 1) > 0 && frequency(input, card + 1) > 0 &&
        CardType(card) == CardType(card - 1) && CardType(card) == CardType(card + 1))
    {
        std::vector<int> tmp = removeFirst(removeFirst(input, card - 1), card + 1);
        double scoreNew = Calc(tmp, guiCard);
        if (scoreNew > scoreNewMax)
        {
            scoreNewMax = scoreNew;
            card1 = card - 1;
            card2 = card + 1;
        }
    }

    if (frequency(input, card + 1) > 0 && frequency(input, card + 2) > 0 &&
        CardType(card) == CardType(card + 1) && CardType(card) == CardType(card + 2))
    {
        std::vector<int> tmp = removeFirst(removeFirst(input, card + 1), card + 2);
        double scoreNew = Calc(tmp, guiCard);
        if (scoreNew > scoreNewMax)
        {
            scoreNewMax = scoreNew;
            card1 = card + 1;
            card2 = card + 2;
        }
    }

    if (scoreNewMax > score)
    {
        ret.push_back(card1);
        ret.push_back(card2);
    }

    return ret;
}

bool PengAI(const std::vector<int> &input, const std::vector<int> &guiCard, int card, double award)
{
    if (containsInt(guiCard, card))
    {
        return false;
    }

    if (frequency(input, card) < 2)
    {
        return false;
    }

    double score = Calc(input, guiCard);

    std::vector<int> tmp = removeFirst(removeFirst(input, card), card);
    double scoreNew = Calc(tmp, guiCard);

    return scoreNew + award >= score;
}

bool GangAI(const std::vector<int> &input, const std::vector<int> &guiCard, int card, double award)
{
    if (containsInt(guiCard, card))
    {
        return false;
    }

    if (frequency(input, card) < 3)
    {
        return false;
    }

    double score = Calc(input, guiCard);

    std::vector<int> tmp = removeFirst(removeFirst(removeFirst(removeFirst(input, card), card), card), card);
    double scoreNew = Calc(tmp, guiCard);

    return scoreNew + award >= score;
}

} // namespace majiang

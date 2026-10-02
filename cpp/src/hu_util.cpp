// 判胡 / 听牌核心逻辑,对应 Go huutil.go / Java HuUtil。
#include "majiang/hu_util.h"

#include "detail.h"

namespace majiang
{

using namespace detail;

namespace
{

// Java: tmp 列表元素可为 null(查不到的键),C++ 用空指针表达同样语义。
using HuEntryList = const std::vector<HuTableInfo> *;

bool isHuTableInfo(const std::vector<HuEntryList> &tmp, size_t index, int guiNum, bool jiang)
{
    if (index >= tmp.size())
    {
        return (guiNum % 3 == 0 && jiang) || (guiNum % 3 == 2 && !jiang);
    }
    for (const HuTableInfo &huTableInfo : *tmp[index])
    {
        if (jiang)
        {
            if (huTableInfo.Hupai == std::nullopt && huTableInfo.NeedGui <= guiNum && !huTableInfo.Jiang)
            {
                if (isHuTableInfo(tmp, index + 1, guiNum - huTableInfo.NeedGui, jiang))
                {
                    return true;
                }
            }
        }
        else
        {
            if (huTableInfo.Hupai == std::nullopt && huTableInfo.NeedGui <= guiNum)
            {
                if (isHuTableInfo(tmp, index + 1, guiNum - huTableInfo.NeedGui, huTableInfo.Jiang))
                {
                    return true;
                }
            }
        }
    }
    return false;
}

bool isTingHuTableInfo(const std::vector<int> &tmpType, const std::vector<HuEntryList> &tmp, size_t index,
                       int guiNum, bool jiang, int tingType)
{
    if (index >= tmp.size())
    {
        return guiNum == 0 && jiang;
    }
    if (tmpType[index] == tingType)
    {
        return isTingHuTableInfo(tmpType, tmp, index + 1, guiNum, jiang, tingType);
    }
    for (const HuTableInfo &huTableInfo : *tmp[index])
    {
        if (huTableInfo.Hupai == std::nullopt && huTableInfo.NeedGui <= guiNum)
        {
            if (jiang)
            {
                if (!huTableInfo.Jiang)
                {
                    if (isTingHuTableInfo(tmpType, tmp, index + 1, guiNum - huTableInfo.NeedGui, jiang, tingType))
                    {
                        return true;
                    }
                }
            }
            else
            {
                if (isTingHuTableInfo(tmpType, tmp, index + 1, guiNum - huTableInfo.NeedGui, huTableInfo.Jiang,
                                      tingType))
                {
                    return true;
                }
            }
        }
    }
    return false;
}

} // namespace

bool IsHu(const std::vector<int> &input, int guiCard)
{
    std::vector<int> cards = countCards(input);
    int guiNum = cards[guiCard - 1];
    cards[guiCard - 1] = 0;
    return IsHuCard(cards, guiNum);
}

bool IsHuExtra(const std::vector<int> &input, const std::vector<int> &guiCard, int extra)
{
    std::vector<int> cards = countCards(input);

    int guiNum = 0;
    for (int gui : guiCard)
    {
        guiNum += cards[gui - 1];
        cards[gui - 1] = 0;
    }

    if (extra != 0)
    {
        cards[extra - 1]++;
    }

    return IsHuCard(cards, guiNum);
}

bool IsHuCard(const std::vector<int> &cards, int guiNum)
{
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

    std::vector<HuEntryList> tmp;
    if (wanKey != 0)
    {
        auto it = HuTable.find(wanKey);
        tmp.push_back(it != HuTable.end() ? &it->second : nullptr);
    }
    if (tongKey != 0)
    {
        auto it = HuTable.find(tongKey);
        tmp.push_back(it != HuTable.end() ? &it->second : nullptr);
    }
    if (tiaoKey != 0)
    {
        auto it = HuTable.find(tiaoKey);
        tmp.push_back(it != HuTable.end() ? &it->second : nullptr);
    }
    if (fengKey != 0)
    {
        auto it = HuTableFeng.find(fengKey);
        tmp.push_back(it != HuTableFeng.end() ? &it->second : nullptr);
    }
    if (jianKey != 0)
    {
        auto it = HuTableJian.find(jianKey);
        tmp.push_back(it != HuTableJian.end() ? &it->second : nullptr);
    }

    std::vector<std::vector<HuTableInfo>> tmp1Storage;
    tmp1Storage.reserve(tmp.size());
    for (HuEntryList huTableInfos : tmp)
    {
        if (huTableInfos == nullptr || huTableInfos->empty())
        {
            return false;
        }
        tmp1Storage.emplace_back();
        std::vector<HuTableInfo> &tmp2 = tmp1Storage.back();
        for (const HuTableInfo &huTableInfo : *huTableInfos)
        {
            if (huTableInfo.Hupai == std::nullopt && huTableInfo.NeedGui <= guiNum)
            {
                tmp2.push_back(huTableInfo);
            }
        }
        if (tmp2.empty())
        {
            return false;
        }
    }

    std::vector<HuEntryList> tmp1;
    tmp1.reserve(tmp1Storage.size());
    for (std::vector<HuTableInfo> &tmp2 : tmp1Storage)
    {
        tmp1.push_back(&tmp2);
    }

    return isHuTableInfo(tmp1, 0, guiNum, false);
}

std::vector<int> IsTing(const std::vector<int> &input, int guiCard)
{
    std::vector<int> cards = countCards(input);
    int guiNum = cards[guiCard - 1];
    cards[guiCard - 1] = 0;
    return IsTingCard(cards, guiNum);
}

std::vector<int> IsTingExtra(const std::vector<int> &input, const std::vector<int> &guiCard)
{
    std::vector<int> cards = countCards(input);

    int guiNum = 0;
    for (int gui : guiCard)
    {
        guiNum += cards[gui - 1];
        cards[gui - 1] = 0;
    }

    return IsTingCard(cards, guiNum);
}

std::vector<int> IsTingCard(const std::vector<int> &cards, int guiNum)
{
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

    std::vector<int> tmpType;
    std::vector<HuEntryList> tmpTing;
    std::vector<HuEntryList> tmp;

    auto wanIt = HuTable.find(wanKey);
    if (wanIt == HuTable.end())
    {
        return {};
    }
    tmpTing.push_back(&wanIt->second);
    if (wanKey != 0)
    {
        tmpType.push_back(TypeWan);
        tmp.push_back(&wanIt->second);
    }
    auto tongIt = HuTable.find(tongKey);
    if (tongIt == HuTable.end())
    {
        return {};
    }
    tmpTing.push_back(&tongIt->second);
    if (tongKey != 0)
    {
        tmpType.push_back(TypeTong);
        tmp.push_back(&tongIt->second);
    }
    auto tiaoIt = HuTable.find(tiaoKey);
    if (tiaoIt == HuTable.end())
    {
        return {};
    }
    tmpTing.push_back(&tiaoIt->second);
    if (tiaoKey != 0)
    {
        tmpType.push_back(TypeTiao);
        tmp.push_back(&tiaoIt->second);
    }
    auto fengIt = HuTableFeng.find(fengKey);
    if (fengIt == HuTableFeng.end())
    {
        return {};
    }
    tmpTing.push_back(&fengIt->second);
    if (fengKey != 0)
    {
        tmpType.push_back(TypeFeng);
        tmp.push_back(&fengIt->second);
    }
    auto jianIt = HuTableJian.find(jianKey);
    if (jianIt == HuTableJian.end())
    {
        return {};
    }
    tmpTing.push_back(&jianIt->second);
    if (jianKey != 0)
    {
        tmpType.push_back(TypeJian);
        tmp.push_back(&jianIt->second);
    }

    std::vector<int> ret;
    for (int t = TypeWan; t <= TypeJian; t++)
    {
        HuEntryList huTableInfos = tmpTing[t - 1];
        int cache[9] = { 0 };
        for (const HuTableInfo &info : *huTableInfos)
        {
            if (info.Hupai != std::nullopt && info.NeedGui <= guiNum)
            {
                bool cached = true;
                for (size_t j = 0; j < info.Hupai->size(); j++)
                {
                    if ((*info.Hupai)[j] > 0 && cache[j] == 0)
                    {
                        cached = false;
                        break;
                    }
                }

                if (!cached && isTingHuTableInfo(tmpType, tmp, 0, guiNum - info.NeedGui, info.Jiang, t))
                {
                    for (size_t j = 0; j < info.Hupai->size(); j++)
                    {
                        if ((*info.Hupai)[j] > 0)
                        {
                            if (cache[j] == 0)
                            {
                                ret.push_back(ToCard(t, static_cast<int>(j)));
                            }
                            cache[j]++;
                        }
                    }
                }
            }
        }
    }
    return ret;
}

} // namespace majiang

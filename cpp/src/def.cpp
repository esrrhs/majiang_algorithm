// 对应 Go def.go / Java MaJiangDef。
#include "majiang/def.h"

#include <cstdlib>

namespace majiang
{

namespace
{

const char *const kFengJianHuaNames[] = { "东", "南", "西", "北", "中", "发", "白",
                                          "春", "夏", "秋", "冬", "梅", "兰", "竹", "菊" };

// 对应 Java Integer.parseInt(str.substring(0, 1)):取第一个字符按数字解析,非数字时返回 0。
int leadingDigit(const std::string &str)
{
    if (str.empty())
    {
        return 0;
    }
    char c = str[0];
    if (c >= '0' && c <= '9')
    {
        return c - '0';
    }
    return 0;
}

bool contains(const std::string &s, const char *sub)
{
    return s.find(sub) != std::string::npos;
}

} // namespace

int ToCard(int type, int index)
{
    switch (type)
    {
        case TypeWan:
            return Wan1 + index;
        case TypeTong:
            return Tong1 + index;
        case TypeTiao:
            return Tiao1 + index;
        case TypeFeng:
            return FengDong + index;
        case TypeJian:
            return JianZhong + index;
        case TypeHua:
            return HuaChun + index;
        default:
            return 0;
    }
}

std::string CardToString(int card)
{
    if (card >= Wan1 && card <= Wan9)
    {
        return std::to_string(card - Wan1 + 1) + "万";
    }
    if (card >= Tong1 && card <= Tong9)
    {
        return std::to_string(card - Tong1 + 1) + "筒";
    }
    if (card >= Tiao1 && card <= Tiao9)
    {
        return std::to_string(card - Tiao1 + 1) + "条";
    }
    if (card >= FengDong && card <= MaxNum)
    {
        return kFengJianHuaNames[card - FengDong];
    }
    return "错误" + std::to_string(card);
}

std::string CardsToString(const std::vector<int> &card)
{
    std::string ret;
    for (int c : card)
    {
        ret += CardToString(c);
        ret += ",";
    }
    return ret;
}

int StringToCard(const std::string &str)
{
    if (contains(str, "万"))
    {
        return Wan1 - 1 + leadingDigit(str);
    }
    if (contains(str, "筒"))
    {
        return Tong1 - 1 + leadingDigit(str);
    }
    if (contains(str, "条"))
    {
        return Tiao1 - 1 + leadingDigit(str);
    }
    int c = FengDong;
    for (const char *s : kFengJianHuaNames)
    {
        if (contains(str, s))
        {
            return c;
        }
        c++;
    }
    return 0;
}

std::vector<int> StringToCards(const std::string &str)
{
    std::vector<int> ret;
    size_t begin = 0;
    while (true)
    {
        size_t end = str.find(',', begin);
        std::string s = str.substr(begin, end == std::string::npos ? std::string::npos : end - begin);
        if (!s.empty())
        {
            ret.push_back(StringToCard(s));
        }
        if (end == std::string::npos)
        {
            break;
        }
        begin = end + 1;
    }
    return ret;
}

int CardType(int card)
{
    if (card >= Wan1 && card <= Wan9)
    {
        return TypeWan;
    }
    if (card >= Tong1 && card <= Tong9)
    {
        return TypeTong;
    }
    if (card >= Tiao1 && card <= Tiao9)
    {
        return TypeTiao;
    }
    if (card >= FengDong && card <= FengBei)
    {
        return TypeFeng;
    }
    if (card >= JianZhong && card <= JianBai)
    {
        return TypeJian;
    }
    if (card >= HuaChun && card <= HuaJu)
    {
        return TypeHua;
    }
    return 0;
}

} // namespace majiang

// 对应 Go common.go:文件查找、Java 浮点格式化、键展开、集合枚举与列表工具。
#include "detail.h"

#include <charconv>
#include <cmath>
#include <cstdio>
#include <filesystem>
#include <fstream>

namespace majiang
{
namespace detail
{

std::optional<std::string> findDataFile(const std::string &name)
{
    namespace fs = std::filesystem;
    const char *candidates[] = { "", "data", "../data", "../../data" };
    for (const char *dir : candidates)
    {
        fs::path p = fs::path(dir) / name;
        std::error_code ec;
        if (fs::is_regular_file(p, ec))
        {
            return p.string();
        }
    }
    return std::nullopt;
}

std::string javaDoubleToString(double d)
{
    if (std::isnan(d))
    {
        return "NaN";
    }
    if (std::isinf(d))
    {
        return d > 0 ? "Infinity" : "-Infinity";
    }
    if (d == 0)
    {
        return std::signbit(d) ? "-0.0" : "0.0";
    }

    double a = std::fabs(d);
    // 最短往返表示的科学计数法形式,如 "1.2345e+07"、"5e-05"
    char buf[64];
    auto res = std::to_chars(buf, buf + sizeof(buf), a, std::chars_format::scientific);
    std::string s(buf, res.ptr);

    size_t epos = s.find('e');
    std::string mant = s.substr(0, epos);
    int exp = std::atoi(s.c_str() + epos + 1);

    std::string digits;
    for (char c : mant)
    {
        if (c != '.')
        {
            digits += c;
        }
    }

    std::string b;
    if (std::signbit(d))
    {
        b += '-';
    }
    if (a >= 1e-3 && a < 1e7)
    {
        if (exp >= 0)
        {
            if (exp + 1 >= static_cast<int>(digits.size()))
            {
                b += digits;
                b.append(static_cast<size_t>(exp + 1 - digits.size()), '0');
                b += ".0";
            }
            else
            {
                b += digits.substr(0, exp + 1);
                b += '.';
                b += digits.substr(exp + 1);
            }
        }
        else
        {
            b += "0.";
            b.append(static_cast<size_t>(-exp - 1), '0');
            b += digits;
        }
    }
    else
    {
        b += digits[0];
        b += '.';
        b += digits.size() == 1 ? "0" : digits.substr(1);
        b += 'E';
        b += std::to_string(exp);
    }
    return b;
}

std::vector<int> keyDigits(int64_t key, int n)
{
    std::vector<int> num(n, 0);
    int64_t tmp = key;
    for (int i = 0; i < n; i++)
    {
        num[n - 1 - i] = static_cast<int>(tmp % 10);
        tmp /= 10;
    }
    return num;
}

static void genCardRec(std::unordered_set<int64_t> &out, std::vector<int> &num, int index, int total)
{
    int n = static_cast<int>(num.size());
    if (index == n - 1)
    {
        if (total > 4)
        {
            return;
        }
        num[index] = total;
        int64_t ret = 0;
        for (int c : num)
        {
            ret = ret * 10 + c;
        }
        out.insert(ret);
        return;
    }
    for (int i = 0; i <= 4; i++)
    {
        num[index] = (i <= total) ? i : 0;
        genCardRec(out, num, index + 1, total - num[index]);
    }
}

std::unordered_set<int64_t> genCardSet(int n, int total)
{
    std::unordered_set<int64_t> out;
    std::vector<int> num(n, 0);
    genCardRec(out, num, 0, total);
    return out;
}

std::unordered_set<int64_t> genCardSetAll(int n)
{
    std::unordered_set<int64_t> out;
    for (int i = 0; i <= 14; i++)
    {
        for (int64_t k : genCardSet(n, i))
        {
            out.insert(k);
        }
    }
    return out;
}

std::vector<int> removeFirst(const std::vector<int> &input, int card)
{
    std::vector<int> tmp(input);
    for (size_t i = 0; i < tmp.size(); i++)
    {
        if (tmp[i] == card)
        {
            tmp.erase(tmp.begin() + static_cast<long>(i));
            break;
        }
    }
    return tmp;
}

bool containsInt(const std::vector<int> &list, int v)
{
    for (int c : list)
    {
        if (c == v)
        {
            return true;
        }
    }
    return false;
}

int frequency(const std::vector<int> &list, int v)
{
    int n = 0;
    for (int c : list)
    {
        if (c == v)
        {
            n++;
        }
    }
    return n;
}

std::vector<int> countCards(const std::vector<int> &input)
{
    std::vector<int> cards(MaxNum, 0);
    for (int c : input)
    {
        cards[c - 1]++;
    }
    return cards;
}

const std::vector<std::string> wanNames = { "1万", "2万", "3万", "4万", "5万", "6万", "7万", "8万", "9万" };
const std::vector<std::string> fengNames = { "东", "南", "西", "北" };
const std::vector<std::string> jianNames = { "中", "发", "白" };

const HuConfig huNormalCfg = { 9, "normal", &wanNames, true };
const HuConfig huFengCfg = { 4, "feng", &fengNames, false };
const HuConfig huJianCfg = { 3, "jian", &jianNames, false };

const AiConfig aiNormalCfg = { 9, "normal", &wanNames, true, 36.0 / 136 };
const AiConfig aiFengCfg = { 4, "feng", &fengNames, false, 16.0 / 136 };
const AiConfig aiJianCfg = { 3, "jian", &jianNames, false, 12.0 / 136 };

int64_t huDigitFold(const std::optional<std::vector<int8_t>> &hupai)
{
    if (!hupai.has_value())
    {
        return -1;
    }
    int64_t hu = 0;
    for (int8_t v : *hupai)
    {
        hu = hu * 10 + v;
    }
    return hu;
}

std::string showCard(const std::vector<std::string> &card, int64_t key, int n)
{
    std::vector<int> num = keyDigits(key, n);
    std::string ret;
    for (size_t index = 0; index < num.size(); index++)
    {
        for (int j = 0; j < num[index]; j++)
        {
            ret += card[index];
        }
    }
    return ret;
}

bool writeLines(const std::string &name, const std::vector<std::string> &lines)
{
    std::ofstream f(name, std::ios::binary | std::ios::trunc);
    if (!f)
    {
        std::fprintf(stderr, "无法写入文件 %s\n", name.c_str());
        return false;
    }
    for (const std::string &line : lines)
    {
        f << line << "\n";
    }
    return static_cast<bool>(f);
}

} // namespace detail
} // namespace majiang

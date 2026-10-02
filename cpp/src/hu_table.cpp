// 判胡查表:定义、加载与重新生成,对应 Go hutable.go / Java HuCommon+HuTable*。
#include "detail.h"

#include <algorithm>
#include <cerrno>
#include <cstdio>
#include <cstring>
#include <fstream>
#include <map>
#include <sstream>
#include <string>

namespace majiang
{

std::unordered_map<int64_t, std::vector<HuTableInfo>> HuTable;
std::unordered_map<int64_t, std::vector<HuTableInfo>> HuTableFeng;
std::unordered_map<int64_t, std::vector<HuTableInfo>> HuTableJian;

namespace detail
{

namespace
{

// 对应 Java HuInfo(去重集合元素)。
struct HuInfoKey
{
    int needGui;
    int jiang;
    int hupai;
};

// needGui 0..8、jiang/hupai -1..8,可安全编码进一个 int。
int32_t encode(const HuInfoKey &k)
{
    return (k.needGui + 1) * 100 + (k.jiang + 1) * 10 + (k.hupai + 1);
}

void checkHuRec(const HuConfig &cfg, std::unordered_set<int32_t> &huInfos, std::vector<int> &num, int jiang,
                int in, int gui)
{
    int n = cfg.n;
    if (cfg.huLian)
    {
        for (int i = 0; i + 2 < n; i++)
        {
            if (num[i] > 0 && num[i + 1] > 0 && num[i + 2] > 0)
            {
                num[i]--;
                num[i + 1]--;
                num[i + 2]--;
                checkHuRec(cfg, huInfos, num, jiang, in, gui);
                num[i]++;
                num[i + 1]++;
                num[i + 2]++;
            }
        }
    }
    for (int i = 0; i < n; i++)
    {
        if (num[i] >= 2 && jiang == -1)
        {
            num[i] -= 2;
            checkHuRec(cfg, huInfos, num, i, in, gui);
            num[i] += 2;
        }
    }
    for (int i = 0; i < n; i++)
    {
        if (num[i] >= 3)
        {
            num[i] -= 3;
            checkHuRec(cfg, huInfos, num, jiang, in, gui);
            num[i] += 3;
        }
    }
    for (int i = 0; i < n; i++)
    {
        if (num[i] != 0)
        {
            return;
        }
    }
    huInfos.insert(encode({ gui, jiang, in }));
}

void checkHuLong(std::unordered_map<int64_t, std::vector<HuTableInfo>> &m, const HuConfig &cfg, int64_t card)
{
    int n = cfg.n;
    std::vector<int> num = keyDigits(card, n);
    int total = 0;
    for (int v : num)
    {
        total += v;
    }

    std::unordered_set<int32_t> huInfos;
    for (int guinum = 0; guinum <= 8 && total + guinum <= 14; guinum++)
    {
        std::unordered_set<int64_t> tmpcard = genCardSet(n, guinum);
        for (int64_t tmpgui : tmpcard)
        {
            std::vector<int> tmpguinum = keyDigits(tmpgui, n);
            bool max = false;
            for (int i = 0; i < n; i++)
            {
                num[i] += tmpguinum[i];
                if (num[i] > 4)
                {
                    max = true;
                }
            }
            if (!max)
            {
                checkHuRec(cfg, huInfos, num, -1, -1, guinum);
            }
            for (int i = 0; i < n && !max; i++)
            {
                num[i]++;
                if (num[i] <= 4)
                {
                    checkHuRec(cfg, huInfos, num, -1, i, guinum);
                }
                num[i]--;
            }
            for (int i = 0; i < n; i++)
            {
                num[i] -= tmpguinum[i];
            }
        }
    }

    // 合并,等价 Java check_hu 里的 HashMap<Integer, HuTableInfo> 归并
    struct Mk
    {
        int needGui;
        bool jiang;
        bool operator<(const Mk &o) const
        {
            return needGui != o.needGui ? needGui < o.needGui : static_cast<int>(jiang) < static_cast<int>(o.jiang);
        }
    };
    std::map<Mk, HuTableInfo> merges;
    for (int32_t e : huInfos)
    {
        HuInfoKey info{ e / 100 - 1, e / 10 % 10 - 1, e % 10 - 1 };
        Mk key{ info.needGui, info.jiang != -1 };
        auto it = merges.find(key);
        if (it == merges.end())
        {
            HuTableInfo cur;
            cur.NeedGui = info.needGui;
            cur.Jiang = info.jiang != -1;
            if (info.hupai == -1)
            {
                cur.Hupai = std::nullopt;
            }
            else
            {
                cur.Hupai = std::vector<int8_t>(n, 0);
                (*cur.Hupai)[info.hupai]++;
            }
            merges.emplace(key, std::move(cur));
        }
        else
        {
            HuTableInfo &cur = it->second;
            if (info.hupai == -1)
            {
                cur.Hupai = std::nullopt;
            }
            else if (cur.Hupai.has_value() && (*cur.Hupai)[info.hupai] == 0)
            {
                (*cur.Hupai)[info.hupai]++;
            }
        }
    }

    std::vector<HuTableInfo> list;
    list.reserve(merges.size());
    for (auto &kv : merges)
    {
        list.push_back(std::move(kv.second));
    }
    std::sort(list.begin(), list.end(), [](const HuTableInfo &a, const HuTableInfo &b) {
        if (a.NeedGui != b.NeedGui)
        {
            return a.NeedGui < b.NeedGui;
        }
        if (a.Jiang != b.Jiang)
        {
            return !a.Jiang;
        }
        return huDigitFold(a.Hupai) < huDigitFold(b.Hupai);
    });
    m[card] = std::move(list);
}

std::vector<int64_t> sortedHuKeys(const std::unordered_map<int64_t, std::vector<HuTableInfo>> &m)
{
    std::vector<int64_t> keys;
    keys.reserve(m.size());
    for (const auto &kv : m)
    {
        keys.push_back(kv.first);
    }
    std::sort(keys.begin(), keys.end());
    return keys;
}

} // namespace

bool loadHuTable(std::unordered_map<int64_t, std::vector<HuTableInfo>> &m, const HuConfig &cfg)
{
    m.clear();
    auto path = findDataFile(std::string("majiang_clien_") + cfg.name + ".txt");
    if (!path)
    {
        std::fprintf(stderr, "找不到查表文件 majiang_clien_%s.txt(已尝试当前目录、data/、../data/)\n", cfg.name);
        return false;
    }
    std::ifstream f(*path);
    if (!f)
    {
        std::fprintf(stderr, "无法打开 %s: %s\n", path->c_str(), std::strerror(errno));
        return false;
    }
    std::string line;
    while (std::getline(f, line))
    {
        if (!line.empty() && line.back() == '\r')
        {
            line.pop_back();
        }
        if (line.empty())
        {
            continue;
        }
        std::istringstream ss(line);
        std::string f0, f1, f2, f3;
        ss >> f0 >> f1 >> f2 >> f3;
        if (f3.empty())
        {
            std::fprintf(stderr, "%s 格式错误: %s\n", path->c_str(), line.c_str());
            return false;
        }
        HuTableInfo info;
        info.NeedGui = std::atoi(f1.c_str());
        info.Jiang = std::atoi(f2.c_str()) != 0;
        long hu = std::strtol(f3.c_str(), nullptr, 10);
        if (hu != -1)
        {
            std::vector<int8_t> hupai(cfg.n, 0);
            long tmp = hu;
            for (int i = 0; i < cfg.n; i++)
            {
                hupai[cfg.n - 1 - i] = static_cast<int8_t>(tmp % 10);
                tmp /= 10;
            }
            info.Hupai = std::move(hupai);
        }
        m[std::strtoll(f0.c_str(), nullptr, 10)].push_back(std::move(info));
    }
    return true;
}

void genHuInto(std::unordered_map<int64_t, std::vector<HuTableInfo>> &m, const HuConfig &cfg)
{
    m.clear();
    for (int64_t card : genCardSetAll(cfg.n))
    {
        checkHuLong(m, cfg, card);
    }
}

std::vector<std::string> buildHuClientLines(const std::unordered_map<int64_t, std::vector<HuTableInfo>> &m)
{
    std::vector<std::string> lines;
    for (int64_t key : sortedHuKeys(m))
    {
        for (const HuTableInfo &info : m.at(key))
        {
            std::ostringstream ss;
            ss << key << " " << info.NeedGui << " " << (info.Jiang ? 1 : 0) << " "
               << huDigitFold(info.Hupai);
            lines.push_back(ss.str());
        }
    }
    return lines;
}

std::vector<std::string> buildHuServerLines(const HuConfig &cfg,
                                            const std::unordered_map<int64_t, std::vector<HuTableInfo>> &m)
{
    std::vector<std::string> lines;
    for (int64_t key : sortedHuKeys(m))
    {
        for (const HuTableInfo &info : m.at(key))
        {
            std::ostringstream ss;
            ss << key << " " << info.NeedGui << " " << (info.Jiang ? "1 " : "0 ");
            if (!info.Hupai.has_value())
            {
                ss << "-1";
            }
            else
            {
                ss << huDigitFold(info.Hupai);
            }
            ss << " " << showCard(*cfg.card, key, cfg.n) << " "
               << "鬼" << info.NeedGui << " " << (info.Jiang ? "有将 " : "无将 ");
            if (!info.Hupai.has_value())
            {
                ss << "胡了";
            }
            else
            {
                for (size_t index = 0; index < info.Hupai->size(); index++)
                {
                    if ((*info.Hupai)[index] > 0)
                    {
                        ss << "胡" << (*cfg.card)[index];
                    }
                }
            }
            lines.push_back(ss.str());
        }
    }
    return lines;
}

bool genHu()
{
    struct HuTableEntry
    {
        std::unordered_map<int64_t, std::vector<HuTableInfo>> *m;
        const HuConfig *cfg;
    };
    HuTableEntry tables[] = {
        { &HuTableJian, &huJianCfg },
        { &HuTableFeng, &huFengCfg },
        { &HuTable, &huNormalCfg },
    };
    for (const HuTableEntry &t : tables)
    {
        genHuInto(*t.m, *t.cfg);
        if (!writeLines("majiang_clien_" + std::string(t.cfg->name) + ".txt",
                        buildHuClientLines(*t.m)))
        {
            return false;
        }
        if (!writeLines("majiang_server_" + std::string(t.cfg->name) + ".txt",
                        buildHuServerLines(*t.cfg, *t.m)))
        {
            return false;
        }
    }
    return true;
}

} // namespace detail
} // namespace majiang

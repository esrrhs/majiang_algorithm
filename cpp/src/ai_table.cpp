// AI 查表:定义、加载与重新生成,对应 Go aitable.go / Java AICommon+AITable*。
#include "detail.h"

#include <algorithm>
#include <cerrno>
#include <cstdio>
#include <cstring>
#include <fstream>
#include <map>
#include <set>
#include <sstream>
#include <string>
#include <utility>

namespace majiang
{

std::unordered_map<int64_t, std::vector<AITableInfo>> AITable;
std::unordered_map<int64_t, std::vector<AITableInfo>> AITableFeng;
std::unordered_map<int64_t, std::vector<AITableInfo>> AITableJian;

namespace detail
{

namespace
{

// 对应 Java AIInfo(去重集合元素):inputNum 固定为当前轮次,jiang -1..n-1。
using AiInfoKey = std::pair<int, int>;

void checkAiRec(const AiConfig &cfg, std::set<AiInfoKey> &aiInfos, std::vector<int> &num, int jiang, int inputNum)
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
                checkAiRec(cfg, aiInfos, num, jiang, inputNum);
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
            checkAiRec(cfg, aiInfos, num, i, inputNum);
            num[i] += 2;
        }
    }
    for (int i = 0; i < n; i++)
    {
        if (num[i] >= 3)
        {
            num[i] -= 3;
            checkAiRec(cfg, aiInfos, num, jiang, inputNum);
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
    aiInfos.insert({ inputNum, jiang });
}

void checkAiLong(std::unordered_map<int64_t, std::vector<AITableInfo>> &m, const AiConfig &cfg, int64_t card,
                 const std::unordered_map<int, std::unordered_set<int64_t>> &tmpcards)
{
    int n = cfg.n;
    std::vector<int> num = keyDigits(card, n);

    // Java 用 HashMap<Integer, AITableInfo>(key 1=有将, 0=无将),两个 key 恒存在
    std::map<bool, AITableInfo> acc;
    acc.emplace(true, AITableInfo{ true, 0.0 });
    acc.emplace(false, AITableInfo{ false, 0.0 });

    for (int inputNum = 0; inputNum <= aiLevel; inputNum++)
    {
        const std::unordered_set<int64_t> &tmpcard = tmpcards.at(inputNum);
        std::set<AiInfoKey> aiInfos;
        int valid = 0;

        for (int64_t tmpc : tmpcard)
        {
            std::vector<int> tmpcnum = keyDigits(tmpc, n);
            bool max = false;
            for (int i = 0; i < n; i++)
            {
                num[i] += tmpcnum[i];
                if (num[i] > 4)
                {
                    max = true;
                }
            }
            if (!max)
            {
                checkAiRec(cfg, aiInfos, num, -1, inputNum);
                valid++;
            }
            for (int i = 0; i < n; i++)
            {
                num[i] -= tmpcnum[i];
            }
        }

        for (const AiInfoKey &info : aiInfos)
        {
            bool key = info.second != -1;
            if (info.first == 0)
            {
                acc[key].P = 1;
            }
            if (acc[key].P != 1)
            {
                acc[key].P += cfg.baseP * 1.0 / valid;
            }
        }
    }

    // Java HashMap 对小整数键 0/1 的遍历顺序恒为 0 先 1 后,即无将在前
    std::vector<AITableInfo> list;
    list.push_back(acc[false]);
    list.push_back(acc[true]);
    m[card] = std::move(list);
}

std::vector<int64_t> sortedAiKeys(const std::unordered_map<int64_t, std::vector<AITableInfo>> &m)
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

bool loadAiTable(std::unordered_map<int64_t, std::vector<AITableInfo>> &m, const AiConfig &cfg)
{
    m.clear();
    auto path = findDataFile(std::string("majiang_ai_") + cfg.name + ".txt");
    if (!path)
    {
        std::fprintf(stderr, "找不到查表文件 majiang_ai_%s.txt(已尝试当前目录、data/、../data/)\n", cfg.name);
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
        std::string f0, f1, f2;
        ss >> f0 >> f1 >> f2;
        if (f2.empty())
        {
            std::fprintf(stderr, "%s 格式错误: %s\n", path->c_str(), line.c_str());
            return false;
        }
        AITableInfo info;
        info.Jiang = std::atoi(f1.c_str()) != 0;
        info.P = std::strtod(f2.c_str(), nullptr);
        m[std::strtoll(f0.c_str(), nullptr, 10)].push_back(std::move(info));
    }
    return true;
}

void genAiInto(std::unordered_map<int64_t, std::vector<AITableInfo>> &m, const AiConfig &cfg)
{
    m.clear();
    std::unordered_map<int, std::unordered_set<int64_t>> tmpcards;
    for (int inputNum = 0; inputNum <= aiLevel; inputNum++)
    {
        tmpcards.emplace(inputNum, genCardSet(cfg.n, inputNum));
    }
    for (int64_t card : genCardSetAll(cfg.n))
    {
        checkAiLong(m, cfg, card, tmpcards);
    }
}

std::vector<std::string> buildAiLines(const AiConfig &cfg,
                                      const std::unordered_map<int64_t, std::vector<AITableInfo>> &m)
{
    std::vector<std::string> lines;
    for (int64_t key : sortedAiKeys(m))
    {
        for (const AITableInfo &info : m.at(key))
        {
            std::ostringstream ss;
            ss << key << " " << (info.Jiang ? "1 " : "0 ") << javaDoubleToString(info.P) << " "
               << showCard(*cfg.card, key, cfg.n) << " " << (info.Jiang ? "有将 " : "无将 ")
               << javaDoubleToString(info.P);
            lines.push_back(ss.str());
        }
    }
    return lines;
}

bool genAi()
{
    struct AiTableEntry
    {
        std::unordered_map<int64_t, std::vector<AITableInfo>> *m;
        const AiConfig *cfg;
    };
    AiTableEntry tables[] = {
        { &AITableJian, &aiJianCfg },
        { &AITableFeng, &aiFengCfg },
        { &AITable, &aiNormalCfg },
    };
    for (const AiTableEntry &t : tables)
    {
        genAiInto(*t.m, *t.cfg);
        if (!writeLines("majiang_ai_" + std::string(t.cfg->name) + ".txt", buildAiLines(*t.cfg, *t.m)))
        {
            return false;
        }
    }
    return true;
}

} // namespace detail
} // namespace majiang

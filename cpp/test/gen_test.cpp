// 用内存生成的表与仓库已提交的查表文件做内容比对(按行排序后比较,与 Go gen_test.go 一致)。
#include <algorithm>
#include <fstream>
#include <iostream>

#include "detail.h"
#include "test_util.h"

using namespace majiang;
using namespace majiang::detail;

namespace
{

std::vector<std::string> readDataLines(const char *name, bool &skipped)
{
    std::vector<std::string> lines;
    auto path = findDataFile(name);
    if (!path)
    {
        std::printf("  [SKIP] 找不到 %s\n", name);
        skipped = true;
        return lines;
    }
    std::ifstream f(*path);
    std::string line;
    while (std::getline(f, line))
    {
        if (!line.empty() && line.back() == '\r')
        {
            line.pop_back();
        }
        if (!line.empty())
        {
            lines.push_back(line);
        }
    }
    return lines;
}

void assertSameLines(const char *name, const std::vector<std::string> &got)
{
    bool skipped = false;
    std::vector<std::string> want = readDataLines(name, skipped);
    EXPECT(!skipped);
    if (skipped)
    {
        return;
    }

    std::vector<std::string> sortedWant(want), sortedGot(got);
    std::sort(sortedWant.begin(), sortedWant.end());
    std::sort(sortedGot.begin(), sortedGot.end());

    EXPECT_EQ(sortedGot.size(), sortedWant.size());
    size_t n = std::min(sortedGot.size(), sortedWant.size());
    for (size_t i = 0; i < n; i++)
    {
        if (sortedGot[i] != sortedWant[i])
        {
            EXPECT_EQ(sortedGot[i], sortedWant[i]);
            return;
        }
    }
}

} // namespace

MAJ_TEST(TestGenHuJianParity)
{
    std::unordered_map<int64_t, std::vector<HuTableInfo>> m;
    genHuInto(m, huJianCfg);
    assertSameLines("majiang_clien_jian.txt", buildHuClientLines(m));
    assertSameLines("majiang_server_jian.txt", buildHuServerLines(huJianCfg, m));
}

MAJ_TEST(TestGenHuFengParity)
{
    std::unordered_map<int64_t, std::vector<HuTableInfo>> m;
    genHuInto(m, huFengCfg);
    assertSameLines("majiang_clien_feng.txt", buildHuClientLines(m));
    assertSameLines("majiang_server_feng.txt", buildHuServerLines(huFengCfg, m));
}

MAJ_TEST(TestGenAiJianParity)
{
    std::unordered_map<int64_t, std::vector<AITableInfo>> m;
    genAiInto(m, aiJianCfg);
    assertSameLines("majiang_ai_jian.txt", buildAiLines(aiJianCfg, m));
}

MAJ_TEST(TestGenAiFengParity)
{
    std::unordered_map<int64_t, std::vector<AITableInfo>> m;
    genAiInto(m, aiFengCfg);
    assertSameLines("majiang_ai_feng.txt", buildAiLines(aiFengCfg, m));
}

MAJ_TEST(TestJavaDoubleToString)
{
    // 与 Java Double.toString 输出逐字符一致
    struct Case
    {
        double v;
        const char *want;
    };
    Case cases[] = {
        { 0, "0.0" },
        { 1, "1.0" },
        { -1, "-1.0" },
        { 0.05610859728506787, "0.05610859728506787" },
        { 0.0016140602582496414, "0.0016140602582496414" },
        { 0.001, "0.001" },
        { 1e-4, "1.0E-4" },
        { 1e7, "1.0E7" },
        { 9999999, "9999999.0" },
        { 36.0 / 136, "0.2647058823529412" },
    };
    for (const Case &c : cases)
    {
        EXPECT_EQ(javaDoubleToString(c.v), std::string(c.want));
    }
}

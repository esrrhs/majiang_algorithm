// 库内部共享的工具函数与配置,供 src/*.cpp 与测试复用;不属于公共 API。
#pragma once

#include <cstdint>
#include <optional>
#include <string>
#include <unordered_set>
#include <vector>

#include "majiang/ai_table.h"
#include "majiang/def.h"
#include "majiang/hu_table.h"

namespace majiang
{
namespace detail
{

// 依序在当前目录、data/、../data/、../../data/ 下查找查表文件,
// 与 Java 侧 HuCommon.findDataFile 的回退顺序一致(第 4 个候选便于从 build 目录直接运行测试)。
std::optional<std::string> findDataFile(const std::string &name);

// 按Java Double.toString 的规则格式化 double,用于生成与 Java 逐字节可比的查表文件。
std::string javaDoubleToString(double d);

// 把表键(每位一个计数,共 n 位)展开成长度为 n 的计数数组。
std::vector<int> keyDigits(int64_t key, int n);

// 枚举 n 个位置、每个位置计数 0..4、总计数为 total 的所有键(去重),等价 Java gen_card。
std::unordered_set<int64_t> genCardSet(int n, int total);

// 枚举总计数 0..14 的全部键,等价 Java gen() 开头的 card 集合构建。
std::unordered_set<int64_t> genCardSetAll(int n);

// 移除切片中第一个等于 card 的元素(对应 Java List.remove((Integer)v))。
std::vector<int> removeFirst(const std::vector<int> &input, int card);

bool containsInt(const std::vector<int> &list, int v);
int frequency(const std::vector<int> &list, int v);

// 把手牌列表转成长度 MaxNum 的计数数组(下标 = 牌编号-1)。
std::vector<int> countCards(const std::vector<int> &input);

// 表配置,对应 Java HuCommon / AICommon 的静态参数。
struct HuConfig
{
    int n;
    const char *name;
    const std::vector<std::string> *card;
    bool huLian;
};

struct AiConfig
{
    int n;
    const char *name;
    const std::vector<std::string> *card;
    bool huLian;
    double baseP;
};

extern const std::vector<std::string> wanNames;
extern const std::vector<std::string> fengNames;
extern const std::vector<std::string> jianNames;

extern const HuConfig huNormalCfg;
extern const HuConfig huFengCfg;
extern const HuConfig huJianCfg;

constexpr int aiLevel = 5; // 对应 Java AICommon.LEVEL
extern const AiConfig aiNormalCfg;
extern const AiConfig aiFengCfg;
extern const AiConfig aiJianCfg;

// 把 hupai 数组折叠成 Java 输出格式的整数(各位为 0/1 标记);nullopt 折叠为 -1。
int64_t huDigitFold(const std::optional<std::vector<int8_t>> &hupai);

// 等价 Java HuCommon.show_card。
std::string showCard(const std::vector<std::string> &card, int64_t key, int n);

// 加载(文件不存在或格式错误返回 false,并输出 stderr)。
bool loadHuTable(std::unordered_map<int64_t, std::vector<HuTableInfo>> &m, const HuConfig &cfg);
bool loadAiTable(std::unordered_map<int64_t, std::vector<AITableInfo>> &m, const AiConfig &cfg);

// 在内存中重新生成一张表(等价 Java check_hu/check_ai 的全量枚举),不写文件。
void genHuInto(std::unordered_map<int64_t, std::vector<HuTableInfo>> &m, const HuConfig &cfg);
void genAiInto(std::unordered_map<int64_t, std::vector<AITableInfo>> &m, const AiConfig &cfg);

// 重新生成全部查表文件(写入当前目录),由 api.cpp 的 HuGen/AiGen 调用。
bool genHu();
bool genAi();

// 按键与条目排序构造与 Java 内容一致的输出行(gen 测试与文件写入共用)。
std::vector<std::string> buildHuClientLines(const std::unordered_map<int64_t, std::vector<HuTableInfo>> &m);
std::vector<std::string> buildHuServerLines(const HuConfig &cfg,
                                            const std::unordered_map<int64_t, std::vector<HuTableInfo>> &m);
std::vector<std::string> buildAiLines(const AiConfig &cfg,
                                      const std::unordered_map<int64_t, std::vector<AITableInfo>> &m);

bool writeLines(const std::string &name, const std::vector<std::string> &lines);

} // namespace detail
} // namespace majiang

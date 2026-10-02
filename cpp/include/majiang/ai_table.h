// AI 查表:表项定义、三张表全局对象与加载接口,对应 Java AITable* / Go aitable.go。
#pragma once

#include <cstdint>
#include <unordered_map>
#include <vector>

namespace majiang
{

// 对应 Java AITableInfo。
struct AITableInfo
{
    bool Jiang = false;
    double P = 0.0;
};

// 三张 AI 表,键与判胡表同构,与 Java AITable* 类一致。
extern std::unordered_map<int64_t, std::vector<AITableInfo>> AITable;     // 万
extern std::unordered_map<int64_t, std::vector<AITableInfo>> AITableFeng; // 风
extern std::unordered_map<int64_t, std::vector<AITableInfo>> AITableJian; // 箭

} // namespace majiang

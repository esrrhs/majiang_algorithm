// 判胡查表:表项定义、三张表全局对象与加载接口,对应 Java HuTable* / Go hutable.go。
#pragma once

#include <cstdint>
#include <optional>
#include <unordered_map>
#include <vector>

namespace majiang
{

// 对应 Java HuTableInfo。Hupai 为 nullopt 表示该组合不需要进牌即已胡(对应 Java hupai == null);
// 否则长度为该花色的牌数,>0 的下标表示听该牌。
struct HuTableInfo
{
    int NeedGui = 0;
    bool Jiang = false;
    std::optional<std::vector<int8_t>> Hupai;
};

// 三张判胡表,键为各花色计数串展开成的十进制长整数,与 Java HuTable* 类一致。
extern std::unordered_map<int64_t, std::vector<HuTableInfo>> HuTable;     // 万
extern std::unordered_map<int64_t, std::vector<HuTableInfo>> HuTableFeng; // 风
extern std::unordered_map<int64_t, std::vector<HuTableInfo>> HuTableJian; // 箭

} // namespace majiang

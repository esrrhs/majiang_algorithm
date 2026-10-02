// 顶层入口:加载与重新生成,对应 Go api.go。
#include "majiang/api.h"

#include <mutex>

#include "detail.h"

namespace majiang
{

namespace
{
std::mutex g_loadMu;
}

bool HuLoad()
{
    std::lock_guard<std::mutex> lock(g_loadMu);
    if (!detail::loadHuTable(HuTableJian, detail::huJianCfg))
    {
        return false;
    }
    if (!detail::loadHuTable(HuTableFeng, detail::huFengCfg))
    {
        return false;
    }
    return detail::loadHuTable(HuTable, detail::huNormalCfg);
}

bool AILoad()
{
    std::lock_guard<std::mutex> lock(g_loadMu);
    if (!detail::loadAiTable(AITableJian, detail::aiJianCfg))
    {
        return false;
    }
    if (!detail::loadAiTable(AITableFeng, detail::aiFengCfg))
    {
        return false;
    }
    return detail::loadAiTable(AITable, detail::aiNormalCfg);
}

bool Load()
{
    return HuLoad() && AILoad();
}

bool HuGen()
{
    std::lock_guard<std::mutex> lock(g_loadMu);
    return detail::genHu();
}

bool AiGen()
{
    std::lock_guard<std::mutex> lock(g_loadMu);
    return detail::genAi();
}

} // namespace majiang

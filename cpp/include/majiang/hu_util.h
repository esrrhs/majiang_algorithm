// 判胡 / 听牌核心接口,对应 Java HuUtil / Go huutil.go。
#pragma once

#include "majiang/def.h"
#include "majiang/hu_table.h"

namespace majiang
{

// 判断手牌是否胡(单鬼牌),对应 Java HuUtil.isHu。
bool IsHu(const std::vector<int> &input, int guiCard);

// 判断手牌是否胡(多鬼牌 + extra 进牌),对应 Java HuUtil.isHuExtra。
bool IsHuExtra(const std::vector<int> &input, const std::vector<int> &guiCard, int extra);

// 基于计数数组的查表判胡(数组长度 MaxNum,下标 = 牌编号-1),对应 Java HuUtil.isHuCard。
bool IsHuCard(const std::vector<int> &cards, int guiNum);

// 返回手牌的听牌列表(单鬼牌),对应 Java HuUtil.isTing。
std::vector<int> IsTing(const std::vector<int> &input, int guiCard);

// 返回手牌的听牌列表(多鬼牌),对应 Java HuUtil.isTingExtra。
std::vector<int> IsTingExtra(const std::vector<int> &input, const std::vector<int> &guiCard);

// 基于计数数组的查表听牌,对应 Java HuUtil.isTingCard。
std::vector<int> IsTingCard(const std::vector<int> &cards, int guiNum);

} // namespace majiang

// AI 决策接口,对应 Java AIUtil / Go aiutil.go。
#pragma once

#include "majiang/def.h"
#include "majiang/hu_util.h"

namespace majiang
{

// 计算手牌向胡牌方向推进的评分:已听牌时返回 听牌数*10,否则为各花色
// AI 表组合出的最大几率,对应 Java AIUtil.calc。
double Calc(const std::vector<int> &input, const std::vector<int> &guiCard);

// 选出应打出的牌;全为鬼牌时返回 0,对应 Java AIUtil.outAI。
int OutAI(const std::vector<int> &input, const std::vector<int> &guiCard);

// 判断是否应该吃(card1、card2 为组成顺子的另外两张牌),对应 Java AIUtil.chiAI 布尔版。
bool ChiAI(const std::vector<int> &input, const std::vector<int> &guiCard, int card, int card1, int card2);

// 返回吃 card 时应用的两张牌;不该吃时为空,对应 Java AIUtil.chiAI 列表版。
std::vector<int> ChiAIChoices(const std::vector<int> &input, const std::vector<int> &guiCard, int card);

// 判断是否应该碰,对应 Java AIUtil.pengAI。
bool PengAI(const std::vector<int> &input, const std::vector<int> &guiCard, int card, double award);

// 判断是否应该杠,对应 Java AIUtil.gangAI。
bool GangAI(const std::vector<int> &input, const std::vector<int> &guiCard, int card, double award);

} // namespace majiang

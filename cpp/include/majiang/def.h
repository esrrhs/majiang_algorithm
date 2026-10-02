// 牌的编号定义与字符串转换,与 Java MaJiangDef / Go def.go 一一对应。
#pragma once

#include <string>
#include <vector>

namespace majiang
{

constexpr int Wan1 = 1;
constexpr int Wan2 = 2;
constexpr int Wan3 = 3;
constexpr int Wan4 = 4;
constexpr int Wan5 = 5;
constexpr int Wan6 = 6;
constexpr int Wan7 = 7;
constexpr int Wan8 = 8;
constexpr int Wan9 = 9;

constexpr int Tong1 = 10;
constexpr int Tong2 = 11;
constexpr int Tong3 = 12;
constexpr int Tong4 = 13;
constexpr int Tong5 = 14;
constexpr int Tong6 = 15;
constexpr int Tong7 = 16;
constexpr int Tong8 = 17;
constexpr int Tong9 = 18;

constexpr int Tiao1 = 19;
constexpr int Tiao2 = 20;
constexpr int Tiao3 = 21;
constexpr int Tiao4 = 22;
constexpr int Tiao5 = 23;
constexpr int Tiao6 = 24;
constexpr int Tiao7 = 25;
constexpr int Tiao8 = 26;
constexpr int Tiao9 = 27;

constexpr int FengDong = 28;
constexpr int FengNan = 29;
constexpr int FengXi = 30;
constexpr int FengBei = 31;

constexpr int JianZhong = 32;
constexpr int JianFa = 33;
constexpr int JianBai = 34;

constexpr int HuaChun = 35;
constexpr int HuaXia = 36;
constexpr int HuaQiu = 37;
constexpr int HuaDong = 38;
constexpr int HuaMei = 39;
constexpr int HuaLan = 40;
constexpr int HuaZhu = 41;
constexpr int HuaJu = 42;

constexpr int MaxNum = 42;

constexpr int TypeWan = 1;
constexpr int TypeTong = 2;
constexpr int TypeTiao = 3;
constexpr int TypeFeng = 4;
constexpr int TypeJian = 5;
constexpr int TypeHua = 6;

// 等价 Java MaJiangDef.toCard。
int ToCard(int type, int index);

// 等价 Java MaJiangDef.type(Java 的 type 是关键字,Go/C++ 侧统一更名 CardType)。
int CardType(int card);

// 等价 Java MaJiangDef.cardToString。
std::string CardToString(int card);

// 等价 Java MaJiangDef.cardsToString。
std::string CardsToString(const std::vector<int> &card);

// 等价 Java MaJiangDef.stringToCard(数牌只取第一个字符作为数字,与 Java 解析一致)。
int StringToCard(const std::string &str);

// 等价 Java MaJiangDef.stringToCards。
std::vector<int> StringToCards(const std::string &str);

} // namespace majiang

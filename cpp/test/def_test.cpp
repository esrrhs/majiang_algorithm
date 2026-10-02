// 移植自 Java MaJiangDefTest / Go def_test.go,断言一一对应。
#include "majiang/def.h"
#include "test_util.h"

using namespace majiang;

MAJ_TEST(TestCardConstantsAndTypes)
{
    EXPECT(CardType(Wan1) == TypeWan && CardType(Wan9) == TypeWan);
    EXPECT(CardType(Tong1) == TypeTong && CardType(Tong9) == TypeTong);
    EXPECT(CardType(Tiao1) == TypeTiao && CardType(Tiao9) == TypeTiao);
    EXPECT(CardType(FengDong) == TypeFeng && CardType(FengBei) == TypeFeng);
    EXPECT(CardType(JianZhong) == TypeJian && CardType(JianBai) == TypeJian);
    EXPECT(CardType(HuaChun) == TypeHua && CardType(HuaJu) == TypeHua);
    EXPECT_EQ(CardType(999), 0);
}

MAJ_TEST(TestToCard)
{
    EXPECT_EQ(ToCard(TypeWan, 0), Wan1);
    EXPECT_EQ(ToCard(TypeWan, 4), Wan5);
    EXPECT_EQ(ToCard(TypeTong, 0), Tong1);
    EXPECT_EQ(ToCard(TypeTiao, 8), Tiao9);
    EXPECT_EQ(ToCard(TypeFeng, 0), FengDong);
    EXPECT_EQ(ToCard(TypeJian, 0), JianZhong);
    EXPECT_EQ(ToCard(TypeHua, 0), HuaChun);
    EXPECT_EQ(ToCard(99, 0), 0);
}

MAJ_TEST(TestCardToStringAndReverse)
{
    struct Case
    {
        int card;
        const char *str;
    };
    Case cases[] = {
        { Wan1, "1万" }, { Wan9, "9万" }, { Tong5, "5筒" }, { Tiao3, "3条" },
        { FengDong, "东" }, { JianZhong, "中" }, { HuaJu, "菊" },
    };
    for (const Case &c : cases)
    {
        EXPECT_EQ(CardToString(c.card), std::string(c.str));
        EXPECT_EQ(StringToCard(c.str), c.card);
    }
    EXPECT_EQ(CardToString(999), std::string("错误999"));
    EXPECT_EQ(StringToCard("未知"), 0);
}

MAJ_TEST(TestCardsToStringAndReverse)
{
    std::vector<int> cards = { Wan1, Wan2, Wan3 };
    EXPECT_EQ(CardsToString(cards), std::string("1万,2万,3万,"));

    std::vector<int> parsed = StringToCards("1万,2万,3万");
    EXPECT_EQ(parsed.size(), cards.size());
    for (size_t i = 0; i < cards.size(); i++)
    {
        EXPECT_EQ(parsed[i], cards[i]);
    }

    // Java 侧用 HashSet 去重后输出,此处同样按去重后的集合断言
    std::vector<int> uniq = { Wan1, Wan2, Wan3 };
    std::string setStr = CardsToString(uniq);
    EXPECT(setStr.find("1万,") != std::string::npos);
    EXPECT(setStr.find("2万,") != std::string::npos);
    EXPECT(setStr.find("3万,") != std::string::npos);

    EXPECT_EQ(CardsToString({}), std::string(""));
    EXPECT_EQ(StringToCards("").size(), static_cast<size_t>(0));
}

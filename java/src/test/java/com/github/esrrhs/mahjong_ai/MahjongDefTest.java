package com.github.esrrhs.mahjong_ai;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MahjongDefTest
{
	@Test
	public void testCardConstantsAndTypes()
	{
		assertEquals(MahjongDef.TYPE_WAN, MahjongDef.type(MahjongDef.WAN1));
		assertEquals(MahjongDef.TYPE_WAN, MahjongDef.type(MahjongDef.WAN9));

		assertEquals(MahjongDef.TYPE_TONG, MahjongDef.type(MahjongDef.TONG1));
		assertEquals(MahjongDef.TYPE_TONG, MahjongDef.type(MahjongDef.TONG9));

		assertEquals(MahjongDef.TYPE_TIAO, MahjongDef.type(MahjongDef.TIAO1));
		assertEquals(MahjongDef.TYPE_TIAO, MahjongDef.type(MahjongDef.TIAO9));

		assertEquals(MahjongDef.TYPE_FENG, MahjongDef.type(MahjongDef.FENG_DONG));
		assertEquals(MahjongDef.TYPE_FENG, MahjongDef.type(MahjongDef.FENG_BEI));

		assertEquals(MahjongDef.TYPE_JIAN, MahjongDef.type(MahjongDef.JIAN_ZHONG));
		assertEquals(MahjongDef.TYPE_JIAN, MahjongDef.type(MahjongDef.JIAN_BAI));

		assertEquals(MahjongDef.TYPE_HUA, MahjongDef.type(MahjongDef.HUA_CHUN));
		assertEquals(MahjongDef.TYPE_HUA, MahjongDef.type(MahjongDef.HUA_JU));

		assertEquals(0, MahjongDef.type(999));
	}

	@Test
	public void testToCard()
	{
		assertEquals(MahjongDef.WAN1, MahjongDef.toCard(MahjongDef.TYPE_WAN, 0));
		assertEquals(MahjongDef.WAN5, MahjongDef.toCard(MahjongDef.TYPE_WAN, 4));
		assertEquals(MahjongDef.TONG1, MahjongDef.toCard(MahjongDef.TYPE_TONG, 0));
		assertEquals(MahjongDef.TIAO9, MahjongDef.toCard(MahjongDef.TYPE_TIAO, 8));
		assertEquals(MahjongDef.FENG_DONG, MahjongDef.toCard(MahjongDef.TYPE_FENG, 0));
		assertEquals(MahjongDef.JIAN_ZHONG, MahjongDef.toCard(MahjongDef.TYPE_JIAN, 0));
		assertEquals(MahjongDef.HUA_CHUN, MahjongDef.toCard(MahjongDef.TYPE_HUA, 0));
		assertEquals(0, MahjongDef.toCard(99, 0));
	}

	@Test
	public void testCardToStringAndReverse()
	{
		assertEquals("1万", MahjongDef.cardToString(MahjongDef.WAN1));
		assertEquals(MahjongDef.WAN1, MahjongDef.stringToCard("1万"));

		assertEquals("9万", MahjongDef.cardToString(MahjongDef.WAN9));
		assertEquals(MahjongDef.WAN9, MahjongDef.stringToCard("9万"));

		assertEquals("5筒", MahjongDef.cardToString(MahjongDef.TONG5));
		assertEquals(MahjongDef.TONG5, MahjongDef.stringToCard("5筒"));

		assertEquals("3条", MahjongDef.cardToString(MahjongDef.TIAO3));
		assertEquals(MahjongDef.TIAO3, MahjongDef.stringToCard("3条"));

		assertEquals("东", MahjongDef.cardToString(MahjongDef.FENG_DONG));
		assertEquals(MahjongDef.FENG_DONG, MahjongDef.stringToCard("东"));

		assertEquals("中", MahjongDef.cardToString(MahjongDef.JIAN_ZHONG));
		assertEquals(MahjongDef.JIAN_ZHONG, MahjongDef.stringToCard("中"));

		assertEquals("菊", MahjongDef.cardToString(MahjongDef.HUA_JU));
		assertEquals(MahjongDef.HUA_JU, MahjongDef.stringToCard("菊"));

		assertEquals("错误999", MahjongDef.cardToString(999));
		assertEquals(0, MahjongDef.stringToCard("未知"));
	}

	@Test
	public void testCardsToStringAndReverse()
	{
		List<Integer> cards = Arrays.asList(MahjongDef.WAN1, MahjongDef.WAN2, MahjongDef.WAN3);
		assertEquals("1万,2万,3万,", MahjongDef.cardsToString(cards));

		List<Integer> parsed = MahjongDef.stringToCards("1万,2万,3万");
		assertEquals(cards, parsed);

		HashSet<Integer> set = new HashSet<>(cards);
		String setStr = MahjongDef.cardsToString(set);
		assertTrue(setStr.contains("1万,"));
		assertTrue(setStr.contains("2万,"));
		assertTrue(setStr.contains("3万,"));

		assertEquals("", MahjongDef.cardsToString((Iterable<Integer>) null));
		assertEquals(0, MahjongDef.stringToCards("").size());
	}
}

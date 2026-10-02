package com.github.esrrhs.majiang_algorithm;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MaJiangDefTest
{
	@Test
	public void testCardConstantsAndTypes()
	{
		assertEquals(MaJiangDef.TYPE_WAN, MaJiangDef.type(MaJiangDef.WAN1));
		assertEquals(MaJiangDef.TYPE_WAN, MaJiangDef.type(MaJiangDef.WAN9));

		assertEquals(MaJiangDef.TYPE_TONG, MaJiangDef.type(MaJiangDef.TONG1));
		assertEquals(MaJiangDef.TYPE_TONG, MaJiangDef.type(MaJiangDef.TONG9));

		assertEquals(MaJiangDef.TYPE_TIAO, MaJiangDef.type(MaJiangDef.TIAO1));
		assertEquals(MaJiangDef.TYPE_TIAO, MaJiangDef.type(MaJiangDef.TIAO9));

		assertEquals(MaJiangDef.TYPE_FENG, MaJiangDef.type(MaJiangDef.FENG_DONG));
		assertEquals(MaJiangDef.TYPE_FENG, MaJiangDef.type(MaJiangDef.FENG_BEI));

		assertEquals(MaJiangDef.TYPE_JIAN, MaJiangDef.type(MaJiangDef.JIAN_ZHONG));
		assertEquals(MaJiangDef.TYPE_JIAN, MaJiangDef.type(MaJiangDef.JIAN_BAI));

		assertEquals(MaJiangDef.TYPE_HUA, MaJiangDef.type(MaJiangDef.HUA_CHUN));
		assertEquals(MaJiangDef.TYPE_HUA, MaJiangDef.type(MaJiangDef.HUA_JU));

		assertEquals(0, MaJiangDef.type(999));
	}

	@Test
	public void testToCard()
	{
		assertEquals(MaJiangDef.WAN1, MaJiangDef.toCard(MaJiangDef.TYPE_WAN, 0));
		assertEquals(MaJiangDef.WAN5, MaJiangDef.toCard(MaJiangDef.TYPE_WAN, 4));
		assertEquals(MaJiangDef.TONG1, MaJiangDef.toCard(MaJiangDef.TYPE_TONG, 0));
		assertEquals(MaJiangDef.TIAO9, MaJiangDef.toCard(MaJiangDef.TYPE_TIAO, 8));
		assertEquals(MaJiangDef.FENG_DONG, MaJiangDef.toCard(MaJiangDef.TYPE_FENG, 0));
		assertEquals(MaJiangDef.JIAN_ZHONG, MaJiangDef.toCard(MaJiangDef.TYPE_JIAN, 0));
		assertEquals(MaJiangDef.HUA_CHUN, MaJiangDef.toCard(MaJiangDef.TYPE_HUA, 0));
		assertEquals(0, MaJiangDef.toCard(99, 0));
	}

	@Test
	public void testCardToStringAndReverse()
	{
		assertEquals("1万", MaJiangDef.cardToString(MaJiangDef.WAN1));
		assertEquals(MaJiangDef.WAN1, MaJiangDef.stringToCard("1万"));

		assertEquals("9万", MaJiangDef.cardToString(MaJiangDef.WAN9));
		assertEquals(MaJiangDef.WAN9, MaJiangDef.stringToCard("9万"));

		assertEquals("5筒", MaJiangDef.cardToString(MaJiangDef.TONG5));
		assertEquals(MaJiangDef.TONG5, MaJiangDef.stringToCard("5筒"));

		assertEquals("3条", MaJiangDef.cardToString(MaJiangDef.TIAO3));
		assertEquals(MaJiangDef.TIAO3, MaJiangDef.stringToCard("3条"));

		assertEquals("东", MaJiangDef.cardToString(MaJiangDef.FENG_DONG));
		assertEquals(MaJiangDef.FENG_DONG, MaJiangDef.stringToCard("东"));

		assertEquals("中", MaJiangDef.cardToString(MaJiangDef.JIAN_ZHONG));
		assertEquals(MaJiangDef.JIAN_ZHONG, MaJiangDef.stringToCard("中"));

		assertEquals("菊", MaJiangDef.cardToString(MaJiangDef.HUA_JU));
		assertEquals(MaJiangDef.HUA_JU, MaJiangDef.stringToCard("菊"));

		assertEquals("错误999", MaJiangDef.cardToString(999));
		assertEquals(0, MaJiangDef.stringToCard("未知"));
	}

	@Test
	public void testCardsToStringAndReverse()
	{
		List<Integer> cards = Arrays.asList(MaJiangDef.WAN1, MaJiangDef.WAN2, MaJiangDef.WAN3);
		assertEquals("1万,2万,3万,", MaJiangDef.cardsToString(cards));

		List<Integer> parsed = MaJiangDef.stringToCards("1万,2万,3万");
		assertEquals(cards, parsed);

		HashSet<Integer> set = new HashSet<>(cards);
		String setStr = MaJiangDef.cardsToString(set);
		assertTrue(setStr.contains("1万,"));
		assertTrue(setStr.contains("2万,"));
		assertTrue(setStr.contains("3万,"));

		assertEquals("", MaJiangDef.cardsToString((Iterable<Integer>) null));
		assertEquals(0, MaJiangDef.stringToCards("").size());
	}
}

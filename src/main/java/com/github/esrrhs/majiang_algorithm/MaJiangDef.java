package com.github.esrrhs.majiang_algorithm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by bjzhaoxin on 2017/12/4.
 */
public class MaJiangDef
{
	public static final int WAN1 = 1;
	public static final int WAN2 = 2;
	public static final int WAN3 = 3;
	public static final int WAN4 = 4;
	public static final int WAN5 = 5;
	public static final int WAN6 = 6;
	public static final int WAN7 = 7;
	public static final int WAN8 = 8;
	public static final int WAN9 = 9;

	public static final int TONG1 = 10;
	public static final int TONG2 = 11;
	public static final int TONG3 = 12;
	public static final int TONG4 = 13;
	public static final int TONG5 = 14;
	public static final int TONG6 = 15;
	public static final int TONG7 = 16;
	public static final int TONG8 = 17;
	public static final int TONG9 = 18;

	public static final int TIAO1 = 19;
	public static final int TIAO2 = 20;
	public static final int TIAO3 = 21;
	public static final int TIAO4 = 22;
	public static final int TIAO5 = 23;
	public static final int TIAO6 = 24;
	public static final int TIAO7 = 25;
	public static final int TIAO8 = 26;
	public static final int TIAO9 = 27;

	public static final int FENG_DONG = 28;
	public static final int FENG_NAN = 29;
	public static final int FENG_XI = 30;
	public static final int FENG_BEI = 31;

	public static final int JIAN_ZHONG = 32;
	public static final int JIAN_FA = 33;
	public static final int JIAN_BAI = 34;

	public static final int HUA_CHUN = 35;
	public static final int HUA_XIA = 36;
	public static final int HUA_QIU = 37;
	public static final int HUA_DONG = 38;
	public static final int HUA_MEI = 39;
	public static final int HUA_LAN = 40;
	public static final int HUA_ZHU = 41;
	public static final int HUA_JU = 42;

	public static final int MAX_NUM = 42;

	public static final int TYPE_WAN = 1;
	public static final int TYPE_TONG = 2;
	public static final int TYPE_TIAO = 3;
	public static final int TYPE_FENG = 4;
	public static final int TYPE_JIAN = 5;
	public static final int TYPE_HUA = 6;

	public static int toCard(int type, int index)
	{
		switch (type)
		{
			case TYPE_WAN:
				return WAN1 + index;
			case TYPE_TONG:
				return TONG1 + index;
			case TYPE_TIAO:
				return TIAO1 + index;
			case TYPE_FENG:
				return FENG_DONG + index;
			case TYPE_JIAN:
				return JIAN_ZHONG + index;
			case TYPE_HUA:
				return HUA_CHUN + index;
		}
		return 0;
	}

	public static String cardsToString(List<Integer> card)
	{
		String ret = "";
		for (int c : card)
		{
			ret += cardToString(c) + ",";
		}
		return ret;
	}

	public static String cardsToString(HashSet<Integer> card)
	{
		String ret = "";
		for (int c : card)
		{
			ret += cardToString(c) + ",";
		}
		return ret;
	}

	public static String cardToString(int card)
	{
		if (card >= WAN1 && card <= WAN9)
		{
			return (card - WAN1 + 1) + "万";
		}
		if (card >= TONG1 && card <= TONG9)
		{
			return (card - TONG1 + 1) + "筒";
		}
		if (card >= TIAO1 && card <= TIAO9)
		{
			return (card - TIAO1 + 1) + "条";
		}

		final String[] strs = new String[]
		{ "东", "南", "西", "北", "中", "发", "白", "春", "夏", "秋", "冬", "梅", "兰", "竹", "菊" };
		if (card >= FENG_DONG && card <= MAX_NUM)
		{
			return strs[card - FENG_DONG];
		}
		return "错误" + card;
	}

	public static List<Integer> stringToCards(String str)
	{
		List<Integer> ret = new ArrayList<>();
		String[] strs = str.split(",");
		for (String s : strs)
		{
			if (s != null && s.length() > 0)
			{
				ret.add(stringToCard(s));
			}
		}
		return ret;
	}

	public static int stringToCard(String str)
	{
		if (str.contains("万"))
		{
			return WAN1 - 1 + Integer.parseInt(str.substring(0, 1));
		}
		if (str.contains("筒"))
		{
			return TONG1 - 1 + Integer.parseInt(str.substring(0, 1));
		}
		if (str.contains("条"))
		{
			return TIAO1 - 1 + Integer.parseInt(str.substring(0, 1));
		}

		final String[] strs = new String[]
		{ "东", "南", "西", "北", "中", "发", "白", "春", "夏", "秋", "冬", "梅", "兰", "竹", "菊" };
		int c = FENG_DONG;
		for (String s : strs)
		{
			if (str.contains(s))
			{
				return c;
			}
			c++;
		}

		return 0;
	}

	public static int type(int card)
	{
		if (card >= WAN1 && card <= WAN9)
		{
			return TYPE_WAN;
		}
		if (card >= TONG1 && card <= TONG9)
		{
			return TYPE_TONG;
		}
		if (card >= TIAO1 && card <= TIAO9)
		{
			return TYPE_TIAO;
		}
		if (card >= FENG_DONG && card <= FENG_BEI)
		{
			return TYPE_FENG;
		}
		if (card >= JIAN_ZHONG && card <= JIAN_BAI)
		{
			return TYPE_JIAN;
		}
		if (card >= HUA_CHUN && card <= HUA_JU)
		{
			return TYPE_HUA;
		}
		return 0;
	}

}
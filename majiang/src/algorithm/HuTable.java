package algorithm;

import java.util.HashSet;

/**
 * Created by bjzhaoxin on 2017/11/17.
 */
public class HuTable
{
	public static String show_card(long card)
	{
		int[] num = new int[9];
		long tmp = card;
		for (int i = 0; i < 9; i++)
		{
			num[8 - i] = (int) (tmp % 10);
			tmp = tmp / 10;
		}
		String str = String.format("%09d", card);
		String ret = "";
		int index = 1;
		for (int i : num)
		{
			String str1 = index + "万";
			for (int j = 0; j < i; j++)
			{
				ret += str1 + " ";
			}
			index++;
		}
		ret += " " + str;
		return ret;
	}

	public static void gen()
	{
		HashSet<Long> card = new HashSet<>();

		int[] num = new int[9];
		gen_card(card, num, 0, 13);

		for (long l : card)
		{
			check_hu(l);
		}

		check_hu(201401032);
	}

	public static void check_hu(long card)
	{
		int[] num = new int[9];
		long tmp = card;
		for (int i = 0; i < 9; i++)
		{
			num[8 - i] = (int) (tmp % 10);
			tmp = tmp / 10;
		}

		HashSet<HuInfo> huInfos = new HashSet<>();
		for (int i = 0; i < 9; i++)
		{
			num[i]++;
			if (num[i] <= 4)
			{
				check_hu(huInfos, num, -1, i);
			}
			num[i]--;
		}

		System.out.println("--------------------------------");
		String str = String.format("%09d", card);
		System.out.println(show_card(card));
		for (HuInfo huInfo : huInfos)
		{
			System.out.println(huInfo);
		}
	}

	public static void check_hu(HashSet<HuInfo> huInfos, int[] num, int jiang, int in)
	{
		for (int i = 0; i < 9; i++)
		{
			if (num[i] > 0 && i + 1 < 9 && num[i + 1] > 0 && i + 2 < 9 && num[i + 2] > 0)
			{
				num[i]--;
				num[i + 1]--;
				num[i + 2]--;
				check_hu(huInfos, num, jiang, in);
				num[i]++;
				num[i + 1]++;
				num[i + 2]++;
			}
		}

		for (int i = 0; i < 9; i++)
		{
			if (num[i] >= 2 && jiang == -1)
			{
				num[i] -= 2;
				check_hu(huInfos, num, i, in);
				num[i] += 2;
			}
		}

		for (int i = 0; i < 9; i++)
		{
			if (num[i] >= 3)
			{
				num[i] -= 3;
				check_hu(huInfos, num, jiang, in);
				num[i] += 3;
			}
		}

		for (int i = 0; i < 9; i++)
		{
			if (num[i] >= 4)
			{
				num[i] -= 4;
				check_hu(huInfos, num, jiang, in);
				num[i] += 4;
			}
		}

		for (int i = 0; i < 9; i++)
		{
			if (num[i] != 0)
			{
				return;
			}
		}

		HuInfo huInfo = new HuInfo();
		huInfo.hupai = in;
		huInfo.jiang = jiang;
		huInfos.add(huInfo);
	}

	private static void gen_card(HashSet<Long> card, int num[], int index, int total)
	{
		if (index == 8)
		{
			if (total > 4)
			{
				return;
			}
			num[index] = total;

			long ret = 0;
			for (int c : num)
			{
				ret = ret * 10 + c;
			}
			card.add(ret);
			return;
		}
		for (int i = 0; i <= 4; i++)
		{
			if (i <= total)
			{
				num[index] = i;
			}
			else
			{
				num[index] = 0;
			}
			gen_card(card, num, index + 1, total - num[index]);
		}
	}
}

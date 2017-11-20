package algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by bjzhaoxin on 2017/11/17.
 */
public class HuTable
{
	public static ConcurrentHashMap<Long, List<HuTableInfo>> table = new ConcurrentHashMap<>();
	public static int N;

	public static void gen()
	{
		HashSet<Long> card = new HashSet<>();

		for (int i = 1; i <= 13; i++)
		{
			int[] num = new int[9];
			gen_card(card, num, 0, i);
		}

		System.out.print(card.size());

		for (final long l : card)
		{
			check_hu(l);
		}
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

		int total = 0;
		for (int i = 0; i < 9; i++)
		{
			total += num[i];
		}

		HashSet<HuInfo> huInfos = new HashSet<>();

		for (int guinum = 0; guinum <= 7 && total + guinum < 13; guinum++)
		{
			int[] tmpnum = new int[9];
			HashSet<Long> tmpcard = new HashSet<>();
			gen_card(tmpcard, tmpnum, 0, guinum);

			for (long tmpgui : tmpcard)
			{
				int[] tmpguinum = new int[9];
				long tt = tmpgui;
				for (int i = 0; i < 9; i++)
				{
					tmpguinum[8 - i] = (int) (tt % 10);
					tt = tt / 10;
				}

				for (int i = 0; i < 9; i++)
				{
					num[i] += tmpguinum[i];
				}

				for (int i = 0; i < 9; i++)
				{
					check_hu(huInfos, num, -1, -1, guinum);
					num[i]++;
					if (num[i] <= 4)
					{
						check_hu(huInfos, num, -1, i, guinum);
					}
					num[i]--;
				}

				for (int i = 0; i < 9; i++)
				{
					num[i] -= tmpguinum[i];
				}
			}
		}

		HashMap<Integer, HuTableInfo> huTableInfos = new HashMap<>();
		for (HuInfo huInfo : huInfos)
		{
			int key = huInfo.needGui * 10 + (huInfo.jiang != -1 ? 1 : 0);

			if (huTableInfos.get(key) != null)
			{
				if (huInfo.hupai == -1)
				{
					huTableInfos.get(key).hupai = null;
				}
				if (huTableInfos.get(key).hupai != null)
				{
					if (huTableInfos.get(key).hupai[huInfo.hupai] == 0)
					{
						huTableInfos.get(key).hupai[huInfo.hupai]++;
					}
				}
			}
			else
			{
				HuTableInfo huTableInfo = new HuTableInfo();
				huTableInfo.needGui = huInfo.needGui;
				huTableInfo.jiang = huInfo.jiang != -1;
				huTableInfo.hupai = new byte[9];
				if (huInfo.hupai == -1)
				{
					huTableInfo.hupai = null;
				}
				else
				{
					huTableInfo.hupai[huInfo.hupai]++;
				}
				huTableInfos.put(key, huTableInfo);
			}
		}

		List<HuTableInfo> tmphu = new ArrayList<>();
		tmphu.addAll(huTableInfos.values());
		table.put(card, tmphu);

		N += tmphu.size();

		System.out.println("-------------------------------- " + N);
		//		System.out.println(show_card(card));
		//		for (HuTableInfo huTableInfo : huTableInfos.values())
		//		{
		//			System.out.println(huTableInfo);
		//		}
	}

	public static void check_hu(HashSet<HuInfo> huInfos, int[] num, int jiang, int in, int gui)
	{
		for (int i = 0; i < 9; i++)
		{
			if (num[i] > 0 && i + 1 < 9 && num[i + 1] > 0 && i + 2 < 9 && num[i + 2] > 0)
			{
				num[i]--;
				num[i + 1]--;
				num[i + 2]--;
				check_hu(huInfos, num, jiang, in, gui);
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
				check_hu(huInfos, num, i, in, gui);
				num[i] += 2;
			}
		}

		for (int i = 0; i < 9; i++)
		{
			if (num[i] >= 3)
			{
				num[i] -= 3;
				check_hu(huInfos, num, jiang, in, gui);
				num[i] += 3;
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
		huInfo.hupai = (byte) in;
		huInfo.jiang = (byte) jiang;
		huInfo.needGui = (byte) gui;
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

}

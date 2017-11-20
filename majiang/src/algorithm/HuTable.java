package algorithm;

import java.io.File;
import java.io.FileOutputStream;
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

	public static void gen()
	{
		HashSet<Long> card = new HashSet<>();

		for (int i = 1; i <= 13; i++)
		{
			int[] num = new int[9];
			gen_card(card, num, 0, i);
		}

		System.out.println(card.size());

		try
		{
			File file = new File("majiang_client.txt");
			if (file.exists())
			{
				file.delete();
			}
			file.createNewFile();
			FileOutputStream out = new FileOutputStream(file, true);

			File file1 = new File("majiang_server.txt");
			if (file1.exists())
			{
				file1.delete();
			}
			file1.createNewFile();
			FileOutputStream out1 = new FileOutputStream(file1, true);

			long begin = System.currentTimeMillis();
			int i = 0;
			for (final long l : card)
			{
				check_hu(l);
				output(l, out);
				output_server(l, out1);

				i++;
				long now = System.currentTimeMillis();
				float per = (float) (now - begin) / i;
				System.out.println((float) i / card.size() + " 需要" + per * (card.size() - i) / 60 / 1000 + "分" + " 用时"
						+ (now - begin) / 60 / 1000 + "分" + " 速度" + i / ((float) (now - begin) / 1000) + "条/秒");
			}

			out.close();
			out1.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private static void output_server(long card, FileOutputStream out) throws Exception
	{
		long key = card;

		List<HuTableInfo> huTableInfos = table.get(card);
		if (huTableInfos.isEmpty())
		{
			String str = String.format("%09d ", key);
			str += show_card(key);
			str += " 不胡";
			str += "\n";

			out.write(str.toString().getBytes("utf-8"));
		}
		else
		{
			for (HuTableInfo huTableInfo : huTableInfos)
			{
				String str = String.format("%09d ", key);
				str += huTableInfo.needGui + " ";
				str += huTableInfo.jiang ? "1 " : "0 ";
				if (huTableInfo.hupai == null)
				{
					str += "000000000";
				}
				else
				{
					for (int i : huTableInfo.hupai)
					{
						str += i + "";
					}
				}
				str += " ";
				str += show_card(key) + " ";
				str += "鬼" + huTableInfo.needGui + " ";
				str += huTableInfo.jiang ? "有将 " : "无将 ";
				if (huTableInfo.hupai == null)
				{
					str += "胡了";
				}
				else
				{
					int index = 1;
					for (int i : huTableInfo.hupai)
					{
						if (i > 0)
						{
							str += "胡" + index + "万";
						}
						index++;
					}
				}

				str += "\n";
				out.write(str.toString().getBytes("utf-8"));
			}
		}
	}

	private static void output(long card, FileOutputStream out) throws Exception
	{
		long key = card;

		List<HuTableInfo> huTableInfos = table.get(card);
		if (huTableInfos.isEmpty())
		{
			String str = String.format("%09d", key);
			str += "\n";
			out.write(str.toString().getBytes("utf-8"));
		}
		else
		{
			for (HuTableInfo huTableInfo : huTableInfos)
			{
				String str = String.format("%09d", key);
				str += " ";
				str += huTableInfo.needGui + " ";
				str += huTableInfo.jiang ? "1 " : "0 ";
				if (huTableInfo.hupai == null)
				{
					str += "0";
				}
				else
				{
					for (int i : huTableInfo.hupai)
					{
						str += i + "";
					}
				}
				str += "\n";
				out.write(str.toString().getBytes("utf-8"));
			}
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

		for (int guinum = 0; guinum <= 7 && total + guinum <= 13; guinum++)
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
		String ret = "";
		int index = 1;
		for (int i : num)
		{
			String str1 = index + "万";
			for (int j = 0; j < i; j++)
			{
				ret += str1 + "";
			}
			index++;
		}
		return ret;
	}

}

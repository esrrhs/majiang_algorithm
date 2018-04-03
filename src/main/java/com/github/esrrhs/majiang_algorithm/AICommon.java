package com.github.esrrhs.majiang_algorithm;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class AICommon
{
	public static ConcurrentHashMap<Long, List<AITableInfo>> table;
	public static int N;
	public static String NAME;
	public static String[] CARD;
	public static boolean huLian;
	public static double baseP;
	public static final int LEVEL = 5;

	public static void main(String[] args)
	{
		AICommon.table = new ConcurrentHashMap<>();
		AICommon.N = 9;
		AICommon.NAME = "normal";
		AICommon.CARD = AITable.names;
		AICommon.huLian = true;
		AICommon.baseP = 36.d / 136;

		HashMap<Integer, HashSet<Long>> tmpcards = new HashMap<>();
		for (int inputNum = 0; inputNum <= LEVEL; inputNum++)
		{
			int[] tmpnum = new int[N];
			HashSet<Long> tmpcard = new HashSet<>();
			gen_card(tmpcard, tmpnum, 0, inputNum);
			tmpcards.put(inputNum, tmpcard);
		}

		long key = 20110000;
		check_ai(key, tmpcards);
		try
		{
			File file = new File("majiang_ai_" + NAME + ".txt");
			if (file.exists())
			{
				file.delete();
			}
			file.createNewFile();
			FileOutputStream out = new FileOutputStream(file, true);
			output(key, out);
			out.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void gen()
	{
		final HashSet<Long> card = new HashSet<>();

		for (int i = 0; i <= 14; i++)
		{
			int[] num = new int[N];
			gen_card(card, num, 0, i);
		}

		HashMap<Integer, HashSet<Long>> tmpcards = new HashMap<>();
		for (int inputNum = 0; inputNum <= LEVEL; inputNum++)
		{
			int[] tmpnum = new int[N];
			HashSet<Long> tmpcard = new HashSet<>();
			gen_card(tmpcard, tmpnum, 0, inputNum);
			tmpcards.put(inputNum, tmpcard);
		}

		System.out.println(card.size());

		try
		{
			File file = new File("majiang_ai_" + NAME + ".txt");
			if (file.exists())
			{
				file.delete();
			}
			file.createNewFile();
			final FileOutputStream out = new FileOutputStream(file, true);

			ExecutorService fixedThreadPool = Executors.newFixedThreadPool(8);

			final long begin = System.currentTimeMillis();
			final AtomicInteger i = new AtomicInteger(0);
			for (final long l : card)
			{
				fixedThreadPool.execute(new Runnable() {
					public void run()
					{
						try
						{
							check_ai(l, tmpcards);
							output(l, out);

							i.addAndGet(1);
							long now = System.currentTimeMillis();
							float per = (float) (now - begin) / i.intValue();
							synchronized (AICommon.class)
							{
								System.out.println((float) i.intValue() / card.size() + " 需要"
										+ per * (card.size() - i.intValue()) / 60 / 1000 + "分" + " 用时"
										+ (now - begin) / 60 / 1000 + "分" + " 速度"
										+ i.intValue() / ((float) (now - begin) / 1000) + "条/秒");
							}
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
				});
			}

			fixedThreadPool.shutdown();
			while (!fixedThreadPool.isTerminated())
			{
				try
				{
					Thread.sleep(100);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}

			out.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private static void output(long card, FileOutputStream out) throws Exception
	{
		long key = card;

		List<AITableInfo> aiTableInfos = table.get(card);
		if (!aiTableInfos.isEmpty())
		{
			for (AITableInfo aiTableInfo : aiTableInfos)
			{
				String str = key + " ";
				str += aiTableInfo.jiang ? "1 " : "0 ";
				str += aiTableInfo.p;
				str += " ";
				str += show_card(key) + " ";
				str += aiTableInfo.jiang ? "有将 " : "无将 ";
				str += aiTableInfo.p;
				str += "\n";
				synchronized (AICommon.class)
				{
					out.write(str.toString().getBytes("utf-8"));
				}
			}
		}
	}

	public static void check_ai(long card, HashMap<Integer, HashSet<Long>> tmpcards)
	{
		int[] num = new int[N];
		long tmp = card;
		for (int i = 0; i < N; i++)
		{
			num[N - 1 - i] = (int) (tmp % 10);
			tmp = tmp / 10;
		}

		int total = 0;
		for (int i = 0; i < N; i++)
		{
			total += num[i];
		}

		HashMap<Integer, AITableInfo> aiTableInfos = new HashMap<>();

		AITableInfo aiTableInfo = new AITableInfo();
		aiTableInfo.p = 0;
		aiTableInfo.jiang = true;
		int key = aiTableInfo.jiang ? 1 : 0;
		aiTableInfos.put(key, aiTableInfo);
		aiTableInfo = new AITableInfo();
		aiTableInfo.p = 0;
		aiTableInfo.jiang = false;
		key = aiTableInfo.jiang ? 1 : 0;
		aiTableInfos.put(key, aiTableInfo);

		for (int inputNum = 0; inputNum <= LEVEL; inputNum++)
		{
			HashSet<Long> tmpcard = tmpcards.get(inputNum);

			HashSet<AIInfo> aiInfos = new HashSet<>();

			int valid = 0;

			for (long tmpc : tmpcard)
			{
				int[] tmpcnum = new int[N];
				long tt = tmpc;
				for (int i = 0; i < N; i++)
				{
					tmpcnum[N - 1 - i] = (int) (tt % 10);
					tt = tt / 10;
				}

				boolean max = false;
				for (int i = 0; i < N; i++)
				{
					num[i] += tmpcnum[i];
					if (num[i] > 4)
					{
						max = true;
					}
				}

				if (!max)
				{
					check_ai(aiInfos, num, -1, inputNum);
					valid++;
				}

				for (int i = 0; i < N; i++)
				{
					num[i] -= tmpcnum[i];
				}
			}

			for (AIInfo aiInfo : aiInfos)
			{
				key = aiInfo.jiang != -1 ? 1 : 0;
				if (aiInfo.inputNum == 0)
				{
					aiTableInfos.get(key).p = 1;
				}
				if (aiTableInfos.get(key).p != 1)
				{
					key = aiInfo.jiang != -1 ? 1 : 0;
					aiTableInfos.get(key).p += baseP * 1.d / valid;
				}
			}
		}

		List<AITableInfo> tmpAI = new ArrayList<>();
		tmpAI.addAll(aiTableInfos.values());
		table.put(card, tmpAI);
	}

	public static void check_ai(HashSet<AIInfo> aiInfos, int[] num, int jiang, int inputNum)
	{
		if (huLian)
		{
			for (int i = 0; i < N; i++)
			{
				if (num[i] > 0 && i + 1 < N && num[i + 1] > 0 && i + 2 < N && num[i + 2] > 0)
				{
					num[i]--;
					num[i + 1]--;
					num[i + 2]--;
					check_ai(aiInfos, num, jiang, inputNum);
					num[i]++;
					num[i + 1]++;
					num[i + 2]++;
				}
			}
		}

		for (int i = 0; i < N; i++)
		{
			if (num[i] >= 2 && jiang == -1)
			{
				num[i] -= 2;
				check_ai(aiInfos, num, i, inputNum);
				num[i] += 2;
			}
		}

		for (int i = 0; i < N; i++)
		{
			if (num[i] >= 3)
			{
				num[i] -= 3;
				check_ai(aiInfos, num, jiang, inputNum);
				num[i] += 3;
			}
		}

		for (int i = 0; i < N; i++)
		{
			if (num[i] != 0)
			{
				return;
			}
		}

		AIInfo aiInfo = new AIInfo();
		aiInfo.inputNum = (byte) inputNum;
		aiInfo.jiang = (byte) jiang;
		aiInfos.add(aiInfo);
	}

	private static void gen_card(HashSet<Long> card, int num[], int index, int total)
	{
		if (index == N - 1)
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
		int[] num = new int[N];
		long tmp = card;
		for (int i = 0; i < N; i++)
		{
			num[N - 1 - i] = (int) (tmp % 10);
			tmp = tmp / 10;
		}
		String ret = "";
		int index = 1;
		for (int i : num)
		{
			String str1 = CARD[index - 1];
			for (int j = 0; j < i; j++)
			{
				ret += str1 + "";
			}
			index++;
		}
		return ret;
	}

	public static void load()
	{
		try
		{
			FileInputStream inputStream = new FileInputStream("majiang_ai_" + NAME + ".txt");
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

			List<String> lines = new ArrayList<>();
			String str = null;
			while ((str = bufferedReader.readLine()) != null)
			{
				lines.add(str);
			}
			load(lines);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void load(List<String> lines)
	{
		int total = 0;
		try
		{
			for (String str : lines)
			{
				String[] strs = str.split(" ");
				long key = Long.parseLong(strs[0]);
				int jiang = Integer.parseInt(strs[1]);
				double p = Double.parseDouble(strs[2]);

				List<AITableInfo> aiTableInfos = table.get(key);
				if (aiTableInfos == null)
				{
					aiTableInfos = new ArrayList<>();
					table.put(key, aiTableInfos);
				}

				AITableInfo aiTableInfo = new AITableInfo();
				aiTableInfo.jiang = jiang != 0;
				aiTableInfo.p = p;
				aiTableInfos.add(aiTableInfo);
				total++;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}

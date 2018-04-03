package com.github.esrrhs.majiang_algorithm;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by bjzhaoxin on 2017/11/17.
 */
public class AITable
{
	public static ConcurrentHashMap<Long, List<AITableInfo>> table = new ConcurrentHashMap<>();
	public static String[] names = new String[]
	{ "1万", "2万", "3万", "4万", "5万", "6万", "7万", "8万", "9万" };

	public static void gen()
	{
		AICommon.table = table;
		AICommon.N = 9;
		AICommon.NAME = "normal";
		AICommon.CARD = names;
		AICommon.huLian = true;
		AICommon.baseP = 36.d / 136;
		AICommon.gen();
	}

	public static void load()
	{
		table.clear();
		AICommon.table = table;
		AICommon.N = 9;
		AICommon.NAME = "normal";
		AICommon.CARD = names;
		AICommon.huLian = true;
		AICommon.baseP = 36.d / 136;
		AICommon.load();
	}

	public static void load(List<String> lines)
	{
		table.clear();
		AICommon.table = table;
		AICommon.N = 9;
		AICommon.NAME = "normal";
		AICommon.CARD = names;
		AICommon.huLian = true;
		AICommon.baseP = 36.d / 136;
		AICommon.load(lines);
	}
}

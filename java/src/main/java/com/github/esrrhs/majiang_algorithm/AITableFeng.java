package com.github.esrrhs.majiang_algorithm;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by bjzhaoxin on 2017/11/17.
 */
public class AITableFeng
{
	public static ConcurrentHashMap<Long, List<AITableInfo>> table = new ConcurrentHashMap<>();
	public static String ziname[] = new String[]
	{ "东", "南", "西", "北" };

	public static void gen()
	{
		AICommon.table = table;
		AICommon.N = 4;
		AICommon.NAME = "feng";
		AICommon.CARD = ziname;
		AICommon.huLian = false;
		AICommon.baseP = 16.d / 136;
		AICommon.gen();
	}

	public static void load()
	{
		table.clear();
		AICommon.table = table;
		AICommon.N = 4;
		AICommon.NAME = "feng";
		AICommon.CARD = ziname;
		AICommon.huLian = false;
		AICommon.baseP = 16.d / 136;
		AICommon.load();
	}

	public static void load(List<String> lines)
	{
		table.clear();
		AICommon.table = table;
		AICommon.N = 4;
		AICommon.NAME = "feng";
		AICommon.CARD = ziname;
		AICommon.huLian = false;
		AICommon.baseP = 16.d / 136;
		AICommon.load(lines);
	}
}

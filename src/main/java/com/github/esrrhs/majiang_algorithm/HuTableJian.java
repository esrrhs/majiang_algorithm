package com.github.esrrhs.majiang_algorithm;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by bjzhaoxin on 2017/11/17.
 */
public class HuTableJian
{
	public static ConcurrentHashMap<Long, List<HuTableInfo>> table = new ConcurrentHashMap<>();
	public static String ziname[] = new String[]
	{ "中", "发", "白" };

	public static void gen()
	{
		HuCommon.table = table;
		HuCommon.N = 3;
		HuCommon.NAME = "jian";
		HuCommon.CARD = ziname;
		HuCommon.huLian = false;
		HuCommon.gen();
	}

	public static void load()
	{
		HuCommon.table = table;
		HuCommon.N = 3;
		HuCommon.NAME = "jian";
		HuCommon.CARD = ziname;
		HuCommon.huLian = false;
		HuCommon.load();
	}

	public static void load(List<String> lines)
	{
		HuCommon.table = table;
		HuCommon.N = 3;
		HuCommon.NAME = "jian";
		HuCommon.CARD = ziname;
		HuCommon.huLian = false;
		HuCommon.load(lines);
	}
}
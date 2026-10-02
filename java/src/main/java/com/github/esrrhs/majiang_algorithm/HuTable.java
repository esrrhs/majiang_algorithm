package com.github.esrrhs.majiang_algorithm;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by bjzhaoxin on 2017/11/17.
 */
public class HuTable
{
	public static ConcurrentHashMap<Long, List<HuTableInfo>> table = new ConcurrentHashMap<>();
	public static String[] names = new String[]
	{ "1万", "2万", "3万", "4万", "5万", "6万", "7万", "8万", "9万" };

	public static void gen()
	{
		HuCommon.table = table;
		HuCommon.N = 9;
		HuCommon.NAME = "normal";
		HuCommon.CARD = names;
		HuCommon.huLian = true;
		HuCommon.gen();
	}

	public static void load()
	{
		table.clear();
		HuCommon.table = table;
		HuCommon.N = 9;
		HuCommon.NAME = "normal";
		HuCommon.CARD = names;
		HuCommon.huLian = true;
		HuCommon.load();
	}

	public static void load(List<String> lines)
	{
		table.clear();
		HuCommon.table = table;
		HuCommon.N = 9;
		HuCommon.NAME = "normal";
		HuCommon.CARD = names;
		HuCommon.huLian = true;
		HuCommon.load(lines);
	}
}

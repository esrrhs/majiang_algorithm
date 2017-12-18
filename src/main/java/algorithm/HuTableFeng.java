package algorithm;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by bjzhaoxin on 2017/11/17.
 */
public class HuTableFeng
{
	public static ConcurrentHashMap<Long, List<HuTableInfo>> table = new ConcurrentHashMap<>();
	public static String ziname[] = new String[]
	{ "东", "南", "西", "北" };

	public static void gen()
	{
		HuCommon.table = table;
		HuCommon.N = 4;
		HuCommon.NAME = "feng";
		HuCommon.CARD = ziname;
		HuCommon.huLian = false;
		HuCommon.gen();
	}

	public static void load()
	{
		HuCommon.table = table;
		HuCommon.N = 4;
		HuCommon.NAME = "feng";
		HuCommon.CARD = ziname;
		HuCommon.huLian = false;
		HuCommon.load();
	}
}

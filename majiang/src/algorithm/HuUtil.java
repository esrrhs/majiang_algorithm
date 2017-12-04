package algorithm;

import java.util.List;

/**
 * Created by bjzhaoxin on 2017/12/4.
 */
public class HuUtil
{
	public static boolean isHu(List<Integer> cards, int guiIndex)
	{
		int guiNum = cards.get(guiIndex);
		cards.set(guiIndex, 0);

		long wan_key = 0;
		long tong_key = 0;
		long tiao_key = 0;
		long feng_key = 0;
		long jian_key = 0;

		for (int i = MaJiangDef.WAN1; i <= MaJiangDef.WAN9; i++)
		{
			int num = cards.get(i - 1);
			wan_key = wan_key * 10 + num;
		}
		for (int i = MaJiangDef.TONG1; i <= MaJiangDef.TONG9; i++)
		{
			int num = cards.get(i - 1);
			tong_key = tong_key * 10 + num;
		}
		for (int i = MaJiangDef.TIAO1; i <= MaJiangDef.TIAO9; i++)
		{
			int num = cards.get(i - 1);
			tiao_key = tiao_key * 10 + num;
		}
		for (int i = MaJiangDef.FENG_DONG; i <= MaJiangDef.FENG_BEI; i++)
		{
			int num = cards.get(i - 1);
			feng_key = feng_key * 10 + num;
		}
		for (int i = MaJiangDef.JIAN_ZHONG; i <= MaJiangDef.JIAN_BAI; i++)
		{
			int num = cards.get(i - 1);
			jian_key = jian_key * 10 + num;
		}
		List<HuTableInfo> wanHuTableInfo = HuTable.table.get(wan_key);
		List<HuTableInfo> tongHuTableInfo = HuTable.table.get(tong_key);
		List<HuTableInfo> tiaoHuTableInfo = HuTable.table.get(tiao_key);
		List<HuTableInfo> fengHuTableInfo = HuTableFeng.table.get(feng_key);
		List<HuTableInfo> jianHuTableInfo = HuTableJian.table.get(jian_key);
		if (wan_key != 0 && wanHuTableInfo == null)
		{
			return false;
		}
		if (tong_key != 0 && tongHuTableInfo == null)
		{
			return false;
		}
		if (tiao_key != 0 && tiaoHuTableInfo == null)
		{
			return false;
		}
		if (feng_key != 0 && fengHuTableInfo == null)
		{
			return false;
		}
		if (jian_key != 0 && jianHuTableInfo == null)
		{
			return false;
		}

		return false;
	}
}

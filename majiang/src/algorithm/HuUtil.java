package algorithm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by bjzhaoxin on 2017/12/4.
 */
public class HuUtil
{
	public static boolean isHu(List<Integer> input, int guiCard)
	{
		List<Integer> cards = new ArrayList<>();
		for (int i = 0; i < MaJiangDef.MAX_NUM; i++)
		{
			cards.add(0);
		}
		for (int c : input)
		{
			cards.set(c - 1, cards.get(c - 1) + 1);
		}
		int guiNum = cards.get(guiCard - 1);
		cards.set(guiCard - 1, 0);

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

		List<List<HuTableInfo>> tmp = new ArrayList<>();
		if (wanHuTableInfo != null)
		{
			tmp.add(wanHuTableInfo);
		}
		if (tongHuTableInfo != null)
		{
			tmp.add(tongHuTableInfo);
		}
		if (tiaoHuTableInfo != null)
		{
			tmp.add(tiaoHuTableInfo);
		}
		if (fengHuTableInfo != null)
		{
			tmp.add(fengHuTableInfo);
		}
		if (jianHuTableInfo != null)
		{
			tmp.add(jianHuTableInfo);
		}

		return isHu(tmp, 0, guiNum, false);
	}

	private static boolean isHu(List<List<HuTableInfo>> tmp, int index, int guiNum, boolean jiang)
	{
		if (index >= tmp.size())
		{
			return guiNum == 0 && jiang == true;
		}
		List<HuTableInfo> huTableInfos = tmp.get(index);
		for (HuTableInfo huTableInfo : huTableInfos)
		{
			if (jiang)
			{
				if (huTableInfo.hupai == null && huTableInfo.needGui <= guiNum && huTableInfo.jiang == false)
				{
					if (isHu(tmp, index + 1, guiNum - huTableInfo.needGui, jiang))
					{
						return true;
					}
				}
			}
			else
			{
				if (huTableInfo.hupai == null && huTableInfo.needGui <= guiNum)
				{
					if (isHu(tmp, index + 1, guiNum - huTableInfo.needGui, huTableInfo.jiang))
					{
						return true;
					}
				}
			}
		}
		return false;
	}
}

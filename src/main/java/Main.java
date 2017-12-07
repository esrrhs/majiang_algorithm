import java.util.List;

import algorithm.*;

/**
 * Created by bjzhaoxin on 2017/11/17.
 */
public class Main
{
	public static void gen()
	{
		HuTableJian.gen();
		HuTableFeng.gen();
		HuTable.gen();
	}

	public static void load()
	{
		HuTableJian.load();
		HuTableFeng.load();
		HuTable.load();
	}

	public static void testHu()
	{
		String init = "1万,1万,1万,1筒,2筒,2条,3条,4条,东,东,东";
		String gui = "1万";
		List<Integer> cards = MaJiangDef.stringToCards(init);
		System.out.println(HuUtil.isHu(cards, MaJiangDef.stringToCard(gui)));
	}

	public static void testTing()
	{
		String init = "1万,1万,1筒,3筒,2筒,2条,3条,4条,东,东";
		String gui = "1筒";
		List<Integer> cards = MaJiangDef.stringToCards(init);
		System.out.println(MaJiangDef.cardsToString(HuUtil.isTing(cards, MaJiangDef.stringToCard(gui))));
	}

	public static void main(String[] args)
	{
		//gen();
		load();
		testHu();
		testTing();
	}
}

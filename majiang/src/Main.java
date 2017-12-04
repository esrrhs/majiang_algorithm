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

	public static void test()
	{
		String init = "1万,1万,1万,2筒,2条,3条,东,中";
		List<Integer> cards = MaJiangDef.stringToCards(init);
		System.out.println(HuUtil.isHu(cards, 3));
	}

	public static void main(String[] args)
	{
		//gen();
		load();
		test();
	}
}

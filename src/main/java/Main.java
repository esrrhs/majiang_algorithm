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
		String init = "1万,1万,1万,1筒,2筒,2条,3条,4条,东,东,东";
		String gui = "1万";
		List<Integer> cards = MaJiangDef.stringToCards(init);
		System.out.println(HuUtil.isHu(cards, MaJiangDef.stringToCard(gui)));
	}

    public static void sql()
    {
        Runtime rt = Runtime.getRuntime();
        Process ps = null;  //Process可以控制该子进程的执行或获取该子进程的信息。
        try
        {
            ps = rt.exec("cmd /c start sql.bat");   //该对象的exec()方法指示Java虚拟机创建一个子进程执行指定的可执行程序，并返回与该子进程对应的Process对象实例。
            ps.waitFor();  //等待子进程完成再往下执行。
        }
        catch (Exception e1)
        {
            e1.printStackTrace();
        }
    }

	public static void main(String[] args)
	{
		gen();
		load();
		test();
        sql();
	}
}

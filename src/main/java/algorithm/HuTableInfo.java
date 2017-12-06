package algorithm;

/**
 * Created by bjzhaoxin on 2017/11/17.
 */
public class HuTableInfo
{
	public byte needGui;
	public boolean jiang;
	public byte[] hupai = new byte[9];

	@Override
	public String toString()
	{
		String tmp = "";
		int index = 1;
		if (hupai == null)
		{
			tmp = "胡清";
		}
		else
		{
			for (byte i : hupai)
			{
				if (i > 0)
				{
					tmp += "胡" + (index);
				}
				index++;
			}
		}
		return tmp + " 将" + (jiang ? "1" : "0") + " 鬼" + needGui;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		HuTableInfo that = (HuTableInfo) o;

		if (needGui != that.needGui)
			return false;
		if (jiang != that.jiang)
			return false;
		return true;

	}

	@Override
	public int hashCode()
	{
		int result = (int) needGui;
		result = 31 * result + (jiang ? 1 : 0);
		return result;
	}
}

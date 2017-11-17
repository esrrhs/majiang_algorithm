package algorithm;

/**
 * Created by bjzhaoxin on 2017/11/17.
 */
public class HuInfo
{
	public int needGui = 0;
	public int jiang = -1;
	public int hupai;

	@Override
	public String toString()
	{
		return "胡" + (hupai + 1) + " 将" + (jiang + 1);
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		HuInfo huInfo = (HuInfo) o;

		if (needGui != huInfo.needGui)
			return false;
		if (jiang != huInfo.jiang)
			return false;
		return hupai == huInfo.hupai;
	}

	@Override
	public int hashCode()
	{
		int result = needGui;
		result = 31 * result + jiang;
		result = 31 * result + hupai;
		return result;
	}
}

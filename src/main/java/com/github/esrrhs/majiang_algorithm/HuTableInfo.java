package com.github.esrrhs.majiang_algorithm;

public class HuTableInfo
{
	public byte needGui;
	public boolean jiang;
	public byte[] hupai = new byte[9];

	@Override
	public String toString()
	{
		StringBuilder tmp = new StringBuilder();
		int index = 1;
		if (hupai == null)
		{
			tmp.append("胡清");
		}
		else
		{
			for (byte i : hupai)
			{
				if (i > 0)
				{
					tmp.append("胡").append(index);
				}
				index++;
			}
		}
		tmp.append(" 将").append(jiang ? "1" : "0").append(" 鬼").append(needGui);
		return tmp.toString();
	}

}

package com.github.esrrhs.majiang_algorithm;

public class HuInfo
{
	public byte needGui;
	public byte jiang;
	public byte hupai;

	@Override
	public String toString()
	{
		return "胡" + (hupai + 1) + " 将" + (jiang + 1) + " 鬼" + needGui;
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
		if (hupai != huInfo.hupai)
			return false;
		return true;
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

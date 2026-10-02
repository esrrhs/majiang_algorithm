package com.github.esrrhs.majiang_algorithm;

public class AIInfo
{
	public byte inputNum;
	public byte jiang;

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		AIInfo huInfo = (AIInfo) o;

		if (inputNum != huInfo.inputNum)
			return false;
		if (jiang != huInfo.jiang)
			return false;
		return true;
	}

	@Override
	public int hashCode()
	{
		int result = inputNum;
		result = 31 * result + jiang;
		return result;
	}
}

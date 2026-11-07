package com.github.esrrhs.mahjong_ai;

public class AITableInfo
{
	public boolean jiang;
	public double p;

	@Override
	public String toString()
	{
		return " 将" + (jiang ? "1" : "0") + " 几率" + p;
	}

}

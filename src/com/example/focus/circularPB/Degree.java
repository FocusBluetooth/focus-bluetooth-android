package com.example.focus.circularPB;

public class Degree {

	double degree;

	
	public Degree(int gradus)
	{
		degree = (double)gradus/ 180 * Math.PI;
	}
	public Degree (double degr)
	{
		degree = degr;
	}
	
	public int getGradus() {
		double value = Math.round(degree * 180 / Math.PI); 
		return (int)value ;
	}
	
	public double getRadian()
	{
		return degree;
	}
}

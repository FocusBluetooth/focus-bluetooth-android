package com.example.focus.circularPB;

import java.lang.Math;

public class DecartPoint {

	
	int x;
	int y;
	
	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getY() {
		return y;
	}
	public DecartPoint(int x, int y) {
		super();
		this.x = x;
		this.y = y;
	}
	public DecartPoint(DecartPoint point) {
		super();
		this.x = point.getX();
		this.y = point.getY();
	}
	
	public static DecartPoint toDecart(PolarPoint point)
	{
		int x = (int)Math.round((point.getRadius() *Math.cos(point.getAngle().getRadian())));
		int y = (int)Math.round((point.getRadius() *Math.sin(point.getAngle().getRadian())));
		return new DecartPoint(x,y);
	}
	
	public static DecartPoint toDecart( Degree angle, double radius)
	{
		double x = radius * Math.cos(angle.getRadian());
		double y = radius * Math.sin(angle.getRadian());
		return new DecartPoint((int)x, (int)y);
	}
	
	public int getWidth(DecartPoint point)
	{
		return Math.abs(point.getX() - this.getX());
	}
	
	public static int getWidth(DecartPoint point1, DecartPoint point2)
	{
		return Math.abs(point1.getX() - point2.getX());
	}
	
	public int getHeight(DecartPoint point)
	{
		return Math.abs(point.getY() - this.getY());
	}
	
	public static int getHeight(DecartPoint point1, DecartPoint point2)
	{
		return Math.abs(point1.getY() - point2.getY());
	}
}

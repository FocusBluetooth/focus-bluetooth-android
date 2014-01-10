package com.example.focus.circularPB;
import java.lang.Math;

public class PolarPoint {

	
	private double radius;
	private Degree angle;
	
	public PolarPoint(double radius, double angle) {
		super();
		this.radius = radius;
		this.angle = new Degree(angle);
	}
	
	public PolarPoint(double radius, int angle)
	{
		super();
		this.radius = radius;
		this.angle = new Degree(angle);
	}
	
	public PolarPoint(double radius, Degree angle)
	{
		super();
		this.radius = radius;
		this.angle = angle;
	}
	
	public static PolarPoint toPolar(DecartPoint point)
	{
		int x = point.getX();
		int y = point.getY();
		double ang = 0.0;
		if (point.getY() < 0 )
		  ang = Math.atan2(y, x) + 2* Math.PI;
		else
			ang = Math.atan2(y, x);
		return new PolarPoint(Math.sqrt(x * x + y * y), ang);
	}

	public PolarPoint(PolarPoint point)
	{
		this.angle = point.getAngle();
		this.radius = point.getRadius();
	}
	public double getRadius() {
		return radius;
	}

	public Degree getAngle() {
		return angle;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	public void setAngle(Degree angle) {
		this.angle = angle;
	}
	
	public void setAngle(int angle)
	{
		this.angle = new Degree(angle);
	}
	
	public void setAngle(double angle)
	{
		this.angle = new Degree(angle);
	}
}


package com.example.focus.circularPB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Sector {

	// 4 points to create sector
	private PolarPoint innerStart;
	private PolarPoint outerStart;
	private PolarPoint innerEnd;
	private PolarPoint outerEnd;

	// 4 decartPoint
	private DecartPoint inStart;
	private DecartPoint outStart;
	private DecartPoint inEnd;
	private DecartPoint outEnd;

	// current Decart Point
	private DecartPoint curDecart = null;

	// 2 Point for TopLeft and BottomRight points
	private DecartPoint topLeft = null;
	private DecartPoint bottomRight = null;

	private DecartPoint right;

	private int radius;
	private int inner_radius;

	private double koef1;
	private double koef2;

	public Sector(double radius, double inner_radius, int startAngle, int endAngle, int centerX, int centerY) {
		this(radius, inner_radius, startAngle, endAngle, new DecartPoint(centerX, centerY));
	}

	public Sector(double radius, double inner_radius, int startAngle, int endAngle, DecartPoint cent) {
		innerStart = new PolarPoint(inner_radius, startAngle);
		outerStart = new PolarPoint(radius, startAngle);
		innerEnd = new PolarPoint(inner_radius, endAngle);
		outerEnd = new PolarPoint(radius, endAngle);
		this.radius = (int) radius;
		this.inner_radius = (int) inner_radius;
		convertAllPointsToDecart();
		makeEvaluation();
	}

	public Sector(int startOuterX, int startOuterY, int endInnerX, int endInnerY, int centerX, int centerY) {
		this(new DecartPoint(centerX, centerY), new DecartPoint(startOuterX, startOuterY), new DecartPoint(endInnerX, endInnerY));
	}

	public Sector(DecartPoint cent, DecartPoint outerPoint, DecartPoint innerPoint) {
		outerStart = PolarPoint.toPolar(outerPoint);
		innerEnd = PolarPoint.toPolar(innerPoint);
		outerEnd = new PolarPoint(outerStart.getRadius(), innerEnd.getAngle());
		innerStart = new PolarPoint(innerEnd.getRadius(), outerStart.getAngle());
		radius = (int) evaluateDistanceToCenter(outerPoint);
		inner_radius = (int) evaluateDistanceToCenter(innerPoint);
		convertAllPointsToDecart();
		makeEvaluation();
	}

	private void makeEvaluation() {
		if (inStart == null)
			throw new NullPointerException("StartPoint is null. Make sure you initialize it.");
		if (inEnd == null)
			throw new NullPointerException("EndPoint is null. Make sure you initialize it.");
		if (inner_radius < 0)
			throw new IndexOutOfBoundsException("Inner radius is less than zero");
		if (radius < 0)
			throw new IndexOutOfBoundsException("Radius radius is less than zero");
		koef1 = evaluateKoef(inStart);
		koef2 = evaluateKoef(inEnd);
	}

	protected double evaluateKoef(DecartPoint point) {
		return (Math.abs((double)point.getX() / (double)point.getY()));
	}

	private void convertAllPointsToDecart() {
		this.inStart = DecartPoint.toDecart(innerStart);
		this.outStart = DecartPoint.toDecart(outerStart);
		this.inEnd = DecartPoint.toDecart(innerEnd);
		this.outEnd = DecartPoint.toDecart(outerEnd);
	}

	public Iterable<DecartPoint> getPixels() {

		ArrayList<DecartPoint> list = new ArrayList<DecartPoint>();
		if (curDecart == null) {
			curDecart = new DecartPoint(getTopLeft());
			// curDecart = getLeftPoint();
		}

		while (curDecart.getX() <= bottomRight.getX() && curDecart.getY() <= bottomRight.getY()) {
			if (isInSector(curDecart)) {
				list.add(new DecartPoint(curDecart));
			}	
			if (curDecart.getY() >= bottomRight.getY()) {
				curDecart.setX(curDecart.getX() + 1);
				curDecart.setY(topLeft.getY());
			}
			else
				 curDecart.setY(curDecart.getY() + 1);
		}
		return list;
	}

	// private double evaluateDistanceToCenter(int x, int y) {
	// return Math.sqrt(x * x + y * y);
	// }
	private double evaluateDistanceToCenter(DecartPoint point) {
		return Math.sqrt(point.getX() * point.getX() + point.getY() * point.getY());
	}

	public boolean isInSector(DecartPoint point) {
		double koef = evaluateKoef(point);
		// Check is point between two lines
		if (!inRange(koef, koef1, koef2)) {
			return false;
		}
		double distance = evaluateDistanceToCenter(point);
		if (!inRange(distance, (double) inner_radius, (double) radius)) {
			return false;
		}
		// point is in Sector
		return true;
	}

	private boolean inRange(double koef, double min, double max) {

		if (max < min) {
			// swap max and min
			double temp = max;
			max = min;
			min = temp;
		}
		if (koef > min && koef < max)
			return true;
		return false;
	}

	public DecartPoint getLeftPoint() {
		DecartPoint left;
		// comparing with only 3 points, because the first set up as default
		DecartPoint[] arr = new DecartPoint[3];
		arr[0] = inStart;
		arr[1] = inEnd;
		arr[2] = outStart;
		left = outEnd;
		for (int i = 0; i < arr.length; i++)
			if (arr[i].getX() < left.getX()) {
				left = arr[i];
			}
		return left;
	}

	public DecartPoint getRightPoint() {
		if (right == null) {
			// comparing with only 3 points, because the first set up as default
			DecartPoint[] arr = new DecartPoint[3];
			arr[0] = inStart;
			arr[1] = inEnd;
			arr[2] = outStart;
			right = outEnd;
			for (int i = 0; i < arr.length; i++)
				if (arr[i].getX() < right.getX()) {
					right = arr[i];
				}
		}
		return right;
	}

	public DecartPoint getTopLeft() {
		if (topLeft == null) {
			int[] arr = new int[4];
			arr[0] = DecartPoint.toDecart(innerEnd).getX();
			arr[1] = DecartPoint.toDecart(innerStart).getX();
			arr[2] = DecartPoint.toDecart(outerEnd).getX();
			arr[3] = DecartPoint.toDecart(outerStart).getX();
			Arrays.sort(arr);
			int minX = arr[0];
			arr[0] = DecartPoint.toDecart(innerEnd).getY();
			arr[1] = DecartPoint.toDecart(innerStart).getY();
			arr[2] = DecartPoint.toDecart(outerEnd).getY();
			arr[3] = DecartPoint.toDecart(outerStart).getY();
			Arrays.sort(arr);
			int minY = arr[0];
			topLeft = new DecartPoint(minX, minY);
		}
		return topLeft;
	}

	public DecartPoint getBottomRight() {
		if (bottomRight == null) {
			int[] arr = new int[4];
			arr[0] = DecartPoint.toDecart(innerEnd).getX();
			arr[1] = DecartPoint.toDecart(innerStart).getX();
			arr[2] = DecartPoint.toDecart(outerEnd).getX();
			arr[3] = DecartPoint.toDecart(outerStart).getX();
			Arrays.sort(arr);
			int maxX = arr[arr.length - 1];
			arr[0] = DecartPoint.toDecart(innerEnd).getY();
			arr[1] = DecartPoint.toDecart(innerStart).getY();
			arr[2] = DecartPoint.toDecart(outerEnd).getY();
			arr[3] = DecartPoint.toDecart(outerStart).getY();
			Arrays.sort(arr);
			int maxY = arr[arr.length - 1];
			bottomRight = new DecartPoint(maxX, maxY);
		}
		return bottomRight;
	}

}

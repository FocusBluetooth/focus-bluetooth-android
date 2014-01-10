
package com.example.focus.circularPB;

import android.graphics.Bitmap;
import android.util.Log;

public class CircularProgressBar {

	private int centerX;
	private int centerY;

	private int radius;
	private int inner_radius;

	private Bitmap bitmap;

	public CircularProgressBar(Bitmap bitmap, int centerX, int centerY, int radius, int inner_radius) {
		super();
		this.centerX = centerX;
		this.centerY = centerY;
		this.radius = radius;
		this.inner_radius = inner_radius;
		this.bitmap = bitmap;
	}

	public Bitmap getBitmap() {
		return bitmap;
	}

	public void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
	}

	public DecartPoint toBitmapPoint(int x, int y) {
		if (centerX > 0 && centerY > 0)
			return new DecartPoint(x + centerX, y + centerY);
		else if (centerX > 0)
			return new DecartPoint(x + centerX, y);
		else if (centerY > 0)
			return new DecartPoint(x, y + centerY);
		return null;
	}

	public DecartPoint toBitmapPoint(DecartPoint p) {
		int x = p.getX();
		int y = p.getY();
		DecartPoint point = new DecartPoint(x, y);
		if (centerX > 0 && centerY > 0) {
			point.setX(x + centerX);
			point.setY(y + centerY);
		} else if (centerX > 0)
			point.setX(x + centerX);

		else if (centerY > 0)
			point.setY(y + centerY);
		return point;
	}

	public DecartPoint toBitmapPoint(DecartPoint p, int startX, int startY) {
		int x = p.getX();
		int y = p.getY();
		DecartPoint point = new DecartPoint(x, y);
		if (centerX > 0 && centerY > 0) {
			point.setX(x + centerX);
			point.setY(y + centerY);
		} else if (centerX > 0)
			point.setX(x + centerX);

		else if (centerY > 0)
			point.setY(y + centerY);
		return point;
	}

	public DecartPoint fromBitmapPoint(int x, int y) {
		if (x > 0)
			x = x - centerX;
		if (y > 0)
			y = y - centerY;
		return new DecartPoint(x, y);
	}

	public void disappearSector(int startAngle, int endAngle, int opacity) {
		if (startAngle > endAngle) {
			int temp = startAngle;
			startAngle = endAngle;
			endAngle = temp;
		}
		Sector sector = new Sector((double) radius, (double) inner_radius, startAngle, endAngle, centerX, centerY);
		// PolarPoint cur;
		DecartPoint cur;
		DecartPoint topLeft = sector.getTopLeft();
		DecartPoint bottomRight = sector.getBottomRight();
		int width = topLeft.getWidth(bottomRight);
		int height = topLeft.getHeight(bottomRight);
		int[] pixels = new int[width * height];
		DecartPoint p = toBitmapPoint(topLeft);
		bitmap.getPixels(pixels, 0, width, toBitmapPoint(topLeft).getX(), toBitmapPoint(topLeft).getY(), width, height);
		ArrayBitmap arr = new ArrayBitmap(pixels, topLeft, bottomRight);

		// Works !!!
		// for (int i = 0; i < width; i++)
		// for (int j = 0; j < height; j++) {
		// int color = arr.getPixel(i, j);
		// if (color != 0) {
		// int opaque = (color & 0xFF000000);
		// if (opaque != 0) {
		// arr.setPixel(i, j, color & 0x00FFFFFF);
		// }
		// }
		// }
		Iterable<DecartPoint> list = sector.getPixels();
		for (DecartPoint point : list) {
			try {

				int color = arr.getPixel(point);
				if (color != 0) {
					int opaque = (color & 0xFF000000);
					if (opaque != 0) {
						arr.setPixel(point, color & 0x00FFFFFF);
					}
				}
			} catch (IllegalArgumentException ex) {
				Log.e("Error", ex.getMessage());
			}
		}
		bitmap.setPixels(arr.getPixels(), 0, width, toBitmapPoint(topLeft).getX(), toBitmapPoint(topLeft).getY(), width, height);
	}
}


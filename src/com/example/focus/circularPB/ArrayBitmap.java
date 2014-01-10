
package com.example.focus.circularPB;

public class ArrayBitmap {

	int[] pixels;
	int width;
	int height;
	DecartPoint topLeft;
	DecartPoint bottomRight;

	public ArrayBitmap(int[] pixels, DecartPoint topLeft, DecartPoint bottomRight) {
		super();
		this.pixels = pixels;
		this.topLeft = topLeft;
		this.bottomRight = bottomRight;
		width = topLeft.getWidth(bottomRight);
		height = topLeft.getHeight(bottomRight);
	}

	public int[] getPixels() {
		return pixels;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public DecartPoint getTopLeft() {
		return topLeft;
	}

	public DecartPoint getBottomRight() {
		return bottomRight;
	}

	public int getOffset(DecartPoint p) {
		DecartPoint point = new DecartPoint(p.getX() - topLeft.getX(), p.getY() - topLeft.getY());
		if (point.getY() == 0)
			return point.getX();
		int offset = (point.getY() - 1) * width + point.getX();
		return offset;
	}

	public int getPixel(DecartPoint p) throws IndexOutOfBoundsException {
		int offset = getOffset(p);
		if (offset < (width * height) && offset >= 0)
			return pixels[offset];
		else
			throw new IndexOutOfBoundsException("Index out of Range in int [] pixels");
	}

	public int getPixel(int x, int y) throws IndexOutOfBoundsException {
		int offset;
		if (y == 0)
			offset = x;
		else
			offset = (y - 1) * width + x;
		if (offset < (width * height) && offset >= 0)
			return pixels[offset];
		else
			throw new IndexOutOfBoundsException("Index out of Range in int [] pixels");
	}

	public void setPixel(DecartPoint p, int color) throws IndexOutOfBoundsException {
		int offset = getOffset(p);
		if (offset < (width * height))
			pixels[offset] = color;
		else
			throw new IndexOutOfBoundsException("Index out of Range in int [] pixels");
	}

	public void setPixel(int x, int y, int color) throws IndexOutOfBoundsException {
		int offset;
		if (y == 0)
			offset = x;
		else
			offset = (y - 1) * width + x;
		if (offset < (width * height) && offset >= 0)
			pixels[offset] = color;
		else
			throw new IndexOutOfBoundsException("Index out of Range in int [] pixels");
	}
}

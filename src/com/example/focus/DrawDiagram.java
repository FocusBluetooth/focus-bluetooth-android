package com.example.focus;

import java.util.Random;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class DrawDiagram extends SurfaceView implements SurfaceHolder.Callback {

	
	public static final int MAX_HEIGHT = 255;
	private DrawThread drawThread;
	private DrawDiagram diagram;

	private SurfaceHolder surfaceHolder;

	private float yBottom;
	private float deltaX;

	private float current;

	private Paint paint;

	private int[] mas;

	private int value;

	public DrawDiagram(Context context) {
		super(context);

		getHolder().addCallback(this);

		diagram = this;
	}

	public DrawDiagram(Context context, AttributeSet attrs) {
		super(context, attrs);

		getHolder().addCallback(this);

		diagram = this;
	}

	public DrawDiagram(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		getHolder().addCallback(this);

		diagram = this;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {

		paint = new Paint();

		mas = new int[diagram.getWidth()];

		drawThread = new DrawThread(getHolder(), getResources());
		// drawThread.setRunning(true);
		drawThread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

		boolean retry = true;

		// drawThread.setRunning(false);

		while (retry) {
			try {
				drawThread.join();
				retry = false;
			} catch (InterruptedException e) {
			}
		}
	}

	class DrawThread extends Thread {

		// private final long time = 200;
		//
		// private boolean runFlag = false;
		// private SurfaceHolder surfaceHolder;
		//
		//
		// private float yBottom;
		// private float deltaX;
		//
		// private float current;
		//
		// private Paint paint;
		//
		// private Random r;
		// private int[] mas;

		public DrawThread(SurfaceHolder surfaceHolder, Resources resources) {
			diagram.surfaceHolder = surfaceHolder;

			init();
		}

		private void init() {

			// paint = new Paint();
			//
			// r = new Random();
			//
			// mas = new int[diagram.getWidth()];
		}

		// public void setRunning(boolean run) {
		// runFlag = run;
		// }

		@Override
		public void run() {
			Random rand = new Random();
			Canvas canvas;

			// while (runFlag) {

			canvas = null;

			try {
				// Thread.sleep(time);

				if (mas.length != diagram.getWidth()) {

					changeArray();
				}

				addElement();

				yBottom = diagram.getY() + diagram.getHeight();

				deltaX = diagram.getX();

				canvas = surfaceHolder.lockCanvas(null);

				for (int i = 0; i < mas.length; i++) {

					// int value1 = rand.nextInt(120);
					// current = yBottom - value1;// * diagram.getHeight() /
					// yBottom;
					current = yBottom - mas[i] * diagram.getHeight() / MAX_HEIGHT;
					// paint.setShader(new LinearGradient(deltaX, yBottom,
					// deltaX, current, Color.CYAN, Color.BLACK,
					// TileMode.CLAMP));

					paint.setShader(new LinearGradient(deltaX, yBottom, deltaX,
							current, Color.BLACK, Color.BLACK, TileMode.CLAMP));

					synchronized (surfaceHolder) {

						// canvas.drawLine(deltaX, yBottom, deltaX, current,
						// paint);
						canvas.drawLine(deltaX, yBottom, deltaX, 0, paint);
					}

					paint.setShader(new LinearGradient(deltaX, yBottom, deltaX,
							current, Color.CYAN, Color.BLACK, TileMode.CLAMP));

					synchronized (surfaceHolder) {

						// canvas.drawLine(deltaX, yBottom, deltaX, current,
						// paint);
						canvas.drawLine(deltaX, yBottom, deltaX, current, paint);
					}

					deltaX++;
				}
				// } catch (InterruptedException e) {
				// e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (canvas != null) {
					surfaceHolder.unlockCanvasAndPost(canvas);
				}
			}
			// }
		}

		private void addElement() {

			for (int i = mas.length - 1; i > 0; i--) {

				mas[i] = mas[i - 1];
			}

			// mas [0] = r.nextInt(201);

			mas[0] = value;
		}

		private void changeArray() {

			int[] temp = new int[diagram.getWidth()];

			int length = 0;

			if (temp.length < mas.length) {

				length = temp.length;
			} else {

				length = mas.length;
			}

			for (int i = 0; i < length; i++) {

				temp[i] = mas[i];
			}

			mas = temp;
		}
	}

	public void drawLine(short value) {

		diagram.value = value;

		drawThread.run();
	}
}

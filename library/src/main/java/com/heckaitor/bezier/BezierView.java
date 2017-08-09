package com.heckaitor.bezier;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.Xfermode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * 在平面上，通过给定的一组点，绘制Bezier曲线
 *
 * Created by heckaitor on 16/8/18.
 */
public class BezierView extends View {

	private float[] values; // 原始数值
	private float progress; // 绘制进度

	private Paint paint;
	private Path path; // bezier路径
	private Shader lineShader; // 曲线渐变
	private Shader areaShader; // 曲线下区域的渐变
	private Xfermode coverXfermode; // 遮罩蒙层

	private int lineStrokeWidth; // 折线宽度

	private boolean drawValuePoints;
	private boolean drawControlPoints;

	public BezierView(Context context) {
		super(context);
		init();
	}

	public BezierView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	/**
	 * 初始化
	 */
	private void init() {
		path = new Path();
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		coverXfermode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
		lineStrokeWidth = 8;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		final int height = getMeasuredHeight();
		final int pl = getPaddingLeft();
		final int pt = getPaddingTop();
		final int pb = getPaddingBottom();

		// 计算渐变
		areaShader = new LinearGradient(pl, pt, pl, height - pb,
				Color.argb(38, 253, 93, 85), // #26FD5D55
				Color.TRANSPARENT, // 透明
				Shader.TileMode.CLAMP);

		lineShader = new LinearGradient(pl, pt, pl, height - pb,
				Color.rgb(255, 58, 41), // #FF3A29
				Color.rgb(255, 176, 122), // #FFB07A
				Shader.TileMode.CLAMP);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		final long anchor = System.nanoTime();

		final PointF[] knots = buildKnots(values); // 数值点
		// bezier曲线至少需要两个点
		if (knots == null || knots.length < 1) {
			return ;
		}

		final int n = knots.length - 1;
		PointF[] firstControlPoints = new PointF[n]; // 左控制点
		PointF[] secondControlPoints = new PointF[n]; // 右控制点

		// 计算bezier曲线的控制点
		BezierSpline.getCurveControlPoints(knots, firstControlPoints, secondControlPoints);

		float maxY, minY; // Y轴显示区域[minY, maxY], 包括所有点
		maxY = knots[0].y;
		minY = knots[0].y;
		for (int i = 1; i <= knots.length - 1; i++) {
			final PointF knot = knots[i];
			final PointF firstControlPoint = firstControlPoints[i - 1];
			final PointF secondControlPoint = secondControlPoints[i - 1];

			maxY = Math.max(maxY, knot.y);
			maxY = Math.max(maxY, firstControlPoint.y);
			maxY = Math.max(maxY, secondControlPoint.y);

			minY = Math.min(minY, knot.y);
			minY = Math.min(minY, firstControlPoint.y);
			minY = Math.min(minY, secondControlPoint.y);
		}

		// 绘制时，将平面坐标系转换为View坐标系
		for (int i = 0; i <= n; i++) {
			float x = transferX(knots[i].x);
			float y = transferY(knots[i].y, minY, maxY);
			knots[i].set(x, y);

			if (i < n) {
				x = transferX(firstControlPoints[i].x);
				y = transferY(firstControlPoints[i].y, minY, maxY);
				firstControlPoints[i].set(x, y);

				x = transferX(secondControlPoints[i].x);
				y = transferY(secondControlPoints[i].y, minY, maxY);
				secondControlPoints[i].set(x, y);
			}
		}

		final int layerId = canvas.saveLayer(0, 0, getWidth(), getHeight(), null, Canvas.ALL_SAVE_FLAG);

		// bezier曲线
		path.reset();
		path.moveTo(knots[0].x, knots[0].y);
		for (int i = 0; i < knots.length - 1; i++) {
			path.cubicTo(firstControlPoints[i].x, firstControlPoints[i].y,
					secondControlPoints[i].x, secondControlPoints[i].y,
					knots[i+1].x, knots[i+1].y);
		}

		paint.setColor(Color.rgb(255, 58, 41)); // #FF3A29;
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(lineStrokeWidth);
		paint.setShader(lineShader);
		canvas.drawPath(path, paint);

		// 曲线下的渐变区域
		path.lineTo(knots[knots.length - 1].x, getHeight());
		path.lineTo(knots[0].x, getHeight());
		path.close();

		paint.setStyle(Paint.Style.FILL);
		paint.setShader(areaShader);
		canvas.drawPath(path, paint);

		// 蒙层
		paint.setXfermode(coverXfermode);
		paint.setShader(null);
		canvas.drawRect(progress * getWidth(), 0, getWidth(), getHeight(), paint);
		paint.setXfermode(null);

		canvas.restoreToCount(layerId);

		if (drawValuePoints) {
			paint.setColor(Color.BLACK);
			drawPoints(knots, canvas, paint);
		}

		if (drawControlPoints) {
			paint.setColor(Color.GREEN);
			drawPoints(firstControlPoints, canvas, paint);
			paint.setColor(Color.BLUE);
			drawPoints(secondControlPoints, canvas, paint);
		}

		Log.d("bezier", "onDraw: " + (System.nanoTime() - anchor)/1000000 + "ms");
	}

	/**
	 * x坐标转换: 数值坐标 -> view坐标
	 * @param x
	 * @return
	 */
	private float transferX(float x) {
		final int w = getWidth();
		final int pl = getPaddingLeft();
		final int pr = getPaddingRight();
		return pl + x * (w - pl - pr);
	}

	/**
	 *  y坐标转换: 数值坐标 -> view坐标
	 * @param y
	 * @param minY
	 * @param maxY
	 * @return
	 */
	private float transferY(float y, float minY, float maxY) {
		final int h = getHeight();
		final int pt = getPaddingTop();
		final int pb = getPaddingBottom();
		return h - pb + (y - minY) * (pt + pb - h) / (maxY - minY);
	}

	/**
	 * 设置数值点
	 * @param values
	 */
	public void setValues(float[] values) {
		this.values = values;

		ObjectAnimator animator = ObjectAnimator.ofFloat(this, "progress", 0, 1);
		animator.setDuration(1000);
		animator.setInterpolator(new AccelerateDecelerateInterpolator());
		animator.start();
	}

	public float[] getValues() {
		return this.values;
	}

	/**
	 * 对给定的数值点，生成对应点的坐标，以平面坐标系为准（左下角为原点）
	 * <ul>
	 *     <li>所有数值点在X轴平均分布：[0, 1]</li>
	 *     <li>Y轴显示区域为数值点的最值[min, max]</li>
	 * </ul>
	 * @param values
	 * @return
	 */
	private PointF[] buildKnots(float[] values) {
		if (values == null) {
			return null;
		}

		final int LEN = values.length;
		if (LEN < 1) {
			return null;
		}

		// 计算数值点坐标
		PointF[] knots = new PointF[LEN];
		final float xOffset = 1.0f / (LEN - 1);
		for (int i = 0; i < LEN; i++) {
			knots[i] = new PointF(i * xOffset, values[i]);
		}

		return knots;
	}

	private void drawPoints(PointF[] points, Canvas canvas, Paint paint) {
		if (points == null || canvas == null || paint == null) {
			return ;
		}

		for (PointF point: points) {
			canvas.drawCircle(point.x, point.y, 10, paint);
		}
	}

	public float getProgress() {
		return progress;
	}

	public void setProgress(float progress) {
		this.progress = progress;
		invalidate();
	}

	public void setDrawValuePoints(boolean drawValuePoints) {
		this.drawValuePoints = drawValuePoints;
	}

	public void setDrawControlPoints(boolean drawControlPoints) {
		this.drawControlPoints = drawControlPoints;
	}
}

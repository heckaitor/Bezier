package com.heckaitor.bezier;

import android.graphics.PointF;

/**
 * Bezier Spline methods
 *
 * @see <a href="http://www.codeproject.com/Articles/31859/Draw-a-Smooth-Curve-through-a-Set-of-D-Points-wit">
 *     Draw a Smooth Curve through a Set of 2D Points with Bezier Primitives</a>
 */
public class BezierSpline {

	/**
	 * Get open-ended Bezier Spline Control Points.
	 * @param knots Knot Bezier spline points, 2 at least.
	 * @param firstControlPoints First Control points, array of knots.Length - 1 length.
	 * @param secondControlPoints Second Control points, array of knots.Length - 1 length.
	 * @throws NullPointerException Original Knots is null
	 * @throws IllegalArgumentException The length of Controls Points should be knots.length - 1 
	 */
	public static void getCurveControlPoints(PointF[] knots, PointF[] firstControlPoints, PointF[] secondControlPoints)
			throws NullPointerException, IllegalArgumentException {
		if (knots == null) {
			throw new NullPointerException("knots is NULL");
		}
		
		int n = knots.length - 1;
		if (n < 1) {
			throw new IllegalArgumentException("At least two knot points required");
		}
		
		if (firstControlPoints == null || firstControlPoints.length != n 
				|| secondControlPoints == null || secondControlPoints.length != n) {
			throw new IllegalArgumentException("Control points' length should be " + n);
		}
		
		if (n == 1) { 
			// Special case: Bezier curve should be a straight line.

			// 3P1 = 2P0 + P3
			firstControlPoints[0] = new PointF();
			firstControlPoints[0].x = (2 * knots[0].x + knots[1].x) / 3;
			firstControlPoints[0].y = (2 * knots[0].y + knots[1].y) / 3;

			// P2 = 2P1 – P0
			secondControlPoints[0] = new PointF();
			secondControlPoints[0].x = 2 * firstControlPoints[0].x - knots[0].x;
			secondControlPoints[0].y = 2 * firstControlPoints[0].y - knots[0].y;

		} else {
			// Calculate first Bezier control points
			// Right hand side vector
			float[] rhs = new float[n];

			// Set right hand side X values
			for (int i = 1; i < n - 1; ++i) {
				rhs[i] = 4 * knots[i].x + 2 * knots[i + 1].x;
			}
			rhs[0] = knots[0].x + 2 * knots[1].x;
			rhs[n - 1] = (8 * knots[n - 1].x + knots[n].x) / 2.0f;

			// Get first control points X-values
			float[] x = getFirstControlPoints(rhs);

			// Set right hand side Y values
			for (int i = 1; i < n - 1; ++i) {
				rhs[i] = 4 * knots[i].y + 2 * knots[i + 1].y;
			}
			rhs[0] = knots[0].y + 2 * knots[1].y;
			rhs[n - 1] = (8 * knots[n - 1].y + knots[n].y) / 2.0f;

			// Get first control points Y-values
			float[] y = getFirstControlPoints(rhs);

			// Fill output arrays.
			for (int i = 0; i < n; ++i) {
				// First control point
				firstControlPoints[i] = new PointF(x[i], y[i]);

				// Second control point
				if (i < n - 1) {
					secondControlPoints[i] = new PointF(2 * knots[i + 1].x - x[i + 1],
							2 * knots[i + 1].y - y[i + 1]);
				} else {
					secondControlPoints[i] = new PointF((knots[n].x + x[n - 1]) / 2,
							(knots[n].y + y[n - 1]) / 2);
				}
			}
		}
	}

	/**
	 * Solves a tridiagonal system for one of coordinates (x or y) of first Bezier control points.
	 * @param rhs Right hand side vector.
	 * @return Solution vector.
	 */
	private static float[] getFirstControlPoints(float[] rhs) {
		int n = rhs.length;
		float[] x = new float[n]; // Solution vector.
		float[] tmp = new float[n]; // Temp workspace.

		float b = 2.0f;
		x[0] = rhs[0] / b;

		// Decomposition and forward substitution.
		for (int i = 1; i < n; i++) {
			tmp[i] = 1 / b;
			b = (i < n - 1 ? 4.0f : 3.5f) - tmp[i];
			x[i] = (rhs[i] - x[i - 1]) / b;
		}

		for (int i = 1; i < n; i++) {
			x[n - i - 1] -= tmp[n - i] * x[n - i]; // Back substitution.
		}

		return x;
	}

}
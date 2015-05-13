package logic.clustering;

public class Transformation {
	private static final double A = 0.15915494309189534;
	private static final double B = 0.5;
	private static final double C = -0.15915494309189534;
	private static final double D = 0.5;
	
	public static Point transform(Point point, int scale) {
		point.x = scale * (A * point.x + B);
		point.y = scale * (C * point.y + D);
		return point;
	}

}

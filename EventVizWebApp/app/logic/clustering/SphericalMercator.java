package logic.clustering;

public class SphericalMercator {
	private static final double MAX_LATITUDE = 85.0511287798;
	private static final double DEG_TO_RAD = 0.017453292519943295;
	
	// *********************************************************************
	// taken from javascript-function: project (leaflet-src.js)
	public static Point project(ILocation latlng)
	{
		// TODO optimize me
		double  d = DEG_TO_RAD;
		double max = MAX_LATITUDE;
		double lat = Math.max(Math.min(max, latlng.getLatitude()), -max);
		double x = latlng.getLongitude() * d;
		double y = lat * d;
		y = Math.log(Math.tan((Math.PI / 4) + (y / 2)));

		return new Point(x, y);
	}

}

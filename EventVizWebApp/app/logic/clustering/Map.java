package logic.clustering;

public class Map {	
	// *********************************************************************
	// taken from javascript-function: project (leaflet-src.js)
	public static Point project(ILocation latlng, int zoom) {
		return latLngToPoint(latlng, zoom);
	}

	private static Point latLngToPoint(ILocation latlng, int zoom) {
		Point projectedPoint = SphericalMercator.project(latlng);
		int scale = scale(zoom);
		return Transformation.transform(projectedPoint, scale);
	}

	private static int scale(int zoom) {
		// TODO optimize me (hold precalculated values in array)
		return (int) (256 * Math.pow(2, zoom));
	}

}

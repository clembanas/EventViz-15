package logic.clustering;

public class Location implements ILocation{
	
	private double lat;
	private double lng;

	public Location(double lat, double lng) {
		this.lat = lat;
		this.lng = lng;
	}

	@Override
	public double getLatitude() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public void setLng(double lng) {
		this.lng = lng;
	}

	@Override
	public double getLongitude() {
		return lng;
	}
}

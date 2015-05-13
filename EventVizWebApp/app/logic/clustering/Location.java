package logic.clustering;

public class Location implements ILocation{
	
	private String id;
	private double lat;
	private double lng;

	public Location(String id, double lat, double lng) {
		this.id = id;
		this.lat = lat;
		this.lng = lng;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

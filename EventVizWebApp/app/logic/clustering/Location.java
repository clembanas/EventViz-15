package logic.clustering;

import java.io.Serializable;

public class Location implements ILocation, Serializable{
	private static final long serialVersionUID = 4486058443709461482L;
	private long id;
	private double lat;
	private double lng;
	private String name;
	

	public Location()
	{
		// just for serialization		
	}

	public Location(long id, double lat, double lng, String name) {
		this.id = id;
		this.lat = lat;
		this.lng = lng;
		this.name = name;
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
}

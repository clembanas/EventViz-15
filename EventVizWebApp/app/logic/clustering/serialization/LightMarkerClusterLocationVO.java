package logic.clustering.serialization;

import logic.clustering.ILocation;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class LightMarkerClusterLocationVO {

	private final ILocation realLocation;

	public LightMarkerClusterLocationVO(ILocation location) {		
		this.realLocation = location;
	}
	
	public double getLat()
	{
		return realLocation.getLatitude();
	}
	
	public double getLng()
	{
		return realLocation.getLongitude();
	}
	
	@JsonIgnore 
	public String getName()
	{
		return realLocation.getName();
	}
}

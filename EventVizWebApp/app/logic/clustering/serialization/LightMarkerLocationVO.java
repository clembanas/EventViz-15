package logic.clustering.serialization;

import java.util.List;

import logic.clustering.ILocation;
import logic.clustering.Marker;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class LightMarkerLocationVO {

	private final Marker marker;

	public LightMarkerLocationVO(Marker marker) {		
		this.marker = marker;
	}
	
	public List<Long> getIds()
	{
		return marker.getIds();
	}
	
	public double getLat()
	{
		return marker.getLatitude();
	}
	
	public double getLng()
	{
		return marker.getLongitude();
	}
	
	@JsonIgnore 
	public String getName()
	{
		return marker.getName();
	}
}

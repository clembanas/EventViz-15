package logic.clustering.serialization;

import logic.clustering.ILocation;

public class LightLocationVO {

	private final ILocation realLocation;

	public LightLocationVO(ILocation location) {		
		this.realLocation = location;
	}
	
	public String getId(){
		return realLocation.getId();
	}
	
	public double getLat()
	{
		return realLocation.getLatitude();
	}
	
	public double getLng()
	{
		return realLocation.getLongitude();
	}

}

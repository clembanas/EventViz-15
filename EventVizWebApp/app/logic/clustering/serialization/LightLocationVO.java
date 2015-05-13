package logic.clustering.serialization;

import logic.clustering.ILocation;
import logic.clustering.MarkerCluster;

public class LightLocationVO {

	private final ILocation realLocation;

	public LightLocationVO(ILocation location) {
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

}

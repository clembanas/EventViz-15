package logic.clustering;

import java.io.Serializable;

public class LocationProxy implements ILocation, Serializable {
	private static final long serialVersionUID = 6182147865946165933L;
	private ILocation realSubject;
	
	// just for serialization
	public LocationProxy()
	{
		
	}
	
	public LocationProxy(ILocation realSubject)
	{
		this.realSubject = realSubject;
	}
	
	public String getId(){
		return realSubject.getId();
	}

	@Override
	public double getLatitude() {
		return realSubject.getLatitude();
	}

	@Override
	public double getLongitude() {
		return realSubject.getLongitude();
	}
	

}

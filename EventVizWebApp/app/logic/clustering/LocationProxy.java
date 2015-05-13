package logic.clustering;

public class LocationProxy implements ILocation {
	private final ILocation realSubject;
	
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

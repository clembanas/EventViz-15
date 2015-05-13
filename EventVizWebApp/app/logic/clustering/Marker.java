package logic.clustering;

public class Marker extends LocationProxy implements IMarker {
	
	private MarkerCluster parent = null;

	public Marker(ILocation realSubject) {
		super(realSubject);
	}

	@Override
	public void setParent(MarkerCluster parent) {
		this.parent = parent;
	}
	
	@Override
	public MarkerCluster getParent() {
		return this.parent;
	}

}

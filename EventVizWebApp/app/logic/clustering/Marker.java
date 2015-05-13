package logic.clustering;

public class Marker extends LocationProxy implements IMarker {
	
	private MarkerCluster parent = null;
	private String id = "";

	public Marker(ILocation realSubject) {
		super(realSubject);
		id = realSubject.getId();
	}

	@Override
	public void setParent(MarkerCluster parent) {
		this.parent = parent;
	}
	
	@Override
	public MarkerCluster getParent() {
		return this.parent;
	}

	@Override
	public String getId(){
		return this.id;
	}
}

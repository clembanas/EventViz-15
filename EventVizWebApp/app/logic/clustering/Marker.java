package logic.clustering;

import java.io.Serializable;

public class Marker extends LocationProxy implements IMarker, Serializable {
	private static final long serialVersionUID = -2795057827017912992L;
	private MarkerCluster parent = null;
	private String id = "";
	
	// just for serialization
	public Marker()
	{
		super(null);
	}

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

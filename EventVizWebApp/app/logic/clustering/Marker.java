package logic.clustering;

import java.io.Serializable;

public class Marker extends LocationProxy implements IMarker, Serializable {
	private static final long serialVersionUID = -2795057827017912992L;
	private MarkerCluster parent = null;
	private long id = -1;
	private String name = "";
	
	// just for serialization
	public Marker()
	{
		super(null);
	}

	public Marker(ILocation realSubject) {
		super(realSubject);
		id = realSubject.getId();
		name = realSubject.getName();
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
	public long getId(){
		return this.id;
	}
	
	public String getName(){
		return this.name;
	}
}

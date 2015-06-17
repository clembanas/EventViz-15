package logic.clustering;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Marker extends LocationProxy implements IMarker, Serializable {
	private static final long serialVersionUID = -2795057827017912993L;
	private MarkerCluster parent = null;
	private List<Long> ids = new ArrayList<Long>();
	


	// just for serialization
	public Marker()
	{
		super(null);
	}

	public Marker(ILocation realSubject) {
		super(realSubject);
		this.addId(realSubject.getId());
	}
	public List<Long> getIds() {
		return ids;
	}

	public void setIds(List<Long> ids) {
		this.ids = ids;
	}
	
	
	@Override
	public void setParent(MarkerCluster parent) {
		this.parent = parent;
	}
	
	@Override
	public MarkerCluster getParent() {
		return this.parent;
	}
	
	
	public void addId(long id) {
		this.ids.add(id);
	}
}

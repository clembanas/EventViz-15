package logic.clustering;

import java.util.List;

public interface IMarker extends ILocation {
	public void setParent(MarkerCluster marker);
	MarkerCluster getParent();
}

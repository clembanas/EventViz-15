package logic.clustering;


public interface IMarker extends ILocation {
	public void setParent(MarkerCluster marker);
	MarkerCluster getParent();
}

package logic.clustering;

public interface IMarker extends ILocation {
	public long getId();
	public void setParent(MarkerCluster marker);
	MarkerCluster getParent();
}

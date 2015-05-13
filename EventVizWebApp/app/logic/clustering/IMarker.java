package logic.clustering;

public interface IMarker extends ILocation {
	public String getId();
	public void setParent(MarkerCluster marker);
	MarkerCluster getParent();
}

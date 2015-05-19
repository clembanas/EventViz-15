package logic.clustering;

public interface ClusteringWorker {

	void addLocation(ILocation location);

	MarkerCluster waitForResult() throws Exception;

}

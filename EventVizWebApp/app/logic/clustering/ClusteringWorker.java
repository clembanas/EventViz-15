package logic.clustering;

public interface ClusteringWorker extends AutoCloseable {

	void addLocation(ILocation location);

	MarkerCluster waitForResult() throws Exception;

}

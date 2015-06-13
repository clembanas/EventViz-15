package logic.clustering;



public class ClusteringUtil {	
	
	private static ClusteringWorker[] createClusteringWorker()
	{
		ClusteringWorker[] workers = new ClusteringWorker[8];
		
		try {
			//workers[0] = new ClusteringNodeClient("localhost", 9999);
			workers[0] = new LocalClusteringWorker();
			workers[1] = new LocalClusteringWorker();
			workers[2] = new LocalClusteringWorker();
			workers[3] = new LocalClusteringWorker();
			workers[4] = new LocalClusteringWorker();
			workers[5] = new LocalClusteringWorker();
			workers[6] = new LocalClusteringWorker();
			workers[7] = new LocalClusteringWorker();
		} catch (Exception e) {
			throw new RuntimeException("Error creating clusteringworkers: '" + e.getMessage() + "'", e);
		}
		
		return workers;
	}
	public static MarkerCluster cluster(Iterable<? extends ILocation> events)
	{
		ClusteringWorker[] workers = createClusteringWorker();
		return cluster(events, workers);
	}
	
	public static MarkerCluster cluster(Iterable<? extends ILocation> events, ClusteringWorker[] workers)
	{		
		int workerCount = workers.length;
		int stripeSize = 360 / workerCount;
		for(ILocation location : events)
		{
			int workerId = ((int)(location.getLongitude() + 180) / stripeSize) % workers.length;
			workers[workerId].addLocation(location);
		}
		
		MarkerCluster resultTop = new MarkerCluster(-1);
		
		for(ClusteringWorker worker : workers)
		{
			MarkerCluster topCluster;
			try {
				topCluster = worker.waitForResult();
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
			
			resultTop.merge(topCluster);
			
			try {
				worker.close();
			} catch (Exception e) {
				// ignore, but print stacktrace
				e.printStackTrace();
			}
		}
		
		return resultTop;
		
		//MarkerClusterGroup group = new MarkerClusterGroup();
		// MarkerCluster topCluster = group.calculateTopClusterLevel(locations);
		
		//// take the first childCluster at zoomlevel 0 (topCluster was used just for calculation)
		
		//return topCluster;
		
		/*if(topCluster.getChildClusters().size() < 1)
		{
			return null;
		}
		
		MarkerCluster clusterAtLevel0 = topCluster.getChildClusters().get(0);
		return clusterAtLevel0;*/
	}
}



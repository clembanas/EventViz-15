package logic.clustering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import logic.clustering.networking.ClusteringNodeClient;
import play.Logger;



public class ClusteringUtil {	
	private static int localWorkers = -1;
	private static List<String> externalWorkers = new ArrayList<String>();
	private static int clusteringWorkerCount = -1;
	
	public static void initialize(List<String> externalWorkerNames, int localWorkerCount)
	{
		if(externalWorkerNames != null)
		{
			externalWorkers = externalWorkerNames;
		}
		
		
		localWorkers = localWorkerCount;
    	clusteringWorkerCount = externalWorkers.size() + localWorkers;
    	
    	Logger.info("clustering.externalWorkers: " + Arrays.toString(externalWorkers.toArray()));
      	Logger.info("clustering.localWorkers: " + localWorkers);
	}
	
	private static ClusteringWorker[] createClusteringWorker()
	{		
		ClusteringWorker[] workers = new ClusteringWorker[clusteringWorkerCount];
		
		try {
			
			int i = 0;
			for(; i < localWorkers;i++)
			{
				workers[i] = new LocalClusteringWorker();
			}
			
			for(String externalWorker : externalWorkers)
			{
				workers[i] = new ClusteringNodeClient(externalWorker, 9999);
				i++;
			}			
			
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



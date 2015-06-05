package logic.clustering;

import java.util.concurrent.ConcurrentLinkedQueue;

public class LocalClusteringWorker extends Thread implements ClusteringWorker {
	private final ConcurrentLinkedQueue<ILocation> queue = new ConcurrentLinkedQueue<ILocation>();
	private final AutoResetEvent queueAutoResetEvent = new AutoResetEvent(false);
	private final AutoResetEvent completedAutoResetEvent = new AutoResetEvent(false);
	
	private boolean receivingNewLocations = true;
	private final MarkerClusterGroup markerClusterGroup;
	private Exception exceptionWhileRunning = null;

	public LocalClusteringWorker() {
		markerClusterGroup = new MarkerClusterGroup();
		start();
	}

	public void run() {
		try {
			ILocation location = null;
			
			do
			{
				queueAutoResetEvent.waitOne();
				
				do
				{
					location = queue.poll();
					if(location != null)
					{
						markerClusterGroup.addLocation(location);
					}
				}while(location != null);
				
			}while(receivingNewLocations);
			
			completedAutoResetEvent.set();
		} catch (Exception e) {
			exceptionWhileRunning = e;
		}
	}

	@Override
	public void addLocation(ILocation location) {
		queue.add(location);
		queueAutoResetEvent.set();
	}

	@Override
	public MarkerCluster waitForResult() throws Exception {
		receivingNewLocations = false;
		queueAutoResetEvent.set();
		completedAutoResetEvent.waitOne();
		
		if(exceptionWhileRunning != null)
		{
			throw exceptionWhileRunning;
		}
		
		return markerClusterGroup.getTopLevelCluster();
	}

	@Override
	public void close() throws Exception {
		// nothing to do...
	}

}

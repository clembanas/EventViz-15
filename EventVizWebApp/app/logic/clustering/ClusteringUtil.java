package logic.clustering;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import logic.clustering.networking.ClusteringNodeClient;
import logic.clustering.serialization.LightMarkerClusterVO;
import play.libs.Json;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


public class ClusteringUtil {
	private static final int WORKER_COUNT = 2;
	private static final int STRIPE_SIZE = 360 / WORKER_COUNT;
	
	
	private static ClusteringWorker[] createClusteringWorker()
	{
		ClusteringWorker[] workers = new ClusteringWorker[WORKER_COUNT];
		
		try {
			//workers[0] = new ClusteringNodeClient("localhost", 9999);
			workers[0] = new LocalClusteringWorker();
			workers[1] = new LocalClusteringWorker();
		} catch (Exception e) {
			throw new RuntimeException("Error creating clusteringworkers: '" + e.getMessage() + "'", e);
		}
		
		return workers;
	}
	
	
	public static MarkerCluster cluster(Iterable<ILocation> locations)
	{
		ClusteringWorker[] workers = createClusteringWorker();
		
		for(ILocation location : locations)
		{
			int workerId = ((int)(location.getLongitude() + 180) / STRIPE_SIZE) % WORKER_COUNT;
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

	private static JsonNode cachedDefaultClusterJsonNode = null;
	public static JsonNode getDefaultClusterJsonNode() {
		if(cachedDefaultClusterJsonNode == null)
		{
			MarkerCluster markerCluster = getDefaultCluster();
			cachedDefaultClusterJsonNode = Json.toJson(new LightMarkerClusterVO(markerCluster));
		}
		
		return cachedDefaultClusterJsonNode;
	}
	
	private static JsonNode cachedEventJsonNode = null;
	public static JsonNode getEventJsonNode(JsonArray events_JSON) {
		if(cachedEventJsonNode == null)
		{
			List<ILocation> locations = new ArrayList<ILocation>();		
			
			for(int i = 0; i < events_JSON.size(); i++){
				JsonObject event = (JsonObject) events_JSON.get(i);
				locations.add(new Location(event.get("eventful_id").toString().replaceAll("\"", ""), Double.parseDouble(event.get("latitude").toString()), Double.parseDouble(event.get("longitude").toString())));
			}
			MarkerCluster markerCluster = ClusteringUtil.cluster(locations);
			cachedEventJsonNode = Json.toJson(new LightMarkerClusterVO(markerCluster));
		}
		
		return cachedEventJsonNode;
	}
	
	private static MarkerCluster getDefaultCluster() {
		/*List<ILocation> locations = new ArrayList<ILocation>();
		
		locations.add(new Location(38.2924721, -122.4565503));
		locations.add(new Location(45.473753, -122.6583744));
		locations.add(new Location(32.8673, -97.2486));
		
		/*locations.add(new Location(39.2428725, -94.6588208));
		locations.add(new Location(38.7208, -75.0764));
		locations.add(new Location(42.4, -7.06667));
		locations.add(new Location(35.7117761, -93.7971925));
		locations.add(new Location(36.1088677, -115.1537786));
		locations.add(new Location(38.915271, -77.021098));
		locations.add(new Location(38.2924721, -122.4565503));
		locations.add(new Location(45.473753, -122.6483744));*/
		//return ClusteringUtil.cluster(locations);*/
		
		List<ILocation> locations = new ArrayList<ILocation>();		
		
		java.io.File f = new java.io.File("./test/resources/ClusterPointsWithID10000.txt");		
		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		    	if(line.isEmpty())
		    	{
		    		continue;
		    	}
		    	String[] splitted = line.split(";");
		    	String id = splitted[0];
		    	String lat = splitted[1];
		    	String lng = splitted[2];
		    	locations.add(new Location(id, Double.parseDouble(lat), Double.parseDouble(lng)));
		    }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		return ClusteringUtil.cluster(locations);
	}
}

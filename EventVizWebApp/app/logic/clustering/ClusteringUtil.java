package logic.clustering;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import logic.clustering.serialization.LightMarkerClusterVO;
import play.Play;
import play.libs.Json;

import com.fasterxml.jackson.databind.JsonNode;


public class ClusteringUtil {
	public static MarkerCluster cluster(Iterable<ILocation> locations)
	{
		MarkerClusterGroup group = new MarkerClusterGroup(locations);
		MarkerCluster topCluster = group.getTopClusterLevel();
		// take the first childCluster at zoomlevel 0 (topCluster was used just for calculation)
		
		if(topCluster.getChildCount() < 1)
		{
			return null;
		}
		
		MarkerCluster clusterAtLevel0 = topCluster.getChildClusters().get(0);
		return clusterAtLevel0;
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

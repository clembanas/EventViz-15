package logic.clustering;

import java.util.ArrayList;
import java.util.List;

import logic.clustering.serialization.LightMarkerClusterVO;
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

	public static JsonNode getDefaultClusterJsonNode() {
		MarkerCluster markerCluster = getDefaultCluster();		
		return Json.toJson(new LightMarkerClusterVO(markerCluster));
	}

	private static MarkerCluster getDefaultCluster() {
		List<ILocation> locations = new ArrayList<ILocation>();
		
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
		return ClusteringUtil.cluster(locations);
	}
}

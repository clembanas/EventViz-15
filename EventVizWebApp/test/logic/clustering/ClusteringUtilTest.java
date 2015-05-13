/**
 * 
 */
package logic.clustering;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * @author marius
 *
 */
public class ClusteringUtilTest {
	
	@Test
	public void testCluster0Points() {
		List<ILocation> locations = new ArrayList<ILocation>();
		
		MarkerCluster clusterZoom0 = ClusteringUtil.cluster(locations);
		
		assertNull(clusterZoom0);
	}
	
	@Test
	public void testGetDefaultCluster() {		
		ClusteringUtil.getDefaultClusterJsonNode();
	}
	

	@Test
	public void testCluster2Points() {
		// public static MarkerClusterGroup Cluster(Iterable<ILocation> locations)
		
		List<ILocation> locations = new ArrayList<ILocation>();
		locations.add(new Location(47.9139476, 11.421561));
		locations.add(new Location(32.70788, 11.40115));
		
		MarkerCluster clusterZoom0 = ClusteringUtil.cluster(locations);
		assertEquals(0, clusterZoom0.getZoom());
		assertEquals(2, clusterZoom0.getChildCount());
		assertEquals(40.3109, clusterZoom0.getLatitude(),  0.01);
		assertEquals(11.4113, clusterZoom0.getLongitude(), 0.01);
		assertEquals(0, clusterZoom0.getMarkers().size());
		
		MarkerCluster clusterZoom1 = clusterZoom0.getChildClusters().get(0);
		assertEquals(1, clusterZoom1.getZoom());
		assertEquals(2, clusterZoom1.getChildCount());
		assertEquals(40.3109, clusterZoom1.getLatitude(),  0.01);
		assertEquals(11.4113, clusterZoom1.getLongitude(), 0.01);
		assertEquals(2, clusterZoom1.getMarkers().size());
		
		
		
		List<ILocation> expectedMarkers = new ArrayList<ILocation>();
		expectedMarkers.add(new Location(47.9139, 11.4215));
		expectedMarkers.add(new Location(32.7078, 11.4011));
		
		for(ILocation expectedMarker : expectedMarkers)
		{
			boolean contains = false;
			
			for(Marker marker : clusterZoom1.getMarkers())
			{
				contains = ((marker.getLatitude() - expectedMarker.getLatitude()) < 0.01)
						&& ((marker.getLongitude() - expectedMarker.getLongitude()) < 0.01);
				
				if(contains)
				{
					break;
				}
			}
			
			if(!contains)
			{
				fail("Expected marker was not in actual markers");
			}
		}
		
		for(Marker marker : clusterZoom1.getMarkers())
		{
			boolean contains = false;
			
			for(ILocation expectedMarker : expectedMarkers)
			{
				contains = ((marker.getLatitude() - expectedMarker.getLatitude()) < 0.01)
						&& ((marker.getLongitude() - expectedMarker.getLongitude()) < 0.01);
				
				if(contains)
				{
					break;
				}
			}
			
			if(!contains)
			{
				fail("Marker was not in expected markers");
			}
		}		
	
	}
	
	@Test
	public void testCluster3FuckingPoints()
	{
		List<ILocation> locations = new ArrayList<ILocation>();
		locations.add(new Location(38.2924721, -122.4565503));
		locations.add(new Location(45.473753, -122.6583744));
		locations.add(new Location(32.8673, -97.2486));
		MarkerCluster clusterZoom0 = ClusteringUtil.cluster(locations);
	}
	
	@Test
	public void testCluster32000Points() throws FileNotFoundException, IOException {
		
		List<ILocation> locations = new ArrayList<ILocation>();		
		
		try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/ClusterPoints32000.txt")))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		    	if(line.isEmpty())
		    	{
		    		continue;
		    	}
		    	String[] splitted = line.split(",");
		    	String lat = splitted[0];
		    	String lng = splitted[1];
		    	
		    	locations.add(new Location(Double.parseDouble(lat), Double.parseDouble(lng)));
		    }
		}
		
		MarkerCluster clusterZoom0 = ClusteringUtil.cluster(locations);
		
	}

}

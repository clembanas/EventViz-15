/**
 * 
 */
package logic.clustering;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import logic.clustering.networking.ClusteringNodeClient;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author marius
 *
 */
public class ClusteringUtilTest {
	private static MarkerCluster markerCluster32000Points = null;
	
	@BeforeClass
	public static void initializeClusteringNodeServer() throws IOException
	{
		new Thread(new Runnable()
		{
			@Override
			public void run() {
				logic.clustering.networking.ServerMain.main(new String[0]);
			}
		}).start();
		
		// use just one thread for clustering..
		ClusteringUtil.initialize(null, 1);
		
		markerCluster32000Points = ClusteringUtil.cluster(createLocationsTestCluster32000Points());
	}
	
	@Test
	public void testBinarySerialization32000Points()
	{
		SerializationUtils.deserialize(SerializationUtils.serialize(markerCluster32000Points));
	}
	
	@Test
	public void testCluster0Points() {
		List<ILocation> locations = new ArrayList<ILocation>();
		
		MarkerCluster clusterTopLevel = ClusteringUtil.cluster(locations);
		assertEquals(-1, clusterTopLevel.getZoom());
		assertEquals(0, clusterTopLevel.getChildCount());
	}
	

	@Test
	public void testCluster2Points1LocalWorker() {
		// public static MarkerClusterGroup Cluster(Iterable<ILocation> locations)
		
		List<ILocation> locations = createLocationsTestCluster2Points();
		
		ClusteringWorker[] workers = new ClusteringWorker[1];
		workers[0] = new LocalClusteringWorker();
		MarkerCluster clusterTopLevel = ClusteringUtil.cluster(locations, workers);
		
		assertTestCluster2Points(clusterTopLevel);
	}
	
	@Test
	public void testCluster2Points1NetworkWorker() throws UnknownHostException, IOException {
		// public static MarkerClusterGroup Cluster(Iterable<ILocation> locations)
		
		List<ILocation> locations = createLocationsTestCluster2Points();
		
		ClusteringWorker[] workers = new ClusteringWorker[1];
		workers[0] = new ClusteringNodeClient("localhost", 9999);
		MarkerCluster clusterTopLevel = ClusteringUtil.cluster(locations, workers);
		
		assertTestCluster2Points(clusterTopLevel);
	}
	
	@Test
	public void testCluster2Points2LocalWorker() {
		// public static MarkerClusterGroup Cluster(Iterable<ILocation> locations)
		
		List<ILocation> locations = createLocationsTestCluster2Points();
		
		ClusteringWorker[] workers = new ClusteringWorker[2];
		workers[0] = new LocalClusteringWorker();
		workers[1] = new LocalClusteringWorker();
		MarkerCluster clusterTopLevel = ClusteringUtil.cluster(locations, workers);
		
		assertTestCluster2Points(clusterTopLevel);
	}

	private List<ILocation> createLocationsTestCluster2Points() {
		List<ILocation> locations = new ArrayList<ILocation>();
		locations.add(new Location(12, 47.9139, 11.4215, "name12"));
		locations.add(new Location(13, 32.7078, 11.4011, "name13"));
		return locations;
	}

	private void assertTestCluster2Points(MarkerCluster clusterTopLevel) {
		MarkerCluster clusterZoom0 = clusterTopLevel.getChildClusters().get(0);
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
		assertEquals(0, clusterZoom1.getMarkers().size());
		
		MarkerCluster clusterZoom2 = clusterZoom1.getChildClusters().get(0);
		assertEquals(2, clusterZoom2.getZoom());
		assertEquals(2, clusterZoom2.getChildCount());
		assertEquals(40.3109, clusterZoom2.getLatitude(),  0.01);
		assertEquals(11.4113, clusterZoom2.getLongitude(), 0.01);
		assertEquals(2, clusterZoom2.getMarkers().size());
		
		
		
		List<ILocation> expectedMarkers = new ArrayList<ILocation>();
		expectedMarkers.add(new Location(12, 47.9139, 11.4215, "name12"));
		expectedMarkers.add(new Location(13, 32.7078, 11.4011, "name13"));
		
		for(ILocation expectedMarker : expectedMarkers)
		{
			boolean contains = false;
			
			for(Marker marker : clusterZoom2.getMarkers())
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
	public void testClusterEventsAtSameLocation() {
		// public static MarkerClusterGroup Cluster(Iterable<ILocation> locations)
		
		List<ILocation> locations = new ArrayList<ILocation>();
		locations.add(new Location(12, 47.9139, 11.4215, "name12"));
		locations.add(new Location(13, 47.9139, 11.4215, "name13"));
		
		ClusteringWorker[] workers = new ClusteringWorker[1];
		workers[0] = new LocalClusteringWorker();
		MarkerCluster clusterTopLevel = ClusteringUtil.cluster(locations, workers);
		
		
	}
	
	@Test
	public void testCluster2PointsFarAway()
	{
		List<ILocation> locations = new ArrayList<ILocation>();
		locations.add(new Location(12, 74.0,-34.6588, "name12"));
		locations.add(new Location(13, 0,0, "name13"));
		
		MarkerCluster clusterZoom0 = ClusteringUtil.cluster(locations);
	}
	
	@Test
	public void testCluster3FuckingPoints()
	{
		List<ILocation> locations = new ArrayList<ILocation>();
		locations.add(new Location(12, 38.2924, -122.4565, "name12"));
		locations.add(new Location(13, 45.4737, -122.6583, "name13"));
		locations.add(new Location(14, 32.8673, -97.2486, "name14"));
		MarkerCluster clusterZoom0 = ClusteringUtil.cluster(locations);
	}
	
	@Test
	public void testCluster32000Points1LocalWorker() throws FileNotFoundException, IOException {		
		List<ILocation> locations = createLocationsTestCluster32000Points();
		
		ClusteringWorker[] workers = new ClusteringWorker[1];
		workers[0] = new LocalClusteringWorker();
		MarkerCluster clusterZoom0 = ClusteringUtil.cluster(locations, workers);
	}
	
	@Test
	public void testCluster32000Points2LocalWorker() throws FileNotFoundException, IOException {		
		List<ILocation> locations = createLocationsTestCluster32000Points();
		
		ClusteringWorker[] workers = new ClusteringWorker[2];
		workers[0] = new LocalClusteringWorker();
		workers[1] = new LocalClusteringWorker();
		MarkerCluster clusterZoom0 = ClusteringUtil.cluster(locations, workers);
	}
	
	@Test
	public void testCluster32000Points1NetworkWorker() throws FileNotFoundException, IOException {		
		List<ILocation> locations = createLocationsTestCluster32000Points();
		
		ClusteringWorker[] workers = new ClusteringWorker[1];
		workers[0] = new ClusteringNodeClient("localhost", 9999);
		
		MarkerCluster clusterZoom0 = ClusteringUtil.cluster(locations, workers);
	}
	
	@Test
	public void testCluster32000Points2NetworkWorker() throws FileNotFoundException, IOException {		
		List<ILocation> locations = createLocationsTestCluster32000Points();
		
		ClusteringWorker[] workers = new ClusteringWorker[2];
		workers[0] = new ClusteringNodeClient("localhost", 9999);
		workers[1] = new ClusteringNodeClient("localhost", 9999);
		
		MarkerCluster clusterZoom0 = ClusteringUtil.cluster(locations, workers);
	}
	
	@Test
	public void testCluster106000Points1NetworkWorker() throws FileNotFoundException, IOException {		
		List<ILocation> locations = createLocationsTestCluster106000Points();
		
		ClusteringWorker[] workers = new ClusteringWorker[1];
		workers[0] = new ClusteringNodeClient("localhost", 9999);
		
		MarkerCluster clusterZoom0 = ClusteringUtil.cluster(locations, workers);
	}

	private static List<ILocation> createLocationsTestCluster32000Points() throws IOException {
		return createLocationsTestCluster("/ClusterPoints32000.txt");
	}
	
	private static List<ILocation> createLocationsTestCluster106000Points() throws IOException {
		return createLocationsTestCluster("/ClusterPoints106744.txt");
	}

	private static List<ILocation> createLocationsTestCluster(String fileName)
			throws IOException {
		List<ILocation> locations = new ArrayList<ILocation>();		
		
		int i = 0;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(ClusteringUtilTest.class.getResourceAsStream(fileName)))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		    	if(line.isEmpty())
		    	{
		    		continue;
		    	}
		    	String[] splitted = line.split(",");
		    	String lat = splitted[0];
		    	String lng = splitted[1];
		    	
		    	locations.add(new Location(i, Double.parseDouble(lat), Double.parseDouble(lng), "name"+i));
		    	i++;
		    }
		}
		
		return locations;
	}

}

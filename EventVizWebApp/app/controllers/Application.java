package controllers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.zip.GZIPOutputStream;

import jsonGeneration.JsonResultGenerator;
import logic.clustering.ClusteringUtil;
import logic.clustering.ClusteringWorker;
import logic.clustering.ILocation;
import logic.clustering.LocalClusteringWorker;
import logic.clustering.MarkerCluster;
import logic.clustering.serialization.LightMarkerClusterVO;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import sentiment_analysis.SocialMentionData;
import sentiment_analysis.SocialMentionSentimentComponent;
import views.html.clusteringtest;
import views.html.index;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import containers.EventVizEventBasics;
import containers.EventVizModelPopulationObject;
import database.EventViz15_DB_MySQLAccess;

public class Application extends Controller {

    static {
        try {
            EventViz15_DB_MySQLAccess.initializeDBAccess();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println();
    }

    public static Result index() {
        return ok(index.render());
    }

    public static Result clusteringtest() {
        return ok(clusteringtest.render());
    }

    public static Result getEventById(int eventId) {
        EventVizModelPopulationObject model = null;
        try {
            model = EventViz15_DB_MySQLAccess.getEventById(eventId);
        } catch (SQLException e) {
            return ok("");
        }
        JsonObject model_JSON = JsonResultGenerator.getEventById_JSON(model);
        return ok(model_JSON.toString());
    }
    
    
    public static void refreshEventsCaching() throws SQLException, IOException
    {
    	refreshEventsCaching(true, true);
    }
    private static byte[] cachedZippedEvents = null;
    private static Object getEventsLockObj = new Object();
    private static boolean cacheClusteredEvents = false;
	private static Iterable<? extends ILocation> cachedEventsFromDB = null;
    public static void refreshEventsCaching(boolean cacheEventsFromDB, boolean cacheClusteredEvents) throws SQLException, IOException {
    	Application.cacheClusteredEvents = cacheClusteredEvents;
    	
    	if(cacheEventsFromDB)
    	{
	    	if(cachedEventsFromDB == null)
	    	{
	    		synchronized(getEventsLockObj)
	    		{
	    			if(cachedEventsFromDB == null)
	    			{
	    				cachedEventsFromDB = EventViz15_DB_MySQLAccess.getEvents();
	    				
	    				if(cacheClusteredEvents)
	    				{
	    					cachedZippedEvents = clusterEventsAndZip(cachedEventsFromDB);
	    				}
	    			}
	    		}
	    	}
    	}
	}
    
    Iterable<? extends ILocation> events = null;
    public static Result getEvents() throws SQLException, IOException {
    	
    	if(!cacheClusteredEvents)
    	{
    		Iterable<? extends ILocation> eventsFromDB = cachedEventsFromDB; 
    		if(eventsFromDB == null)
    		{
    			eventsFromDB = EventViz15_DB_MySQLAccess.getEvents();
    		}
    		
    		cachedZippedEvents = clusterEventsAndZip(eventsFromDB);
    	}
    	
    	response().setHeader("Content-Encoding", "gzip");
        response().setHeader("Content-Length", cachedZippedEvents.length + "");
        return ok(cachedZippedEvents);
    }

	private static byte[] clusterEventsAndZip(Iterable<? extends ILocation> eventsFromDB) throws IOException {
		MarkerCluster markerCluster = ClusteringUtil.cluster(eventsFromDB);    		
		String json = Json.stringify(Json.toJson(new LightMarkerClusterVO(markerCluster)));
		final ByteArrayOutputStream gzip = gzip(json);
		return gzip.toByteArray();
	}
    

    
    public static ByteArrayOutputStream gzip(final String input)
            throws IOException {
        final InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        final ByteArrayOutputStream stringOutputStream = new ByteArrayOutputStream((int) (input.length() * 0.75));
        final OutputStream gzipOutputStream = new GZIPOutputStream(stringOutputStream);
 
        final byte[] buf = new byte[5000];
        int len;
        while ((len = inputStream.read(buf)) > 0) {
            gzipOutputStream.write(buf, 0, len);
        }
 
        inputStream.close();
        gzipOutputStream.close();
 
        return stringOutputStream;
    }


    public static Result getSentiment(String terms, String location) {
        JsonArray jso = (JsonArray) new JsonParser().parse(terms);
        LinkedList<String> list = new LinkedList<String>();
        for (int i = 0; i < jso.size(); i++) {
            list.add(jso.get(i).toString());
        }
        SocialMentionData data = null;
        try {
            data = SocialMentionSentimentComponent.getSocialMentionData(list, location);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ok(JsonResultGenerator.getSocialMentionSentiment_JSON(data).toString());
    }
    
    private static String NEWLINE = null;
    public static Result timemeasurement() throws SQLException
    {
    	NEWLINE = System.getProperty("line.separator");
    	
    	Iterable<EventVizEventBasics> eventsFromDB = EventViz15_DB_MySQLAccess.getEvents();
    	
    	StringBuilder resultBuilder = new StringBuilder();
    	
    	for(int i = 1; i < 20; i++)
    	{
    		measureTime(eventsFromDB, i, resultBuilder);
    	}
    	
    	return ok(resultBuilder.toString());
    }

	private static void measureTime(Iterable<EventVizEventBasics> eventsFromDB, int localWorkers, StringBuilder resultBuilder) {    	
    	int count = 10;
    	
    	long tmp = 0;
    	for(int i = 0; i < count; i++)
    	{
    		long before = System.currentTimeMillis();
    		
    		ClusteringWorker[] workers = new ClusteringWorker[localWorkers];
        	
        	for(int l = 0; l < localWorkers; l++)
        	{
        		workers[l] = new LocalClusteringWorker();
        	}
	    	
	    	ClusteringUtil.cluster(eventsFromDB, workers);
	    	tmp += System.currentTimeMillis() - before;
    	}
    	
    	long result = tmp / count;
    	
    	resultBuilder.append("Localworkers: "+localWorkers+", " + result + " ms = " + (result /1000) + "s" + NEWLINE); 
	}

	
}
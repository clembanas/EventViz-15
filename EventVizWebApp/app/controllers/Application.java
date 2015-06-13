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
import logic.clustering.ILocation;
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
    
    
    /*private static Iterable<? extends ILocation> events = null;
    private static Object getEventsLockObj = new Object(); 
    public static Result getEvents() throws SQLException, IOException {       
    	
    	if(events == null)
    	{
    		synchronized(getEventsLockObj)
    		{
    			if(events == null)
    			{
    				events = EventViz15_DB_MySQLAccess.getEvents();
    			}
    		}
    	}
    	
    	MarkerCluster markerCluster = ClusteringUtil.cluster(events);    		
		String json = Json.stringify(Json.toJson(new LightMarkerClusterVO(markerCluster)));
		return ok(json);
    }*/
    
    /*private static Iterable<? extends ILocation> events = null;
    private static Object getEventsLockObj = new Object(); 
    public static Result getEvents() throws SQLException, IOException {       
    	
    	if(events == null)
    	{
    		synchronized(getEventsLockObj)
    		{
    			if(events == null)
    			{
    				events = EventViz15_DB_MySQLAccess.getEvents();    				
    			}
    		}
    	}
    	
    	MarkerCluster markerCluster = ClusteringUtil.cluster(events);    		
		String json = Json.stringify(Json.toJson(new LightMarkerClusterVO(markerCluster)));
		final ByteArrayOutputStream gzip = gzip(json);
		byte[] cachedZippedEvents = gzip.toByteArray();
    	
    	response().setHeader("Content-Encoding", "gzip");
        response().setHeader("Content-Length", cachedZippedEvents.length + "");
        return ok(cachedZippedEvents);
    }*/
    
    private static byte[] cachedZippedEvents = null;
    private static Object getEventsLockObj = new Object(); 
    public static Result getEvents() throws SQLException, IOException {       
    	
    	if(cachedZippedEvents == null)
    	{
    		synchronized(getEventsLockObj)
    		{
    			if(cachedZippedEvents == null)
    			{
    				Iterable<? extends ILocation> events = EventViz15_DB_MySQLAccess.getEvents();
    				
    				MarkerCluster markerCluster = ClusteringUtil.cluster(events);    		
    				String json = Json.stringify(Json.toJson(new LightMarkerClusterVO(markerCluster)));
    				final ByteArrayOutputStream gzip = gzip(json);
    				cachedZippedEvents = gzip.toByteArray();
    			}
    		}
    	}
    	
    	response().setHeader("Content-Encoding", "gzip");
        response().setHeader("Content-Length", cachedZippedEvents.length + "");
        return ok(cachedZippedEvents);
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
}
package controllers;


import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import jsonGeneration.JsonResultGenerator;
import logic.clustering.ClusteringUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.avaje.ebean.text.json.JsonElementArray;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import containers.*;
import database.EventViz15_DB_MySQLAccess;
import play.libs.Json;
import play.*;
import play.mvc.*;
import views.html.*;
import sentiment_analysis.SocialMentionData;
import sentiment_analysis.SocialMentionSentimentComponent;

public class Application extends Controller {
	
	static {
		try {
			EventViz15_DB_MySQLAccess.initializeDBAccess();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static Result index() {
		return ok(index.render());
	}
	
	public static Result clusteringtest() {
		return ok(clusteringtest.render());
	}

	public static Result getEvents() {
		List<EventVizEventBasics> events = null;
		try {
		events = EventViz15_DB_MySQLAccess.getEvents();
		} catch (SQLException e) {
			return ok("");
		}
		JsonArray events_JSON = JsonResultGenerator.getEvents_JSON(events);
		
		return ok(ClusteringUtil.getEventJsonNode(events_JSON));
	}

	public static Result getCity(String cityname, String country) {
		if(cityname.equals("")) {
			return badRequest("Missing parameter [city]");
		} else if(country.equals("")){
			return badRequest("Missing parameter [country]");
		} else {
			List<EventVizCity> cities = null;
			try {
				cities = EventViz15_DB_MySQLAccess.getCity(cityname, country);
			} catch (SQLException e) {
				return ok("");
			}
			JsonArray cities_JSON = JsonResultGenerator.getCity_JSON(cities);
			return ok(cities_JSON.toString());
		}
	}
	
	public static Result getEventById(String eventful_id) {
		EventVizEvent event = null;
		try {
			event = EventViz15_DB_MySQLAccess.getEventById(eventful_id);
		} catch (SQLException e) {
			return ok("");
		}
		JsonObject event_JSON = JsonResultGenerator.getEventById_JSON(event);
		return ok(event_JSON.toString());
	}
	
	public static Result getArtist(String artistName) {
		EventVizArtist artist = null;
		try {
			artist = EventViz15_DB_MySQLAccess.getArtist(artistName);
		} catch (SQLException e) {
			return ok("");
		}
		JsonObject artist_JSON = JsonResultGenerator.getArtist_JSON(artist);
		return ok(artist_JSON.toString());
	}
	
	public static Result getBand(String bandName) {
		EventVizBand band = null;
		try {
			band = EventViz15_DB_MySQLAccess.getBand(bandName);
		} catch (SQLException e) {
			return ok("");
		}
		JsonObject band_JSON = JsonResultGenerator.getBand_JSON(band);
		return ok(band_JSON.toString());
	}
	
	public static Result getSentiment(String terms, String location){
		JsonArray jso = (JsonArray) new JsonParser().parse(terms);
		System.out.println( jso.get(0));
		LinkedList<String> list = new LinkedList<String>();
		for(int i = 0; i < jso.size(); i++){
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
	
	public static Result getDefaultCluster()
	{
		return ok(ClusteringUtil.getDefaultClusterJsonNode());
	}
	
}
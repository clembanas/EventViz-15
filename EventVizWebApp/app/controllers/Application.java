package controllers;


import java.io.IOException;
import java.util.LinkedList;

import jsonGeneration.JsonResultGenerator;
import logic.clustering.ClusteringUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.avaje.ebean.text.json.JsonElementArray;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import containers.EventVizCity;
import containers.EventVizEvent;
import play.libs.Json;
import play.*;
import play.mvc.*;
import views.html.*;
import sentiment_analysis.SocialMentionData;
import sentiment_analysis.SocialMentionSentimentComponent;

public class Application extends Controller {

	public static Result index() {
		return ok(index.render());
	}
	
	public static Result clusteringtest() {
		return ok(clusteringtest.render());
	}

	//dummy without database connection
	public static Result getEvents() {
		return ok(JsonResultGenerator.getEvents_JSON("hello").toString());
	}

	//dummy without database connection
	public static Result getCity(String cityname, String country) {
		if(cityname.equals("")) {
			return badRequest("Missing parameter [city]");
		}else if(country.equals("")){
			return badRequest("Missing parameter [country]");
		}else {
			EventVizCity city = new EventVizCity(cityname, country, 5000);
			return ok(JsonResultGenerator.getCity_JSON(city).toString());
		}
	}
	
	//dummy without database connection
	public static Result getEventById(String id){
		EventVizEvent event = new EventVizEvent("name", "description", "city", "country", "location", "09.00", 2)
		return ok(JsonResultGenerator.getSpecificEvent_JSON(event).toString());
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
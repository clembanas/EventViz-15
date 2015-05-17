package controllers;


import java.io.IOException;
import java.util.LinkedList;

import logic.clustering.ClusteringUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.avaje.ebean.text.json.JsonElementArray;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

	public static Result getEvents() {
		ObjectNode result = Json.newObject();
		result.put("name", "hello");
		return ok(result);
	}

	//dummy without database connection
	public static Result getCity(String city, String country) {
		if(city.equals("")) {
			return badRequest("Missing parameter [city]");
		}else if(country.equals("")){
			return badRequest("Missing parameter [country]");
		}else {
			ObjectNode result = Json.newObject();
			result.put("name", city);
			result.put("country", country);
			result.put("population", "5000");
			return ok(result);
		}
	}
	
	//dummy without database connection
	public static Result getEventById(String id){
		ObjectNode result = Json.newObject();
		result.put("name", "name");
		result.put("description", "description");
		result.put("city", "city");
		result.put("country", "country");
		result.put("location", "location");
		result.put("startTime", "09.00");
		result.put("duration", "2");
		return ok(result);
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
		return ok(data.getJSON().toString());
	}
	
	public static Result getDefaultCluster()
	{
		return ok(ClusteringUtil.getDefaultClusterJsonNode());
	}
}

package controllers;

import logic.clustering.ClusteringUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.*;
import play.mvc.*;
import views.html.*;

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

	public static Result getCity(String city, String country) {
		System.out.println(ctx().request().body());
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
	
	public static Result getDefaultCluster()
	{
		return ok(ClusteringUtil.getDefaultClusterJsonNode());
	}
}

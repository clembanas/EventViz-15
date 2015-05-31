package jsonGeneration;

import java.util.List;
import java.util.Map;

import sentiment_analysis.SocialMentionData;
import sentiment_analysis.SocialMentionSentiment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import containers.EventVizArtist;
import containers.EventVizBand;
import containers.EventVizBandMember;
import containers.EventVizCity;
import containers.EventVizEvent;
import containers.EventVizEventBasics;
import containers.EventVizLocation;
import containers.EventVizModelPopulationObject;


public class JsonResultGenerator {

	public static JsonArray getEvents_JSON(List<EventVizEventBasics> events) {
		JsonArray jsonResult = new JsonArray();
		for(EventVizEventBasics event : events) {
			JsonObject jsonEvent = new JsonObject();
			jsonEvent.addProperty("eventful_id", event.getEventful_id());
			jsonEvent.addProperty("latitude", event.getLatitude());
			jsonEvent.addProperty("longitude", event.getLongitude());
			jsonResult.add(jsonEvent);
		}
		return jsonResult;
	}
	
	public static JsonObject getEventById_JSON(EventVizModelPopulationObject model) {
		JsonObject jsonResult = new JsonObject();
		
		EventVizEvent event = model.getEvent();
		JsonObject eventJSON = new JsonObject();
		eventJSON.addProperty("id", event.getId());
		eventJSON.addProperty("name", event.getName());
		eventJSON.addProperty("description", event.getDescription());
		eventJSON.addProperty("event_type", event.getEvent_type());
		eventJSON.addProperty("location_id", event.getLocation_id());
		eventJSON.addProperty("eventful_id", event.getEventful_id());
		jsonResult.add("event", eventJSON);
		
		EventVizLocation location = model.getLocation();
		EventVizCity city = location.getCity();
		JsonObject locationJSON = new JsonObject();
		locationJSON.addProperty("locationName", location.getName());
		locationJSON.addProperty("cityName", city.getName());
		locationJSON.addProperty("region", city.getRegion());
		locationJSON.addProperty("country", city.getCountry());
		locationJSON.addProperty("latitude", city.getLatitude());
		locationJSON.addProperty("longitude", city.getLongitude());
		locationJSON.addProperty("dbpedia_resource", city.getDbpedia_resource());
		jsonResult.add("location", locationJSON);
		
		List<EventVizBand> bands = model.getBands();
		JsonArray bandsJSON = new JsonArray();
		for(EventVizBand band : bands) {
			bandsJSON.add(JsonResultGenerator.getBand_JSON(band));
		}
		jsonResult.add("bands", bandsJSON);
		return jsonResult;
	}
	
	public static JsonArray getCity_JSON(List<EventVizCity> cities) {
		JsonArray jsonResult = new JsonArray();
		for(EventVizCity city : cities) {
			JsonObject jsonCity = new JsonObject();
			jsonCity.addProperty("id", city.getId());
			jsonCity.addProperty("name", city.getName());
			jsonCity.addProperty("region", city.getRegion());
			jsonCity.addProperty("country", city.getCountry());
			jsonCity.addProperty("latitude", city.getLatitude());
			jsonCity.addProperty("longitude", city.getLongitude());
			jsonCity.addProperty("dbpedia_resource", city.getDbpedia_resource());
			jsonResult.add(jsonCity);
		}
		return jsonResult;
	}
	
	public static JsonObject getArtist_JSON(EventVizArtist artist) {
		JsonObject jsonResult = new JsonObject();
		jsonResult.addProperty("id", artist.getId());
		jsonResult.addProperty("name", artist.getName());
		jsonResult.addProperty("alternate_name", artist.getAlternate_name());
		jsonResult.addProperty("dbpedia_resource", artist.getDbpedia_resource());
		jsonResult.addProperty("bandName", artist.getBandName());
		return jsonResult;
	}
	
	public static JsonObject getBand_JSON(EventVizBand band) {
		JsonObject jsonResult = new JsonObject();
		jsonResult.addProperty("id", band.getId());
		jsonResult.addProperty("name", band.getName());

		JsonArray members = new JsonArray();
		for(EventVizBandMember bm : band.getMembers()) {
			JsonObject jsonMember = new JsonObject();
			jsonMember.addProperty("id", bm.getId());
			jsonMember.addProperty("name", bm.getName());
			jsonMember.addProperty("alternate_name", bm.getAlternate_name());
			jsonMember.addProperty("member_type", bm.getMember_type());
			jsonMember.addProperty("dbpedia_resource", bm.getDbpedia_resource());
			members.add(jsonMember);
		}
		
		jsonResult.add("members", members);
		jsonResult.addProperty("dbpedia_resource", band.getDbpedia_resource());
		return jsonResult;
	}
	
	public static JsonObject getSocialMentionSentiment_JSON(SocialMentionData sentimentAnalysisResult){
		JsonObject jsonResult = new JsonObject();
		jsonResult.addProperty("score_strength", sentimentAnalysisResult.getScore_strength());
		jsonResult.addProperty("score_sentiment", sentimentAnalysisResult.getScore_sentiment());
		jsonResult.addProperty("score_passion", sentimentAnalysisResult.getScore_passion());
		jsonResult.addProperty("score_reach", sentimentAnalysisResult.getScore_reach());
		
		SocialMentionSentiment sentiment = sentimentAnalysisResult.getSentiment();
		JsonObject jsonSentiment = new JsonObject();
		jsonSentiment.addProperty("positive", sentiment.getPositive());
		jsonSentiment.addProperty("neutral", sentiment.getNeutral());
		jsonSentiment.addProperty("negative", sentiment.getNegative());
		jsonResult.add("sentiment", jsonSentiment);

		JsonArray keywords = new JsonArray();
		Map<String, Integer> sortedKeywords = sentimentAnalysisResult.getKeywords().getSortedKeywords();
		for(String s : sortedKeywords.keySet()){
			JsonObject keyword = new JsonObject();
			keyword.addProperty("keyword", s);
			keyword.addProperty("occurance", sortedKeywords.get(s));
			keywords.add(keyword);
		}
		jsonResult.add("keywords", keywords);
		
		JsonArray hashtags = new JsonArray();
		Map<String, Integer> sortedHashtags = sentimentAnalysisResult.getHashtags().getSortedHashtags();
		for(String s : sortedHashtags.keySet()){
			JsonObject hashtag = new JsonObject();
			hashtag.addProperty("hashtag", s);
			hashtag.addProperty("occurance", sortedHashtags.get(s));
			hashtags.add(hashtag);
		}
		jsonResult.add("hashtags", hashtags);
		
		JsonArray users = new JsonArray();
		Map<String, Integer> sortedUsers = sentimentAnalysisResult.getUsers().getSortedUsers();
		for(String s : sortedUsers.keySet()){
			JsonObject user = new JsonObject();
			user.addProperty("user", s);
			user.addProperty("occurance", sortedUsers.get(s));
			users.add(user);
		}
		jsonResult.add("users", users);
		
		return jsonResult;
	}

}
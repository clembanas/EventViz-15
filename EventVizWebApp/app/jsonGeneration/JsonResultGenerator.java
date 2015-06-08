package jsonGeneration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import containers.*;
import sentiment_analysis.SocialMentionData;
import sentiment_analysis.SocialMentionSentiment;

import java.util.List;
import java.util.Map;


public class JsonResultGenerator {

	public static JsonObject getEventById_JSON(EventVizModelPopulationObject model) {
		JsonObject jsonResult = new JsonObject();
		
		EventVizEvent event = model.getEvent();
		JsonObject eventJSON = new JsonObject();
		eventJSON.addProperty("id", event.getId());
		eventJSON.addProperty("eventName", event.getName());
		eventJSON.addProperty("description", event.getDescription());
		eventJSON.addProperty("event_type", event.getEvent_type());
		eventJSON.addProperty("start_date", String.valueOf(event.getStart_date()));
		eventJSON.addProperty("end_date", String.valueOf(event.getEnd_date()));
		eventJSON.addProperty("start_time", String.valueOf(event.getStart_time()));
		eventJSON.addProperty("end_time", String.valueOf(event.getEnd_time()));
		eventJSON.addProperty("location_id", event.getLocation_id());
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
		locationJSON.addProperty("dbpedia_res_city", city.getDbpedia_res_city());
		locationJSON.addProperty("dbpedia_res_region", city.getDbpedia_res_region());
		locationJSON.addProperty("dbpedia_res_country", city.getDbpedia_res_country());
		jsonResult.add("location", locationJSON);
		
		List<EventVizBand> bands = model.getBands();
		JsonArray bandsJSON = new JsonArray();
		for(EventVizBand band : bands) {
			bandsJSON.add(JsonResultGenerator.getBand_JSON(band));
		}
		jsonResult.add("bands", bandsJSON);
		return jsonResult;
	}
	
	private static JsonObject getBand_JSON(EventVizBand band) {
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

	public static JsonArray getEvents_JSON(List<EventVizEventBasics> events) {
		JsonArray jsonResult = new JsonArray();
		for(EventVizEventBasics event : events) {
			JsonObject jsonEvent = new JsonObject();
			jsonEvent.addProperty("id", event.getId());
			jsonEvent.addProperty("name", event.getName());
			jsonEvent.addProperty("latitude", event.getLatitude());
			jsonEvent.addProperty("longitude", event.getLongitude());
			jsonResult.add(jsonEvent);
		}
		return jsonResult;
	}

	public static JsonObject getSocialMentionSentiment_JSON(SocialMentionData sentimentAnalysisResult){
		JsonObject jsonResult = new JsonObject();
		if(sentimentAnalysisResult == null){
			return jsonResult;
		}
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
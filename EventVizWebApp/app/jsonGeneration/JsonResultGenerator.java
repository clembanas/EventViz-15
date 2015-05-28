package jsonGeneration;

import java.util.Map;

import sentiment_analysis.SocialMentionData;
import sentiment_analysis.SocialMentionSentiment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import containers.EventVizCity;
import containers.EventVizCountry;
import containers.EventVizEvent;


public class JsonResultGenerator {
	
	public static JsonObject getEvents_JSON(String eventName) {
		JsonObject jsonResult = new JsonObject();
		jsonResult.addProperty("name", eventName);
		return jsonResult;
	}
	
	public static JsonObject getSpecificEvent_JSON(EventVizEvent event) {
		JsonObject jsonResult = new JsonObject();
		jsonResult.addProperty("name", event.getName());
		jsonResult.addProperty("description", event.getDescription());
		jsonResult.addProperty("city", event.getCity());
		jsonResult.addProperty("country", event.getCountry());
		jsonResult.addProperty("location", event.getLocation());
		jsonResult.addProperty("startTime", event.getStartTime());
		jsonResult.addProperty("duration", event.getDuration());
		return jsonResult;
	}
	
	public static JsonObject getCity_JSON(EventVizCity city) {
		JsonObject jsonResult = new JsonObject();
		jsonResult.addProperty("name", city.getName());
		jsonResult.addProperty("country", city.getCounty());
		jsonResult.addProperty("dbpediaURI", city.getDbpediaURI());
		return jsonResult;
	}
	
	public static JsonObject getCountry_JSON(EventVizCountry country) {
		JsonObject jsonResult = new JsonObject();
		jsonResult.addProperty("name", country.getName());
		jsonResult.addProperty("dbpediaURI", country.getDbpediaURI());
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
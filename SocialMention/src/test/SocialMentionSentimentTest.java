package test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import sentiment.SocialMentionSentimentComponent;
import data.SocialMentionData;
import data.SocialMentionSentiment;

public class SocialMentionSentimentTest {

	@SuppressWarnings("serial")
	public static void main(String[] args) {
		final String location = "madrid";
		final List<String> queryTerms = new ArrayList<String>() {
			{
				add("juventus");
				add("real");
			}
		};
		

		SocialMentionData socialMentionData = null;
		long start = System.currentTimeMillis();
		try {
			socialMentionData = SocialMentionSentimentComponent.getSocialMentionData(queryTerms, location);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long end = System.currentTimeMillis();
		System.out.println("duration: " + (end-start) + "ms\n");
		
		//output of retrieved data elements
		System.out.println("results:");
		System.out.println("score_strength: " + socialMentionData.getScore_strength());
		System.out.println("score_sentiment: " + socialMentionData.getScore_sentiment());
		System.out.println("score_passion: " + socialMentionData.getScore_passion());
		System.out.println("score_reach: " + socialMentionData.getScore_reach());
		System.out.println("last_mention: " + socialMentionData.getLast_mention());
		System.out.println("");
		
		SocialMentionSentiment sentiment = socialMentionData.getSentiment();
		System.out.println("sentiment --> positive: " + sentiment.getPositive() + " negative: " + sentiment.getNegative() + " neutral: " + sentiment.getNeutral());
		System.out.println("keywords: " + socialMentionData.getKeywords().getKeywords());
		System.out.println("keywords (sorted): " + socialMentionData.getKeywords().getSortedKeywords());
		System.out.println("users: " + socialMentionData.getUsers().getUsers());
		System.out.println("users (sorted): " + socialMentionData.getUsers().getSortedUsers());
		System.out.println("hashtags: " + socialMentionData.getHashtags().getHashtags());
		System.out.println("hashtags (sorted): " + socialMentionData.getHashtags().getSortedHashtags());
		System.out.println("sources: " + socialMentionData.getSources().getSources());
		System.out.println("sources (sorted): " + socialMentionData.getSources().getSortedSources());
	}

}

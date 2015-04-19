package parser;

import java.io.File;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import data.SocialMentionData;
import data.SocialMentionHashtags;
import data.SocialMentionKeywords;
import data.SocialMentionSentiment;
import data.SocialMentionSources;
import data.SocialMentionUsers;

public class SocialMentionParser {

	public static SocialMentionData parseHTMLContent(final File input, final String baseURI) throws IOException {
		final Document doc = Jsoup.parse(input, "UTF-8", baseURI);
		final int score_strength = parseStrengthScore(doc);
		final String score_sentiment = parseSentimentScore(doc);
		final int score_passion = parsePassionScore(doc);
		final int score_reach = parseReachScore(doc);
		final String last_mention = parseLastMention(doc);
		final SocialMentionSentiment sentiment = parseSentiment(doc);
		final SocialMentionKeywords keywords = parseKeywords(doc);
		final SocialMentionUsers users = parseUsers(doc);
		final SocialMentionHashtags hashtags = parseHashtags(doc);
		final SocialMentionSources sources = parseSources(doc);
		return new SocialMentionData(score_strength, score_sentiment, score_passion, score_reach, last_mention, sentiment, keywords, users, hashtags,
				sources);
	}

	private static int parseStrengthScore(final Document doc) {
		final Element score_strength_element = doc.getElementById("score_strength");
		final Element child = score_strength_element.child(0);
		child.children().remove(); // remove inner <span>
		final String score_strength = child.html();
		return Integer.parseInt(score_strength);
	}

	private static String parseSentimentScore(final Document doc) {
		final Element score_sentiment_element = doc.getElementById("score_sentiment");
		final Element child = score_sentiment_element.child(0);
		return child.html();
	}

	private static int parsePassionScore(final Document doc) {
		final Element score_passion_element = doc.getElementById("score_passion");
		final Element child = score_passion_element.child(0);
		child.children().remove();
		final String score_passion = child.html();
		return Integer.parseInt(score_passion);
	}

	private static int parseReachScore(final Document doc) {
		final Element score_reach_element = doc.getElementById("score_reach");
		final Element child = score_reach_element.child(0);
		child.children().remove();
		final String score_reach = child.html();
		return Integer.parseInt(score_reach);
	}

	private static String parseLastMention(final Document doc) {
		final Element column_left_element = doc.getElementById("column_left");
		final Element parent_element = column_left_element.child(4);
		return parent_element.child(1).html();
	}

	private static SocialMentionSentiment parseSentiment(final Document doc) {
		final Element column_left_element = doc.getElementById("column_left");
		final Element parent_element = column_left_element.child(5);
		final Elements tableContent = parent_element.select("tr");
		final String positive = tableContent.get(0).child(2).html();
		final String neutral = tableContent.get(1).child(2).html();
		final String negative = tableContent.get(2).child(2).html();
		return new SocialMentionSentiment(Integer.parseInt(positive), Integer.parseInt(neutral), Integer.parseInt(negative));
	}

	private static SocialMentionKeywords parseKeywords(final Document doc) {
		final SocialMentionKeywords keywords = new SocialMentionKeywords();
		final Element column_left_element = doc.getElementById("column_left");
		final Element parent_element = column_left_element.child(6);
		final Elements tableContent = parent_element.select("tr");
		for (int i = 0; i < tableContent.size(); i++) {
			final String keyword = tableContent.get(i).child(0).child(0).html();
			final int occurrence = Integer.parseInt(tableContent.get(i).child(2).html());
			keywords.addKeyword(keyword, occurrence);
		}
		return keywords;
	}

	private static SocialMentionUsers parseUsers(final Document doc) {
		final SocialMentionUsers users = new SocialMentionUsers();
		final Element column_left_element = doc.getElementById("column_left");
		final Element parent_element = column_left_element.child(7);
		final Elements tableContent = parent_element.select("tr");
		for (int i = 0; i < tableContent.size(); i++) {
			final String user = tableContent.get(i).child(0).child(0).html();
			final int occurrence = Integer.parseInt(tableContent.get(i).child(2).html());
			users.addUser(user, occurrence);
		}
		return users;
	}

	private static SocialMentionHashtags parseHashtags(final Document doc) {
		final SocialMentionHashtags hashtags = new SocialMentionHashtags();
		final Element column_left_element = doc.getElementById("column_left");
		final Element parent_element = column_left_element.child(8);
		final Elements tableContent = parent_element.select("tr");
		for (int i = 0; i < tableContent.size(); i++) {
			final String hashtag = tableContent.get(i).child(0).child(0).html();
			final int occurrence = Integer.parseInt(tableContent.get(i).child(2).html());
			hashtags.addHashtag(hashtag, occurrence);
		}
		return hashtags;
	}

	private static SocialMentionSources parseSources(final Document doc) {
		final SocialMentionSources sources = new SocialMentionSources();
		final Element column_left_element = doc.getElementById("column_left");
		final Element parent_element = column_left_element.child(9);
		final Elements tableContent = parent_element.select("tr");
		for (int i = 0; i < tableContent.size(); i++) {
			final String source = tableContent.get(i).child(0).child(0).html();
			final int occurrence = Integer.parseInt(tableContent.get(i).child(2).html());
			sources.addSource(source, occurrence);
		}
		return sources;
	}
}
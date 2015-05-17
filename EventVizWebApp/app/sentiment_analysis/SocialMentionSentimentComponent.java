package sentiment_analysis;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;


@SuppressWarnings("serial")
public class SocialMentionSentimentComponent {
	private static final String baseURL = "http://socialmention.com/";
	private static final List<String> searchType = new ArrayList<String>() {
		{
			add("all"); //searches in all available blogs, networks, events, videos
		}
	};
	
	/**
	 * Performs the SocialMention query process and the parsing of the resulting information.
	 * 
	 * @param queryTerms
	 * @param location
	 * @return sentiment data
	 * @throws IOException 
	 */
	public static SocialMentionData getSocialMentionData(final List<String> queryTerms, final String location) throws IOException {
		SocialMentionData sentimentData = null;
		String url = SocialMentionSentimentComponent.createQueryURL(queryTerms,  location);
		
		Element test_element = null;
		Document htmlContent = null;
		while (test_element == null) {
			htmlContent = Jsoup.connect(url).timeout(100 * 1000).userAgent("Mozilla").ignoreContentType(true).followRedirects(true).get();
			test_element = htmlContent.getElementById("score_strength");
		}
		sentimentData = SocialMentionParser.parseHTMLContent(htmlContent);
		return sentimentData;
	}
	
	/**
	 * Build the URL-String to query the Social Mention API
	 * 
	 * @see https://code.google.com/p/socialmention-api/wiki/APIDocumentation
	 * 
	 * @param queryTerms
	 * @param searchType (blogs, ...)
	 * @param location
	 * 
	 * @return URL string
	 * @throws MalformedURLException
	 */
	private static String createQueryURL(final List<String> queryTerms, final String location)
			throws MalformedURLException {
		final StringBuilder sb = new StringBuilder();
		sb.append(SocialMentionSentimentComponent.baseURL);
		sb.append("search?");

		// append queryTerms
		if (queryTerms == null || queryTerms.isEmpty()) {
			throw new MalformedURLException("no query term was given");
		}
		sb.append("q=");
		for (int index = 0; index < queryTerms.size(); index++) {
			sb.append(queryTerms.get(index));
			if (index < queryTerms.size() - 1) {
				sb.append("+");
			}
		}

		// append search type
		sb.append("&");
		if (searchType == null || searchType.isEmpty()) {
			sb.append("t=");
			sb.append("all");
		} else if (searchType.size() == 1) {
			sb.append("t=");
			sb.append(searchType.get(0));
		} else {
			for (int index = 0; index < searchType.size(); index++) {
				sb.append("t[]=");
				sb.append(searchType.get(index));
				sb.append("&");
			}
		}

		// append location string
		if (!(location == null || location == "")) {
			sb.append("&l=");
			sb.append(location);
		}
		return sb.toString();
	}
	
}
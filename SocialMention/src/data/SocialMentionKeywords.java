package data;

import java.util.Map;
import java.util.TreeMap;

public class SocialMentionKeywords {
	private Map<String, Integer> keywords;

	public SocialMentionKeywords() {
		this.keywords = new TreeMap<String, Integer>();
	}

	public Map<String, Integer> getKeywords() {
		return keywords;
	}

	public void setKeywords(final Map<String, Integer> keywords) {
		this.keywords = keywords;
	}

	public void addKeyword(final String keyword, final int occurrence) {
		this.keywords.put(keyword, occurrence);
	}

}
package data;

import java.util.Map;
import java.util.TreeMap;

public class SocialMentionHashtags {
	private Map<String, Integer> hashtags;

	public SocialMentionHashtags() {
		this.hashtags = new TreeMap<String, Integer>();
	}

	public Map<String, Integer> getHashtags() {
		return hashtags;
	}

	public void setHashtags(final Map<String, Integer> hashtags) {
		this.hashtags = hashtags;
	}

	public void addHashtag(final String hashtag, final int occurence) {
		this.hashtags.put(hashtag, occurence);
	}

}
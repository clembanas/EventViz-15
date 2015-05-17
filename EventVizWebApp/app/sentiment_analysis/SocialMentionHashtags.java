package sentiment_analysis;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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

	public Map<String, Integer> getSortedHashtags() {
		final List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(this.getHashtags().entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			@Override
			public int compare(final Map.Entry<String, Integer> o1, final Map.Entry<String, Integer> o2) {
				return (o2.getValue().compareTo(o1.getValue()));
			}
		});

		final Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
		for (final Iterator<Map.Entry<String, Integer>> it = list.iterator(); it.hasNext();) {
			final Entry<String, Integer> entry = it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}
	
	public JsonArray getSortedHashtagsJSON(){
		JsonArray jsa = new JsonArray();
		Map<String, Integer> sortedMap = getSortedHashtags();
		for(String s : sortedMap.keySet()){
			JsonObject keyword = new JsonObject();
			keyword.addProperty("hashtag", s);
			keyword.addProperty("occurance", sortedMap.get(s));
			jsa.add(keyword);
		}
		return jsa;
	}
}
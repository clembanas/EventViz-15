package sentiment_analysis;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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

	public Map<String, Integer> getSortedKeywords() {
		final List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(this.getKeywords().entrySet());
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
	
	public JsonArray getSortedKeywordJSON(){
		JsonArray jsa = new JsonArray();
		Map<String, Integer> sortedMap = getSortedKeywords();
		for(String s : sortedMap.keySet()){
			JsonObject keyword = new JsonObject();
			keyword.addProperty("keyword", s);
			keyword.addProperty("occurance", sortedMap.get(s));
			jsa.add(keyword);
		}
		return jsa;
	}

}
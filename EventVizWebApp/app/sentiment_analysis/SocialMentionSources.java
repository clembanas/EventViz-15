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

public class SocialMentionSources {
	private Map<String, Integer> sources;

	public SocialMentionSources() {
		this.sources = new TreeMap<String, Integer>();
	}

	public Map<String, Integer> getSources() {
		return sources;
	}

	public void setSources(final Map<String, Integer> sources) {
		this.sources = sources;
	}

	public void addSource(final String source, final int occurrence) {
		this.sources.put(source, occurrence);
	}
	
	public Map<String, Integer> getSortedSources() {
		final List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(this.getSources().entrySet());
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

}
package data;

import java.util.Map;
import java.util.TreeMap;

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

}
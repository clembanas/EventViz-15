package data;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

public class SocialMentionUsers {
	private Map<String, Integer> users;

	public SocialMentionUsers() {
		this.users = new TreeMap<String, Integer>();
	}

	public Map<String, Integer> getUsers() {
		return users;
	}

	public void setUsers(final Map<String, Integer> users) {
		this.users = users;
	}

	public void addUser(final String user, final int occurrence) {
		this.users.put(user, occurrence);
	}

	public Map<String, Integer> getSortedUsers() {
		final List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(this.getUsers().entrySet());
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
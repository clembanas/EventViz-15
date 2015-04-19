package data;

import java.util.Map;
import java.util.TreeMap;

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

}
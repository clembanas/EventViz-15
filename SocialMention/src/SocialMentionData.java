import java.util.List;

public class SocialMentionData {
	private String title;
	private long timestamp;
	private int count;
	private List<SocialMentionItem> items;

	public SocialMentionData() {
		// TODO Auto-generated constructor stub
	}

	public SocialMentionData(final String title, final long timestamp, final int count, final List<SocialMentionItem> items) {
		this.title = title;
		this.timestamp = timestamp;
		this.count = count;
		this.items = items;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(final long timestamp) {
		this.timestamp = timestamp;
	}

	public int getCount() {
		return count;
	}

	public void setCount(final int count) {
		this.count = count;
	}

	public List<SocialMentionItem> getItems() {
		return items;
	}

	public void setItems(final List<SocialMentionItem> items) {
		this.items = items;
	}

}
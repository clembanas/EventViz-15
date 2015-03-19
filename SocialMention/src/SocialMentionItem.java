public class SocialMentionItem {
	private String id;
	private String title;
	private String description;
	private String link;
	private long timestamp;
	private String language;
	private String image;
	private String embed;
	private String user;
	private String user_id;
	private String user_image;
	private String user_link;
	private String domain;
	private String source;
	private String favicon;
	private String type;
	private String geo;

	public SocialMentionItem() {
		// TODO Auto-generated constructor stub
	}

	public SocialMentionItem(final String id, final String title, final String description, final String link, final long timestamp,
			final String language, final String image, final String embed, final String user, final String user_id, final String user_image,
			final String user_link, final String domain, final String source, final String favicon, final String type, final String geo) {
		this.id = id;
		this.title = title;
		this.description = description;
		this.link = link;
		this.timestamp = timestamp;
		this.language = language;
		this.image = image;
		this.embed = embed;
		this.user = user;
		this.user_id = user_id;
		this.user_image = user_image;
		this.user_link = user_link;
		this.domain = domain;
		this.source = source;
		this.favicon = favicon;
		this.type = type;
		this.geo = geo;
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public String getLink() {
		return link;
	}

	public void setLink(final String link) {
		this.link = link;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(final long timestamp) {
		this.timestamp = timestamp;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(final String language) {
		this.language = language;
	}

	public String getImage() {
		return image;
	}

	public void setImage(final String image) {
		this.image = image;
	}

	public String getEmbed() {
		return embed;
	}

	public void setEmbed(final String embed) {
		this.embed = embed;
	}

	public String getUser() {
		return user;
	}

	public void setUser(final String user) {
		this.user = user;
	}

	public String getUser_id() {
		return user_id;
	}

	public void setUser_id(final String user_id) {
		this.user_id = user_id;
	}

	public String getUser_image() {
		return user_image;
	}

	public void setUser_image(final String user_image) {
		this.user_image = user_image;
	}

	public String getUser_link() {
		return user_link;
	}

	public void setUser_link(final String user_link) {
		this.user_link = user_link;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(final String domain) {
		this.domain = domain;
	}

	public String getSource() {
		return source;
	}

	public void setSource(final String source) {
		this.source = source;
	}

	public String getFavicon() {
		return favicon;
	}

	public void setFavicon(final String favicon) {
		this.favicon = favicon;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public String getGeo() {
		return geo;
	}

	public void setGeo(final String geo) {
		this.geo = geo;
	}

}
package data;

public class SocialMentionData {
	private int score_strength;
	private String score_sentiment;
	private int score_passion;
	private int score_reach;

	private String last_mention;
	private SocialMentionSentiment sentiment;
	private SocialMentionKeywords keywords;
	private SocialMentionUsers users;
	private SocialMentionHashtags hashtags;
	private SocialMentionSources sources;

	public SocialMentionData(final int score_strength, final String score_sentiment, final int score_passion, final int score_reach,
			final String last_mention, final SocialMentionSentiment sentiment, final SocialMentionKeywords keywords, final SocialMentionUsers users,
			final SocialMentionHashtags hashtags, final SocialMentionSources sources) {
		this.score_strength = score_strength;
		this.score_sentiment = score_sentiment;
		this.score_passion = score_passion;
		this.score_reach = score_reach;
		this.last_mention = last_mention;
		this.sentiment = sentiment;
		this.keywords = keywords;
		this.users = users;
		this.hashtags = hashtags;
		this.sources = sources;
	}

	public int getScore_strength() {
		return score_strength;
	}

	public void setScore_strength(final int score_strength) {
		this.score_strength = score_strength;
	}

	public String getScore_sentiment() {
		return score_sentiment;
	}

	public void setScore_sentiment(final String score_sentiment) {
		this.score_sentiment = score_sentiment;
	}

	public int getScore_passion() {
		return score_passion;
	}

	public void setScore_passion(final int score_passion) {
		this.score_passion = score_passion;
	}

	public int getScore_reach() {
		return score_reach;
	}

	public void setScore_reach(final int score_reach) {
		this.score_reach = score_reach;
	}

	public String getLast_mention() {
		return last_mention;
	}

	public void setLast_mention(final String last_mention) {
		this.last_mention = last_mention;
	}

	public SocialMentionSentiment getSentiment() {
		return sentiment;
	}

	public void setSentiment(final SocialMentionSentiment sentiment) {
		this.sentiment = sentiment;
	}

	public SocialMentionKeywords getKeywords() {
		return keywords;
	}

	public void setKeywords(final SocialMentionKeywords keywords) {
		this.keywords = keywords;
	}

	public SocialMentionUsers getUsers() {
		return users;
	}

	public void setUsers(final SocialMentionUsers users) {
		this.users = users;
	}

	public SocialMentionHashtags getHashtags() {
		return hashtags;
	}

	public void setHashtags(final SocialMentionHashtags hashtags) {
		this.hashtags = hashtags;
	}

	public SocialMentionSources getSources() {
		return sources;
	}

	public void setSources(final SocialMentionSources sources) {
		this.sources = sources;
	}

}

package data;

public class SocialMentionSentiment {
	private int positive;
	private int neutral;
	private int negative;

	public SocialMentionSentiment(final int positive, final int neutral, final int negative) {
		this.positive = positive;
		this.neutral = neutral;
		this.negative = negative;
	}

	public int getPositive() {
		return positive;
	}

	public void setPositive(final int positive) {
		this.positive = positive;
	}

	public int getNeutral() {
		return neutral;
	}

	public void setNeutral(final int neutral) {
		this.neutral = neutral;
	}

	public int getNegative() {
		return negative;
	}

	public void setNegative(final int negative) {
		this.negative = negative;
	}

}
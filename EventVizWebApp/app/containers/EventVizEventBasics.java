package containers;

public class EventVizEventBasics {
	private String eventful_id;
	private float latitude;
	private float longitude;
	
	public EventVizEventBasics(String eventful_id, float latitude,
			float longitude) {
		this.eventful_id = eventful_id;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public String getEventful_id() {
		return eventful_id;
	}

	public void setEventful_id(String eventful_id) {
		this.eventful_id = eventful_id;
	}

	public float getLatitude() {
		return latitude;
	}

	public void setLatitude(float latitude) {
		this.latitude = latitude;
	}

	public float getLongitude() {
		return longitude;
	}

	public void setLongitude(float longitude) {
		this.longitude = longitude;
	}
	
}
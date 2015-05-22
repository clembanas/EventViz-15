package containers;

public class EventVizEvent {
	private String name;
	private String description;
	private String city;
	private String country;
	private String location;
	private String startTime;
	private int duration;
	
	public EventVizEvent(String name, String description, String city,
			String country, String location, String startTime, int duration) {
		this.name = name;
		this.description = description;
		this.city = city;
		this.country = country;
		this.location = location;
		this.startTime = startTime;
		this.duration = duration;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}
	
}
package containers;

public class EventVizLocation {
	private String name;
	private EventVizCity city;
	
	public EventVizLocation(String name, EventVizCity city) {
		this.name = name;
		this.city = city;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public EventVizCity getCity() {
		return city;
	}

	public void setCity(EventVizCity city) {
		this.city = city;
	}
	
}
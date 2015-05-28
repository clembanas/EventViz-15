package containers;

public class EventVizEvent {
	private int id;
	private String name;
	private String description;
	private String event_type;
	private int location_id;
	private String eventful_id;
	
	public EventVizEvent(int id, String name, String description,
			String event_type, int location_id, String eventful_id) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.event_type = event_type;
		this.location_id = location_id;
		this.eventful_id = eventful_id;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
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

	public String getEvent_type() {
		return event_type;
	}

	public void setEvent_type(String event_type) {
		this.event_type = event_type;
	}

	public int getLocation_id() {
		return location_id;
	}

	public void setLocation_id(int location_id) {
		this.location_id = location_id;
	}

	public String getEventful_id() {
		return eventful_id;
	}

	public void setEventful_id(String eventful_id) {
		this.eventful_id = eventful_id;
	}
	
}
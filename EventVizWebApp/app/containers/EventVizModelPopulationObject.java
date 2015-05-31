package containers;

import java.util.List;

public class EventVizModelPopulationObject {
	private EventVizEvent event;
	private EventVizLocation location;
	private List<EventVizBand> bands;
	
	public EventVizModelPopulationObject(EventVizEvent event,
			EventVizLocation location, List<EventVizBand> bands) {
		this.event = event;
		this.location = location;
		this.bands = bands;
	}

	public EventVizEvent getEvent() {
		return event;
	}

	public void setEvent(EventVizEvent event) {
		this.event = event;
	}

	public EventVizLocation getLocation() {
		return location;
	}

	public void setLocation(EventVizLocation location) {
		this.location = location;
	}

	public List<EventVizBand> getBands() {
		return bands;
	}

	public void setBands(List<EventVizBand> bands) {
		this.bands = bands;
	}
	
}
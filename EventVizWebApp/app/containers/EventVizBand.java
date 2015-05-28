package containers;

import java.util.List;

public class EventVizBand {
	private int id;
	private String name;
	private List<EventVizBandMember> members;
	private String dbpedia_resource;
	
	public EventVizBand(int id, String name, List<EventVizBandMember> members,
			String dbpedia_resource) {
		this.id = id;
		this.name = name;
		this.members = members;
		this.dbpedia_resource = dbpedia_resource;
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

	public List<EventVizBandMember> getMembers() {
		return members;
	}

	public void setMembers(List<EventVizBandMember> members) {
		this.members = members;
	}

	public String getDbpedia_resource() {
		return dbpedia_resource;
	}

	public void setDbpedia_resource(String dbpedia_resource) {
		this.dbpedia_resource = dbpedia_resource;
	}
	
}
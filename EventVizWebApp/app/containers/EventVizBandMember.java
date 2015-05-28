package containers;

public class EventVizBandMember {
	private int id;
	private String name;
	private String alternate_name;
	private String member_type;
	private String dbpedia_resource;
	
	public EventVizBandMember(int id, String name, String alternate_name,
			String member_type, String dbpedia_resource) {
		this.id = id;
		this.name = name;
		this.alternate_name = alternate_name;
		this.member_type = member_type;
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

	public String getAlternate_name() {
		return alternate_name;
	}

	public void setAlternate_name(String alternate_name) {
		this.alternate_name = alternate_name;
	}

	public String getMember_type() {
		return member_type;
	}

	public void setMember_type(String member_type) {
		this.member_type = member_type;
	}

	public String getDbpedia_resource() {
		return dbpedia_resource;
	}

	public void setDbpedia_resource(String dbpedia_resource) {
		this.dbpedia_resource = dbpedia_resource;
	}
	
}
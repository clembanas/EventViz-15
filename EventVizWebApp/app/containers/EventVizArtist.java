package containers;

public class EventVizArtist {
	private int id;
	private String name;
	private String alternate_name;
	private String dbpedia_resource;
	private String bandName;
	
	public EventVizArtist(int id, String name, String alternate_name,
			String dbpedia_resource, String bandName) {
		this.id = id;
		this.name = name;
		this.alternate_name = alternate_name;
		this.dbpedia_resource = dbpedia_resource;
		this.bandName = bandName;
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
	
	public String getDbpedia_resource() {
		return dbpedia_resource;
	}
	
	public void setDbpedia_resource(String dbpedia_resource) {
		this.dbpedia_resource = dbpedia_resource;
	}
	
	public String getBandName() {
		return bandName;
	}
	
	public void setBandName(String bandName) {
		this.bandName = bandName;
	}
	
}
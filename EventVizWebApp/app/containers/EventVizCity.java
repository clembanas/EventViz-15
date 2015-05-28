package containers;

public class EventVizCity {
	private String name;
	private String county;
	private String dbpediaURI;
	
	public EventVizCity(String name, String county, String dbpediaURI) {
		this.name = name;
		this.county = county;
		this.dbpediaURI = dbpediaURI;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCounty() {
		return county;
	}

	public void setCounty(String county) {
		this.county = county;
	}

	public String getDbpediaURI() {
		return dbpediaURI;
	}

	public void setDbpediaURI(String dbpediaURI) {
		this.dbpediaURI = dbpediaURI;
	}
}
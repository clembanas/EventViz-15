package containers;

public class EventVizCountry {
	private String name;
	private String dbpediaURI;
	
	public EventVizCountry(String name, String dbpediaURI) {
		this.name = name;
		this.dbpediaURI = dbpediaURI;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDbpediaURI() {
		return dbpediaURI;
	}

	public void setDbpediaURI(String dbpediaURI) {
		this.dbpediaURI = dbpediaURI;
	}
}

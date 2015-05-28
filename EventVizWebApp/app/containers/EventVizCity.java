package containers;

public class EventVizCity {
	private int id;
	private String name;
	private String region;
	private String county;
	private float latitude;
	private float longitude;
	private String dbpedia_resource;
	
	public EventVizCity(int id, String name, String region, String county,
			float latitude, float longitude, String dbpedia_resource) {
		this.id = id;
		this.name = name;
		this.region = region;
		this.county = county;
		this.latitude = latitude;
		this.longitude = longitude;
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

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getCounty() {
		return county;
	}

	public void setCounty(String county) {
		this.county = county;
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

	public String getDbpedia_resource() {
		return dbpedia_resource;
	}

	public void setDbpedia_resource(String dbpedia_resource) {
		this.dbpedia_resource = dbpedia_resource;
	}
	
}
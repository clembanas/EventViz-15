package containers;

public class EventVizCity {
	private int id;
	private String name;
	private String region;
	private String country;
	private double latitude;
	private double longitude;
	private String dbpedia_res_city;
	private String dbpedia_res_region;
	private String dbpedia_res_country;

    public EventVizCity(int id, String name, String region, String country, double latitude, double longitude, String dbpedia_res_city, String dbpedia_res_region, String dbpedia_res_country) {
        this.id = id;
        this.name = name;
        this.region = region;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.dbpedia_res_city = dbpedia_res_city;
        this.dbpedia_res_region = dbpedia_res_region;
        this.dbpedia_res_country = dbpedia_res_country;
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

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getDbpedia_res_city() {
        return dbpedia_res_city;
    }

    public void setDbpedia_res_city(String dbpedia_res_city) {
        this.dbpedia_res_city = dbpedia_res_city;
    }

    public String getDbpedia_res_region() {
        return dbpedia_res_region;
    }

    public void setDbpedia_res_region(String dbpedia_res_region) {
        this.dbpedia_res_region = dbpedia_res_region;
    }

    public String getDbpedia_res_country() {
        return dbpedia_res_country;
    }

    public void setDbpedia_res_country(String dbpedia_res_country) {
        this.dbpedia_res_country = dbpedia_res_country;
    }

}
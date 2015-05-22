package containers;

public class EventVizCity {
	private String name;
	private String county;
	private int population;
	
	public EventVizCity(String name, String county, int population) {
		this.name = name;
		this.county = county;
		this.population = population;
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

	public int getPopulation() {
		return population;
	}

	public void setPopulation(int population) {
		this.population = population;
	}
	
}
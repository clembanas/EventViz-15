/**
 * @author Bernhard Weber
 */

public class Main {
	
	//Debug settings
	public static final boolean DEBUG_DB = true;
	public static final boolean DEBUG_DB_RESULTS = true;
	public static final boolean DEBUG_SPARQL = false;
	public static final boolean DEBUG_SPARQL_QUERIES = true;
	public static final boolean DEBUG_SPARQL_RESULTS = true;
	public static final boolean DEBUG_SPARQL_NO_QUERY_STR = false;
	public static final boolean DEBUG_CRAWLER_MGR = true;
	//DBPedia endpoints
	public static final String[] DBPEDIA_ENDPOINTS = new String[]{"http://dbpedia.org/sparql", 
		"http://live.de.dbpedia.org/sparql"};
	//Database update interval in hours
	public static final int DB_UPDATE_INTERVAL = 120;

	
	public static void main(String[] args) 
	{
		//Setup debug settings
		DBConnection.DEBUG = DEBUG_DB;
		DBConnection.DEBUG_RESULTS = DEBUG_DB_RESULTS;
		SparqlQuery.DEBUG = DEBUG_SPARQL;
		SparqlQuery.DEBUG_QUERIES = DEBUG_SPARQL_QUERIES;
		SparqlQuery.DEBUG_RESULTS = DEBUG_SPARQL_RESULTS;
		SparqlQuery.DEBUG_NO_QUERY_STR = DEBUG_SPARQL_NO_QUERY_STR;
		CrawlerManager.DEBUG = DEBUG_CRAWLER_MGR;
		CrawlerBase.debug_crawlers(CrawlerBase.class, DBQueryBasedCrawler.class, 
			BandInfoCrawler.class, CityInfoCrawler.class);
		//Register crawler classes
		CrawlerManager.registerCrawler(BandInfoCrawler.class, 
			new Utils.Pair<String[], Integer>(DBPEDIA_ENDPOINTS, DB_UPDATE_INTERVAL));
		CrawlerManager.registerCrawler(CityInfoCrawler.class,  
			new Utils.Pair<String[], Integer>(DBPEDIA_ENDPOINTS, DB_UPDATE_INTERVAL));
		//Execute crawlers
		CrawlerManager.executeAll();
		CrawlerManager.shutdown();
	}
}

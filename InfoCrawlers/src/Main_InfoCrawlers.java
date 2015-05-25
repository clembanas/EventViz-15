/**
 * @author Bernhard Weber
 */

public class Main_InfoCrawlers {
	
	//Debug settings
	public static final boolean DEBUG_DB = false;
	public static final boolean DEBUG_DB_UPDATES = true;
	public static final boolean DEBUG_DB_RESULTS = true;
	public static final boolean DEBUG_SPARQL = false;
	public static final boolean DEBUG_SPARQL_QUERIES = true;
	public static final boolean DEBUG_SPARQL_RESULTS = true;
	public static final boolean DEBUG_SPARQL_NO_QUERY_STR = false;
	public static final boolean DEBUG_CRAWLER_MGR = true;
	//Database settings
	public static final Class<? extends DBConnection> DB_CONNECTION_CLASS = 
		DBConnection_MySQL.class;
	//Event Crawler settings
	public static final int EVENT_CRAWLER_MAX_DAYS = 30;	 //Max days to be crawled from today
	public static final int EVENT_CRAWLER_PAGE_SIZE = 100;	 //Each worker thread processes one page
	public static final int EVENT_CRAWLER_MAX_PAGES = 1000;  //Maximum count of pages 
															 //(Integer.MAX_VALUE = unlimited)	
	//Band- and City crawler settings
	public static final String[] DBPEDIA_ENDPOINTS = new String[]{"http://dbpedia.org/sparql", 
		"http://live.de.dbpedia.org/sparql"};
	public static final int BAND_INFO_CRAWLER_UPDATE_INT = 120;	 //Update interval in hours
	public static final int CITY_INFO_CRAWLER_UPDATE_INT = 120;	 //Update interval in hours

	
	public static void main(String[] args) 
	{
		//Setup debug settings
		DBConnection.DEBUG = DEBUG_DB;
		DBConnection.DEBUG_UPDATES = DEBUG_DB_UPDATES;
		DBConnection.DEBUG_QUERY_RESULTS = DEBUG_DB_RESULTS;
		SparqlQuery.DEBUG = DEBUG_SPARQL;
		SparqlQuery.DEBUG_QUERIES = DEBUG_SPARQL_QUERIES;
		SparqlQuery.DEBUG_RESULTS = DEBUG_SPARQL_RESULTS;
		SparqlQuery.DEBUG_NO_QUERY_STR = DEBUG_SPARQL_NO_QUERY_STR;
		CrawlerManager.DEBUG = DEBUG_CRAWLER_MGR;
		CrawlerBase.debug_crawlers(CrawlerBase.class, DBQueryBasedCrawler.class, 
			EventCrawler.class, BandInfoCrawler.class, CityInfoCrawler.class);
		//Setup database connection class
		DBConnection.DB_CONNECTION_CLASS = DB_CONNECTION_CLASS;
		//Register crawler classes
		CrawlerManager.registerCrawler(EventCrawler.class, 
			Utils.createTriple(EVENT_CRAWLER_MAX_DAYS, EVENT_CRAWLER_PAGE_SIZE, 
			EVENT_CRAWLER_MAX_PAGES));
		CrawlerManager.registerCrawler(BandInfoCrawler.class, 
			Utils.createPair(DBPEDIA_ENDPOINTS, BAND_INFO_CRAWLER_UPDATE_INT), EventCrawler.class);
		CrawlerManager.registerCrawler(CityInfoCrawler.class,  
			Utils.createPair(DBPEDIA_ENDPOINTS, CITY_INFO_CRAWLER_UPDATE_INT), EventCrawler.class);
		//Execute crawlers
		CrawlerManager.executeAll();
		CrawlerManager.shutdown();
	}
}

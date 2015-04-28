/**
 * @author Bernhard Weber
 */

public class Main {
	
	public static final boolean DEBUG_DB = true;
	public static final boolean DEBUG_DB_RESULTS = true;
	public static final boolean DEBUG_SPARQL = true;
	public static final boolean DEBUG_SPARQL_QUERIES = false;
	public static final boolean DEBUG_SPARQL_RESULTS = true;
	public static final boolean DEBUG_SPARQL_NO_QUERY_STR = true;
	public static final boolean DEBUG_CRAWLER_MGR = true;

	
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
			SparqlCrawlerBase.class, CityInfoCrawler.class);
		//Register crawler classes
		CrawlerManager.registerCrawler(BandInfoCrawler.class);
		CrawlerManager.registerCrawler(CityInfoCrawler.class);
		//Execute crawlers
		CrawlerManager.executeAll();
		CrawlerManager.shutdown();
	}
}

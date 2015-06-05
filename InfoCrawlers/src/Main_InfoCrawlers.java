/**
 * @author Bernhard Weber
 */

public class Main_InfoCrawlers {
	
	public static void main(String[] args) 
	{
		try {
			CrawlerConfig.load();
			//Setup debug settings
			if (CrawlerConfig.canDbgDB())
				DebugUtils.debugClass(DBConnector.class, 
					(CrawlerConfig.canDbgDBUpdates() ? DBConnector.DebugFlag.UPDATES.toInt() : 0) | 
					(CrawlerConfig.canDbgDBResults() ? DBConnector.DebugFlag.QUERY_RESULTS.toInt() : 
						0));
			if (CrawlerConfig.canDbgSparql())
				DebugUtils.debugClass(SparqlQuery.class, 
					(CrawlerConfig.canDbgSparqlQueries() ? SparqlQuery.DebugFlag.QUERIES.toInt() : 
						0) | 
					(CrawlerConfig.canDbgSparqlResults() ? 
						SparqlQuery.DebugFlag.QUERY_RESULTS.toInt() : 0) |
					(!CrawlerConfig.canDbgSparqlQueryString() ? 
						SparqlQuery.DebugFlag.NO_QUERY_STRING.toInt() : 0) |
					(!CrawlerConfig.canDbgSparqlQueryPrefix() ? 
						SparqlQuery.DebugFlag.NO_QUERY_PREFIXES.toInt() : 0));
			if (CrawlerConfig.canDbgCrawlerManger())
				DebugUtils.debugClass(CrawlerManager.class);
			if (CrawlerConfig.canDbgCrawlerBase())
				DebugUtils.debugClass(CrawlerBase.class);
			if (CrawlerConfig.canDbgJobBasedCrawler())
				DebugUtils.debugClass(JobBasedCrawler.class);
			if (CrawlerConfig.canDbgDBQueryBasedCrawler())
				DebugUtils.debugClass(DBQueryBasedCrawler.class);
			if (CrawlerConfig.canDbgSparqlBasedCrawler())
				DebugUtils.debugClass(SparqlBasedCrawler.class);
			if (CrawlerConfig.canDbgEventfulCrawler())
				DebugUtils.debugClass(EventfulCrawler.class);
			if (CrawlerConfig.canDbgBandInfosCrawler())
				DebugUtils.debugClass(BandInfoCrawler.class);
			if (CrawlerConfig.canDbgCityInfosCrawler())
				DebugUtils.debugClass(CityInfoCrawler.class);
			//Setup database connector class
			DBConnector.DB_CONNECTOR_CLASS = CrawlerConfig.getDBConnectorClass();
			//Register crawler classes
			CrawlerManager.registerCrawler(EventfulCrawler.class);
			CrawlerManager.registerCrawler(BandInfoCrawler.class, EventfulCrawler.class);
			CrawlerManager.registerCrawler(CityInfoCrawler.class, EventfulCrawler.class);
			//Execute crawlers
			CrawlerManager.executeAll();
			CrawlerManager.shutdown();
		}
		catch (Exception e) {
			System.out.println("ERROR: " + e.getMessage() + "\nExiting now!");
			e.printStackTrace();
		}
	}
}

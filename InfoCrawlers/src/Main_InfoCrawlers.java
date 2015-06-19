/**
 * @author Bernhard Weber
 */

public class Main_InfoCrawlers {
	
	private static void applyDebugSettings()
	{
		if (CrawlerConfig.canDbgDB())
			DebugUtils.debugClass(DBConnector.class, 
				(CrawlerConfig.canDbgDBUpdates() ? DBConnector.DebugFlag.UPDATES.toInt() : 0) | 
				(CrawlerConfig.canDbgDBResults() ? DBConnector.DebugFlag.QUERY_RESULTS.toInt() : 
					0));
		if (CrawlerConfig.canDbgRemoteObjMgr()) 
			DebugUtils.debugClass(RemoteObjectManager.class, 
				(CrawlerConfig.canDbgRemObjMgrRemoteObject() ? 
					RemoteObjectManager.DebugFlag.REMOTE_OBJECT.toInt() : 0) | 
				(CrawlerConfig.canDbgRemObjMgrConnection() ? 
					RemoteObjectManager.DebugFlag.CONNECTION.toInt() : 0) | 
				(CrawlerConfig.canDbgRemObjMgrMethodArgs() ? 
						RemoteObjectManager.DebugFlag.METHOD_ARGUMENTS.toInt() : 0));
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
		if (CrawlerConfig.getDbgThdMonitorInterval() > 0)
			DebugUtils.debugClass(ThreadMonitor.class, 
				(CrawlerConfig.getDbgWarnNoThdProgressTime() == 0 ? 0 : 
					ThreadMonitor.DebugFlag.WARN_NO_THD_PROGRESS.toInt()) | 
				(CrawlerConfig.getDbgThdLivenessInfoInterval() == 0 ? 0 : 
					ThreadMonitor.DebugFlag.THD_LIVENESS_INFO.toInt()) | 
				(CrawlerConfig.getDbgRuntimeInfoInterval() == 0 ? 0 : 
					ThreadMonitor.DebugFlag.RUNTIME_INFO.toInt()));
	}
	
	public static void main(String[] args) 
	{
		try {
			boolean isMasterNode = true;
			boolean execCrawlerNow = false;
			
			if (args.length > 0) {
				for (String arg: args) {
					if (arg.equalsIgnoreCase("slave"))
						isMasterNode = false;
					else if (arg.equalsIgnoreCase("now"))
						execCrawlerNow = true;
					else
						throw new Exception("Invalid argument '" + arg + "'!");
				}
			}
			CrawlerConfig.load();
			//Setup debug settings
			applyDebugSettings();
			//Register crawler classes
			CrawlerManager.registerCrawler(EventfulCrawler.class, 
				CrawlerConfig.getCrawlerEventfulHosts());
			CrawlerManager.registerCrawler(BandInfoCrawler.class, 
				CrawlerConfig.getBandInfoCrawlerHosts(), EventfulCrawler.class);
			CrawlerManager.registerCrawler(CityInfoCrawler.class, 
				CrawlerConfig.getCityInfoCrawlerHosts(), EventfulCrawler.class);
			//Execute crawlers
			if (CrawlerManager.start(isMasterNode)) {
				CrawlerManager.run(execCrawlerNow);
				CrawlerManager.shutdown();
			}
			else
				System.out.println("ERROR: Failed to start crawler manager!\nExiting now!");
		}
		catch (Throwable e) {
			System.out.println("ERROR: " + e.getMessage() + "\nExiting now!\n");
			e.printStackTrace();
		}
	}
}

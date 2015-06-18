/**
 * @author Bernhard Weber
 */
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


/**
 * Class used to access all Crawler specific settings
 */
public class CrawlerConfig {
	
	private static Properties props;
	
	private static InetAddress[] loadInetAddresses(String keyName)
	{
		try {
			String hosts = props.getProperty("crawler.eventful.hosts");
			if (hosts.isEmpty())
				return new InetAddress[0];
			
			String[] hostAddrs = hosts.split(",");
			InetAddress[] hostInetAddrs = new InetAddress[hostAddrs.length]; 
			
			for (int i = 0; i < hostAddrs.length; ++i)
				hostInetAddrs[i] = InetAddress.getByName(hostAddrs[i].trim());
			return hostInetAddrs;
		}
		catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage());
		} 
	}
	
	public static synchronized void load() throws Exception
	{
		if (props == null) {
			props = new Properties();
			BufferedInputStream stream = new BufferedInputStream(new FileInputStream(
					 							 "crawler_config.properties"));
			
			props.load(stream);
			stream.close();
		}
	}

	public static boolean canDbgDB()
	{	
		return Boolean.valueOf(props.getProperty("debug.database"));
	}

	public static boolean canDbgDBUpdates()
	{	
		return Boolean.valueOf(props.getProperty("debug.database.updates"));
	}

	public static boolean canDbgDBResults()
	{	
		return Boolean.valueOf(props.getProperty("debug.database.results"));
	}
	
	public static boolean canDbgRemoteObjMgr()
	{	
		return Boolean.valueOf(props.getProperty("debug.remote_obj_mgr"));
	}

	public static boolean canDbgRemObjMgrConnection()
	{	
		return Boolean.valueOf(props.getProperty("debug.remote_obj_mgr.connection"));
	}

	public static boolean canDbgRemObjMgrRemoteObject()
	{	
		return Boolean.valueOf(props.getProperty("debug.remote_obj_mgr.remote_object"));
	}
	
	public static boolean canDbgRemObjMgrMethodArgs()
	{	
		return Boolean.valueOf(props.getProperty("debug.remote_obj_mgr.method_args"));
	}

	public static boolean canDbgSparql()
	{	
		return Boolean.valueOf(props.getProperty("debug.sparql"));
	}

	public static boolean canDbgSparqlQueries()
	{	
		return Boolean.valueOf(props.getProperty("debug.sparql.queries"));
	}

	public static boolean canDbgSparqlResults()
	{	
		return Boolean.valueOf(props.getProperty("debug.sparql.results"));
	}

	public static boolean canDbgSparqlQueryString()
	{	
		return !Boolean.valueOf(props.getProperty("debug.sparql.no_query_string"));
	}

	public static boolean canDbgSparqlQueryPrefix()
	{	
		return !Boolean.valueOf(props.getProperty("debug.sparql.no_query_prefix"));
	}

	public static boolean canDbgCrawlerManger()
	{	
		return Boolean.valueOf(props.getProperty("debug.crawler.manger"));
	}

	public static boolean canDbgCrawlerBase()
	{	
		return Boolean.valueOf(props.getProperty("debug.crawler.base"));
	}

	public static boolean canDbgDBQueryBasedCrawler()
	{	
		return Boolean.valueOf(props.getProperty("debug.crawler.db_query_based"));
	}

	public static boolean canDbgSparqlBasedCrawler()
	{	
		return Boolean.valueOf(props.getProperty("debug.crawler.sparql_based"));
	}

	public static boolean canDbgEventfulCrawler()
	{	
		return Boolean.valueOf(props.getProperty("debug.crawler.eventful"));
	}

	public static boolean canDbgCityInfosCrawler()
	{	
		return Boolean.valueOf(props.getProperty("debug.crawler.city_infos"));
	}

	public static boolean canDbgBandInfosCrawler()
	{	
		return Boolean.valueOf(props.getProperty("debug.crawler.band_infos"));
	}
	
	public static int getDbgMaxLogs()
	{
		return Integer.valueOf(props.getProperty("debug.max_db_logs"));
	}
	
	public static String getDbgLogFile()
	{
		return props.getProperty("debug.exception_file");
	}
	
	public static String getDbgExceptionLogFile()
	{
		return props.getProperty("debug.exception_log_file");
	}
	
	public static int getDbgThdMonitorInterval()
	{
		return Integer.valueOf(props.getProperty("debug.thread_monitor.interval"));
	}
	
	public static int getDbgWarnNoThdProgressTime()
	{
		return Integer.valueOf(props.getProperty("debug.thread_monitor.warn_no_thd_progress"));
	}
	
	public static int getDbgThdLivenessInfoInterval()
	{
		return Integer.valueOf(props.getProperty("debug.thread_monitor.thd_liveness_info"));
	}
	
	public static int getDbgRuntimeInfoInterval()
	{
		return Integer.valueOf(props.getProperty("debug.thread_monitor.runtime_info"));
	}

	@SuppressWarnings("deprecation")
	public static Class<? extends DBConnector> getDBConnectorClass()
	{	
		String connClassName = props.getProperty("database.connector_class");
		
		if (connClassName.equalsIgnoreCase(DBConnector_MySQL.class.getName()))
			return DBConnector_MySQL.class;
		if (connClassName.equalsIgnoreCase(DBConnector_Derby.class.getName()))
			return DBConnector_Derby.class;
		if (connClassName.equalsIgnoreCase(DBConnector_Impala.class.getName()))
			return DBConnector_Impala.class;
		throw new IllegalArgumentException("Unknown connector class '" + connClassName + "'!");
	}
	
	public static int getRemoteObjMgrPort() 
	{
		return Integer.valueOf(props.getProperty("remote_obj_mgr.port"));
	}
	
	public static InetAddress getCrawlerMasterHost()
	{
		try {
			return InetAddress.getByName(props.getProperty("crawler.master.host"));
		}
		catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage());
		} 
	}
	
	public static InetAddress getCrawlerJobControllerHost() 
	{
		try {
			if (props.containsKey("crawler.job_controller.host"))
				return InetAddress.getByName(props.getProperty("crawler.job_controller.host"));
			return getCrawlerMasterHost();
		}
		catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	public static int getEventfulCrawlerMaxDays()
	{	
		return Integer.valueOf(props.getProperty("crawler.eventful.max_days"));
	}

	public static int getEventfulCrawlerPageSize()
	{	
		return Integer.valueOf(props.getProperty("crawler.eventful.page_size"));
	}

	public static int getEventfulCrawlerMaxPages()
	{	
		return Integer.valueOf(props.getProperty("crawler.eventful.max_pages"));
	}

	public static int getEventfulCrawlerWorkerThdCount()
	{	
		return Integer.valueOf(props.getProperty("crawler.eventful.worker_thd_count"));
	}

	public static String getEventfulCrawlerUname()
	{	
		return props.getProperty("crawler.eventful.uname");
	}

	public static String getEventfulCrawlerPword()
	{	
		return props.getProperty("crawler.eventful.pword");
	}

	public static String getEventfulCrawlerApikey()
	{	
		return props.getProperty("crawler.eventful.apikey");
	}

	public static String getEventfulCrawlerCategory()
	{	
		return props.getProperty("crawler.eventful.category");
	}
	
	public static InetAddress[] getCrawlerEventfulHosts()
	{
		return loadInetAddresses("crawler.eventful.hosts");
	}

	public static String[] getSparqlBasedCrawlerDBPediaEndpoints()
	{	
		List<String> result = new ArrayList<String>();
		
		for (int i = 1; i < 100; ++i) {
			if (props.containsKey("crawler.sparql_based.dbpedia_endpoint" + i))
				result.add(props.getProperty("crawler.sparql_based.dbpedia_endpoint" + i));
		}
		return result.toArray(new String[result.size()]);
	}

	public static int getSparqlBasedCrawlerMaxQueryRetries()
	{	
		return Integer.valueOf(props.getProperty("crawler.sparql_based.max_query_retries"));
	}

	public static int getSparqlBasedCrawlerQueryRetryDelay()
	{	
		return Integer.valueOf(props.getProperty("crawler.sparql_based.query_retry_delay"));
	}

	public static int getSparqlBasedCrawlerDefQueryLimit()
	{	
		return Integer.valueOf(props.getProperty("crawler.sparql_based.def_query_limit"));
	}

	public static int getSparqlBasedCrawlerMaxCacheSize()
	{	
		return Integer.valueOf(props.getProperty("crawler.sparql_based.max_cache_size"));
	}

	public static int getCityInfoCrawlerUpdateInterval()
	{	
		return Integer.valueOf(props.getProperty("crawler.city_info.update_interval"));
	}

	public static int getCityInfoCrawlerPageSize()
	{	
		return Integer.valueOf(props.getProperty("crawler.city_info.page_size"));
	}

	public static int getCityInfoCrawlerWorkerThdCount()
	{	
		return Integer.valueOf(props.getProperty("crawler.city_info.worker_thd_count"));
	}
	
	public static InetAddress[] getCityInfoCrawlerHosts()
	{
		return loadInetAddresses("crawler.city_info.hosts");
	}

	public static int getBandInfoCrawlerUpdateInterval()
	{	
		return Integer.valueOf(props.getProperty("crawler.band_info.update_interval"));
	}

	public static int getBandInfoCrawlerPageSize()
	{
		return Integer.valueOf(props.getProperty("crawler.band_info.page_size"));
	}

	public static int getBandInfoCrawlerWorkerThdCount()
	{	
		return Integer.valueOf(props.getProperty("crawler.band_info.worker_thd_count"));
	}
	
	public static InetAddress[] getBandInfoCrawlerHosts()
	{
		return loadInetAddresses("crawler.band_info.hosts");
	}
}

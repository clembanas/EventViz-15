/**
 * @author Bernhard Weber
 */
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


/**
 * Class used to access all Crawler specific settings
 */
public class CrawlerConfig {
	
	private static Properties props;
	
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

	public static boolean canDbgJobBasedCrawler()
	{	
		return Boolean.valueOf(props.getProperty("debug.crawler.job_based"));
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

	public static String[] getSparqlBasedCrawlerDBPediaEndpoints()
	{	
		List<String> result = new ArrayList<String>();
		
		for (int i = 1; i < 100; ++i) {
			if (props.containsKey("crawler.sparql_based.dbpedia_endpoint" + i))
				result.add(props.getProperty("crawler.sparql_based.dbpedia_endpoint" + i));
		}
		return (String[])result.toArray();
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
}

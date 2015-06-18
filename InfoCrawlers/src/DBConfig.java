/**
 * @author Bernhard Weber
 */
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.Properties;


/**
 * Class used to access all DB specific settings
 */
public class DBConfig {
	
	public static enum DBType {
		MYSQL("mysql"), 
		IMPALA("impala"), 
		DERBY("derby");
		
		private final String dbType;
		
		private DBType(String dbType)
		{
			this.dbType = dbType;
		}
		
		public String toString()
		{
			return dbType;
		}
	}
	
	private static DBType dbType;
	private static Properties props;
	
	public static synchronized void load() throws Exception
	{
		if (props == null) {
			props = new Properties();
			BufferedInputStream stream = new BufferedInputStream(new FileInputStream(
					 							 "db_config.properties"));
			
			props.load(stream);
			stream.close();
		}
	}

	public static void setDBType(DBType dbType)
	{
		DBConfig.dbType = dbType;
	}
	
	public static DBType getDBType()
	{
		return dbType;
	}
	
	public static String getDBName()
	{
		return props.getProperty(dbType.toString() + ".db_name");
	}
	
	public static String getDBHost()
	{
		return props.getProperty(dbType.toString() + ".db_host");
	}
	
	public static String getDBUser()
	{
		return props.getProperty(dbType.toString() + ".db_user");
	}
	
	public static String getDBPword()
	{
		return props.getProperty(dbType.toString() + ".db_pword");
	}
	
	public static String getDBLocation()
	{
		return props.getProperty(dbType.toString() + ".db_location");
	}

	public static int getMaxLenCrawlerDbgLogHost() 
	{
		return Integer.valueOf(props.getProperty("maxlen.crawler_debug_info_logs.host"));
	}

	public static int getMaxLenCrawlerDbgLogClassPath() 
	{
		return Integer.valueOf(props.getProperty("maxlen.crawler_debug_info_logs.class_path"));
	}

	public static int getMaxLenCrawlerDbgLogInfo() 
	{
		return Integer.valueOf(props.getProperty("maxlen.crawler_debug_info_logs.info"));
	}

	public static int getMaxLenCrawlerExceptLogHost() 
	{
		return Integer.valueOf(props.getProperty("maxlen.crawler_exception_logs.host"));
	}

	public static int getMaxLenCrawlerExceptLogClassPath() 
	{
		return Integer.valueOf(props.getProperty("maxlen.crawler_exception_logs.class_path"));
	}

	public static int getMaxLenCrawlerExceptLogInfo() 
	{
		return Integer.valueOf(props.getProperty("maxlen.crawler_exception_logs.info"));
	}

	public static int getMaxLenCrawlerExceptLogMsg() 
	{
		return Integer.valueOf(props.getProperty("maxlen.crawler_exception_logs.msg"));
	}

	public static int getMaxLenCrawlerExceptLogClass() 
	{
		return Integer.valueOf(props.getProperty("maxlen.crawler_exception_logs.class"));
	}

	public static int getMaxLenCrawlerExceptLogStack() 
	{
		return Integer.valueOf(props.getProperty("maxlen.crawler_exception_logs.stack"));
	}

	public static int getMaxLenCrawlerInfoClass() 
	{
		return Integer.valueOf(props.getProperty("maxlen.crawler_info.class"));
	}
	
	public static int getMaxLenCrawlerInfoJobsPerHosts() 
	{
		return Integer.valueOf(props.getProperty("maxlen.crawler_info.jobs_per_hosts"));
	}

	public static int getMaxLenCrawlerInfoSummary() 
	{
		return Integer.valueOf(props.getProperty("maxlen.crawler_info.summary"));
	}

	public static int getMaxLenCityName() 
	{
		return Integer.valueOf(props.getProperty("maxlen.cities.name"));
	}

	public static int getMaxLenCityRegion() 
	{
		return Integer.valueOf(props.getProperty("maxlen.cities.region"));
	}

	public static int getMaxLenCityCountry() 
	{
		return Integer.valueOf(props.getProperty("maxlen.cities.country"));
	}

	public static int getMaxLenCityDBPediaRes() 
	{
		return Integer.valueOf(props.getProperty("maxlen.cities.dbpedia_res"));
	}

	public static int getMaxLenLocationName() 
	{
		return Integer.valueOf(props.getProperty("maxlen.locations.name"));
	}

	public static int getMaxLenEventName() 
	{
		return Integer.valueOf(props.getProperty("maxlen.events.name"));
	}

	public static int getMaxLenEventDesc() 
	{
		return Integer.valueOf(props.getProperty("maxlen.events.desc"));
	}

	public static int getMaxLenEventType() 
	{
		return Integer.valueOf(props.getProperty("maxlen.events.type"));
	}

	public static int getMaxLenEventEventfulId() 
	{
		return Integer.valueOf(props.getProperty("maxlen.events.eventful_id"));
	}

	public static int getMaxLenBandName() 
	{
		return Integer.valueOf(props.getProperty("maxlen.bands.name"));
	}

	public static int getMaxLenBandDBPediaRes() 
	{
		return Integer.valueOf(props.getProperty("maxlen.bands.dbpedia_res"));
	}

	public static int getMaxLenArtistName() 
	{
		return Integer.valueOf(props.getProperty("maxlen.artists.name"));
	}

	public static int getMaxLenArtistAltName() 
	{
		return Integer.valueOf(props.getProperty("maxlen.artists.alt_name"));
	}

	public static int getMaxLenArtistDBPediaRes() 
	{
		return Integer.valueOf(props.getProperty("maxlen.artists.dbpedia_res"));
	}
}

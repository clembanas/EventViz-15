/**
 * @author Bernhard Weber
 */
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.locks.*;


/**
 * Actual DBConnector implementation for working with Derby databases
 * 
 * @deprecated (At the moment up-to-date but won't be updated in future)
 */
public class DBConnector_Derby extends DBConnector_SQLConform {
	
	public static final String DRIVER_NAME = "org.apache.derby.jdbc.EmbeddedDriver";
	public static String CONNECTION_STR = "jdbc:derby:temp/derby/Events/db;create=true";
	
	
	/**
	 * Actual primary key implementation for Derby databases (primary key is of type INTEGER)
	 */
	public static class DerbyPrimaryKey extends PrimaryKey {
		
		public DerbyPrimaryKey(Object obj) throws Exception
		{
			if (obj.getClass() != Integer.class)
				throw new IllegalArgumentException("Derby primary keys must be of type INTEGER!");
			this.data = obj;
		}
		
		public DerbyPrimaryKey(PrimaryKey primKey) throws Exception
		{
			if (primKey.data.getClass() != Integer.class)
				throw new IllegalArgumentException("Derby primary keys must be of type INTEGER!");
			this.data = primKey.data;
		}
		
		public DerbyPrimaryKey(String str) throws Exception
		{
			this.data = Integer.valueOf(str);
		}
		
		public DerbyPrimaryKey(ResultSet resSet) throws Exception
		{
			this.data = resSet.getInt(1);
		}
		
		public void addToStatement(PreparedStatement stmt, int idx) throws Exception
		{
			stmt.setInt(idx, (Integer)data);
		}
	}

	
	private Lock updateLock = new ReentrantLock();
	
	protected DBConnector_Derby() throws Exception	 //Singleton
	{
		PrimaryKey.PrimaryKeyClass = DerbyPrimaryKey.class;
		DBConfig.load();
		DBConfig.setDBType(DBConfig.DBType.MYSQL);
		CONNECTION_STR = String.format("jdbc:derby:%s;create=true", DBConfig.getDBLocation()); 
	}
	
	protected String getDriverName()
	{
		return DRIVER_NAME;
	}
	
	protected String getConnectionStr()
	{
		return CONNECTION_STR;
	}
	
	protected boolean queryPagingSupported()
	{
		return false;
	}
	
	protected void beginUpdate() throws Exception 
	{
		updateLock.lock();
		dbConn.setAutoCommit(false);
	}
	
	protected void endUpdate() throws Exception 
	{
		dbConn.commit();
		dbConn.setAutoCommit(true);
		updateLock.unlock();
	}
	
	protected void cancelUpdate() throws Exception 
	{
		try {
			try {
				dbConn.rollback();
			}
			finally {
				dbConn.setAutoCommit(true);
			}
		}
		finally {
			updateLock.unlock();
		}
	}
	
	protected String getStmtCreateTblDebugInfoLogs()
	{
		return "CREATE TABLE Crawler_debug_info_logs(" +
				   "ts TIMESTAMP, " +
				   "hostname VARCHAR(" + MAX_LEN_CRAWLER_DEBUG_LOG_HOST + "), " +
				   "thread_id BIGINT, " +
				   "class_path VARCHAR(" + MAX_LEN_CRAWLER_DEBUG_LOG_CLASS_PATH + "), " +
				   "info VARCHAR(" + MAX_LEN_CRAWLER_DEBUG_LOG_INFO + "))";
	}
	
	protected String getStmtCreateTblExceptionLogs()
	{
		return "CREATE TABLE Crawler_exception_logs(" +
				   "ts TIMESTAMP, " +
				   "hostname VARCHAR(" + MAX_LEN_CRAWLER_EXCEPT_LOG_HOST + "), " +
				   "thread_id BIGINT, " +
				   "class_path VARCHAR(" + MAX_LEN_CRAWLER_EXCEPT_LOG_CLASS_PATH + "), " +
				   "info VARCHAR(" + MAX_LEN_CRAWLER_EXCEPT_LOG_INFO + "), " +
				   "message VARCHAR(" + MAX_LEN_CRAWLER_EXCEPT_LOG_MSG + "), " + 
				   "exception_class VARCHAR(" + MAX_LEN_CRAWLER_EXCEPT_LOG_CLASS + "), " +
				   "stack_trace VARCHAR(" + MAX_LEN_CRAWLER_EXCEPT_LOG_STACK + "))";
	}
	
	protected String getStmtCreateTblCities()
	{
		return "CREATE TABLE Cities(" +
				   "ID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, " +
				   "INCREMENT BY 1), " +
				   "NAME VARCHAR(" + MAX_LEN_CITY_NAME + ") NOT NULL, " +
				   "REGION VARCHAR(" + MAX_LEN_CITY_REGION + "), " +
				   "COUNTRY VARCHAR(" + MAX_LEN_CITY_COUNTRY + "), " +
				   "LONGITUDE FLOAT, " +
				   "LATITUDE FLOAT, " +
				   "CITY_CRAWLER_TS TIMESTAMP, " +
				   "DBPEDIA_RES_CITY VARCHAR(" + MAX_LEN_CITY_DBPEDIA_RES + ")," +
				   "DBPEDIA_RES_REGION VARCHAR(" + MAX_LEN_CITY_DBPEDIA_RES + ")," +
				   "DBPEDIA_RES_COUNTRY VARCHAR(" + MAX_LEN_CITY_DBPEDIA_RES + "))";
	}
	
	protected String getStmtCreateTblLocations()
	{
		return "CREATE TABLE Locations(" +
				   "ID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, " +
				   "INCREMENT BY 1)," +
				   "NAME VARCHAR(" + MAX_LEN_LOCATION_NAME + ") NOT NULL," +
				   "LONGITUDE FLOAT NOT NULL," +
				   "LATITUDE FLOAT NOT NULL," +
				   "CITY_ID INTEGER NOT NULL)";
	}
	
	protected String getStmtCreateTblEvents()
	{
		return "CREATE TABLE Events(" +
				   "ID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, " +
				   "INCREMENT BY 1)," +
				   "NAME VARCHAR(" + MAX_LEN_EVENT_NAME + ") NOT NULL," +
				   "DESCRIPTION VARCHAR(" + MAX_LEN_EVENT_DESC + ")," +
				   "EVENT_TYPE VARCHAR(" + MAX_LEN_EVENT_TYPE + ")," +
				   "start_time DATE, " +
				   "end_time DATE, " +
				   "EVENTFUL_ID VARCHAR(" + MAX_LEN_EVENT_EVENTFUL_ID + ")," +
				   "LOCATION_ID INTEGER NOT NULL)";
	}

	protected String getStmtCreateTblBands()
	{
		return "CREATE TABLE Bands(" +
				   "ID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, " +
				   "INCREMENT BY 1)," +
				   "NAME VARCHAR(" + MAX_LEN_BAND_NAME + ") NOT NULL, " +
				   "BAND_CRAWLER_TS TIMESTAMP, " +
				   "DBPEDIA_RESOURCE VARCHAR(" + MAX_LEN_BAND_DBPEDIA_RES + "))";
	}

	protected String getStmtCreateTblArtists()
	{
		return "CREATE TABLE Artists(" +
				   "ID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, " +
				   "INCREMENT BY 1)," +
				   "NAME VARCHAR(" + MAX_LEN_ARTIST_NAME + ") NOT NULL, " +
				   "ALTERNATE_NAME VARCHAR(" + MAX_LEN_ARTIST_ALT_NAME + "), " +
				   "DBPEDIA_RESOURCE VARCHAR(" + MAX_LEN_ARTIST_DBPEDIA_RES + "))";
	}

	protected String getStmtCreateTblEventPerformers()
	{
		return "CREATE TABLE EventPerformers(" +
				   "EVENT_ID INTEGER NOT NULL," +
				   "BAND_ID INTEGER NOT NULL)";
	}

	protected String getStmtCreateTblBandMembers()
	{
		return "CREATE TABLE BandMembers(" +
				   "ARTIST_ID INTEGER NOT NULL," +
				   "BAND_ID INTEGER NOT NULL, " +
				   "MEMBER_TYPE CHAR)";
	}
	
	protected String getStmtIncompleteBandsCount()
	{
		return "SELECT COUNT(id) FROM Bands WHERE " +
				   "band_crawler_ts IS NULL OR " +
				   "{fn TIMESTAMPDIFF(SQL_TSI_HOUR, band_crawler_ts, ?)} >= ?";
	}

	protected String getStmtIncompleteBands()
	{
		return "SELECT id, name FROM Bands WHERE " +
				   "band_crawler_ts IS NULL OR " +
				   "{fn TIMESTAMPDIFF(SQL_TSI_HOUR, band_crawler_ts, ?)} >= ?";
	}
	
	protected String getStmtIncompleteCitiesCount()
	{
		return "SELECT COUNT(id) FROM Cities " +
				   "WHERE city_crawler_ts IS NULL OR " +
				   "{fn TIMESTAMPDIFF(SQL_TSI_HOUR, city_crawler_ts, ?)} >= ?";
	}
	
	protected String getStmtIncompleteCities()
	{
		return "SELECT id, name, region, country FROM Cities " +
				   "WHERE city_crawler_ts IS NULL OR " +
				   "{fn TIMESTAMPDIFF(SQL_TSI_HOUR, city_crawler_ts, ?)} >= ?";
	}
}

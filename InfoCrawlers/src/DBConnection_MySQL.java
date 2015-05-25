/**
 * @author Bernhard Weber
 */
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.locks.*;


/**
 * Actual DBConnection implementation for working with MySQL databases
 */
public class DBConnection_MySQL extends DBConnection_SQLConform {
	
	/**
	 * Actual primary key implementation for MySQL databases (primary key is of type INTEGER)
	 */
	public static class MySQLPrimaryKey extends PrimaryKey {
		
		public MySQLPrimaryKey(Object obj) throws Exception
		{
			if (obj.getClass() != Integer.class)
				throw new IllegalArgumentException("MySQL primary keys must be of type INTEGER!");
			this.data = obj;
		}
		
		public MySQLPrimaryKey(PrimaryKey primKey) throws Exception
		{
			if (primKey.data.getClass() != Integer.class)
				throw new IllegalArgumentException("MySQL primary keys must be of type INTEGER!");
			this.data = primKey.data;
		}
		
		public MySQLPrimaryKey(String str) throws Exception
		{
			this.data = Integer.valueOf(str);
		}
		
		public MySQLPrimaryKey(ResultSet resSet) throws Exception
		{
			this.data = resSet.getInt(1);
		}
		
		public void addToStatement(PreparedStatement stmt, int idx) throws Exception
		{
			stmt.setInt(idx, (Integer)data);
		}
	}
	
	public static final String DB_NAME = "EventViz15";
	public static final String DRIVER_NAME = "com.mysql.jdbc.Driver";
	public static final String CONNECTION_STR = "jdbc:mysql://127.0.0.1/" + DB_NAME + "?" + 
			"user=EventVizUser&password=e1V2i3Z";
	//FIXME
//	public static final String CONNECTION_STR = "jdbc:mysql://138.232.65.248/EventViz15?" + 
//													"user=EventVizUser&password=e1V2i3Z";
	
	private Lock updateLock = new ReentrantLock();
	
	protected DBConnection_MySQL()	 //Singleton
	{
		PrimaryKey.PrimaryKeyClass = MySQLPrimaryKey.class;
	}
	
	protected boolean tableExists(Statement stmt, String tableName) throws Exception 
	{
		DatabaseMetaData dbMeta = dbConn.getMetaData();
		ResultSet resSet = dbMeta.getTables(DB_NAME, null, null, null);
		
		while (resSet.next()) {
			if (resSet.getString(3).equalsIgnoreCase(tableName))
				return true;
		}
		return false;
	}
	
	protected String getStmtCreateTblCities()
	{
		return "CREATE TABLE Cities(" +
				   "id INT NOT NULL AUTO_INCREMENT, " +
				   "name VARCHAR(" + MAX_LEN_CITY_NAME + ") NOT NULL, " +
				   "region VARCHAR(" + MAX_LEN_CITY_REGION + "), " +
				   "country VARCHAR(" + MAX_LEN_CITY_COUNTRY + "), " +
				   "longitude FLOAT, " +
				   "latitude FLOAT, " +
				   "city_crawler_ts TIMESTAMP NULL, " +
				   "dbpedia_resource VARCHAR(" + MAX_LEN_CITY_DBPEDIA_RES + ")," +
				   "PRIMARY KEY (id))";
	}
	
	protected String getStmtCreateTblLocations()
	{
		return "CREATE TABLE Locations(" +
				   "id INT NOT NULL AUTO_INCREMENT," +
				   "name VARCHAR(" + MAX_LEN_LOCATION_NAME + ") NOT NULL," +
				   "longitude FLOAT NOT NULL," +
				   "latitude FLOAT NOT NULL," +
				   "city_id INT NOT NULL," +
				   "PRIMARY KEY (id))";
	}
	
	protected String getStmtCreateTblEvents()
	{
		return "CREATE TABLE Events(" +
				   "id INT NOT NULL AUTO_INCREMENT," +
				   "name VARCHAR(" + MAX_LEN_EVENT_NAME + ") NOT NULL," +
				   "description VARCHAR(" + MAX_LEN_EVENT_DESC + ")," +
				   "event_type VARCHAR(" + MAX_LEN_EVENT_TYPE + ")," +
				   "eventful_id VARCHAR(" + MAX_LEN_EVENT_EVENTFUL_ID + ")," +
				   "location_id INT NOT NULL," +
				   "PRIMARY KEY (id))";
	}

	protected String getStmtCreateTblBands()
	{
		return "CREATE TABLE Bands(" +
				   "id INT NOT NULL AUTO_INCREMENT," +
				   "name VARCHAR(" + MAX_LEN_BAND_NAME + ") NOT NULL, " +
				   "band_crawler_ts TIMESTAMP NULL, " +
				   "dbpedia_resource VARCHAR(" + MAX_LEN_BAND_DBPEDIA_RES + ")," +
				   "PRIMARY KEY (id))";
	}

	protected String getStmtCreateTblArtists()
	{
		return "CREATE TABLE Artists(" +
				   "id INT NOT NULL AUTO_INCREMENT," +
				   "name VARCHAR(" + MAX_LEN_ARTIST_NAME + ") NOT NULL, " +
				   "alternate_name VARCHAR(" + MAX_LEN_ARTIST_ALT_NAME + "), " +
				   "dbpedia_resource VARCHAR(" + MAX_LEN_ARTIST_DBPEDIA_RES + ")," +
				   "PRIMARY KEY (id))";
	}

	protected String getStmtCreateTblEventPerformers()
	{
		return "CREATE TABLE EventPerformers(" +
				   "event_id INT NOT NULL," +
				   "band_id INT NOT NULL)";
	}

	protected String getStmtCreateTblBandMembers()
	{
		return "CREATE TABLE BandMembers(" +
				   "artist_id INT NOT NULL," +
				   "band_id INT NOT NULL, " +
				   "member_type CHAR)";
	}
	
	protected String getStmtCreateTblExceptionLogs()
	{
		return "CREATE TABLE Crawler_exception_logs(" +
				   "city_crawler_ts TIMESTAMP NULL, " +
				   "message VARCHAR(" + MAX_LEN_CRAWLER_EXCEPT_LOG_MSG + "), " + 
				   "exception_class VARCHAR(" + MAX_LEN_CRAWLER_EXCEPT_LOG_CLASS + "), " +
				   "stack_trace VARCHAR(" + MAX_LEN_CRAWLER_EXCEPT_LOG_STACK + "))";
	}
	
	protected String getStmtCreateTblCrawlerInfos()
	{
		return "CREATE TABLE Crawler_infos(" +
				   "crawler_class VARCHAR(" + MAX_LEN_CRAWLER_INFO_CLASS + "), " +
				   "started TIMESTAMP NULL, " + 
				   "finished TIMESTAMP NULL, " +
				   "progress INT, " +
				   "summary VARCHAR(" + MAX_LEN_CRAWLER_INFO_SUMMARY + "))";
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
		return true;
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
	
	protected String getStmtIncompleteBandsCount()
	{
		return "SELECT COUNT(id) FROM Bands WHERE " +
				   "band_crawler_ts IS NULL OR " +
				   "unix_timestamp(?) - unix_timestamp(band_crawler_ts) >= ? * 3600";
	}

	protected String getStmtIncompleteBands()
	{
		return "SELECT id, name FROM Bands WHERE " +
				   "band_crawler_ts IS NULL OR " +
				   "unix_timestamp(?) - unix_timestamp(band_crawler_ts) >= ? * 3600 LIMIT ?, ?";
	}
	
	protected String getStmtIncompleteCitiesCount()
	{
		return "SELECT COUNT(id) FROM Cities " +
				   "WHERE city_crawler_ts IS NULL OR " +
				   "unix_timestamp(?) - unix_timestamp(city_crawler_ts) >= ? * 3600";
	}
	
	protected String getStmtIncompleteCities()
	{
		return "SELECT id, name, region, country FROM Cities " +
				   "WHERE city_crawler_ts IS NULL OR " +
				   "unix_timestamp(?) - unix_timestamp(city_crawler_ts) >= ? * 3600 LIMIT ?, ?";
	}
}

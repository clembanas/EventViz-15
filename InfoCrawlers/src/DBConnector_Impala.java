/**
 * @author Bernhard Weber
 */
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Actual DBConnector implementation for working with Cloudera Impala databases
 * 
 * @deprecated Not supported anymore
 */
public class DBConnector_Impala extends DBConnector_SQLConform {
	
	public static final String DRIVER_NAME = "com.cloudera.impala.jdbc4.Driver";
	public static final String CONNECTION_STR = "jdbc:impala://138.232.65.251:21050";
	
	
	/**
	 * Actual primary key implementation for Cloudera Impala databases (primary key is of type 
	 * BIGINT)
	 */
	public static class ImpalaPrimaryKey extends PrimaryKey {
		
		private static long lastGenPrimKey = 0;
		
		private ImpalaPrimaryKey(long val)
		{
			data = val;
		}
		
		public ImpalaPrimaryKey(Object obj) throws Exception
		{
			if (obj.getClass() != Long.class)
				throw new IllegalArgumentException("Cloudera Impala primary keys must be of type " +
							  "BIGINT!");
			this.data = obj;
		}
		
		public ImpalaPrimaryKey(PrimaryKey primKey) throws Exception
		{
			if (primKey.data.getClass() != Long.class)
				throw new IllegalArgumentException("Cloudera Impala  primary keys must be of type" +
							 "BIGINT!");
			this.data = primKey.data;
		}
		
		public ImpalaPrimaryKey(String str) throws Exception
		{
			this.data = Long.valueOf(str);
		}
		
		public ImpalaPrimaryKey(ResultSet resSet) throws Exception
		{
			this.data = resSet.getLong(1);
		}
		
		public void addToStatement(PreparedStatement stmt, int idx) throws Exception
		{
			stmt.setLong(idx, (Long)data);
		}

		public synchronized static PrimaryKey generate() 
		{
			long newPrimKey = System.currentTimeMillis();
			
			while (newPrimKey == lastGenPrimKey)
				newPrimKey = System.currentTimeMillis();
			lastGenPrimKey = newPrimKey;
			return new ImpalaPrimaryKey(newPrimKey);
		}
	}

	
	private Lock updateLock = new ReentrantLock();
	
	protected DBConnector_Impala() throws Exception		 //Singleton
	{
		PrimaryKey.PrimaryKeyClass = ImpalaPrimaryKey.class;
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
	}
	
	protected void endUpdate() throws Exception 
	{
		updateLock.unlock();
	}
	
	protected void cancelUpdate() throws Exception 
	{
		updateLock.unlock();
	}

	protected String getStmtCreateTblCities() 
	{
		return "CREATE TABLE Cities(" +
				   "ID INT, " +
				   "NAME VARCHAR(" + MAX_LEN_CITY_NAME + "), " +
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
				   "ID INT," +
				   "NAME VARCHAR(" + MAX_LEN_LOCATION_NAME + ")," +
				   "LONGITUDE FLOAT," +
				   "LATITUDE FLOAT," +
				   "CITY_ID INT)";
	}
	
	protected String getStmtCreateTblEvents()
	{
		return "CREATE TABLE Events(" +
				   "ID INT," +
				   "NAME VARCHAR(" + MAX_LEN_EVENT_NAME + ")," +
				   "DESCRIPTION VARCHAR(" + MAX_LEN_EVENT_DESC + ")," +
				   "EVENT_TYPE VARCHAR(" + MAX_LEN_EVENT_TYPE + ")," +
				   "start_time DATETIME, " +
				   "end_time DATETIME, " +
				   "EVENTFUL_ID VARCHAR(" + MAX_LEN_EVENT_EVENTFUL_ID + ")," +
				   "LOCATION_ID INT)";
	}

	protected String getStmtCreateTblBands()
	{
		return "CREATE TABLE Bands(" +
				   "ID INT," +
				   "NAME VARCHAR(" + MAX_LEN_BAND_NAME + "), " +
				   "BAND_CRAWLER_TS TIMESTAMP, " +
				   "DBPEDIA_RESOURCE VARCHAR(" + MAX_LEN_BAND_DBPEDIA_RES + "))";
	}

	protected String getStmtCreateTblArtists()
	{
		return "CREATE TABLE Artists(" +
				   "ID INT, " +
				   "NAME VARCHAR(" + MAX_LEN_ARTIST_NAME + "), " +
				   "ALTERNATE_NAME VARCHAR(" + MAX_LEN_ARTIST_ALT_NAME + "), " +
				   "DBPEDIA_RESOURCE VARCHAR(" + MAX_LEN_ARTIST_DBPEDIA_RES + "))";
	}

	protected String getStmtCreateTblEventPerformers()
	{
		return "CREATE TABLE EventPerformers(" +
				   "EVENT_ID INT," +
				   "BAND_ID INT)";
	}

	protected String getStmtCreateTblBandMembers()
	{
		return "CREATE TABLE BandMembers(" +
				   "ARTIST_ID INT," +
				   "BAND_ID INT, " +
				   "MEMBER_TYPE CHAR(1))";
	}
	
	protected Utils.Pair<String, PrimaryKey> getStmtInsertEvent() 
	{
		PrimaryKey primKey = ImpalaPrimaryKey.generate();
		
		return Utils.createPair("INSERT INTO Events (id, name, description, event_type, " +
				  "eventful_id, location_id) VALUES(" + primKey + ", ?, ?, ?, ?)", primKey);
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
				   "unix_timestamp(?) - unix_timestamp(band_crawler_ts) >= ? * 3600";
	}
	
	protected String getStmtUpdateBandTS() 
	{
		return "UPDATE Bands SET band_crawler_ts = ? WHERE " +
				   "band_crawler_ts IS NULL OR " +
				   "unix_timestamp(?) - unix_timestamp(band_crawler_ts) >= ? * 3600";
	}
	
	protected Utils.Pair<String, PrimaryKey> getStmtInsertBand() 
	{
		PrimaryKey primKey = ImpalaPrimaryKey.generate();
		
		return Utils.createPair("INSERT INTO Bands (id, name) VALUES(" + primKey + ", ?)", primKey);
	}
	
	protected Utils.Pair<String, PrimaryKey> getStmtInsertArtist() 
	{
		PrimaryKey primKey = ImpalaPrimaryKey.generate();
		
		return Utils.createPair("INSERT INTO Artists (id, name, alternate_name, dbpedia_resource)" +
				  " VALUES(" + primKey + ", ?, ?, ?)", primKey);
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
				   "unix_timestamp(?) - unix_timestamp(city_crawler_ts) >= ? * 3600";
	}

	protected Utils.Pair<String, PrimaryKey> getStmtInsertCity() 
	{
		PrimaryKey primKey = ImpalaPrimaryKey.generate();
		
		return Utils.createPair("INSERT INTO Cities (id, name, region, country) " +
				  "VALUES(" + primKey + ", ?, ?, ?)", primKey);
	}

	protected Utils.Pair<String, PrimaryKey> getStmtInsertLocation() 
	{
		PrimaryKey primKey = ImpalaPrimaryKey.generate();
		
		return Utils.createPair("INSERT INTO Locations (id, name, longitude, latitude, city_id) " +
				  "VALUES(" + primKey + ", ?, ?, ?, ?)", primKey);
	}
}

/**
 * @author Bernhard Weber
 */
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Actual DBConnection implementation for working with Cloudera Impala databases
 */
public class DBConnection_Impala extends DBConnection_SQLConform {
	
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
	
	
	public static final String DRIVER_NAME = "com.cloudera.impala.jdbc4.Driver";
	public static final String CONNECTION_STR = "jdbc:impala://138.232.65.251:21050";
	
	private Lock updateLock = new ReentrantLock();
	
	protected DBConnection_Impala()	 //Singleton
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
				   "DBPEDIA_RESOURCE VARCHAR(" + MAX_LEN_CITY_DBPEDIA_RES + "))";
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
		
		return Utils.createPair("INSERT INTO events (id, name, description, eventful_id, " +
				  "location_id) VALUES(" + primKey + ", ?, ?, ?, ?)", primKey);
	}
	
	protected String getStmtIncompleteBandsCount()
	{
		return "SELECT COUNT(id) FROM bands WHERE " +
				   "band_crawler_ts IS NULL OR " +
				   "unix_timestamp(?) - unix_timestamp(band_crawler_ts) >= ? * 3600";
	}
	
	protected String getStmtIncompleteBands()
	{
		return "SELECT id, name FROM bands WHERE " +
				   "band_crawler_ts IS NULL OR " +
				   "unix_timestamp(?) - unix_timestamp(band_crawler_ts) >= ? * 3600";
	}
	
	protected String getStmtUpdateBandTS() 
	{
		return "UPDATE bands SET band_crawler_ts = ? WHERE " +
				   "band_crawler_ts IS NULL OR " +
				   "unix_timestamp(?) - unix_timestamp(band_crawler_ts) >= ? * 3600";
	}
	
	protected Utils.Pair<String, PrimaryKey> getStmtInsertBand() 
	{
		PrimaryKey primKey = ImpalaPrimaryKey.generate();
		
		return Utils.createPair("INSERT INTO bands (id, name) VALUES(" + primKey + ", ?)", primKey);
	}
	
	protected Utils.Pair<String, PrimaryKey> getStmtInsertArtist() 
	{
		PrimaryKey primKey = ImpalaPrimaryKey.generate();
		
		return Utils.createPair("INSERT INTO artists (id, name, alternate_name, dbpedia_resource)" +
				  " VALUES(" + primKey + ", ?, ?, ?)", primKey);
	}
	
	protected String getStmtIncompleteCitiesCount()
	{
		return "SELECT COUNT(id) FROM cities " +
				   "WHERE city_crawler_ts IS NULL OR " +
				   "unix_timestamp(?) - unix_timestamp(city_crawler_ts) >= ? * 3600";
	}
	
	protected String getStmtIncompleteCities()
	{
		return "SELECT id, name, region, country FROM cities " +
				   "WHERE city_crawler_ts IS NULL OR " +
				   "unix_timestamp(?) - unix_timestamp(city_crawler_ts) >= ? * 3600";
	}
	
	protected String getStmtUpdateCityTS() 
	{
		return "UPDATE cities SET city_crawler_ts = ? " +
				   "WHERE city_crawler_ts IS NULL OR " +
				   "unix_timestamp(?) - unix_timestamp(city_crawler_ts) >= ? * 3600";
	}

	protected Utils.Pair<String, PrimaryKey> getStmtInsertCity() 
	{
		PrimaryKey primKey = ImpalaPrimaryKey.generate();
		
		return Utils.createPair("INSERT INTO cities (id, name, region, country) " +
				  "VALUES(" + primKey + ", ?, ?, ?)", primKey);
	}

	protected Utils.Pair<String, PrimaryKey> getStmtInsertLocation() 
	{
		PrimaryKey primKey = ImpalaPrimaryKey.generate();
		
		return Utils.createPair("INSERT INTO locations (id, name, longitude, latitude, city_id) " +
				  "VALUES(" + primKey + ", ?, ?, ?, ?)", primKey);
	}
}
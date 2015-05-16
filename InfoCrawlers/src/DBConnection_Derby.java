/**
 * @author Bernhard Weber
 */
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.locks.*;


/**
 * Actual DBConnection implementation for working with Derby databases
 * 
 * @deprecated (At the moment up-to-date but won't be updated in future)
 */
public class DBConnection_Derby extends DBConnection_SQLConform {
	
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
	
	
	public static final String DRIVER_NAME = "org.apache.derby.jdbc.EmbeddedDriver";
	public static final String CONNECTION_STR = "jdbc:derby:temp/derby/Events/db;create=true";
	
	private Lock updateLock = new ReentrantLock();
	
	protected DBConnection_Derby()	 //Singleton
	{
		PrimaryKey.PrimaryKeyClass = DerbyPrimaryKey.class;
	}
	
	protected boolean tableExists(Statement stmt, String tableName) throws Exception 
	{
		DatabaseMetaData dbMeta = dbConn.getMetaData();
		
		return dbMeta.getTables(null, null, tableName.toUpperCase(), null).next();
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
				   "DBPEDIA_RESOURCE VARCHAR(" + MAX_LEN_CITY_DBPEDIA_RES + "))";
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
	
	protected Utils.Pair<String, PrimaryKey> getStmtInsertEvent() 
	{
		return Utils.createPair("INSERT INTO events (name, description, eventful_id, " +
				   "location_id) VALUES(?, ?, ?, ?)", null);
	}
	
	protected String getStmtIncompleteBandsCount()
	{
		return "SELECT COUNT(id) FROM bands WHERE " +
				   "band_crawler_ts IS NULL OR " +
				   "{fn TIMESTAMPDIFF(SQL_TSI_HOUR, band_crawler_ts, ?)} >= ?";
	}

	protected String getStmtIncompleteBands()
	{
		return "SELECT id, name FROM bands WHERE " +
				   "band_crawler_ts IS NULL OR " +
				   "{fn TIMESTAMPDIFF(SQL_TSI_HOUR, band_crawler_ts, ?)} >= ?";
	}
	
	protected Utils.Pair<String, PrimaryKey> getStmtInsertBand() 
	{
		return Utils.createPair("INSERT INTO bands (name) VALUES(?)", null);
	}

	protected Utils.Pair<String, PrimaryKey> getStmtInsertArtist() 
	{
		return Utils.createPair("INSERT INTO artists (name, alternate_name, dbpedia_resource) " +
			      "VALUES(?, ?, ?)", null);
	}
	
	protected String getStmtIncompleteCitiesCount()
	{
		return "SELECT COUNT(id) FROM cities " +
				   "WHERE city_crawler_ts IS NULL OR " +
				   "{fn TIMESTAMPDIFF(SQL_TSI_HOUR, city_crawler_ts, ?)} >= ?";
	}
	
	protected String getStmtIncompleteCities()
	{
		return "SELECT id, name, region, country FROM cities " +
				   "WHERE city_crawler_ts IS NULL OR " +
				   "{fn TIMESTAMPDIFF(SQL_TSI_HOUR, city_crawler_ts, ?)} >= ?";
	}
	
	protected Utils.Pair<String, PrimaryKey> getStmtInsertCity() 
	{
		return Utils.createPair("INSERT INTO cities (name, region, country) VALUES(?, ?, ?)", null);
	}

	protected Utils.Pair<String, PrimaryKey> getStmtInsertLocation() 
	{
		return Utils.createPair("INSERT INTO locations (name, longitude, latitude, city_id) " +
				   "VALUES(?, ?, ?, ?)", null);
	}
}

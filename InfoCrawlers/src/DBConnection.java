/**
 * @author Bernhard Weber
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.concurrent.locks.*;


public class DBConnection {
	
	public static boolean DEBUG = true;
	public static boolean DEBUG_RESULTS = true;
	public static final String DRIVER_NAME = "org.apache.derby.jdbc.EmbeddedDriver";
	public static final String CONNECTION_STR = "jdbc:derby:C:/temp/derby/Events/db;create=true";
	
	private static Connection dbConn = null;
	private static Lock updateLock = new ReentrantLock();
	
	private static void debug_print(final String info)
	{
		if (DEBUG) 
			DebugUtils.debug_printf("[DBConnection (Thread %s)]: %s\n", 
				Thread.currentThread().getId(),	info);
	}
	
	private static void debug_queryResult(String query, ResultSet resSet) throws Exception
	{
		if (resSet.next()) {
			ResultSetMetaData meta = resSet.getMetaData();
			int colCnt = meta.getColumnCount();
			String[] entries = new String[colCnt];
			DebugUtils.TableDebugger tableDebugger = new DebugUtils.TableDebugger(20);
			
			for (int i = 1; i <= colCnt; ++i)
				entries[i - 1] = meta.getColumnName(i);
			tableDebugger.setHeader(entries);
			do {
				entries = new String[colCnt];
				for (int i = 1; i <= colCnt; ++i)
					entries[i - 1] = resSet.getString(i);
				tableDebugger.addRow(entries);
			}
			while (resSet.next());
			debug_print("Results of query '" + query + "':\n" + tableDebugger);
		}
		else
			debug_print("Query '" + query + "' returned no results!");
	}
	
	private static ResultSet executeQuery(String query, Object ... args) throws Exception
	{
		String dbgQueryStr = query;
		
		if (DEBUG) {
			for (int i = 0; i < args.length; ++i) 
				dbgQueryStr = dbgQueryStr.replaceFirst("\\?", "arg:'" + args[i].toString() + "'");
			debug_print("Executing query '" + dbgQueryStr + "'...");
		}
		
		PreparedStatement stmnt = dbConn.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, 
							   		  ResultSet.CONCUR_READ_ONLY);
		for (int i = 0; i < args.length; ++i) 
			stmnt.setObject(i + 1, args[i]);
		
		ResultSet resSet = stmnt.executeQuery();
		if (DEBUG && DEBUG_RESULTS) {
			debug_queryResult(dbgQueryStr, resSet);
			resSet.beforeFirst();
		}
		return resSet;
	}
	
	@SuppressWarnings("unused")
	private static ResultSet executeQuery(String query) throws Exception
	{
		debug_print("Executing query '" + query + "'...");
		
		ResultSet resSet = dbConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, 
							   ResultSet.CONCUR_READ_ONLY).executeQuery(query);
		
		if (DEBUG_RESULTS) {
			debug_queryResult(query, resSet);
			resSet.beforeFirst();
		}
		return resSet;
	}
	
	private static void beginUpdate() throws Exception 
	{
		updateLock.lock();
		dbConn.setAutoCommit(false);
	}
	
	private static void endUpdate() throws Exception 
	{
		dbConn.commit();
		dbConn.setAutoCommit(true);
		updateLock.unlock();
	}
	
	private static int insertArtist(String name, String altName, String resID) throws Exception
	{
		PreparedStatement stmnt = dbConn.prepareStatement("INSERT INTO artists (name, " +
									  "alternate_name, dbpedia_resource) VALUES(?, ?, ?)", 
									  Statement.RETURN_GENERATED_KEYS);

		stmnt.setString(1, name.trim());
		if (altName == null)
			stmnt.setNull(2, java.sql.Types.VARCHAR);
		else
			stmnt.setString(2, altName.trim());
		if (resID == null)
			stmnt.setNull(3, java.sql.Types.VARCHAR);
		else
			stmnt.setString(3, resID.trim());
		stmnt.executeUpdate();
		
		ResultSet resSet = stmnt.getGeneratedKeys();
		resSet.next();
		return resSet.getInt(1);
	}
	
	private static void updateArtist(int artistID, String name, String altName, String currResID, 
		String newResID) throws Exception
	{
		if (newResID == null)
			return;
		if (currResID == null || !currResID.equalsIgnoreCase(newResID)) {
			PreparedStatement stmnt = dbConn.prepareStatement("UPDATE artists " +
										  "SET dbpedia_resource = ?, alternate_name = ? " +
										  "WHERE id = ?");
			
			stmnt.setString(1, newResID.trim());
			if (altName == null)
				stmnt.setNull(2, java.sql.Types.VARCHAR);
			else
				stmnt.setString(2, altName.trim());
			stmnt.setInt(3, artistID);
			stmnt.executeUpdate();
		}
	}
	
	private static void insertBandMember(int bandID, int artistID, char memberType) 
		throws Exception
	{
		PreparedStatement stmnt = dbConn.prepareStatement("INSERT INTO bandmembers (band_id, " +
									  "artist_id, member_type) VALUES(?, ?, ?)");
									  
		stmnt.setInt(1, bandID);
		stmnt.setInt(2, artistID);
		stmnt.setString(3, String.valueOf(memberType));
		stmnt.executeUpdate();
	}
	
	private static void updateBandMember(int bandID, int artistID, char memberType) throws Exception
	{
		PreparedStatement stmnt = dbConn.prepareStatement("SELECT band_id FROM bandmembers " +
				  					  " WHERE band_id = ? AND artist_id = ?");
		
		stmnt.setInt(1, bandID);
		stmnt.setInt(2, artistID);
		//Update band member
		if (stmnt.executeQuery().next()) {
			stmnt.close();
			stmnt = dbConn.prepareStatement("UPDATE bandmembers SET member_type = ?" +
						" WHERE band_id = ? AND artist_id = ?");
			stmnt.setString(1, String.valueOf(memberType));
			stmnt.setInt(2, bandID);
			stmnt.setInt(3, artistID);
		}
		//Insert new band member
		else {
			stmnt.close();
			stmnt = dbConn.prepareStatement("INSERT INTO bandmembers (band_id, artist_id, " +
						"member_type) VALUES(?, ?, ?)");
			stmnt.setInt(1, bandID);
			stmnt.setInt(2, artistID);
			stmnt.setString(3, String.valueOf(memberType));
		}
		stmnt.executeUpdate();
	}

	public static void connect() throws Exception
	{
		if (dbConn == null) {
			Class.forName(DRIVER_NAME).newInstance();
			dbConn = DriverManager.getConnection(CONNECTION_STR);
			debug_print("Connected to DB...");
		}
	}
	
	public static void disconnect() throws Exception 
	{
		if (dbConn != null) {
			dbConn.close();
			dbConn = null;
		}
	}
	
	public static ResultSet getIncompleteBands(int dbUpdateInterval) throws Exception
	{
		return executeQuery("SELECT id, name FROM bands WHERE " +
					"(band_crawler_ts IS NULL OR " +
				    "{fn TIMESTAMPDIFF(SQL_TSI_HOUR, band_crawler_ts, ?)} >= ?) " +
					"AND id NOT IN (SELECT band_id AS id FROM bandmembers)", 
					new Timestamp(System.currentTimeMillis()), dbUpdateInterval);
	}
	
	public static ResultSet getIncompleteCities(int dbUpdateInterval) throws Exception
	{
		return executeQuery("SELECT id, name, region, country FROM cities " +
				   "WHERE city_crawler_ts IS NULL OR " +
				   "{fn TIMESTAMPDIFF(SQL_TSI_HOUR, city_crawler_ts, ?)} >= ?",
				   new Timestamp(System.currentTimeMillis()), dbUpdateInterval); 
	}
	
	public synchronized static void insertBandArtist(int bandID, String name, String altName, 
		char memberType, String resID) throws Exception
	{
		int artistID;
		
		try {
			beginUpdate();
			
			PreparedStatement stmnt = dbConn.prepareStatement("SELECT id, name, dbpedia_resource " +
										  "FROM artists WHERE LCASE(name) = ?");
			stmnt.setString(1, name.trim().toLowerCase());
			ResultSet resSet = stmnt.executeQuery();
	
			if (resSet.next()) {
				artistID = resSet.getInt(1);
				updateArtist(artistID, name, altName, resSet.getString(3), resID);
				updateBandMember(bandID, artistID, memberType);
			}
			else {
				artistID = insertArtist(name, altName, resID);
				insertBandMember(bandID, artistID, memberType);
			}
		}
		finally {
			endUpdate();
		}
	}
	
	public synchronized static void updateBand(int bandID, String bandResID) throws Exception
	{
		try {
			beginUpdate();

			PreparedStatement stmnt;
		
			if (bandResID == null) {
				stmnt = dbConn.prepareStatement("UPDATE bands SET band_crawler_ts = ? " +
							"WHERE id = ?");
				stmnt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
				stmnt.setInt(2, bandID);
			}
			else {
				stmnt = dbConn.prepareStatement("UPDATE bands " +
							"SET dbpedia_resource = ?, band_crawler_ts = ? WHERE id = ?");
				stmnt.setString(1, bandResID);
				stmnt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
				stmnt.setInt(3, bandID);
			}
			stmnt.executeUpdate();
		}
		finally {
			endUpdate();
		}
	}
	
	public synchronized static void updateCity(int cityID, float latitude, float longitude, 
		String regName, String ctryName, String cityResID) throws Exception
	{
		try {
			beginUpdate();

			PreparedStatement stmnt = dbConn.prepareStatement("UPDATE cities " +
										  "SET region = ?, country = ?, latitude = ?," +
										  "    longitude = ?, dbpedia_resource = ?, " +
										  "    city_crawler_ts = ? WHERE id = ?");
			if (regName == null || regName.isEmpty())
				stmnt.setNull(1, java.sql.Types.VARCHAR);
			else
				stmnt.setString(1, regName);
			if (ctryName == null || ctryName.isEmpty())
				stmnt.setNull(2, java.sql.Types.VARCHAR);
			else
				stmnt.setString(2, ctryName);
			stmnt.setFloat(3, latitude);
			stmnt.setFloat(4, longitude);
			if (cityResID == null || cityResID.isEmpty())
				stmnt.setNull(5, java.sql.Types.VARCHAR);
			else
				stmnt.setString(5, cityResID);
			stmnt.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
			stmnt.setInt(7, cityID);
			stmnt.executeUpdate();
		}
		finally {
			endUpdate();
		}
	}
}

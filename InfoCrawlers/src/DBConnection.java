/**
 * @author Bernhard Weber
 */
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Timestamp;


/**
 * Wrapper class/ interface for the actual database connection
 */
public abstract class DBConnection {
	
	/**
	 * Wrapper class for the database's primary key
	 */
	public static abstract class PrimaryKey {

		protected static Class<? extends PrimaryKey> PrimaryKeyClass;
		protected Object data;
		
		protected PrimaryKey() {}	//Singleton
		
		public static PrimaryKey create(Object obj) throws Exception
		{
			return PrimaryKeyClass.getConstructor(obj.getClass()).newInstance(obj);
		}
		
		public static PrimaryKey create(PrimaryKey primKey) throws Exception
		{
			return PrimaryKeyClass.getConstructor(primKey.getClass()).newInstance(primKey);
		}
		
		public static PrimaryKey create(String str) throws Exception
		{
			return PrimaryKeyClass.getConstructor(String.class).newInstance(str);
		}
		
		public static PrimaryKey create(ResultSet resSet) throws Exception
		{
			return PrimaryKeyClass.getConstructor(ResultSet.class).newInstance(resSet);
		}

		public static PrimaryKey create(PreparedStatement stmt,	PrimaryKey primKey) throws Exception
		{
			if (primKey == null) {
				ResultSet resSet = stmt.getGeneratedKeys();
				
				resSet.next();
				return create(resSet);
			}
			else
				return primKey;
		}
		
		public abstract void addToStatement(PreparedStatement stmt, int idx) throws Exception;
		
		public String toString()
		{
			return data.toString();
		}
	}
	
	public static class UpdateDBValue {
		
	}
	
	
	public static boolean DEBUG = true;
	public static boolean DEBUG_UPDATES = true;
	public static boolean DEBUG_QUERY_RESULTS = true;
	public static Class<? extends DBConnection> DB_CONNECTION_CLASS = null;
	//Maximum length settings of various table entries
	public static final int MAX_LEN_CRAWLER_EXCEPT_LOG_MSG = 255;
	public static final int MAX_LEN_CRAWLER_EXCEPT_LOG_CLASS = 100;
	public static final int MAX_LEN_CRAWLER_EXCEPT_LOG_STACK = 255;
	public static final int MAX_LEN_CRAWLER_INFO_CLASS = 100;
	public static final int MAX_LEN_CRAWLER_INFO_SUMMARY = 255;
	public static final int MAX_LEN_CITY_NAME = 255;
	public static final int MAX_LEN_CITY_REGION = 255;
	public static final int MAX_LEN_CITY_COUNTRY = 255;
	public static final int MAX_LEN_CITY_DBPEDIA_RES = 255;
	public static final int MAX_LEN_LOCATION_NAME = 255;
	public static final int MAX_LEN_EVENT_NAME = 255;
	public static final int MAX_LEN_EVENT_DESC = 255;
	public static final int MAX_LEN_EVENT_TYPE = 50;
	public static final int MAX_LEN_EVENT_EVENTFUL_ID = 50;
	public static final int MAX_LEN_BAND_NAME = 255;
	public static final int MAX_LEN_BAND_DBPEDIA_RES = 255;
	public static final int MAX_LEN_ARTIST_NAME = 255;
	public static final int MAX_LEN_ARTIST_ALT_NAME = 255;
	public static final int MAX_LEN_ARTIST_DBPEDIA_RES = 255;

	private static DBConnection instance = null;
	protected Connection dbConn = null;
	
	protected abstract String getDriverName();
	protected abstract String getConnectionStr();
	protected abstract void beginUpdate() throws Exception;
	protected abstract void endUpdate() throws Exception;
	protected abstract void cancelUpdate() throws Exception;
	protected abstract String getStmtCreateTblExceptionLogs();
	protected abstract String getStmtCreateTblCrawlerInfos();
	protected abstract String getStmtCreateTblCities();
	protected abstract String getStmtCreateTblLocations();
	protected abstract String getStmtCreateTblEvents();
	protected abstract String getStmtCreateTblBands();
	protected abstract String getStmtCreateTblArtists();
	protected abstract String getStmtCreateTblEventPerformers();
	protected abstract String getStmtCreateTblBandMembers();
	protected abstract String getStmtDropTable(String tableName);
	protected abstract String getStmtLogException();
	protected abstract String getStmtInsertCrawlerInfoStarted();
	protected abstract String getStmtUpdateCrawlerInfoStarted();
	protected abstract String getStmtUpdateCrawlerInfoFinished();
	protected abstract String getStmtUpdateCrawlerInfoProgress();
	protected abstract String getStmtEventExists();
	protected abstract Utils.Pair<String, PrimaryKey> getStmtInsertEvent();
	protected abstract String getStmtEventPerformerExists();
	protected abstract String getStmtInsertEventPerformer();
	protected abstract String getStmtIncompleteBandsCount();
	protected abstract String getStmtIncompleteBands();
	protected abstract String getStmtUpdateBandTS();
	protected abstract String getStmtBandExists();
	protected abstract Utils.Pair<String, PrimaryKey> getStmtInsertBand();
	protected abstract String getStmtUpdateBand(boolean tsOnly);
	protected abstract String getStmtBandMemberExists();
	protected abstract String getStmtInsertBandMember(); 
	protected abstract String getStmtUpdateBandMember();
	protected abstract String getStmtArtistExists(); 
	protected abstract Utils.Pair<String, PrimaryKey> getStmtInsertArtist();
	protected abstract String getStmtUpdateArtist();
	protected abstract String getStmtIncompleteCitiesCount();
	protected abstract String getStmtIncompleteCities();
	protected abstract String getStmtUpdateCityTS();
	protected abstract String getStmtCityExists(boolean regionExists, boolean countryExists);
	protected abstract Utils.Pair<String, PrimaryKey> getStmtInsertCity();
	protected abstract String getStmtUpdateCity();
	protected abstract String getStmtLocationExists();
	protected abstract Utils.Pair<String, PrimaryKey> getStmtInsertLocation();
	
	protected DBConnection() {}	//Singleton class
	
	protected void debug_print(final String info)
	{
		if (DEBUG) 
			DebugUtils.debug_printf("[DBConnection (Thread %s)]: %s\n", 
				Thread.currentThread().getId(),	info);
	}
	
	protected void debug_queryResult(String query, ResultSet resSet) throws Exception
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
		resSet.beforeFirst();
	}
	
	protected void debug_update(String stmt, Object ... args)
	{
		if (DEBUG && DEBUG_UPDATES) {
			debug_print("Executing update:\n   " + Utils.wrapString(Utils.replaceEach(stmt, "?", 
				"arg:'%s'", args), 50, "\n   "));
		}
	}
	
	protected String trimAndTrunc(String s, int maxLen)
	{
		if (DEBUG) { 
			int len = s.length();
			
			s = Utils.trimAndTrunc(s, maxLen);
			if (s.length() != len) 
				debug_print("WARNING: '" + s + "' truncated to length '" + maxLen + "'!");
			return s;
		}
		return Utils.trimAndTrunc(s, maxLen); 
	}
	
	protected String lcTrimAndTrunc(String s, int maxLen)
	{
		s = trimAndTrunc(s, maxLen);
		return s == null ? null : s.toLowerCase();
	}
	
	protected boolean tableExists(Statement stmt, String tableName) throws Exception 
	{
		DatabaseMetaData dbMeta = dbConn.getMetaData();
		
		return dbMeta.getTables(null, null, tableName.toLowerCase(), null).next();
	}
	
	protected void createTable(Statement stmt, String tableName, String query,
		boolean dropExisting) throws Exception
	{
		try {
			if (tableExists(stmt, tableName)) {
				if (dropExisting) {
					debug_print("Dropping table '" + tableName + "' ...");
					try {
						dropTable(stmt, tableName);
					}
					catch (Exception e) {
						ExceptionHandler.handle(e, "Failed to drop table '" + tableName + "'!");
						throw e;
					}
					debug_print("Dropping table '" + tableName + "' ... DONE");
				}
				else {
					debug_print("Skipping creation of table '" + tableName + 
						"' since it already exists!");
					return;
				}
			}
			debug_print("Creating table '" + tableName + "' using statement:\n   " + 
				Utils.wrapString(query,	50, "\n   "));
			stmt.executeUpdate(query);
			debug_print("Creating table '" + tableName + "' ... DONE\n");
		}
		catch (Exception e) {
			ExceptionHandler.handle(e, "Failed to create table '" + tableName + "'!");
			throw e;
		}
	}

	protected void dropTable(Statement stmt, String tableName)	throws Exception 
	{
		stmt.executeUpdate(getStmtDropTable(tableName));
	}
	
	protected PreparedStatement setStatementArgs(PreparedStatement stmt, Object ... args) 
		throws Exception
	{
		for (int i = 0; i < args.length; ++i) {
			if (args[i] == null)
				stmt.setNull(i + 1, java.sql.Types.VARCHAR);
			else if (args[i] instanceof PrimaryKey)
				((PrimaryKey)args[i]).addToStatement(stmt, i + 1);
			else
				stmt.setObject(i + 1, args[i]);
		}
		return stmt;
	}
	
	protected ResultSet executeQuery(String query, Object ... args) throws Exception
	{
		String dbgQueryStr = "";
		
		if (DEBUG) {
			dbgQueryStr = Utils.replaceEach(query, "?", "arg:'%s'", args);
			debug_print("Executing query '" + dbgQueryStr +	"'...");
		}
		
		PreparedStatement stmt = dbConn.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, 
							   		  ResultSet.CONCUR_READ_ONLY);
		setStatementArgs(stmt, args);
		
		ResultSet resSet = stmt.executeQuery();
		if (DEBUG && DEBUG_QUERY_RESULTS) 
			debug_queryResult(dbgQueryStr, resSet);
		return resSet;
	}
	
	protected ResultSet executeQuery(String query) throws Exception
	{
		debug_print("Executing query '" + query + "'...");
		
		ResultSet resSet = dbConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, 
							   ResultSet.CONCUR_READ_ONLY).executeQuery(query);
		
		if (DEBUG_QUERY_RESULTS) {
			debug_queryResult(query, resSet);
			resSet.beforeFirst();
		}
		return resSet;
	}
	
	protected PreparedStatement executeUpdate(String updStmt, Object ... args) throws Exception
	{
		debug_print("Executing update '" + updStmt + "'...");
		
		PreparedStatement stmt = dbConn.prepareStatement(updStmt, 
				 					 PreparedStatement.RETURN_GENERATED_KEYS);
		
		setStatementArgs(stmt, args);
		debug_update(updStmt, args);
		stmt.executeUpdate();
		return stmt;
	}
	
	protected void updateCrawlerTS(String updtStmt)
	{
		try {
			PreparedStatement stmt = executeUpdate(updtStmt, 
										 new Timestamp(System.currentTimeMillis()));
			
			debug_print(stmt.getUpdateCount() + " timestamps updated");
		}
		catch (Exception e) {
			ExceptionHandler.handle(e, "Failed to update crawler timestamps!");
		}
	}
	
	protected PrimaryKey insertArtist(String name, String altName, String resID) throws Exception
	{
		Utils.Pair<String, PrimaryKey> insertArtistStmt = getStmtInsertArtist();
		
		return PrimaryKey.create(executeUpdate(insertArtistStmt.first, 
				   trimAndTrunc(name, MAX_LEN_ARTIST_NAME), 
				   trimAndTrunc(altName, MAX_LEN_ARTIST_ALT_NAME),
				   trimAndTrunc(resID, MAX_LEN_ARTIST_DBPEDIA_RES)), insertArtistStmt.second);
	}
	
	protected void updateArtist(PrimaryKey artistID, String name, String altName, String currResID, 
		String newResID) throws Exception
	{
		if (newResID == null)
			return;
		if (currResID == null || !currResID.equalsIgnoreCase(newResID)) 
			executeUpdate(getStmtUpdateArtist(), trimAndTrunc(newResID, MAX_LEN_ARTIST_DBPEDIA_RES),
				trimAndTrunc(altName, MAX_LEN_ARTIST_ALT_NAME), artistID);
	}
	
	protected void insertBandMember(PrimaryKey bandID, PrimaryKey artistID, char memberType) 
		throws Exception
	{
		executeUpdate(getStmtInsertBandMember(), bandID, artistID, String.valueOf(memberType));
	}
	
	protected void updateBandMember(PrimaryKey bandID, PrimaryKey artistID, char memberType) 
		throws Exception
	{
		//Update band member
		if (executeQuery(getStmtBandMemberExists(), bandID, artistID).next()) 
			executeUpdate(getStmtUpdateBandMember(), String.valueOf(memberType), bandID, artistID);
		//Insert new band member
		else 
			insertBandMember(bandID, artistID, memberType);
	}
	
	public void createTables(boolean dropExisting) throws Exception
	{
		Statement stmt = dbConn.createStatement();
		
		try {
			createTable(stmt, "Crawler_exception_logs", getStmtCreateTblExceptionLogs(), 
				dropExisting);
			createTable(stmt, "Crawler_infos", getStmtCreateTblCrawlerInfos(), dropExisting);
			createTable(stmt, "Cities", getStmtCreateTblCities(), dropExisting);
			createTable(stmt, "Locations", getStmtCreateTblLocations(), dropExisting);
			createTable(stmt, "Events", getStmtCreateTblEvents(), dropExisting);
			createTable(stmt, "Bands", getStmtCreateTblBands(), dropExisting);
			createTable(stmt, "artists", getStmtCreateTblArtists(),	dropExisting);
			createTable(stmt, "EventPerformers", getStmtCreateTblEventPerformers(), dropExisting);
			createTable(stmt, "BandMembers", getStmtCreateTblBandMembers(),	dropExisting);
		}
		finally {
			stmt.close();
		}
	}
	
	public synchronized void connect() throws Exception
	{
		if (dbConn == null) {
			Class.forName(getDriverName()).newInstance();
			dbConn = DriverManager.getConnection(getConnectionStr());
			debug_print("Connected to DB...");
		}
	}
	
	public synchronized void disconnect() throws Exception 
	{
		if (dbConn != null) {
			dbConn.close();
			dbConn = null;
			debug_print("Disconnected from DB...");
		}
	}
	
	public synchronized void logException(final Exception e, final String info, 
		final String stackTrace) throws Exception
	{
		executeUpdate(getStmtLogException(), new Timestamp(System.currentTimeMillis()), 
			trimAndTrunc(info, MAX_LEN_CRAWLER_EXCEPT_LOG_MSG), 
			trimAndTrunc(e.getClass().getName(), MAX_LEN_CRAWLER_EXCEPT_LOG_CLASS),
			trimAndTrunc(stackTrace, MAX_LEN_CRAWLER_EXCEPT_LOG_STACK));
	}
	
	public synchronized void logCrawlerStarted(Class<? extends CrawlerBase> crawlerClass) 
	{
		try {
			PreparedStatement stmt = executeUpdate(getStmtUpdateCrawlerInfoStarted(), 
										 new Timestamp(System.currentTimeMillis()),
										 trimAndTrunc(crawlerClass.getName(), 
											 MAX_LEN_CRAWLER_INFO_CLASS));
			
			if (stmt.getUpdateCount() == 0)
				executeUpdate(getStmtInsertCrawlerInfoStarted(), 
					trimAndTrunc(crawlerClass.getName(), MAX_LEN_CRAWLER_INFO_CLASS), 
					new Timestamp(System.currentTimeMillis()));
		}
		catch (Exception e) {
			ExceptionHandler.handle(e, "Failed to write crawler-start info!");
		}
	}
	
	public synchronized void logCrawlerFinished(Class<? extends CrawlerBase> crawlerClass,
		String summary)
	{
		try {
			executeUpdate(getStmtUpdateCrawlerInfoFinished(), 
				new Timestamp(System.currentTimeMillis()),
				trimAndTrunc(summary, MAX_LEN_CRAWLER_INFO_SUMMARY),
				trimAndTrunc(crawlerClass.getName(), MAX_LEN_CRAWLER_INFO_CLASS));
		}
		catch (Exception e) {
			ExceptionHandler.handle(e, "Failed to write crawler-finish info!");
		}
	}
	
	public synchronized void logCrawlerProgress(Class<? extends CrawlerBase> crawlerClass,
		int progress)
	{
		try {
			executeUpdate(getStmtUpdateCrawlerInfoProgress(), progress,
				trimAndTrunc(crawlerClass.getName(), MAX_LEN_CRAWLER_INFO_CLASS));
		}
		catch (Exception e) {
			ExceptionHandler.handle(e, "Failed to update crawler progress!");
		}
	}
	
	public Utils.Pair<PrimaryKey, Boolean> insertEvent(String name, String desc, String eventfulID,
		PrimaryKey locationID) throws Exception
	{
		try {
			beginUpdate();

			ResultSet resSet = executeQuery(getStmtEventExists(), 
					 			   trimAndTrunc(eventfulID, MAX_LEN_EVENT_EVENTFUL_ID));
			if (resSet.next()) {
				endUpdate();
				return Utils.createPair(PrimaryKey.create(resSet), false);
			}
			
			Utils.Pair<String, PrimaryKey> insertEventStmt = getStmtInsertEvent();
			PrimaryKey primKey = PrimaryKey.create(executeUpdate(insertEventStmt.first,
									 trimAndTrunc(name, MAX_LEN_EVENT_NAME),
									 trimAndTrunc(desc, MAX_LEN_EVENT_DESC),
									 trimAndTrunc(eventfulID, MAX_LEN_EVENT_EVENTFUL_ID),
									 locationID), insertEventStmt.second);
			endUpdate();
			return Utils.createPair(primKey, true);
		}
		catch (Exception e) {
			cancelUpdate();
			throw e;
		}
	}
	
	public void insertEventPerformer(PrimaryKey eventID, PrimaryKey bandID)	throws Exception
	{
		try {
			beginUpdate();
			
			ResultSet resSet = executeQuery(getStmtEventPerformerExists(), eventID, bandID);
			if (!resSet.next())
				executeUpdate(getStmtInsertEventPerformer(), eventID, bandID);
			endUpdate();
		}
		catch (Exception e) {
			cancelUpdate();
			throw e;
		}
	}
	
	protected int getIncompleteBandsCount(int dbUpdateInterval) throws Exception
	{
		ResultSet resSet = executeQuery(getStmtIncompleteBandsCount(), 
							   new Timestamp(System.currentTimeMillis()), dbUpdateInterval);
		return resSet.next() ? resSet.getInt(1) : 0; 
	}
	
	public ResultSet getIncompleteBands(int dbUpdateInterval) throws Exception
	{
		return executeQuery(getStmtIncompleteBands(), new Timestamp(System.currentTimeMillis()),
				   dbUpdateInterval);
	}
	
	public Utils.Pair<PrimaryKey, Boolean> insertBand(String name) throws Exception
	{
		try {
			beginUpdate();
			
			ResultSet resSet = executeQuery(getStmtBandExists(), 
					               lcTrimAndTrunc(name, MAX_LEN_BAND_NAME));
			if (resSet.next()) {
				endUpdate();
				return Utils.createPair(PrimaryKey.create(resSet), false);
			}
			
			Utils.Pair<String, PrimaryKey> insertBandStmt = getStmtInsertBand();
			PrimaryKey primKey = PrimaryKey.create(executeUpdate(insertBandStmt.first, 
									 trimAndTrunc(name, MAX_LEN_BAND_NAME)), 
									 insertBandStmt.second);
			
			endUpdate();
			return Utils.createPair(primKey, true);
		}
		catch (Exception e) {
			cancelUpdate();
			throw e;
		}
	}
	
	public void updateBand(PrimaryKey bandID, String bandResID) throws Exception
	{
		try {
			beginUpdate();
			if (bandResID == null) 
				executeUpdate(getStmtUpdateBand(true), new Timestamp(System.currentTimeMillis()),
					bandID);
			else 
				executeUpdate(getStmtUpdateBand(false), 
					trimAndTrunc(bandResID, MAX_LEN_BAND_DBPEDIA_RES), 
					new Timestamp(System.currentTimeMillis()),
					bandID);
			endUpdate();
		}
		catch (Exception e) {
			cancelUpdate();
			throw e;
		}
	}
	
	public void updateBandCrawlerTS()
	{
		updateCrawlerTS(getStmtUpdateBandTS());
	}
	
	public void insertBandArtist(PrimaryKey bandID, String name, String altName, char memberType, 
		String resID) throws Exception
	{
		PrimaryKey artistID;
		
		try {
			beginUpdate();
			
			ResultSet resSet = executeQuery(getStmtArtistExists(), 
								   lcTrimAndTrunc(name, MAX_LEN_ARTIST_NAME));
			if (resSet.next()) {
				artistID = PrimaryKey.create(resSet);
				updateArtist(artistID, name, altName, resSet.getString(3), resID);
				updateBandMember(bandID, artistID, memberType);
			}
			else {
				artistID = insertArtist(name, altName, resID);
				insertBandMember(bandID, artistID, memberType);
			}
			endUpdate();
		}
		catch (Exception e) {
			cancelUpdate();
			throw e;
		}
	}
	
	protected int getIncompleteCitiesCount(int dbUpdateInterval) throws Exception
	{
		ResultSet resSet = executeQuery(getStmtIncompleteCitiesCount(), 
							   new Timestamp(System.currentTimeMillis()), dbUpdateInterval);
		return resSet.next() ? resSet.getInt(1) : 0; 
	}
	
	public ResultSet getIncompleteCities(int dbUpdateInterval) throws Exception
	{
		return executeQuery(getStmtIncompleteCities(), new Timestamp(System.currentTimeMillis()), 
				   dbUpdateInterval);
	}
	
	public Utils.Pair<PrimaryKey, Boolean> insertCity(String name, String region, String country) 
		throws Exception
	{
		try {
			beginUpdate();
			
			ResultSet resSet;
			if (region != null && country != null)
				resSet = executeQuery(getStmtCityExists(true, true), 
							 lcTrimAndTrunc(name, MAX_LEN_CITY_NAME), 
							 lcTrimAndTrunc(region, MAX_LEN_CITY_REGION),
							 lcTrimAndTrunc(country, MAX_LEN_CITY_COUNTRY));
			else if (region != null)
				resSet = executeQuery(getStmtCityExists(true, false), 
							 lcTrimAndTrunc(name, MAX_LEN_CITY_NAME), 
							 lcTrimAndTrunc(region, MAX_LEN_CITY_REGION));
			else if (country != null)
				resSet = executeQuery(getStmtCityExists(false, true), 
							 lcTrimAndTrunc(name, MAX_LEN_CITY_NAME), 
							 lcTrimAndTrunc(country, MAX_LEN_CITY_COUNTRY));
			else
				resSet = executeQuery(getStmtCityExists(false, false), 
							 lcTrimAndTrunc(name, MAX_LEN_CITY_NAME));
			if (resSet.next()) {
				endUpdate();
				return Utils.createPair(PrimaryKey.create(resSet), false);
			}
			
			Utils.Pair<String, PrimaryKey> insertCityStmt = getStmtInsertCity();
			PrimaryKey primKey = PrimaryKey.create(executeUpdate(insertCityStmt.first, 
									 trimAndTrunc(name, MAX_LEN_CITY_NAME),
									 trimAndTrunc(region, MAX_LEN_CITY_REGION),
									 Utils.trimAndTrunc(country, MAX_LEN_CITY_COUNTRY)), 
									 insertCityStmt.second);
			
			endUpdate();
			return Utils.createPair(primKey, true);
		}
		catch (Exception e) {
			cancelUpdate();
			throw e;
		}
	}
	
	public void updateCity(PrimaryKey cityID, float latitude, float longitude, String regName,	
		String ctryName, String cityResID) throws Exception
	{
		try {
			beginUpdate();
			executeUpdate(getStmtUpdateCity(), trimAndTrunc(regName, MAX_LEN_CITY_REGION),
				trimAndTrunc(ctryName, MAX_LEN_CITY_COUNTRY), latitude, longitude, 
				trimAndTrunc(cityResID, MAX_LEN_CITY_DBPEDIA_RES), 
				new Timestamp(System.currentTimeMillis()), cityID);
			endUpdate();
		}
		catch (Exception e) {
			cancelUpdate();
			throw e;
		}
	}
	
	public void updateCityCrawlerTS()
	{
		updateCrawlerTS(getStmtUpdateCityTS());
	}

	public Utils.Pair<PrimaryKey, Boolean> insertLocation(String name, double longitude, 
		double latitude, PrimaryKey cityID) throws Exception
	{
		try {
			beginUpdate();
			
			ResultSet resSet = executeQuery(getStmtLocationExists(), 
								   lcTrimAndTrunc(name, MAX_LEN_LOCATION_NAME), longitude, 
								   latitude, cityID);
			if (resSet.next()) {
				endUpdate();
				return Utils.createPair(PrimaryKey.create(resSet), false);
			}
			
			Utils.Pair<String, PrimaryKey> insertLocationStmt = getStmtInsertLocation();
			PrimaryKey primKey = PrimaryKey.create(executeUpdate(insertLocationStmt.first,
									 trimAndTrunc(name, MAX_LEN_LOCATION_NAME), longitude,
									 latitude, cityID), insertLocationStmt.second);
			
			endUpdate();
			return Utils.createPair(primKey, true);
		}
		catch (Exception e) {
			cancelUpdate();
			throw e;
		}
	}
	
	public static synchronized DBConnection getInstance() throws Exception 
	{
		if (instance == null)
			instance = DB_CONNECTION_CLASS.newInstance();
		return instance;
	}
}

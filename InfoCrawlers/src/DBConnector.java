/**
 * @author Bernhard Weber
 */
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;


/**
 * Wrapper class for the actual database connection
 */
public abstract class DBConnector {
	
	//Constants which are loaded from config-file
	protected static Class<? extends DBConnector> DB_CONNECTOR_CLASS = null;
	protected int MAX_LEN_CRAWLER_DEBUG_LOG_HOST;
	protected int MAX_LEN_CRAWLER_DEBUG_LOG_CLASS_PATH;
	protected int MAX_LEN_CRAWLER_DEBUG_LOG_INFO;
	protected int MAX_LEN_CRAWLER_EXCEPT_LOG_HOST;
	protected int MAX_LEN_CRAWLER_EXCEPT_LOG_CLASS_PATH;
	protected int MAX_LEN_CRAWLER_EXCEPT_LOG_INFO;
	protected int MAX_LEN_CRAWLER_EXCEPT_LOG_MSG;
	protected int MAX_LEN_CRAWLER_EXCEPT_LOG_CLASS;
	protected int MAX_LEN_CRAWLER_EXCEPT_LOG_STACK;
	protected int MAX_LEN_CRAWLER_INFO_CLASS;
	protected int MAX_LEN_CRAWLER_INFO_SUMMARY;
	protected int MAX_LEN_CITY_NAME;
	protected int MAX_LEN_CITY_REGION;
	protected int MAX_LEN_CITY_COUNTRY;
	protected int MAX_LEN_CITY_DBPEDIA_RES;
	protected int MAX_LEN_LOCATION_NAME;
	protected int MAX_LEN_EVENT_NAME;
	protected int MAX_LEN_EVENT_DESC;
	protected int MAX_LEN_EVENT_TYPE;
	protected int MAX_LEN_EVENT_EVENTFUL_ID;
	protected int MAX_LEN_BAND_NAME;
	protected int MAX_LEN_BAND_DBPEDIA_RES;
	protected int MAX_LEN_ARTIST_NAME;
	protected int MAX_LEN_ARTIST_ALT_NAME;
	protected int MAX_LEN_ARTIST_DBPEDIA_RES;
		
	
	/**
	 * Available debug flags
	 */
	public static enum DebugFlag implements DebugUtils.DebugFlagBase {
		UPDATES(1),
		QUERY_RESULTS(2);
		
		public final int value;
		
		DebugFlag(int value) 
		{
			this.value = value;
		}

		public int toInt() 
		{
			return value;
		}
	}
	
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

	
	//Methods which needs to be implemented by derived classes
	protected abstract String getDriverName();
	protected abstract String getConnectionStr();
	protected abstract boolean queryPagingSupported();
	protected abstract void beginUpdate() throws Exception;
	protected abstract void endUpdate() throws Exception;
	protected abstract void cancelUpdate() throws Exception;
	protected abstract String getStmtCreateTblDebugInfoLogs();
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
	protected abstract String getStmtLogDebugInfo();
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
	
	private static DBConnector instance = null;
	protected Connection dbConn = null;
	protected String hostname = "Unknown";
	
	protected DBConnector() throws Exception	//Singleton class
	{
		DBConfig.load();
		//Load settings from database-config file
		MAX_LEN_CRAWLER_DEBUG_LOG_HOST = DBConfig.getMaxLenCrawlerDbgLogHost();
		MAX_LEN_CRAWLER_DEBUG_LOG_CLASS_PATH = DBConfig.getMaxLenCrawlerDbgLogClassPath();
		MAX_LEN_CRAWLER_DEBUG_LOG_INFO = DBConfig.getMaxLenCrawlerDbgLogInfo();
		MAX_LEN_CRAWLER_EXCEPT_LOG_HOST = DBConfig.getMaxLenCrawlerExceptLogHost();
		MAX_LEN_CRAWLER_EXCEPT_LOG_CLASS_PATH = DBConfig.getMaxLenCrawlerExceptLogClassPath();
		MAX_LEN_CRAWLER_EXCEPT_LOG_INFO = DBConfig.getMaxLenCrawlerExceptLogInfo();
		MAX_LEN_CRAWLER_EXCEPT_LOG_MSG = DBConfig.getMaxLenCrawlerExceptLogMsg();
		MAX_LEN_CRAWLER_EXCEPT_LOG_CLASS = DBConfig.getMaxLenCrawlerExceptLogClass();
		MAX_LEN_CRAWLER_EXCEPT_LOG_STACK = DBConfig.getMaxLenCrawlerExceptLogStack();
		MAX_LEN_CRAWLER_INFO_CLASS = DBConfig.getMaxLenCrawlerInfoClass();
		MAX_LEN_CRAWLER_INFO_SUMMARY = DBConfig.getMaxLenCrawlerInfoSummary();
		MAX_LEN_CITY_NAME = DBConfig.getMaxLenCityName();
		MAX_LEN_CITY_REGION = DBConfig.getMaxLenCityRegion();
		MAX_LEN_CITY_COUNTRY = DBConfig.getMaxLenCityCountry();
		MAX_LEN_CITY_DBPEDIA_RES = DBConfig.getMaxLenCityDBPediaRes();
		MAX_LEN_LOCATION_NAME = DBConfig.getMaxLenLocationName();
		MAX_LEN_EVENT_NAME = DBConfig.getMaxLenEventName();
		MAX_LEN_EVENT_DESC = DBConfig.getMaxLenEventDesc();
		MAX_LEN_EVENT_TYPE = DBConfig.getMaxLenEventType();
		MAX_LEN_EVENT_EVENTFUL_ID = DBConfig.getMaxLenEventEventfulId();
		MAX_LEN_BAND_NAME = DBConfig.getMaxLenBandName();
		MAX_LEN_BAND_DBPEDIA_RES = DBConfig.getMaxLenBandDBPediaRes();
		MAX_LEN_ARTIST_NAME = DBConfig.getMaxLenArtistName();
		MAX_LEN_ARTIST_ALT_NAME = DBConfig.getMaxLenArtistAltName();
		MAX_LEN_ARTIST_DBPEDIA_RES = DBConfig.getMaxLenArtistDBPediaRes();
		//Determine hostname
		try {
			hostname = InetAddress.getLocalHost().getHostAddress();
		} 
		catch (Exception e) {}
		try {
			hostname = InetAddress.getLocalHost().getHostName() + " (" + hostname + ")";
		} 
		catch (Exception e) {}
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
			DebugUtils.printDebugInfo("Results of query '" + query + "':\n" + tableDebugger,
				getClass(), DBConnector.class, DebugFlag.QUERY_RESULTS);
		}
		else
			DebugUtils.printDebugInfo("Query '" + query + "' returned no results!", getClass(),
				DBConnector.class, DebugFlag.QUERY_RESULTS);
		resSet.beforeFirst();
	}
	
	protected void debug_update(String stmt, Object ... args)
	{
		if (DebugUtils.canDebug(getClass(), DBConnector.class, DebugFlag.UPDATES)) {
			DebugUtils.printDebugInfo("Executing update:\n   " + Utils.wrapString(
				Utils.replaceEach(stmt, "?", "arg:'%s'", args), 50, "\n   "), getClass(), 
				DBConnector.class, false, DebugFlag.UPDATES);
		}
	}
	
	protected String trimAndTrunc(String s, int maxLen)
	{
		if (DebugUtils.canDebug(getClass(), DBConnector.class, DebugFlag.UPDATES) && s != null) { 
			int len = s.length();
			
			s = Utils.trimAndTrunc(s, maxLen);
			if (s.length() != len) 
				DebugUtils.printDebugInfo("WARNING: '" + s + "...' truncated to length '" + maxLen + 
					"'!", getClass(), DBConnector.class, false);
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
		ResultSet resSet = dbMeta.getTables(null, null, null, null);
		
		while (resSet.next()) {
			if (resSet.getString(3).equalsIgnoreCase(tableName))
				return true;
		}
		return false;
	}
	
	protected void createTable(Statement stmt, String tableName, String query,
		boolean dropExisting) throws Exception
	{
		try {
			if (tableExists(stmt, tableName)) {
				if (dropExisting) {
					DebugUtils.printDebugInfo("Dropping table '" + tableName + "' ...", getClass(),
						DBConnector.class, false, DebugFlag.UPDATES);
					try {
						dropTable(stmt, tableName);
					}
					catch (Exception e) {
						ExceptionHandler.handle("Failed to drop table '" + tableName + "'!", e, 
							getClass(), DBConnector.class, false);
						throw e;
					}
					DebugUtils.printDebugInfo("Dropping table '" + tableName + "' ... DONE",
						getClass(), DBConnector.class, false, DebugFlag.UPDATES);
				}
				else {
					DebugUtils.printDebugInfo("Skipping creation of table '" + tableName + 
						"' since it already exists!", getClass(), DBConnector.class, false);
					return;
				}
			}
			DebugUtils.printDebugInfo("Creating table '" + tableName + "' using statement:\n   " + 
				Utils.wrapString(query,	50, "\n   "), getClass(), DBConnector.class, false,
				DebugFlag.UPDATES);
			stmt.executeUpdate(query);
			DebugUtils.printDebugInfo("Creating table '" + tableName + "' ... DONE\n", getClass(),
				DBConnector.class, false, DebugFlag.UPDATES);
		}
		catch (Exception e) {
			ExceptionHandler.handle("Failed to create table '" + tableName + "'!", e, getClass(),
				DBConnector.class, false);
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
		
		if (DebugUtils.canDebug(getClass(), DBConnector.class, DebugFlag.QUERY_RESULTS)) {
			dbgQueryStr = Utils.replaceEach(query, "?", "arg:'%s'", args);
			DebugUtils.printDebugInfo("Executing query '" + dbgQueryStr +	"'...", getClass(),
				DBConnector.class, DebugFlag.QUERY_RESULTS);
		}
		
		PreparedStatement stmt = dbConn.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, 
							   		  ResultSet.CONCUR_READ_ONLY);
		setStatementArgs(stmt, args);
		
		ResultSet resSet = stmt.executeQuery();
		if (DebugUtils.canDebug(getClass(), DBConnector.class, DebugFlag.QUERY_RESULTS)) 
			debug_queryResult(dbgQueryStr, resSet);
		return resSet;
	}
	
	protected ResultSet executeQuery(String query) throws Exception
	{
		DebugUtils.printDebugInfo("Executing query '" + query + "'...", getClass(), 
			DBConnector.class);
		
		ResultSet resSet = dbConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, 
							   ResultSet.CONCUR_READ_ONLY).executeQuery(query);
		
		if (DebugUtils.canDebug(getClass(), DBConnector.class, DebugFlag.QUERY_RESULTS)) {
			debug_queryResult(query, resSet);
			resSet.beforeFirst();
		}
		return resSet;
	}
	
	protected PreparedStatement executeUpdate(String updStmt, Object ... args) throws Exception
	{
		DebugUtils.printDebugInfo("Executing update '" + updStmt + "'...", getClass(), 
			DBConnector.class, false, DebugFlag.UPDATES);
		
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
			
			DebugUtils.printDebugInfo(stmt.getUpdateCount() + " timestamps updated", getClass(),
				DBConnector.class);
		}
		catch (Exception e) {
			ExceptionHandler.handle("Failed to update crawler timestamps!", e, getClass(),
				DBConnector.class);
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
	
	public synchronized void createTables(boolean dropExisting) throws Exception
	{
		Statement stmt = dbConn.createStatement();
		
		try {
			createTable(stmt, "Crawler_debug_info_logs", getStmtCreateTblDebugInfoLogs(), 
				dropExisting);
			createTable(stmt, "Crawler_exception_logs", getStmtCreateTblExceptionLogs(), 
				dropExisting);
			createTable(stmt, "Crawler_infos", getStmtCreateTblCrawlerInfos(), dropExisting);
			createTable(stmt, "Cities", getStmtCreateTblCities(), dropExisting);
			createTable(stmt, "Locations", getStmtCreateTblLocations(), dropExisting);
			createTable(stmt, "Events", getStmtCreateTblEvents(), dropExisting);
			createTable(stmt, "Bands", getStmtCreateTblBands(), dropExisting);
			createTable(stmt, "Artists", getStmtCreateTblArtists(),	dropExisting);
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
			DebugUtils.printDebugInfo("Connected to DB...", getClass(), DBConnector.class);
		}
	}
	
	public synchronized void disconnect() throws Exception 
	{
		if (dbConn != null) {
			dbConn.close();
			dbConn = null;
			DebugUtils.printDebugInfo("Disconnected from DB...", getClass(), DBConnector.class);
		}
	}
	
	public boolean supportsQueryPaging()
	{
		return queryPagingSupported();
	}
	
	public synchronized void logException(final String classPath, final long threadID, 
		final String info, final Exception e, final String stackTrace) throws Exception
	{
		if (dbConn != null) {
			executeUpdate(getStmtLogException(), new Timestamp(System.currentTimeMillis()), 
				trimAndTrunc(hostname, MAX_LEN_CRAWLER_EXCEPT_LOG_HOST), threadID,
				trimAndTrunc(classPath, MAX_LEN_CRAWLER_EXCEPT_LOG_CLASS_PATH), 
				trimAndTrunc(info, MAX_LEN_CRAWLER_EXCEPT_LOG_INFO),
				trimAndTrunc(e.getMessage() + (e.getCause() != null ? " (cause: " + 
					e.getCause().getMessage() + " [" + e.getCause().getClass().getName() + 
					"])" : ""), MAX_LEN_CRAWLER_EXCEPT_LOG_MSG),
				trimAndTrunc(e.getClass().getName(), MAX_LEN_CRAWLER_EXCEPT_LOG_CLASS),
				trimAndTrunc(stackTrace, MAX_LEN_CRAWLER_EXCEPT_LOG_STACK));
		}
	}
	
	public synchronized void logDebugInfo(final String classPath, final long threadID, 
		final String info) throws Exception
	{
		if (dbConn != null) {
			executeUpdate(getStmtLogDebugInfo(), new Timestamp(System.currentTimeMillis()), 
				trimAndTrunc(hostname, MAX_LEN_CRAWLER_DEBUG_LOG_HOST), threadID,
				trimAndTrunc(classPath, MAX_LEN_CRAWLER_DEBUG_LOG_CLASS_PATH), 
				trimAndTrunc(info, MAX_LEN_CRAWLER_DEBUG_LOG_INFO));
		}
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
			ExceptionHandler.handle("Failed to write crawler-start info!", e, getClass(),
				DBConnector.class);
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
			ExceptionHandler.handle("Failed to write crawler-finish info!", e, getClass(),
				DBConnector.class);
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
			ExceptionHandler.handle("Failed to update crawler progress!", e, getClass(),
				DBConnector.class);
		}
	}
	
	public synchronized Utils.Pair<PrimaryKey, Boolean> insertEvent(String name, String desc, 
		String type, Date startTime, Date stopTime, String eventfulID, PrimaryKey locationID) 
		throws Exception
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
									 trimAndTrunc(type, MAX_LEN_EVENT_TYPE),
									 startTime, stopTime,
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
	
	public synchronized void insertEventPerformer(PrimaryKey eventID, PrimaryKey bandID)	
		throws Exception
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
	
	public synchronized int getIncompleteBandsCount(int dbUpdateInterval) throws Exception
	{
		ResultSet resSet = executeQuery(getStmtIncompleteBandsCount(), 
							   new Timestamp(System.currentTimeMillis()), dbUpdateInterval);
		return resSet.next() ? resSet.getInt(1) : 0; 
	}
	
	public synchronized ResultSet getIncompleteBands(int dbUpdateInterval, int pageIdx, 
		int pageSize) throws Exception
	{
		if (queryPagingSupported())
			return executeQuery(getStmtIncompleteBands(), new Timestamp(System.currentTimeMillis()),
					   dbUpdateInterval, pageIdx * pageSize, pageSize);
		return executeQuery(getStmtIncompleteBands(), new Timestamp(System.currentTimeMillis()),
				   dbUpdateInterval);
	}
	
	public synchronized Utils.Pair<PrimaryKey, Boolean> insertBand(String name) throws Exception
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
	
	public synchronized void updateBand(PrimaryKey bandID, String bandResID) throws Exception
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
	
	public synchronized void updateBandCrawlerTS()
	{
		updateCrawlerTS(getStmtUpdateBandTS());
	}
	
	public synchronized void insertBandArtist(PrimaryKey bandID, String name, String altName,
		char memberType, String resID) throws Exception
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
	
	public synchronized int getIncompleteCitiesCount(int dbUpdateInterval) throws Exception
	{
		ResultSet resSet = executeQuery(getStmtIncompleteCitiesCount(), 
							   new Timestamp(System.currentTimeMillis()), dbUpdateInterval);
		return resSet.next() ? resSet.getInt(1) : 0; 
	}
	
	public synchronized ResultSet getIncompleteCities(int dbUpdateInterval, int pageIdx, 
		int pageSize) throws Exception
	{
		if (queryPagingSupported())
			return executeQuery(getStmtIncompleteCities(),  
					   new Timestamp(System.currentTimeMillis()), dbUpdateInterval, 
					   pageIdx * pageSize, pageSize);
		return executeQuery(getStmtIncompleteCities(), new Timestamp(System.currentTimeMillis()), 
				   dbUpdateInterval);
	}
	
	public synchronized Utils.Pair<PrimaryKey, Boolean> insertCity(String name, String region, 
		String country) throws Exception
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
	
	public synchronized void updateCity(PrimaryKey cityID, double latitude, double longitude, 
		String regName,	String ctryName, String cityResID, String regResID, String ctryResID) 
		throws Exception
	{
		try {
			beginUpdate();
			executeUpdate(getStmtUpdateCity(), trimAndTrunc(regName, MAX_LEN_CITY_REGION),
				trimAndTrunc(ctryName, MAX_LEN_CITY_COUNTRY), latitude, longitude, 
				trimAndTrunc(cityResID, MAX_LEN_CITY_DBPEDIA_RES),
				trimAndTrunc(regResID, MAX_LEN_CITY_DBPEDIA_RES), 
				trimAndTrunc(ctryResID, MAX_LEN_CITY_DBPEDIA_RES), 
				new Timestamp(System.currentTimeMillis()), cityID);
			endUpdate();
		}
		catch (Exception e) {
			cancelUpdate();
			throw e;
		}
	}
	
	public synchronized void updateCityCrawlerTS()
	{
		updateCrawlerTS(getStmtUpdateCityTS());
	}

	public synchronized Utils.Pair<PrimaryKey, Boolean> insertLocation(String name, 
		double longitude, double latitude, PrimaryKey cityID) throws Exception
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
	
	public static synchronized DBConnector getInstance() throws Exception 
	{
		if (instance == null) {
			DB_CONNECTOR_CLASS = CrawlerConfig.getDBConnectorClass();
			instance = DB_CONNECTOR_CLASS.newInstance();
		}
		return instance;
	}
}

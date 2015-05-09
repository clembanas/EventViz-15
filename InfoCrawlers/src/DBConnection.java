/**
 * @author Bernhard Weber
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;


/**
 * Wrapper class/ interface for the actual database connection
 */
public abstract class DBConnection {
	
	public static boolean DEBUG = true;
	public static boolean DEBUG_RESULTS = true;
	public static Class<? extends DBConnection> DB_CONNECTION_CLASS = null;
	
	private static DBConnection instance = null;
	protected Connection dbConn = null;
	
	protected abstract String getDriverName();
	protected abstract String getConnectionStr();
	protected abstract void beginUpdate() throws Exception;
	protected abstract void endUpdate() throws Exception; 
	
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
	}
	
	protected ResultSet executeQuery(String query, Object ... args) throws Exception
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
	
	protected ResultSet executeQuery(String query) throws Exception
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
	
	public abstract ResultSet getIncompleteBands(int dbUpdateInterval) throws Exception;
	public abstract ResultSet getIncompleteCities(int dbUpdateInterval) throws Exception;
	public abstract void insertBandArtist(int bandID, String name, String altName, char memberType, 
		String resID) throws Exception;
	public abstract void updateBand(int bandID, String bandResID) throws Exception;
	public abstract void updateCity(int cityID, float latitude, float longitude, String regName, 
		String ctryName, String cityResID) throws Exception;
	
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
		}
	}
	
	public static synchronized DBConnection getInstance() throws Exception 
	{
		if (instance == null)
			instance = DB_CONNECTION_CLASS.newInstance();
		return instance;
	}
}

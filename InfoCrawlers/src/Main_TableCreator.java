/**
 * @author Bernhard Weber
 */


public class Main_TableCreator {
	
	//Debug settings
	public static final boolean DEBUG_DB = true;
	//Database settings
	public static final boolean DROP_EXISTING_RESOURCE = true;
	public static final Class<? extends DBConnection> DB_CONNECTION_CLASS = 
		DBConnection_MySQL.class;
	
	
	public static void main(String[] args) 
	{
		DBConnection dbConn = null;
		
		//Setup debug settings
		DBConnection.DEBUG = DEBUG_DB;
		//Setup database connection class
		DBConnection.DB_CONNECTION_CLASS = DB_CONNECTION_CLASS;
		//Create tables
		try {
			dbConn = DBConnection.getInstance();
			dbConn.connect();
			try {
				dbConn.createTables(DROP_EXISTING_RESOURCE);
			}
			catch (Exception e) {
				ExceptionHandler.handle(e, "Failed to create tables!");
			}
		}
		catch (Exception e) {
			ExceptionHandler.handle(e, "Failed to connect to database!");
		}
		finally {
			if (dbConn != null) {
				try {
					dbConn.disconnect();
				}
				catch (Exception e) {
					ExceptionHandler.handle(e, "Failed to disconnect from database!");
				}
			}
		}
	}
}

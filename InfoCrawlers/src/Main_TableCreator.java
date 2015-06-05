/**
 * @author Bernhard Weber
 */


public class Main_TableCreator {
	
	public static final boolean DROP_EXISTING_TABLES = true;
	
	
	public static void main(String[] args) 
	{
		DBConnector dbConn = null;
		
		try {
			CrawlerConfig.load();
			//Setup debug settings
			if (CrawlerConfig.canDbgDB())
				DebugUtils.debugClass(DBConnector.class, 
					(CrawlerConfig.canDbgDBUpdates() ? DBConnector.DebugFlag.UPDATES.toInt() : 0) | 
					(CrawlerConfig.canDbgDBResults() ? DBConnector.DebugFlag.QUERY_RESULTS.toInt() : 
						0));
			//Setup database connector class
			DBConnector.DB_CONNECTOR_CLASS = CrawlerConfig.getDBConnectorClass();
			//Create tables
			try {
				dbConn = DBConnector.getInstance();
				dbConn.connect();
				try {
					dbConn.createTables(DROP_EXISTING_TABLES);
				}
				catch (Exception e) {
					ExceptionHandler.handle("Failed to create tables!", e, Main_TableCreator.class);
				}
			}
			catch (Exception e) {
				ExceptionHandler.handle("Failed to connect to database!", e, 
					Main_TableCreator.class);
			}
			finally {
				if (dbConn != null) {
					try {
						dbConn.disconnect();
					}
					catch (Exception e) {
						ExceptionHandler.handle("Failed to disconnect from database!", e, 
							Main_TableCreator.class);
					}
				}
			}
		}
		catch (Exception e) {
			System.out.println("ERROR: " + e.getMessage() + "\nExiting now!");
			e.printStackTrace();
		}
	}
}

/**
 * @author Bernhard Weber
 */


public class Main_TableCreator {
	
	public static final boolean DROP_EXISTING_TABLES = true;
	
	
	public static void main(String[] args) 
	{
		DBConnector dbConn = null;
		
		try {
			System.out.println("Table-creator started");
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
					System.out.println("\nCreating tables ...");
					dbConn.createTables(DROP_EXISTING_TABLES);
					System.out.println("Creating tables ... DONE");
				}
				catch (Exception e) {
					ExceptionHandler.handle("Failed to create tables!", e, Main_TableCreator.class,
						false);
				}
			}
			catch (Exception e) {
				ExceptionHandler.handle("Failed to connect to database!", e, 
					Main_TableCreator.class, false);
			}
			finally {
				if (dbConn != null) {
					try {
						dbConn.disconnect();
					}
					catch (Exception e) {
						ExceptionHandler.handle("Failed to disconnect from database!", e, 
							Main_TableCreator.class, false);
					}
				}
			}
		}
		catch (Exception e) {
			System.out.println("ERROR: " + e.getMessage() + "\nExiting now!");
			e.printStackTrace();
		}
		System.out.println("\nTable-creator finished!");
	}
}

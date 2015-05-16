/**
 * @author Bernhard Weber
 */


/**
 * Partial implementation for working with a SQL conform database.
 * Contains all necessary statements in a SQL standard conforming syntax. 
 * Note: The create table statements are omitted, since neither Cloudera Impala nor Derby supports  
 *       SQL conform create table statements with primary keys.
 */
public abstract class DBConnection_SQLConform extends DBConnection {
	
	protected DBConnection_SQLConform() {} 	//Singleton class
	
	protected String getStmtCreateTblExceptionLogs()
	{
		return "CREATE TABLE Crawler_exception_logs(" +
				   "CITY_CRAWLER_TS TIMESTAMP, " +
				   "MESSAGE VARCHAR(" + MAX_LEN_CRAWLER_EXCEPT_LOG_MSG + "), " + 
				   "EXCEPTION_CLASS VARCHAR(" + MAX_LEN_CRAWLER_EXCEPT_LOG_CLASS + "), " +
				   "STACK_TRACE VARCHAR(" + MAX_LEN_CRAWLER_EXCEPT_LOG_STACK + "))";
	}
	
	protected String getStmtCreateTblCrawlerInfos()
	{
		return "CREATE TABLE Crawler_infos(" +
				   "CRAWLER_CLASS VARCHAR(" + MAX_LEN_CRAWLER_INFO_CLASS + "), " +
				   "STARTED TIMESTAMP, " + 
				   "FINISHED TIMESTAMP, " +
				   "PROGRESS INT, " +
				   "SUMMARY VARCHAR(" + MAX_LEN_CRAWLER_INFO_SUMMARY + "))";
	}
	
	protected String getStmtDropTable(String tableName)
	{
		return "DROP TABLE " + tableName;
	}
	
	protected String getStmtLogException()
	{
		return "INSERT INTO Crawler_exception_logs (ts, message, exception_class, stack_trace) " +
				   "VALUES(?, ?, ?, ?)";
	}
	
	protected String getStmtInsertCrawlerInfoStarted()
	{
		return "INSERT INTO Crawler_infos (crawler_class, started, progress) VALUES(?, ?, 0)";
	}
	
	protected String getStmtUpdateCrawlerInfoStarted()
	{
		return "UPDATE Crawler_infos SET started = ?, finished = NULL, progress = 0, " +
				   "summary = NULL WHERE crawler_class = ?";
	}
	
	protected String getStmtUpdateCrawlerInfoFinished()
	{
		return "UPDATE Crawler_infos SET finished = ?, progress = 100, summary = ? " +
				   "WHERE crawler_class = ?";
	}
	
	protected String getStmtUpdateCrawlerInfoProgress()
	{
		return "UPDATE Crawler_infos SET progress = ? WHERE crawler_class = ?";
	}
	
	protected String getStmtEventExists() 
	{
		return "SELECT id FROM events WHERE eventful_id = ?";
	}
	
	protected String getStmtEventPerformerExists() 
	{
		return "SELECT event_id FROM eventPerformers WHERE event_id = ? and band_id = ?";
	}
	
	protected String getStmtInsertEventPerformer() 
	{
		return "INSERT INTO eventPerformers (event_id, band_id) VALUES(?, ?)";
	}
	
	protected String getStmtUpdateBandTS() 
	{
		return "UPDATE bands SET band_crawler_ts = ? WHERE band_crawler_ts IS NULL";
	}
	
	protected String getStmtBandExists() 
	{
		return "SELECT id FROM bands WHERE lower(name) = ?";
	}
	
	protected String getStmtUpdateBand(boolean tsOnly) 
	{
		if (tsOnly)
			return "UPDATE bands SET band_crawler_ts = ? WHERE id = ?";
		return "UPDATE bands SET dbpedia_resource = ?, band_crawler_ts = ? WHERE id = ?";
	}
	
	protected String getStmtBandMemberExists()
	{
		return "SELECT band_id FROM bandmembers WHERE band_id = ? AND artist_id = ?";
	}
	
	protected String getStmtInsertBandMember() 
	{
		return "INSERT INTO bandmembers (band_id, artist_id, member_type) VALUES(?, ?, ?)";
	}
	
	protected String getStmtUpdateBandMember() 
	{
		return "UPDATE bandmembers SET member_type = ? WHERE band_id = ? AND artist_id = ?";
	}
	
	protected String getStmtArtistExists() 
	{
		return "SELECT id, name, dbpedia_resource FROM artists WHERE LCASE(name) = ?";
	}
	
	protected String getStmtUpdateArtist() 
	{
		return "UPDATE artists SET dbpedia_resource = ?, alternate_name = ? WHERE id = ?";
	}	
	
	protected String getStmtUpdateCityTS() 
	{
		return "UPDATE cities SET city_crawler_ts = ? WHERE city_crawler_ts IS NULL";
	}
	
	protected String getStmtCityExists(boolean regionExists, boolean countryExists) 
	{
		String query = "SELECT id FROM cities WHERE lower(name) = ? ";
		
		if (regionExists)
			query += "AND region IS NOT NULL AND lower(region) = ? ";
		else 
			query += "AND region IS NULL ";
		if (countryExists)
			query += "AND country IS NOT NULL AND lower(country) = ?";
		else 
			query += "AND country IS NULL";
		return query;
	}

	protected String getStmtUpdateCity() 
	{
		return "UPDATE cities " +
				   "SET region = ?, country = ?, latitude = ?," +
				   "    longitude = ?, dbpedia_resource = ?, " +
				   "    city_crawler_ts = ? WHERE id = ?";
	}
	
	protected String getStmtLocationExists() 
	{
		return "SELECT id FROM locations WHERE lower(name) = ? AND longitude = ? AND " +
				   "latitude = ? AND city_id = ?";
	}
}

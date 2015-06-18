/**
 * @author Bernhard Weber
 */


/**
 * Partial implementation for working with a SQL conform database.
 * Contains all necessary statements in a SQL standard conforming syntax. 
 */
public abstract class DBConnector_SQLConform extends DBConnector {
	
	protected DBConnector_SQLConform() throws Exception {}	  //Singleton class
	
	protected String getStmtCreateTblDebugInfoLogs()
	{
		return "CREATE TABLE Crawler_debug_info_logs(" +
				   "ts TIMESTAMP, " +
				   "hostname VARCHAR(" + MAX_LEN_CRAWLER_DEBUG_LOG_HOST + "), " +
				   "thread_id LONG, " +
				   "class_path VARCHAR(" + MAX_LEN_CRAWLER_DEBUG_LOG_CLASS_PATH + "), " +
				   "info VARCHAR(" + MAX_LEN_CRAWLER_DEBUG_LOG_INFO + "))";
	}
	
	protected String getStmtCreateTblExceptionLogs()
	{
		return "CREATE TABLE Crawler_exception_logs(" +
				   "ts TIMESTAMP, " +
				   "hostname VARCHAR(" + MAX_LEN_CRAWLER_EXCEPT_LOG_HOST + "), " +
				   "thread_id LONG, " +
				   "class_path VARCHAR(" + MAX_LEN_CRAWLER_EXCEPT_LOG_CLASS_PATH + "), " +
				   "info VARCHAR(" + MAX_LEN_CRAWLER_EXCEPT_LOG_INFO + "), " +
				   "message VARCHAR(" + MAX_LEN_CRAWLER_EXCEPT_LOG_MSG + "), " + 
				   "exception_class VARCHAR(" + MAX_LEN_CRAWLER_EXCEPT_LOG_CLASS + "), " +
				   "stack_trace VARCHAR(" + MAX_LEN_CRAWLER_EXCEPT_LOG_STACK + "))";
	}
	
	protected String getStmtCreateTblCrawlerInfos()
	{
		return "CREATE TABLE Crawler_infos(" +
				   "crawler_class VARCHAR(" + MAX_LEN_CRAWLER_INFO_CLASS + "), " +
				   "started TIMESTAMP, " + 
				   "finished TIMESTAMP, " +
				   "progress INT, " +
				   "summary VARCHAR(" + MAX_LEN_CRAWLER_INFO_SUMMARY + "))";
	}
	
	protected String getStmtDropTable(String tableName)
	{
		return "DROP TABLE " + tableName;
	}
	
	protected String getStmtLogDebugInfo()
	{
		return "INSERT INTO Crawler_debug_info_logs (ts, hostname, thread_id, class_path, info) " +
				   "VALUES(?, ?, ?, ?, ?)";
	}
	
	protected String getStmtLogCount()
	{
		return "SELECT COUNT(*) FROM Crawler_debug_info_logs";
	}
	
	protected String getStmtClearLogs()
	{
		return "DELETE FROM Crawler_debug_info_logs";
	}
	
	protected String getStmtLogException()
	{
		return "INSERT INTO Crawler_exception_logs (ts, hostname, thread_id, class_path, info, " +
				   "message, exception_class, stack_trace) " +
				   "VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
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
		return "SELECT id FROM Events WHERE eventful_id = ?";
	}
	
	protected Utils.Pair<String, PrimaryKey> getStmtInsertEvent() 
	{
		return Utils.createPair("INSERT INTO Events (name, description, event_type, start_time, " +
				   "end_time, eventful_id, location_id) VALUES(?, ?, ?, ?, ?, ?, ?)", null);
	}
	
	protected String getStmtEventPerformerExists() 
	{
		return "SELECT event_id FROM EventPerformers WHERE event_id = ? and band_id = ?";
	}
	
	protected String getStmtInsertEventPerformer() 
	{
		return "INSERT INTO EventPerformers (event_id, band_id) VALUES(?, ?)";
	}
	
	protected String getStmtUpdateBandTS() 
	{
		return "UPDATE Bands SET band_crawler_ts = ? WHERE band_crawler_ts IS NULL";
	}
	
	protected String getStmtBandExists() 
	{
		return "SELECT id FROM Bands WHERE lower(name) = ?";
	}
	
	protected Utils.Pair<String, PrimaryKey> getStmtInsertBand() 
	{
		return Utils.createPair("INSERT INTO Bands (name) VALUES(?)", null);
	}
	
	protected String getStmtUpdateBand(boolean tsOnly) 
	{
		if (tsOnly)
			return "UPDATE Bands SET band_crawler_ts = ? WHERE id = ?";
		return "UPDATE Bands SET dbpedia_resource = ?, band_crawler_ts = ? WHERE id = ?";
	}
	
	protected String getStmtBandMemberExists()
	{
		return "SELECT band_id FROM BandMembers WHERE band_id = ? AND artist_id = ?";
	}
	
	protected String getStmtInsertBandMember() 
	{
		return "INSERT INTO BandMembers (band_id, artist_id, member_type) VALUES(?, ?, ?)";
	}
	
	protected String getStmtUpdateBandMember() 
	{
		return "UPDATE BandMembers SET member_type = ? WHERE band_id = ? AND artist_id = ?";
	}
	
	protected String getStmtArtistExists() 
	{
		return "SELECT id, name, dbpedia_resource FROM Artists WHERE LCASE(name) = ?";
	}
	
	protected Utils.Pair<String, PrimaryKey> getStmtInsertArtist() 
	{
		return Utils.createPair("INSERT INTO Artists (name, alternate_name, dbpedia_resource) " +
			      "VALUES(?, ?, ?)", null);
	}
	
	protected String getStmtUpdateArtist() 
	{
		return "UPDATE Artists SET dbpedia_resource = ?, alternate_name = ? WHERE id = ?";
	}	
	
	protected String getStmtUpdateCityTS() 
	{
		return "UPDATE Cities SET city_crawler_ts = ? WHERE city_crawler_ts IS NULL";
	}
	
	protected String getStmtCityExists(boolean regionExists, boolean countryExists) 
	{
		String query = "SELECT id FROM Cities WHERE lower(name) = ? ";
		
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
	
	protected Utils.Pair<String, PrimaryKey> getStmtInsertCity() 
	{
		return Utils.createPair("INSERT INTO Cities (name, region, country) VALUES(?, ?, ?)", null);
	}

	protected String getStmtUpdateCity() 
	{
		return "UPDATE Cities " +
				   "SET region = ?, country = ?, longitude = ?, latitude = ?, " +
				   "    dbpedia_res_city = ?, dbpedia_res_region = ?, dbpedia_res_country = ?, " +
				   "    city_crawler_ts = ? WHERE id = ?";
	}
	
	protected String getStmtLocationExists() 
	{
		return "SELECT id FROM Locations WHERE lower(name) = ? AND longitude = ? AND " +
				   "latitude = ? AND city_id = ?";
	}
	
	protected Utils.Pair<String, PrimaryKey> getStmtInsertLocation() 
	{
		return Utils.createPair("INSERT INTO Locations (name, longitude, latitude, city_id) " +
				   "VALUES(?, ?, ?, ?)", null);
	}
}

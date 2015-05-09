/**
 * @author Bernhard Weber
 */
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.concurrent.locks.*;


/**
 * Actual DBConnection implementation for Derby database
 * 
 * @deprecated
 */
public class DBConnection_Derby extends DBConnection {
	
	public static final String DRIVER_NAME = "org.apache.derby.jdbc.EmbeddedDriver";
	public static final String CONNECTION_STR = "jdbc:derby:C:/temp/derby/Events/db;create=true";
	
	private Lock updateLock = new ReentrantLock();
	
	private int insertArtist(String name, String altName, String resID) throws Exception
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
	
	private void updateArtist(int artistID, String name, String altName, String currResID, 
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
	
	private void insertBandMember(int bandID, int artistID, char memberType) throws Exception
	{
		PreparedStatement stmnt = dbConn.prepareStatement("INSERT INTO bandmembers (band_id, " +
									  "artist_id, member_type) VALUES(?, ?, ?)");
									  
		stmnt.setInt(1, bandID);
		stmnt.setInt(2, artistID);
		stmnt.setString(3, String.valueOf(memberType));
		stmnt.executeUpdate();
	}
	
	private void updateBandMember(int bandID, int artistID, char memberType) throws Exception
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
	
	protected DBConnection_Derby() {} 	//Singleton class
	
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
	
	public ResultSet getIncompleteBands(int dbUpdateInterval) throws Exception
	{
		return executeQuery("SELECT id, name FROM bands WHERE " +
					"(band_crawler_ts IS NULL OR " +
				    "{fn TIMESTAMPDIFF(SQL_TSI_HOUR, band_crawler_ts, ?)} >= ?) " +
					"AND id NOT IN (SELECT band_id AS id FROM bandmembers)", 
					new Timestamp(System.currentTimeMillis()), dbUpdateInterval);
	}
	
	public ResultSet getIncompleteCities(int dbUpdateInterval) throws Exception
	{
		return executeQuery("SELECT id, name, region, country FROM cities " +
				   "WHERE city_crawler_ts IS NULL OR " +
				   "{fn TIMESTAMPDIFF(SQL_TSI_HOUR, city_crawler_ts, ?)} >= ?",
				   new Timestamp(System.currentTimeMillis()), dbUpdateInterval); 
	}
	
	public synchronized void insertBandArtist(int bandID, String name, String altName, 
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
	
	public synchronized void updateBand(int bandID, String bandResID) throws Exception
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
	
	public synchronized void updateCity(int cityID, float latitude, float longitude, String regName,
		String ctryName, String cityResID) throws Exception
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

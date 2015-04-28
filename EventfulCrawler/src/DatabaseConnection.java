import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
	private Connection conn = null;
	private Statement stmt = null;
	
	public DatabaseConnection(){
		connect();
	}

	public void connect() {
		try {
			Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
			conn = DriverManager.getConnection("jdbc:derby:C:/temp/derby/Events/db;create=true");
			stmt = conn.createStatement();
			stmt.executeUpdate("Drop table events");
		} catch (final SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		createTableCities();
		createTableLocations();
		createTableEvents();
		createTableBands();
		createTableArtists();
		createTableEventPerformers();
		createTableBandMembers();
	}
	
	private void createTableCities(){
		try{
			final String createQ = "CREATE TABLE Cities(ID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), " +
									"NAME VARCHAR(255) NOT NULL, " +
									"REGION VARCHAR(255)," +
									"COUNTRY VARCHAR(255)," +
									"POSTAL_CODE VARCHAR(16)," +
									"LONGITUDE FLOAT," +
									"LATITUDE FLOAT," +
									"CITY_CRAWLER_TS TIMESTAMP, "+
									"DBPEDIA_RESOURCE VARCHAR(255))";
			stmt.executeUpdate(createQ);
		}catch(SQLException e){
			if(e.getSQLState().equals("X0Y32")) { return; }
			else { e.printStackTrace(); }
		}
	}
	
	private void createTableLocations(){
		try{
			final String createQ = "CREATE TABLE Locations(ID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
									"NAME VARCHAR(255) NOT NULL," +
									"LONGITUDE FLOAT NOT NULL," +
									"LATITUDE FLOAT NOT NULL," +
									"CITY_ID INTEGER NOT NULL)";
			stmt.executeUpdate(createQ);
		}catch(SQLException e){
			if(e.getSQLState().equals("X0Y32")) { return; }
			else { e.printStackTrace(); }
		}
	}
	
	private void createTableEvents(){
		try{
			final String createQ = "CREATE TABLE Events(ID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
									"NAME VARCHAR(255) NOT NULL," +
									"DESCRIPTION VARCHAR(255)," +
									"EVENT_TYPE VARCHAR(50)," +
									"EVENTFUL_ID VARCHAR(50)," +
									"LOCATION_ID INTEGER NOT NULL)";
			stmt.executeUpdate(createQ);
		}catch(SQLException e){
			if(e.getSQLState().equals("X0Y32")) { return; }
			else { e.printStackTrace(); }
		}
	}
	
	private void createTableBands(){
		try{
			final String createQ = "CREATE TABLE Bands(ID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
									"NAME VARCHAR(255) NOT NULL, BAND_CRAWLER_TS TIMESTAMP, " +
									"DBPEDIA_RESOURCE VARCHAR(255))";
			stmt.executeUpdate(createQ);
		}catch(SQLException e){
			if(e.getSQLState().equals("X0Y32")) { return; }
			else { e.printStackTrace(); }
		}
	}
	
	private void createTableArtists(){
		try{
			final String createQ = "CREATE TABLE Artists(ID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
									"NAME VARCHAR(255) NOT NULL, ALTERNATE_NAME VARCHAR(255), DBPEDIA_RESOURCE VARCHAR(255))";
			stmt.executeUpdate(createQ);
		}catch(SQLException e){
			if(e.getSQLState().equals("X0Y32")) { return; }
			else { e.printStackTrace(); }
		}
	}
	
	private void createTableEventPerformers(){
		try{
			final String createQ = "CREATE TABLE EventPerformers(EVENT_ID INTEGER NOT NULL," +
									"BAND_ID INTEGER NOT NULL)";
			stmt.executeUpdate(createQ);
		}catch(SQLException e){
			if(e.getSQLState().equals("X0Y32")) { return; }
			else { e.printStackTrace(); }
		}
	}
	
	private void createTableBandMembers(){
		try{
			final String createQ = "CREATE TABLE BandMembers(ARTIST_ID INTEGER NOT NULL," +
									"BAND_ID INTEGER NOT NULL, MEMBER_TYPE CHAR)";
			stmt.executeUpdate(createQ);
		}catch(SQLException e){
			if(e.getSQLState().equals("X0Y32")) { return; }
			else { e.printStackTrace(); }
		}
	}
	
	public int insertCity(String name, String region, String country, String postal, double latitude, double longitude){
		try{
			PreparedStatement sta = conn.prepareStatement("SELECT id FROM Cities WHERE NAME = ? AND POSTAL_CODE = ?");
			sta.setString(1, name);
			sta.setString(2, postal);
			ResultSet rs = sta.executeQuery();
			if(!rs.next()){
				sta = conn.prepareStatement("INSERT INTO Cities(name, region, country, " +
						"postal_code, longitude, latitude, city_crawler_ts, dbpedia_resource) " +
						"VALUES(?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
				sta.setString(1, name);
				sta.setString(2, region);
				sta.setString(3, country);
				sta.setString(4, postal);
				sta.setFloat(5, 0f);
				sta.setFloat(6, 0f);
				sta.setNull(7, java.sql.Types.TIMESTAMP);
				sta.setNull(8, java.sql.Types.TIMESTAMP);
				sta.executeUpdate();
				rs = sta.getGeneratedKeys();
				rs.next();
				return rs.getInt(1);
			}
			else{
				return rs.getInt("ID");
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		return 0;
	}
	
	public int insertLocation(String name, double longitude, double latitude, int cityID){
		try{
			PreparedStatement sta = conn.prepareStatement("SELECT id FROM Locations WHERE NAME = ? AND CITY_ID = ?");
			sta.setString(1, name);
			sta.setInt(2, cityID);
			ResultSet rs = sta.executeQuery();
			if(!rs.next()){
				sta = conn.prepareStatement("INSERT INTO Locations(name, longitude, latitude, city_ID) VALUES(?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
				sta.setString(1, name);
				sta.setDouble(2, longitude);
				sta.setDouble(3, latitude);
				sta.setInt(4, cityID);
				sta.executeUpdate();
				rs = sta.getGeneratedKeys();
				rs.next();
				return rs.getInt(1);
			}
			else{
				return rs.getInt("ID");
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		return 0;
	}
	
	public int insertEvent(String name, String description, String eventType, String eventfulID, int locationID){
		name = name.replaceAll("'", "''");
		try{
			PreparedStatement sta = conn.prepareStatement("SELECT id FROM Events WHERE EVENTFUL_ID = ?");
			sta.setString(1, eventfulID);
			ResultSet rs = sta.executeQuery();
			if(!rs.next()){
				sta = conn.prepareStatement("INSERT INTO Events(name, description, event_type, eventful_ID, location_ID) VALUES(?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
				sta.setString(1, name);
				sta.setString(2, description.substring(0, description.length() > 250 ? 250 : description.length()));
				sta.setString(3, eventType);
				sta.setString(4, eventfulID);
				sta.setInt(5, locationID);
				sta.executeUpdate();
				rs = sta.getGeneratedKeys();
				rs.next();
				return rs.getInt(1);
			}
			else{
				return rs.getInt("ID");
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		return 0;
	}
	
	public int insertBand(String name){
		name = name.replaceAll("'", "''");
		try{
			PreparedStatement sta = conn.prepareStatement("SELECT id FROM Bands WHERE Name = ?");
			sta.setString(1, name);
			ResultSet rs = sta.executeQuery();
			if(!rs.next()){
				sta = conn.prepareStatement("INSERT INTO Bands(name, band_crawler_ts, " +
						"dbpedia_resource) VALUES(?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
				sta.setString(1, name);
				sta.setNull(2, java.sql.Types.TIMESTAMP);
				sta.setNull(3, java.sql.Types.TIMESTAMP);
				sta.executeUpdate();
				rs = sta.getGeneratedKeys();
				rs.next();
				return rs.getInt(1);
			}
			else{
				return rs.getInt("ID");
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		return 0;
	}
	
	public void insertEventPerformer(int eventId, int bandId){
		try{
			PreparedStatement sta = conn.prepareStatement("SELECT * FROM EventPerformers WHERE Event_ID = ? AND Band_ID = ?");
			sta.setInt(1, eventId);
			sta.setInt(2, bandId);
			ResultSet rs = sta.executeQuery();
			if(!rs.next()){
				sta = conn.prepareStatement("INSERT INTO EventPerformers(Event_ID, Band_ID) VALUES(?, ?)", Statement.RETURN_GENERATED_KEYS);
				sta.setInt(1, eventId);
				sta.setInt(2, bandId);
				sta.executeUpdate();
				rs = sta.getGeneratedKeys();
				rs.next();
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
	}

	public void close(){
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
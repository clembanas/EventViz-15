package database;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import containers.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventViz15_DB_MySQLAccess {
	private static MysqlDataSource dataSource = new MysqlDataSource();
	private static Connection conn;

	private static String db_host = "138.232.65.248";
	private static String db_name = "EventViz15";
	private static String db_user = "EventVizUser";
	private static String db_pword = "e1V2i3Z";

	public static void initializeDBAccess() throws SQLException {
		EventViz15_DB_MySQLAccess.dataSource.setServerName(db_host);
		EventViz15_DB_MySQLAccess.dataSource.setDatabaseName(db_name);
		EventViz15_DB_MySQLAccess.dataSource.setUser(db_user);
		EventViz15_DB_MySQLAccess.dataSource.setPassword(db_pword);
		EventViz15_DB_MySQLAccess.conn = dataSource.getConnection();
	}

	public static EventVizModelPopulationObject getEventById(int eventId) throws SQLException {
		String sqlQuery = "SELECT e.id AS eventId, e.name AS eventName, e.description AS eventDescription, e.event_type, e.start_time, e.end_time, e.location_id, l.name AS locationName, c.id AS cityId, c.name AS cityName, c.region, c.country, c.latitude, c.longitude, c.dbpedia_res_city, c.dbpedia_res_region, c.dbpedia_res_country FROM Events AS e JOIN Locations AS l ON e.location_id=l.id JOIN Cities AS c ON l.city_id=c.id WHERE e.id=?";
		PreparedStatement stmt = conn.prepareStatement(sqlQuery);
		stmt.setInt(1, eventId);
		ResultSet rs = stmt.executeQuery();

		rs.next();
		String eventName = rs.getString("eventName");
		String eventDescription = rs.getString("eventDescription");
		String event_type = rs.getString("event_type");
		Date start_Date = rs.getDate("start_time");
		Date end_Date = rs.getDate("end_time");
		Time start_Time = rs.getTime("start_time");
		Time end_Time = rs.getTime("end_time");
		int location_id = rs.getInt("location_id");
		EventVizEvent event = new EventVizEvent(eventId, eventName, eventDescription, event_type, start_Date, end_Date, start_Time, end_Time, location_id);
	
		String locationName = rs.getString("locationName");
		int cityId = rs.getInt("cityId");
		String cityName = rs.getString("cityName");
		String region = rs.getString("region");
		String country = rs.getString("country");
		float latitude = rs.getFloat("latitude");
		float longitude = rs.getFloat("longitude");
		String dbpedia_res_city = rs.getString("dbpedia_res_city");
		String dbpedia_res_region = rs.getString("dbpedia_res_region");
		String dbpedia_res_country = rs.getString("dbpedia_res_country");
		EventVizLocation location = new EventVizLocation(locationName, new EventVizCity(cityId, cityName, region, country, latitude, longitude, dbpedia_res_city, dbpedia_res_region, dbpedia_res_country));
		
		rs.close();
		stmt.close();
		
		sqlQuery = "SELECT b.name FROM Events AS e JOIN EventPerformers AS p ON e.id=p.event_id JOIN Bands AS b ON p.band_id=b.id WHERE e.id=?";
		stmt = conn.prepareStatement(sqlQuery);
		stmt.setInt(1, eventId);
		rs = stmt.executeQuery();
		
		List<EventVizBand> bands = new ArrayList<>();
		while(rs.next()) {
			EventVizBand band = EventViz15_DB_MySQLAccess.getBand(rs.getString("name"));
			bands.add(band);
		}
		rs.close();
		stmt.close();
		
		return new EventVizModelPopulationObject(event, location, bands);
	}

	private static EventVizBand getBand(String bandName) throws SQLException {
		String sqlQuery = "SELECT * FROM Bands WHERE name=?";
		PreparedStatement stmt = conn.prepareStatement(sqlQuery);
		stmt.setString(1, bandName);
		ResultSet rs = stmt.executeQuery();

		rs.next();
		int bandId = rs.getInt("id");
		String band_dbpedia_resource = rs.getString("dbpedia_resource");
		rs.close();
		stmt.close();

		//get info about members
		sqlQuery = "SELECT a.id, a.name, a.alternate_name, bm.member_type, a.dbpedia_resource FROM Bands AS b JOIN BandMembers AS bm ON b.id=bm.band_id JOIN Artists AS a ON bm.artist_id=a.id WHERE b.name=?";
		stmt = conn.prepareStatement(sqlQuery);
		stmt.setString(1, bandName);
		rs = stmt.executeQuery();

		List<EventVizBandMember> members = new ArrayList<>();
		while(rs.next()) {
			int id = rs.getInt("id");
			String name = rs.getString("name");
			String alternate_name = rs.getString("alternate_name");
			String member_type = rs.getString("member_type");
			String dbpedia_resource = rs.getString("dbpedia_resource");
			members.add(new EventVizBandMember(id, name, alternate_name, member_type, dbpedia_resource));
		}
		EventVizBand band = new EventVizBand(bandId, bandName, members, band_dbpedia_resource);
		rs.close();
		stmt.close();
		return band;
	}

	public static void closeDBAccess() {
		try {
			conn.close();
		} catch (SQLException e) {
			// ignore
		}
	}
	
}
package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import containers.EventVizArtist;
import containers.EventVizBand;
import containers.EventVizBandMember;
import containers.EventVizCity;
import containers.EventVizEvent;
import containers.EventVizEventBasics;
import containers.EventVizLocation;
import containers.EventVizModelPopulationObject;

public class EventViz15_DB_MySQLAccess {
	private static MysqlDataSource dataSource = new MysqlDataSource();
	private static Connection conn;
	
	public static void initializeDBAccess() throws SQLException {
		EventViz15_DB_MySQLAccess.dataSource.setServerName("138.232.65.248");
		EventViz15_DB_MySQLAccess.dataSource.setDatabaseName("EventViz15");
		EventViz15_DB_MySQLAccess.dataSource.setUser("EventVizUser");
		EventViz15_DB_MySQLAccess.dataSource.setPassword("e1V2i3Z");
		EventViz15_DB_MySQLAccess.conn = dataSource.getConnection();
	}
	
	public static List<EventVizEventBasics> getEvents() throws SQLException {
		String sqlQuery = "SELECT eventful_id, latitude, longitude FROM Events JOIN Locations ON Events.location_id=Locations.id";
		PreparedStatement stmt = conn.prepareStatement(sqlQuery);
		ResultSet rs = stmt.executeQuery();
		
		List<EventVizEventBasics> events = new ArrayList<EventVizEventBasics>();
		while(rs.next()) {
			String eventful_id = rs.getString("eventful_id");
			float latitude = rs.getFloat("latitude");
			float longitude = rs.getFloat("longitude");
			events.add(new EventVizEventBasics(eventful_id, latitude, longitude));
		}
		rs.close();
		stmt.close();
		return events;
	}
	
	public static EventVizModelPopulationObject getEventById(String eventful_id) throws SQLException {
		String sqlQuery = "SELECT e.id AS eventId, e.name AS eventName, e.description AS eventDescription, e.event_type, e.location_id, l.name AS locationName, c.id AS cityId, c.name AS cityName, c.region, c.country, c.latitude, c.longitude, c.dbpedia_resource FROM Events AS e JOIN Locations AS l ON e.location_id=l.id JOIN Cities AS c ON l.city_id=c.id WHERE eventful_id=?";
		PreparedStatement stmt = conn.prepareStatement(sqlQuery);
		stmt.setString(1, eventful_id);
		ResultSet rs = stmt.executeQuery();
		
		rs.next();
		int eventId = rs.getInt("eventId");
		String eventName = rs.getString("eventName");
		String eventDescription = rs.getString("eventDescription");
		String event_type = rs.getString("event_type");
		int location_id = rs.getInt("location_id");
		EventVizEvent event = new EventVizEvent(eventId, eventName, eventDescription, event_type, location_id, eventful_id);
	
		String locationName = rs.getString("locationName");
		int cityId = rs.getInt("cityId");
		String cityName = rs.getString("cityName");
		String region = rs.getString("region");
		String country = rs.getString("country");
		float latitude = rs.getFloat("latitude");
		float longitude = rs.getFloat("longitude");
		String dbpedia_resource = rs.getString("dbpedia_resource");
		EventVizLocation location = new EventVizLocation(locationName, new EventVizCity(cityId, cityName, region, country, latitude, longitude, dbpedia_resource));
		
		rs.close();
		stmt.close();
		
		sqlQuery = "SELECT b.name FROM Events AS e JOIN EventPerformers AS p ON e.id=p.event_id JOIN Bands AS b ON p.band_id=b.id WHERE eventful_id=?";
		stmt = conn.prepareStatement(sqlQuery);
		stmt.setString(1, eventful_id);
		rs = stmt.executeQuery();
		
		List<EventVizBand> bands = new ArrayList<EventVizBand>();
		while(rs.next()) {
			EventVizBand band = EventViz15_DB_MySQLAccess.getBand(rs.getString("name"));
			bands.add(band);
		}
		rs.close();
		stmt.close();
		
		return new EventVizModelPopulationObject(event, location, bands);
	}
	
	public static List<EventVizCity> getCity(String name, String country) throws SQLException {
		String sqlQuery = "SELECT * FROM Cities WHERE name=? AND country LIKE ?";
		PreparedStatement stmt = conn.prepareStatement(sqlQuery);
		stmt.setString(1, name);
		stmt.setString(2, "%" + country);
		ResultSet rs = stmt.executeQuery();
		
		List<EventVizCity> matchingCities = new ArrayList<EventVizCity>();
		while(rs.next()) {
			int id = rs.getInt("id");
			String region = rs.getString("region");
			float latitude = rs.getFloat("latitude");
			float longitude = rs.getFloat("longitude");
			String dbpedia_resource = rs.getString("dbpedia_resource");
			matchingCities.add(new EventVizCity(id,name,region,country,latitude,longitude,dbpedia_resource));
		}
		rs.close();
		stmt.close();
		return matchingCities;
	}
	
	public static EventVizArtist getArtist(String artistName) throws SQLException {
		String sqlQuery = "SELECT a.id, a.name, a.alternate_name, a.dbpedia_resource, Bands.name AS bandName FROM Artists AS a JOIN BandMembers AS b ON a.id=b.artist_id JOIN Bands ON b.band_id=Bands.id WHERE a.name=?";
		PreparedStatement stmt = conn.prepareStatement(sqlQuery);
		stmt.setString(1, artistName);
		ResultSet rs = stmt.executeQuery();
		
		rs.next();
		int id = rs.getInt("id");
		String alternate_name = rs.getString("alternate_name");
		String dbpedia_resource = rs.getString("dbpedia_resource");
		String bandName = rs.getString("bandName");
		EventVizArtist artist = new EventVizArtist(id, artistName, alternate_name, dbpedia_resource, bandName);
		
		rs.close();
		stmt.close();
		return artist;
	}
	
	public static EventVizBand getBand(String bandName) throws SQLException {
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
		
		List<EventVizBandMember> members = new ArrayList<EventVizBandMember>();
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
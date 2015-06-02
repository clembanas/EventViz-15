package db;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import containers.*;
import database.EventViz15_DB_MySQLAccess;
import jsonGeneration.JsonResultGenerator;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.fail;

public class DbConnectionTests {

	@Before
	public void initialize() {
		try {
			EventViz15_DB_MySQLAccess.initializeDBAccess();
		} catch (SQLException e) {
			fail();
		}
	}
	
	@Test
	public void getCity_JSON() {
		List<EventVizCity> cities;
		JsonArray city_JSON = null;
		try {
			cities = EventViz15_DB_MySQLAccess.getCity("Dublin", "Ireland");
			city_JSON = JsonResultGenerator.getCity_JSON(cities);
		} catch (SQLException e) {
			fail();
		}
		System.out.println("get city:");
		System.out.println(city_JSON);
		System.out.println();
	}
	
	@Test
	public void getEventById_JSON() {
		String eventful_id = "E0-001-078479724-7@2015052514";
		EventVizModelPopulationObject event;
		JsonObject specificEvent_JSON = null;
		try {
			event = EventViz15_DB_MySQLAccess.getEventById(eventful_id);
			specificEvent_JSON = JsonResultGenerator.getEventById_JSON(event);
		} catch (SQLException e) {
			e.printStackTrace();
			fail();
		}
		System.out.println("get event by id:");
		System.out.println(specificEvent_JSON);
		System.out.println("");
	}
	
	@Test
	public void getEvents_JSON() {
		List<EventVizEventBasics> events;
		JsonArray events_JSON = null;
		try {
			events = EventViz15_DB_MySQLAccess.getEvents();
			events_JSON = JsonResultGenerator.getEvents_JSON(events);
		} catch (SQLException e) {
			fail();
		}
		System.out.println("overview of events:");
		System.out.println(events_JSON);
		System.out.println();
	}

	@Test
	public void artist() {
		String artistName = "Buczek";
		EventVizArtist artist = null;
		try {
			artist = EventViz15_DB_MySQLAccess.getArtist(artistName);
		} catch (SQLException e) {
			fail();
		}
		JsonObject artist_JSON = JsonResultGenerator.getArtist_JSON(artist);
		System.out.println(artist_JSON);
	}

	@Test
	public void band() {
		String bandName = "Santana";
		EventVizBand band = null;
		try {
			band = EventViz15_DB_MySQLAccess.getBand(bandName);
		} catch (SQLException e) {
			fail();
		}
		JsonObject band_JSON = JsonResultGenerator.getBand_JSON(band);
		System.out.println(band_JSON);
	}
	
}
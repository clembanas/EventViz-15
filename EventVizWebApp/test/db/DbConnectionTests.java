package db;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import containers.EventVizEventBasics;
import containers.EventVizModelPopulationObject;
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
	public void getEventById_JSON() {
		int eventId = 51589;
		EventVizModelPopulationObject event;
		JsonObject specificEvent_JSON = null;
		try {
			event = EventViz15_DB_MySQLAccess.getEventById(eventId);
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

}
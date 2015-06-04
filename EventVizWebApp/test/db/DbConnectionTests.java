package db;

import com.google.gson.JsonObject;
import containers.EventVizModelPopulationObject;
import database.EventViz15_DB_MySQLAccess;
import jsonGeneration.JsonResultGenerator;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;

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
		int eventId = 89779;
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
	
}
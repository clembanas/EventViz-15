import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import logic.CronJobFacade;
import logic.clustering.ClusteringUtil;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.api.Play;

public class Global extends GlobalSettings {
	@Override
	public void onStart(Application app) {
		initializeClusteringUtil();
		CronJobFacade.getInstance().initialize();
		initializeEventCache();
	}

	private void initializeClusteringUtil() {
		List<String> externalWorkers = Play.current().configuration()
				.getStringList("clustering.externalWorkers").get();
		int localWorkers = (int) Play.current().configuration()
				.getInt("clustering.localWorkers").get();

		ClusteringUtil.initialize(externalWorkers, localWorkers);
	}

	private void initializeEventCache() {
		boolean cacheClusteredEvents = (boolean)Play.current().configuration()
				.getBoolean("clustering.cacheClusteredEvents").get();
		
		boolean cacheEventsFromDB = (boolean)Play.current().configuration()
				.getBoolean("clustering.cacheEventsFromDB").get();
		
		
		Logger.info("CacheEventsFromDB: " + (cacheEventsFromDB ? "On" : "Off") 
				+ " CacheClusteredEvents: " + (cacheClusteredEvents ? "On" : "Off"));

		try {
			controllers.Application.refreshEventsCaching(cacheEventsFromDB, cacheClusteredEvents);
		} catch (SQLException | IOException e) {
			// do not handle exception because after first pageload we know that there is something wrong
			e.printStackTrace();
		}
	}

	@Override
	public void onStop(Application app) {

	}

}
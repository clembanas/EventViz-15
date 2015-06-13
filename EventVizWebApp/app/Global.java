import logic.CronJobFacade;
import play.Application;
import play.GlobalSettings;
import play.Logger;

public class Global extends GlobalSettings {
  @Override
  public void onStart(Application app) {
	  CronJobFacade.getInstance().initialize();
	  initializeCache();
  }  
  
  private void initializeCache()
  {
	  Logger.info("Getting events and initialize cache");
	  try {
		controllers.Application.getEvents();
	} catch (Exception e) {
		// ignore all exceptions, errors will be detected after first page load
	}
  }
  
  @Override
  public void onStop(Application app) {
	  
  }  
    
}
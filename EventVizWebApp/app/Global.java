import logic.CronJobFacade;
import play.Application;
import play.GlobalSettings;
import play.Logger;

public class Global extends GlobalSettings {

  @Override
  public void onStart(Application app) {
	  CronJobFacade.getInstance().initialize();
  }  
  
  @Override
  public void onStop(Application app) {
	  
  }  
    
}
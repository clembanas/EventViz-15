package logic;

import java.util.concurrent.TimeUnit;

import logic.jobs.ClusteringJob;
import logic.jobs.EventfulCrawlerJob;
import logic.jobs.SocialMentionCrawlerJob;

import org.joda.time.DateTime;

import play.Logger;
import play.libs.Akka;
import scala.concurrent.duration.Duration;

/**
 * The Class CronJobFacade. It is a singleton-facade that encapsulates all cronjobs that are running periodically in background
 */
public class CronJobFacade {
	  private static CronJobFacade instance = null;
	  private static Object lockObject = new Object();
	  
	  private CronJobFacade () {}
	  
	  /**
  	 * Gets the single instance of CronJobFacade.
  	 *
  	 * @return single instance of CronJobFacade
  	 */
  	public static CronJobFacade getInstance () {
	    if (instance == null) {
	    	synchronized(lockObject)
	    	{
	    		if(instance == null)
	    		{
	    			instance = new CronJobFacade ();
	    		}
	    	}
	    }
	    return instance;
	  }
	  
	  private boolean initialized = false;  
	  
	  /**
  	 * Initialize the facade - if it is already initialized nothing is done
  	 */
  	public void initialize()
	  {
		  if(!initialized)
		  {
			  synchronized(this)
			  {
				  if(!initialized)
				  {
					  initializeInternal();
					  initialized = true;
				  }
			  }
		  }
	  }

	private void initializeInternal() {
		Logger.info("Initialize " + this.getClass().getName());
		
		DateTime now = new DateTime();
		
		// schedule job for "crawling of Eventful": every day at 00:00 am
		Akka.system().scheduler().schedule(
				Duration.create(now.plusDays(1).withTimeAtStartOfDay().getMillis() - now.getMillis(), TimeUnit.MILLISECONDS),
				Duration.create(1, TimeUnit.DAYS),
				new EventfulCrawlerJob(),
				Akka.system().dispatcher()
		);
		
		// schedule job for "crawling of SocialMention": every day at 02:00 am
		Akka.system().scheduler().schedule(
				Duration.create(now.plusDays(1).withTimeAtStartOfDay().plusHours(2).getMillis() - now.getMillis(), TimeUnit.MILLISECONDS),
				Duration.create(1, TimeUnit.DAYS),
				new SocialMentionCrawlerJob(),
				Akka.system().dispatcher()
		);
		
		// schedule job for "clustering": start immediately, repeat every day at same time
		Akka.system().scheduler().schedule(
				Duration.create(0, TimeUnit.MILLISECONDS),
				Duration.create(1, TimeUnit.DAYS),
				new ClusteringJob(),
				Akka.system().dispatcher()
		);
	}
}

package logic.jobs;

import play.Logger;

/**
 * The Class EventfulCrawlerJob. Crawls Eventful and stores data into database.
 * It gets executed repeatedly by CronJobFacade
 */
public class EventfulCrawlerJob implements Runnable {

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		Logger.info(this.getClass().getName() + " started running");
		//TODO implement me
	}

}

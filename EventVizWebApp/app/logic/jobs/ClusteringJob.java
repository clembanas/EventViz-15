package logic.jobs;

import play.Logger;

/**
 * The Class ClusteringJob. Prepares and clusters venues that are stored in database
 * It gets executed repeatedly by CronJobFacade 
 */
public class ClusteringJob implements Runnable {

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		Logger.info(this.getClass().getName() + " started running");
		
	}

}

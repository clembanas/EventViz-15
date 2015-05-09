/**
 * @author Bernhard Weber
 */
import java.util.concurrent.*;


/**
 * Base class of all crawlers which operates based on jobs
 */
public abstract class JobBasedCrawler extends CrawlerBase {
	
	public static int QUEUE_SIZE_MULTI = 2;
	

	/**
	 * Base class of worker thread jobs  
	 */
	protected class WorkerJobBase {
		
		private boolean done = false;
		
		private WorkerJobBase(boolean done) { this.done = done; }
		public WorkerJobBase() {}
	};
	

	protected ArrayBlockingQueue<WorkerJobBase> workerJobs; 
	
	protected abstract int getWorkerThdCount();
	protected abstract Utils.Pair<WorkerJobBase, Object> getNextWorkerJob(Object customData)
		throws Exception;
	protected abstract void processWorkerJob(WorkerJobBase job) throws Exception;
	
	protected void workerThd_run()
	{
		WorkerJobBase job = null;
		
		do {
			try {
				job = workerJobs.take();
				//No further worker jobs exist
				if (job.done) { 
					workerJobs.put(job);
					debug_print("Worker thread ... Done", JobBasedCrawler.class);
				}
				else
					processWorkerJob(job);
			} 
			catch (Exception e) {
				JobBasedCrawler.this.handleException(e, "Error in worker thread of crawler '" + 
					JobBasedCrawler.this.getClass().getName() + "'!");
			}
		} 
		while (!job.done);
	}
	
	public void execute(ExecutorService thdPool, DBConnection dbConnection)
	{
		final int workerThdCnt = getWorkerThdCount();
		workerJobs = new  ArrayBlockingQueue<WorkerJobBase>(QUEUE_SIZE_MULTI * workerThdCnt);
		int startedWorkerThds = 0;
		Utils.Pair<WorkerJobBase, Object> nextJob;

		associateThdPool(thdPool);
		associateDBConnection(dbConnection);
		debug_print("Crawler started...", JobBasedCrawler.class);
		try {
			nextJob = getNextWorkerJob(null);
			//Keep filling queue with worker jobs until no more jobs exist
			while (nextJob != null && nextJob.first != null) {
				if (startedWorkerThds < workerThdCnt) {
					startWorkerThd();
					startedWorkerThds++;
				}
				workerJobs.put(nextJob.first);
				nextJob = getNextWorkerJob(nextJob.second);
			}
			//Notify all threads that no further dataset entries exist
			workerJobs.put(new WorkerJobBase(true));
		} 
		catch (Exception e) {
			handleException(e, "Error in crawler '" + getClass().getName() + "'!");
		}
		finally {
			joinWorkerThds();
			finished();
		}
	}
}

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
		
		ThreadMonitor.addThread(Thread.currentThread(), getClass().getName() + "::WorkerThd");
		while (true) {
			try {
				job = workerJobs.take();
				//No further worker jobs exist
				if (job.done) { 
					workerJobs.put(job);
					DebugUtils.printDebugInfo("Worker thread ... Done", 
						JobBasedCrawler.this.getClass(), JobBasedCrawler.class);
					return;
				}
				else
					processWorkerJob(job);
			} 
			catch (Exception e) {
				ExceptionHandler.handle("Error in worker thread of crawler '" + 
					JobBasedCrawler.this.getClass().getName() + "'!", e,
					JobBasedCrawler.this.getClass(), JobBasedCrawler.class);
			}
		} 
	}
	
	public void execute()
	{
		final int workerThdCnt = getWorkerThdCount();
		workerJobs = new  ArrayBlockingQueue<WorkerJobBase>(QUEUE_SIZE_MULTI * workerThdCnt);
		int startedWorkerThds = 0;
		Utils.Pair<WorkerJobBase, Object> nextJob;
		boolean exceptionThrown = false;

		DebugUtils.printDebugInfo("Crawler started...", getClass(), JobBasedCrawler.class);
		started();
		ThreadMonitor.addThread(Thread.currentThread());
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
		} 
		catch (Exception e) {
			ExceptionHandler.handle("Error in crawler '" + getClass().getName() + "'!", e, 
				getClass(), JobBasedCrawler.class);
			exceptionThrown = true;
		}
		finally {
			ThreadMonitor.removeThread(Thread.currentThread());
			try {
				workerJobs.put(new WorkerJobBase(true));
			} catch (InterruptedException e1) {}
			joinWorkerThds();
			finished(exceptionThrown);
		}
	}
}

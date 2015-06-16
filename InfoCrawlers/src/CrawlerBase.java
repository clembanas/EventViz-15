/**
 * @author Bernhard Weber
 */
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


/**
 * Base class of crawlers
 */
public abstract class CrawlerBase implements CrawlerInstance {
	
	private final int QUEUE_SIZE_MULTI = 2;
	private InetAddress JOB_CONTROLLER_HOST_ADDR;
	
	
	/**
	 * Base class of worker thread jobs  
	 */
	protected static class JobBase {
		
		private boolean done = false;
		private int jobIdx = 0, jobGroupIdx = 0, jobGroupCnt = 0, jobsPerGroup = 0, totalJobCnt = 0;

		private void setContext(int jobIdx, int jobGroupIdx, int jobGroupCnt, int jobsPerGroup, 
			int totalJobCnt) 
		{
			this.jobIdx = jobIdx;
			this.jobGroupIdx = jobGroupIdx;
			this.jobGroupCnt = jobGroupCnt;
			this.jobsPerGroup = jobsPerGroup;
			this.totalJobCnt = totalJobCnt;
		}
		
		private JobBase(boolean done) 
		{ 
			this.done = done; 
		}
		
		public JobBase() {}
		
		public int getJobIndex()
		{
			return jobIdx;
		}
		
		public int getGroupIndex()
		{
			return jobGroupIdx;
		}
		
		public int getGroupCount()
		{
			return jobGroupCnt;
		}
		
		public int getJobsPerGroup()
		{
			return jobsPerGroup;
		}
		
		public int getTotalJobCount()
		{
			return totalJobCnt;
		}
	};
	
	/**
	 * Job-group provider interface
	 * Note: Always called from Crawler's main thread
	 */
	protected static interface JobGroupProvider {
		
		//Note: the count of returned jobs must not necessarily correspond to jobsPerGroup 
		public JobBase[] getJobsOfGroup(int groupIdx, int groupCnt, int jobsPerGroup, 
			int totalJobCnt) throws Exception;
	}
	
	/**
	 * Job-group controller interface
	 * Note: May be called by different threads simultaneously
	 */
	protected static interface JobGroupController {
		
		public int getJobGroupCount();			//Maximum count of job-groups
		public int getNextJobGroupIndex();		//Index of the next job-group
		public int getJobsPerGroup();			//General size of a job-group
		public int getTotalJobCount();			//Total count of jobs
	}
	
	/**
	 * Worker thread
	 */
	private class WorkerThd implements Runnable {
		
		public void run() 
		{
			JobBase job = null;
			
			DebugUtils.printDebugInfo("Worker thread started...", CrawlerBase.this.getClass(), 
				CrawlerBase.class, getClass());
			ThreadMonitor.addThread(Thread.currentThread(), getClass().getName() + "::WorkerThd");
			while (true) {
				try {
					job = jobQueue.take();
					//No further worker jobs exist
					if (job.done) { 
						jobQueue.put(job);
						DebugUtils.printDebugInfo("Worker thread ... Done", 
							CrawlerBase.this.getClass(), CrawlerBase.class);
						break;
					}
					else {
						jobStarted(job, (int)((100.0/ (float)job.jobGroupCnt) *
							(float)job.jobGroupIdx));
						processJob(job);
						jobFinished(job, (int)((100.0/ (float)job.jobGroupCnt) * 
							((float)job.jobGroupIdx + 1.0)));
					}
				} 
				catch (Exception e) {
					ExceptionHandler.handle("Error in worker thread of crawler '" + 
						CrawlerBase.this.getClass().getName() + "'!", e,
						CrawlerBase.this.getClass(), CrawlerBase.class);
				}
			}
			ThreadMonitor.removeThread(Thread.currentThread());
			DebugUtils.printDebugInfo("Worker thread finished", CrawlerBase.this.getClass(), 
				CrawlerBase.class, getClass());
		}
	}
	

	private BlockingQueue<JobBase> jobQueue;
	private List<Future<?>> pendingWorkerThds;
	private int lastProgress;
	private static ExecutorService thdPool;
	protected static DBConnector dbConnector;
	protected static boolean isMasterNode;
	
	protected abstract int getWorkerThdCount();
	protected abstract JobGroupProvider getJobGroupProvider();
	protected abstract Class<? extends JobGroupController> getJobGroupControllerClass();
	protected abstract RemoteObjectManager.RemoteObjectCreator<JobGroupController> 
		getJobGroupControllerCreator();
	protected abstract void processJob(JobBase job) throws Exception;
	public abstract int[] getStatistics();
	public abstract String getSummary(int[] crawlerStats);
	
	protected JobGroupController getJobGroupController() throws Exception
	{
		Class<? extends JobGroupController> jobGroupCtrlrClass = getJobGroupControllerClass();
		
		RemoteObjectManager.registerRemoteObject(JobGroupController.class, 
			jobGroupCtrlrClass, getJobGroupControllerCreator(), getClass().getName());
		return RemoteObjectManager.getRemoteObject(jobGroupCtrlrClass, JOB_CONTROLLER_HOST_ADDR, 
				   getClass().getName());
	}
	
	protected void releaseJobGroupController(JobGroupController jobCtrlr)
	{
		try {
			RemoteObjectManager.closeRemoteObject(getJobGroupControllerClass(), jobCtrlr, 
				getClass().getName());
		} 
		catch (Exception e) {
			ExceptionHandler.handle("Failed to release job-group controller!", e, getClass(), 
				CrawlerBase.class);
		}
	}
	
	protected void startWorkerThd()
	{
		synchronized (pendingWorkerThds) {
			pendingWorkerThds.add(thdPool.submit(new WorkerThd()));
		}
	}
	
	protected void joinWorkerThds()
	{
		int idx = 0;
		Future<?> currTask = null;
		
		if (pendingWorkerThds == null)
			return;
		while (true) {
			try {
				synchronized (pendingWorkerThds) {
					if (idx >= pendingWorkerThds.size())
						break;
					currTask = pendingWorkerThds.get(idx);
				}
				currTask.get();
				idx++;
			} 
			catch (Exception e) { 
				ExceptionHandler.handle("Error when joining worker thread of crawler '" +
					getClass().getName() + "'!", e, getClass(), CrawlerBase.class);
			}
		}
		synchronized (pendingWorkerThds) {
			pendingWorkerThds.clear();
		}
	}
	
	protected void started()
	{
		dbConnector.logCrawlerStarted(getClass());
	}
	
	protected void finished(boolean exceptionThrown)
	{
	}
	
	protected void jobStarted(JobBase job, int progress)
	{
	}
	
	protected void jobFinished(JobBase job,	int progress)
	{
		if (isMasterNode) {
			synchronized (this) {
				if (lastProgress >= progress)
					return;
				lastProgress = progress; 
			}
			dbConnector.logCrawlerProgress(getClass(), progress);
		}
	}
	
	public CrawlerBase()
	{
		JOB_CONTROLLER_HOST_ADDR = CrawlerConfig.getCrawlerJobControllerHost();
	}
	
	public boolean execute() throws Exception
	{
		JobGroupController jobCtrlr = getJobGroupController();
		boolean exceptionThrown = false;
		
		try {
			JobGroupProvider jobProvider = getJobGroupProvider();
			final int workerThdCnt = getWorkerThdCount();
			final int jobGroupCnt = jobCtrlr.getJobGroupCount();
			final int jobsPerGroup = jobCtrlr.getJobsPerGroup();
			final int totalJobCnt = jobCtrlr.getTotalJobCount();
			int jobGroupIdx, jobIdx, startedWorkerThds = 0;
			JobBase[] jobs;
			
			jobQueue = new ArrayBlockingQueue<JobBase>(QUEUE_SIZE_MULTI * workerThdCnt);
			pendingWorkerThds = new ArrayList<Future<?>>(workerThdCnt);
			DebugUtils.printDebugInfo("Crawler started...", getClass(), CrawlerBase.class);
			started();
			ThreadMonitor.addThread(Thread.currentThread());
			jobGroupIdx = jobCtrlr.getNextJobGroupIndex();
			//Keep filling queue with jobs until no more jobs exist
			while (jobGroupIdx < jobGroupCnt) {
				if (startedWorkerThds < workerThdCnt) {
					startWorkerThd();
					startedWorkerThds++;
				}
				jobs = jobProvider.getJobsOfGroup(jobGroupIdx, jobGroupCnt, jobsPerGroup, 
						   totalJobCnt);
				if (jobs == null || jobs.length == 0)
					break;
				jobIdx = 0;
				for (JobBase job: jobs) {
					job.setContext(jobIdx++, jobGroupIdx, jobGroupCnt, jobsPerGroup, totalJobCnt);
					jobQueue.put(job);
				}
				jobGroupIdx = jobCtrlr.getNextJobGroupIndex();
			}
		} 
		catch (Exception e) {
			ExceptionHandler.handle("Error in crawler '" + getClass().getName() + "'!", e, 
				getClass(), CrawlerBase.class);
			exceptionThrown = true;
		}
		finally {
			try {
				//Notify all worker threads that no further jobs exist
				jobQueue.put(new JobBase(true));
			} catch (InterruptedException e1) {}
			joinWorkerThds();
			jobQueue.clear();
			finished(exceptionThrown);
			ThreadMonitor.removeThread(Thread.currentThread());
			releaseJobGroupController(jobCtrlr);
		}
		return !exceptionThrown;
	}
	
	public int[] combineStatistics(List<int[]> crawlerStats)
	{
		if (crawlerStats.isEmpty())
			return new int[0];
		
		int[] result = new int[crawlerStats.get(0).length];
		
		for (int[] instStats: crawlerStats) {
			for (int i = 0; i < instStats.length; ++i)
				result[i] += instStats[i];
		}
		return result;
	}
	
	public void allInstancesFinished(boolean exceptionThrown, int[] crawlerStats)
	{
		if (isMasterNode) {
			if (exceptionThrown)
				dbConnector.logCrawlerFinished(getClass(), "WARNING: An exception was thrown!\n" + 
					getSummary(crawlerStats));
			else
				dbConnector.logCrawlerFinished(getClass(), getSummary(crawlerStats));
		}
	}
	
	public static void setExecutionEnvironment(ExecutorService thdPool, DBConnector dbConnector,
		boolean isMasterNode)
	{
		CrawlerBase.thdPool = thdPool;
		CrawlerBase.dbConnector = dbConnector;
		CrawlerBase.isMasterNode = isMasterNode;
	}
}

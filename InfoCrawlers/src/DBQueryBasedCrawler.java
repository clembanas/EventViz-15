/**
 * @author Bernhard Weber
 */
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Base class of crawlers which collects information based on the results of a DB query
 */
public abstract class DBQueryBasedCrawler extends CrawlerBase {
	
	/**
	 * Job whose associated data row has to be processed by a worker thread
	 */
	private class DBQueryBasedJob extends JobBase {
		
		public String[] dataRow = null;
		public int dataRowIdx;
		
		DBQueryBasedJob(ResultSet dataset, ResultSetMetaData datasetMeta) 
		{
			try {
				int colCount = datasetMeta.getColumnCount();
				
				dataRowIdx = dataset.getRow(); 
				dataRow = new String[colCount];
				for (int i = 0; i < colCount; ++i) {
					this.dataRow[i] = dataset.getString(i + 1);
					//Convert SQL-NULL entries to empty string
					if (dataRow[i] == null)
						dataRow[i] = "";
				}
			} 
			catch (Exception e) {
				ExceptionHandler.handle("Failed to create database based job for crawler '" + 
					DBQueryBasedCrawler.this.getClass().getName() + "'!", e, 
					DBQueryBasedCrawler.this.getClass(), DBQueryBasedCrawler.class, getClass());
			}
		}
	};
	
	/**
	 * Job controller for database-query based crawlers.
	 */
	protected class DBBasedJobGroupController implements JobGroupController {

		private int datasetCnt = 0;
		private int pageCnt = 0;
		private AtomicInteger pageIdx = new AtomicInteger(0);
		private int pageSize = getPageSize();
		
		public DBBasedJobGroupController()
		{
			try {
				datasetCnt = getDatasetCount();
			} 
			catch (Exception e) {
				ExceptionHandler.handle("Failed to retrieve dataset size information!", e, 
						EventfulCrawler.class, null, getClass());
			}
			pageCnt = (int)Math.ceil(datasetCnt/ pageSize);
		}
		
		public int getJobGroupCount() 
		{
			return pageCnt;
		}

		public int getNextJobGroupIndex() 
		{
			return pageIdx.getAndIncrement();
		}

		public int getJobsPerGroup() 
		{
			return pageSize;
		}

		public int getTotalJobCount() 
		{
			return datasetCnt;
		}
	}
	
	
	private JobGroupController jobController = null;
	
	protected abstract int getWorkerThdCount();
	protected abstract int getPageSize();
	protected abstract int getDatasetCount() throws Exception;
	protected abstract ResultSet getDataset(int pageIdx, int pageSize) throws Exception;
	protected abstract void processDataRow(String[] dataRow, int dataRowIdx) throws Exception;
	public abstract int[] getStatistics();
	public abstract String getSummary(int[] crawlerStats);
	
	protected JobGroupProvider getJobGroupProvider()
	{
		return new JobGroupProvider() {
			
			public JobBase[] getJobsOfGroup(int groupIdx, int groupCnt, int jobsPerGroup, 
				int totalJobCnt) throws Exception 
			{
				if (groupIdx > 0 && !dbConnector.supportsQueryPaging()) 
					return null;
				
				ResultSet resSet = getDataset(groupIdx, jobsPerGroup);
				ResultSetMetaData resSetMeta;
				List<JobBase> jobs = new ArrayList<JobBase>(jobsPerGroup);
				
				if (resSet == null)
					return null;
				try {
					resSetMeta = resSet.getMetaData();
					while (resSet.next())
						jobs.add(new DBQueryBasedJob(resSet, resSetMeta));
				}
				finally {
					try {
						resSet.close();
					}
					catch (Exception e1) {};
				}
				return jobs.toArray(new JobBase[jobs.size()]);
			}
		};
	}
	
	protected Class<? extends JobGroupController> getJobGroupControllerClass()
	{
		return DBBasedJobGroupController.class;
	}
	
	protected RemoteObjectManager.RemoteObjectCreator<JobGroupController> 
		getJobGroupControllerCreator()
	{
		return new RemoteObjectManager.RemoteObjectCreator<JobGroupController>() {

			public synchronized JobGroupController createRemoteObject() 
			{
				if (jobController == null)
					jobController = new DBBasedJobGroupController();
				return jobController;
			}
		};
	}
	
	protected void processJob(JobBase job) throws Exception
	{
		if (((DBQueryBasedJob)job).dataRow != null)
			processDataRow(((DBQueryBasedJob)job).dataRow, ((DBQueryBasedJob)job).dataRowIdx);
	}
	
	protected void jobStarted(JobBase job, int progress)
	{
		super.jobStarted(job, progress);
		DebugUtils.printDebugInfo("Processing " + (job.getJobIndex() + 1) + ". dataset of " +
			Math.min(job.getJobsPerGroup(),	job.getTotalJobCount() - job.getGroupIndex() * 
			job.getJobsPerGroup()) + " datasets on page " + (job.getGroupIndex() + 1) + " (" + 
			progress + "%) ...", getClass(), DBQueryBasedCrawler.class);
	}
	
	protected void jobFinished(JobBase job,	int progress)
	{
		super.jobFinished(job, progress);
		DebugUtils.printDebugInfo("Processing " + (job.getJobIndex() + 1) + ". dataset of " +
				Math.min(job.getJobsPerGroup(),	job.getTotalJobCount() - job.getGroupIndex() * 
				job.getJobsPerGroup()) + " datasets on page " + (job.getGroupIndex() + 1) + " (" + 
				progress + "%) ... DONE", getClass(), DBQueryBasedCrawler.class);
	}
}

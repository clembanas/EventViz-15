/**
 * @author Bernhard Weber
 */
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


/**
 * Base class of crawlers
 */
public abstract class CrawlerBase {
	
	/**
	 * Worker thread
	 */
	private class WorkerThd implements Runnable {
		
		public void run() 
		{
			DebugUtils.printDebugInfo("Worker thread started...", CrawlerBase.this.getClass(), 
				CrawlerBase.class, getClass());
			workerThd_run();
			DebugUtils.printDebugInfo("Worker thread finished", CrawlerBase.this.getClass(), 
				CrawlerBase.class, getClass());
		}
	}
	

	private ExecutorService thdPool;
	private List<Future<?>> pendingWorkerThds = new ArrayList<Future<?>>();
	protected DBConnector dbConnector;
	
	protected abstract int getWorkerThdCount();
	protected abstract void workerThd_run();
	
	protected void startWorkerThd()
	{
		synchronized (pendingWorkerThds) {
			pendingWorkerThds.add(thdPool.submit(new WorkerThd()));
		}
	}
	
	protected void joinWorkerThds()
	{
		Future<?> currTask = null;
		
		while (true) {
			try {
				synchronized (pendingWorkerThds) {
					if (pendingWorkerThds.isEmpty())
						break;
					currTask = pendingWorkerThds.get(0);
				}
				currTask.get();
				synchronized (pendingWorkerThds) {
					pendingWorkerThds.remove(currTask);
				}
			} 
			catch (Exception e) { 
				ExceptionHandler.handle("Failed to join workers of crawler '" +
					getClass().getName() + "'!", e, getClass(), CrawlerBase.class);
			}
		}
	}
	
	protected void started()
	{
		dbConnector.logCrawlerStarted(getClass());
	}
	
	protected void finished(boolean exceptionThrown)
	{
	}
	
	public void associateThdPool(ExecutorService thdPool)
	{
		this.thdPool = thdPool;
	}
	
	public void associateDBConnector(DBConnector dbConn)
	{
		this.dbConnector = dbConn;
	}
	
	public void execute()
	{
		final int workerThdCnt = getWorkerThdCount();
		boolean exceptionThrown = false;

		DebugUtils.printDebugInfo("Crawler started...", getClass(), CrawlerBase.class);
		started();
		try {
			for (int i = 0; i < workerThdCnt; ++i)
				startWorkerThd();
		} 
		catch (Exception e) {
			ExceptionHandler.handle("Error in crawler '" + getClass().getName() + "'!", e, 
				getClass(), CrawlerBase.class);
			exceptionThrown = true;
		}
		finally {
			joinWorkerThds();
			finished(exceptionThrown);
		}
	}
}

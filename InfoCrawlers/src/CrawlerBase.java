/**
 * @author Bernhard Weber
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
			debug_print("Worker thread started...", CrawlerBase.class);
			workerThd_run();
			debug_print("Worker thread finished", CrawlerBase.class);
		}
	}
	

	private static Set<Class<?>> debug_crawlerClasses = new HashSet<Class<?>>();
	private ExecutorService thdPool;
	private List<Future<?>> pendingWorkerThds = new ArrayList<Future<?>>();
	protected DBConnection dbConnection;
	
	protected abstract int getWorkerThdCount();
	protected abstract void workerThd_run();
	
	protected boolean debug_canDebug(Class<? extends CrawlerBase> crawlerClass)
	{
		return debug_crawlerClasses.contains(crawlerClass);
	}
	
	protected void debug_print(final String info, Class<? extends CrawlerBase> crawlerClass)
	{
		if (debug_canDebug(crawlerClass)) 
			DebugUtils.debug_printf("[%s (Thread %s)]: %s\n", crawlerClass != getClass() ? 
				getClass().getName() + "::" + crawlerClass.getName() : getClass().getName(),
				Thread.currentThread().getId(), info);
	}
	
	protected void debug_print(final String info)
	{
		debug_print(info, getClass());
	}
	
	protected void handleException(final Exception e, final String info, final boolean printTrace)
	{
		ExceptionHandler.handle(e, "[" + getClass().getName() + " (Thread " + 
			Thread.currentThread().getId() + ")]: " + info, printTrace);
	}
	
	protected void handleException(final Exception e, final String info)
	{
		ExceptionHandler.handle(e, "[" + getClass().getName() + " (Thread " + 
			Thread.currentThread().getId() + ")]: " + info);
	}
	
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
				handleException(e, "Failed to join workers of crawler '" + 
					getClass().getName() + "'!");
			}
		}
	}
	
	protected void started()
	{
		dbConnection.logCrawlerStarted(getClass());
	}
	
	protected void finished(boolean exceptionThrown)
	{
	}
	
	public void associateThdPool(ExecutorService thdPool)
	{
		this.thdPool = thdPool;
	}
	
	public void associateDBConnection(DBConnection dbConn)
	{
		this.dbConnection = dbConn;
	}
	
	public void execute()
	{
		final int workerThdCnt = getWorkerThdCount();
		boolean exceptionThrown = false;

		debug_print("Crawler started...", CrawlerBase.class);
		started();
		try {
			for (int i = 0; i < workerThdCnt; ++i)
				startWorkerThd();
		} 
		catch (Exception e) {
			handleException(e, "Error in crawler '" + getClass().getName() + "'!");
			exceptionThrown = true;
		}
		finally {
			joinWorkerThds();
			finished(exceptionThrown);
		}
	}
	
	@SafeVarargs
	public static void debug_crawlers(Class<? extends CrawlerBase> ... crawlerClasses)
	{
		debug_crawlerClasses.addAll(Arrays.asList(crawlerClasses));
	}
}

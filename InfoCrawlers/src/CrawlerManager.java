/**
 * @author Bernhard Weber
 */
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Manages and executes the different crawlers.
 * Use "registerCrawler" to add a new crawler class to the manager.
 */
public class CrawlerManager {
	
	/**
	 * Controls the instantiation and execution of a crawler. 
	 */
	private static class CrawlerCtrlr {
		
		/**
		 * Executes a crawler 
		 */
		private class CrawlerExecutor extends Thread {
			
			private void awaitDependedCrawlers() throws Exception
			{
				if (dependencies != null) {
					CrawlerCtrlr crawlerCtrlr; 

					DebugUtils.printDebugInfo("Waiting for depending crawlers of '" + 
						crawlerClass.getName() + "' to finish ...", CrawlerManager.class, null,
						getClass());
					for (Class<? extends CrawlerBase> dependsOnCrawlerClass: dependencies) {
						crawlerCtrlr = getCrawlerCtrlr(dependsOnCrawlerClass);
						if (crawlerCtrlr == null)
							throw new Exception("Crawler class '" + crawlerClass.getName() + 
									  	  "' depends on unregistered crawler class '" + 
									      dependsOnCrawlerClass.getName() + "'!");
						crawlerCtrlr.verifyDependencies(crawlerClass);
						crawlerCtrlr.start();
						crawlerCtrlr.join();
					}
					DebugUtils.printDebugInfo("Waiting for depending crawlers of '" + 
						crawlerClass.getName() + "' to finish ... Done", CrawlerManager.class, 
						null, getClass());
				}
			}
			
			public void run()
			{
				CrawlerBase crawler;
				
				try {
					awaitDependedCrawlers();
					if (ctorData == null)
						crawler = crawlerClass.getConstructor().newInstance();
					else
						crawler = crawlerClass.getConstructor(ctorData.getClass()).newInstance(
									  ctorData);
					DebugUtils.printDebugInfo("Executing crawler '" + crawlerClass.getName() + 
						"' ... ", CrawlerManager.class, null, getClass());
					crawler.associateThdPool(thdPool);
					crawler.associateDBConnector(dbConn);
					crawler.execute();
					crawler = null;
					DebugUtils.printDebugInfo("Executing crawler '" + crawlerClass.getName() + 
						"' ... Done", CrawlerManager.class, null, getClass());
				} 
				catch (Exception e) {
					ExceptionHandler.handle("Failed to execute crawler of class '" + 
						crawlerClass.getName() + "'!\n", e, CrawlerManager.class, null, getClass());
				}
			}
		}
		
		
		Class<? extends CrawlerBase> crawlerClass;
		Class<? extends CrawlerBase>[] dependencies;
		Object ctorData;
		CrawlerExecutor crawlerExec = null;
		
		public void verifyDependencies(Class<? extends CrawlerBase> dependCrawlerClass)
			throws Exception
		{
			if (dependencies != null) {
				CrawlerCtrlr crawlerCtrlr; 
				
				for (Class<? extends CrawlerBase> dependsOnCrawlerClass: dependencies) {
					if (dependsOnCrawlerClass == dependCrawlerClass) 
						throw new Exception("Circular dependency between crawler class '" + 
									  dependsOnCrawlerClass.getName() + "' and '" + 
									  dependCrawlerClass.getName() + "' detected!");
					crawlerCtrlr = CrawlerManager.getCrawlerCtrlr(dependsOnCrawlerClass);
					if (crawlerCtrlr != null)
						crawlerCtrlr.verifyDependencies(dependCrawlerClass);
				}
			}
		}
		
		public CrawlerCtrlr(Class<? extends CrawlerBase> crawlerClass, Object ctorData,
			Class<? extends CrawlerBase>[] dependencies) 
		{
			this.crawlerClass = crawlerClass;
			this.dependencies = dependencies;
			this.ctorData = ctorData;
		}
		
		public void start()
		{
			synchronized (this) {
				if (crawlerExec == null) {
					crawlerExec = new CrawlerExecutor();
					crawlerExec.start();
				}
			}
		}
		
		public void join()
		{
			try {
				CrawlerExecutor crawlerExec;
				
				synchronized (this) {
					crawlerExec = this.crawlerExec;
				}
				if (crawlerExec != null) 
					crawlerExec.join();
			} 
			catch (Exception e) {
				ExceptionHandler.handle("Failed to await crawler-executor termination of " +
					"crawler class '" + crawlerClass.getName() + "'!\n", e, CrawlerManager.class, 
					null, getClass());
			}
		}
	}
	
	
	private static boolean headNodeMode;
	private static Boolean running = new Boolean(true);
	private static ExecutorService thdPool = Executors.newCachedThreadPool();
	private static DBConnector dbConn = null;
	private static Map<Class<? extends CrawlerBase>, CrawlerCtrlr> crawlerCtrlrs = 
		new HashMap<Class<? extends CrawlerBase>, CrawlerCtrlr>();
	
	private static CrawlerCtrlr getCrawlerCtrlr(Class<? extends CrawlerBase> crawlerClass)
	{
		return crawlerCtrlrs.get(crawlerClass);
	}
	
	public static void registerCrawler(Class<? extends CrawlerBase> crawlerClass)
	{
		crawlerCtrlrs.put(crawlerClass, new CrawlerCtrlr(crawlerClass, null, null));
	}
	
	@SafeVarargs
	public static void registerCrawler(Class<? extends CrawlerBase> crawlerClass,
		Class<? extends CrawlerBase> ... dependsOnCrawlers)
	{
		crawlerCtrlrs.put(crawlerClass, new CrawlerCtrlr(crawlerClass, null, dependsOnCrawlers));
	}
	
	@SafeVarargs
	public static void registerCrawler(Class<? extends CrawlerBase> crawlerClass,
		Object ctorData, Class<? extends CrawlerBase> ... dependsOnCrawlers)
	{
		crawlerCtrlrs.put(crawlerClass, new CrawlerCtrlr(crawlerClass, ctorData, 
			dependsOnCrawlers));
	}
	
	public static boolean start(boolean headNodeMode) throws Exception
	{
		CrawlerManager.headNodeMode = headNodeMode;
		try {
			dbConn = DBConnector.getInstance();
			dbConn.connect();
		} 
		catch (Exception e) {
			ExceptionHandler.handle("Failed to connect to database!\n", e, CrawlerManager.class);
			return false;
		}
		ThreadMonitor.start();
		RemoteObjectManager.start(thdPool);
		return true;
	}
	
	public static void run()
	{
		//Current host is the distributed crawlers' head node
		if (headNodeMode) {
			for (CrawlerCtrlr crawlerCtrlr: crawlerCtrlrs.values()) 
				crawlerCtrlr.start();
			for (CrawlerCtrlr crawlerCtrlr: crawlerCtrlrs.values()) 
				crawlerCtrlr.join();
		}
		//Current host is a child node
		else {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				
				public void run()
				{
					CrawlerManager.shutdown();
				}
			});
			synchronized (running) {
				while (running) {
					try {
						running.wait();
					} 
					catch (InterruptedException e) {}
				}
			}
		}
	}
	
	public static void shutdown()
	{
		synchronized (running) {
			if (!running)
				return;
			running = false;
			running.notifyAll();
		}
		try {
			if (dbConn != null)
				dbConn.disconnect();
			dbConn = null;
		}
		catch (Exception e) {
			ExceptionHandler.handle("Failed to disconnect from database!\n", e, 
				CrawlerManager.class);
		}
		RemoteObjectManager.shutdown();
		ThreadMonitor.shutdown();
		thdPool.shutdown();
		try {
			thdPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
		} 
		catch (InterruptedException e) {
			ExceptionHandler.handle("Failed to shutdown thread pool!", e, CrawlerManager.class);
		}
	}
}

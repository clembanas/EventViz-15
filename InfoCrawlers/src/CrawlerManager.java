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
	
	public static boolean DEBUG = true;
	

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

					CrawlerManager.debug_print("Waiting for depending crawlers to finish ...");
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
					CrawlerManager.debug_print("Waiting for depending crawlers to finish ... Done");
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
					CrawlerManager.debug_print("Executing crawler '" + crawlerClass.getName() + 
						"' ... ");
					crawler.execute(thdPool);
					crawler = null;
					CrawlerManager.debug_print("Executing crawler '" + crawlerClass.getName() + 
						"' ... Done");
				} 
				catch (Exception e) {
					ExceptionHandler.handle(e, "Failed to execute crawler of class '" + 
						crawlerClass.getName() + "'!\n");
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
				ExceptionHandler.handle(e, "Failed to await crawler-executor termination of " +
					"crawler class '" + crawlerClass.getName() + "'!\n");
			}
		}
	}
	
	
	private static ExecutorService thdPool = Executors.newCachedThreadPool();
	private static Map<Class<? extends CrawlerBase>, CrawlerCtrlr> crawlerCtrlrs = 
		new HashMap<Class<? extends CrawlerBase>, CrawlerCtrlr>();
	
	private static CrawlerCtrlr getCrawlerCtrlr(Class<? extends CrawlerBase> crawlerClass)
	{
		return crawlerCtrlrs.get(crawlerClass);
	}
	
	private static void debug_print(final String info)
	{
		if (DEBUG) 
			DebugUtils.debug_printf("[%s (Thread %s)]: %s\n", CrawlerManager.class.getName(), 
				Thread.currentThread().getId(), info);
	}
	
	public static void registerCrawler(Class<? extends CrawlerBase> crawlerClass)
	{
		crawlerCtrlrs.put(crawlerClass, new CrawlerCtrlr(crawlerClass, null, null));
	}
	
	public static void registerCrawler(Class<? extends CrawlerBase> crawlerClass,
		Object ctorData)
	{
		crawlerCtrlrs.put(crawlerClass, new CrawlerCtrlr(crawlerClass, ctorData, null));
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
	
	public static void executeAll()
	{
		try {
			DBConnection.connect();
			for (CrawlerCtrlr crawlerCtrlr: crawlerCtrlrs.values()) 
				crawlerCtrlr.start();
		} 
		catch (Exception e) {
			ExceptionHandler.handle(e, "Failed to connect to database!\n");
		}
		finally {
			for (CrawlerCtrlr crawlerCtrlr: crawlerCtrlrs.values()) 
				crawlerCtrlr.join();
			try {
				DBConnection.disconnect();
			}
			catch (Exception e) {
				ExceptionHandler.handle(e, "Failed to disconnect from database!\n");
			}
		}
	}
	
	public static void shutdown()
	{
		thdPool.shutdown();
		try {
			thdPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
		} 
		catch (InterruptedException e) {
			ExceptionHandler.handle(e, "Failed to shutdown thread pool!");
		}
	}
}

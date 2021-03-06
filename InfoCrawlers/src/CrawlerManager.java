/**
 * @author Bernhard Weber
 */
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Manages and executes the different crawlers.
 * Use "registerCrawler" to add a new crawler class to the manager.
 */
public class CrawlerManager {
	
	/**
	 * Controls the instantiation and execution of a crawler. 
	 */
	private static class CrawlerController {
		
		/**
		 * Executes a single crawler instance
		 */
		private static class CrawlerInstanceExecutor implements Callable<Boolean> {

			private CrawlerInstance crawlerInst;
			
			public CrawlerInstanceExecutor(Class<? extends CrawlerInstance> crawlerClass, 
				InetAddress remHostAddr) throws Exception
			{
				crawlerInst = RemoteObjectManager.getRemoteObject(crawlerClass, remHostAddr);
			}
			
			public Boolean call() throws Exception 
			{
				return !crawlerInst.execute();
			}
			
			public void allInstancesFinished(boolean exceptionThrown, String jobsPerHostsInfo, 
				int[] crawlerStats)
			{
				crawlerInst.allInstancesFinished(exceptionThrown, jobsPerHostsInfo, crawlerStats);
			}
			
			public int getProcessedJobCount()
			{
				return crawlerInst.getProcessedJobCount();
			}
			
			public int[] getCrawlerStatistics() 
			{
				return crawlerInst.getStatistics();
			}

			public int[] combineStatistics(List<int[]> crawlerStats) 
			{
				return crawlerInst.combineStatistics(crawlerStats);
			}
		}
		
		/**
		 * Executes all distributed instances of a crawler
		 */
		private class CrawlerExecutor implements Runnable {
			
			private void awaitDependentCrawlers() throws Exception
			{
				if (dependencies != null) {
					CrawlerController crawlerCtrlr; 

					DebugUtils.printDebugInfo("Waiting for dependent crawlers of '" + 
						crawlerClass.getName() + "' to finish ...", CrawlerManager.class, null,
						getClass());
					for (Class<? extends CrawlerInstance> dependsOnCrawlerClass: dependencies) {
						crawlerCtrlr = getCrawlerCtrlr(dependsOnCrawlerClass);
						if (crawlerCtrlr == null)
							throw new Exception("Crawler class '" + crawlerClass.getName() + 
									  	  "' depends on unregistered crawler class '" + 
									      dependsOnCrawlerClass.getName() + "'!");
						crawlerCtrlr.verifyDependencies(crawlerClass);
						crawlerCtrlr.start();
						crawlerCtrlr.join();
					}
					DebugUtils.printDebugInfo("Waiting for dependent crawlers of '" + 
						crawlerClass.getName() + "' to finish ... Done", CrawlerManager.class, 
						null, getClass());
				}
			}
			
			private boolean startCrawlerInstances(Utils.Pair<CrawlerInstanceExecutor, 
				Future<Boolean>>[] crawlerInsts)
			{
				CrawlerInstanceExecutor crawlerInstExec;
				
				//Start and execute local instance
				try {
					crawlerInstExec = new CrawlerInstanceExecutor(crawlerClass, null);
					crawlerInsts[0] = Utils.createPair(crawlerInstExec, 
										  thdPool.submit(crawlerInstExec));
				}
				catch (Exception e) {
					ExceptionHandler.handle("Failed to start local crawler instance of '" + 
						crawlerClass.getName() + "!", e, CrawlerManager.class, null, getClass());
					return false;
				}
				//Start and execute remote instances
				if (remoteHostAddrs != null) {
					for (int i = 0; i < remoteHostAddrs.length; ++i) {
						try {
							crawlerInstExec = new CrawlerInstanceExecutor(crawlerClass,
													   remoteHostAddrs[i]);
							crawlerInsts[i + 1] = Utils.createPair(crawlerInstExec, 
									  				  thdPool.submit(crawlerInstExec));
						}
						catch (Exception e) {
							ExceptionHandler.handle("Failed to start crawler instance of '" + 
								crawlerClass.getName() + " on host '" + 
								Utils.inetAddressesToString(remoteHostAddrs[i]) + "'!", e, 
								CrawlerManager.class, null, getClass());
							crawlerInsts[i + 1] = null;
						}
					}
				}
				return true;
			}
			
			private boolean joinCrawlerInstances(Utils.Pair<CrawlerInstanceExecutor, 
				Future<Boolean>>[] crawlerInsts)
			{
				boolean exceptionThrown = false;
				
				for (Utils.Pair<CrawlerInstanceExecutor, Future<Boolean>> crawlerInst:
					crawlerInsts) {
					try {
						if (crawlerInst != null)
							exceptionThrown |= crawlerInst.second.get();
					}
					catch (Exception e) {
						exceptionThrown = true;
						ExceptionHandler.handle("Failed to await termination of crawler instances" +
							" of class '" + crawlerClass.getName() + "'!\n", e,
							CrawlerManager.class, null, getClass());
					}
				}
				return exceptionThrown;
			}
			
			private Utils.Pair<String, int[]> getCrawlerInstancesStats(
				Utils.Pair<CrawlerInstanceExecutor,	Future<Boolean>>[] crawlerInsts)
			{
				int remHostAddrIdx = 0;
				String jobsPerHostsInfo = "";
				List<int[]> crawlerStats = new ArrayList<int[]>(crawlerInsts.length);
				
				for (Utils.Pair<CrawlerInstanceExecutor, Future<Boolean>> crawlerInst:
					crawlerInsts) {
					try {
						if (crawlerInst != null) {
							if (remHostAddrIdx == 0)
								jobsPerHostsInfo = "'localhost': ";
							else 
								jobsPerHostsInfo += "\n'" + remoteHostAddrs[remHostAddrIdx - 1].
													    getHostAddress() + "': ";
							remHostAddrIdx++;
							jobsPerHostsInfo += crawlerInst.first.getProcessedJobCount();
							crawlerStats.add(crawlerInst.first.getCrawlerStatistics());
						}
					}
					catch (Exception e) {
						ExceptionHandler.handle("Failed to get statistics of crawler instances" +
							"of class '" + crawlerClass.getName() + "'!\n", e,
							CrawlerManager.class, null, getClass());
					}
				}
				return Utils.createPair(jobsPerHostsInfo,
							crawlerInsts[0].first.combineStatistics(crawlerStats));
			}
			
			private void allCrawlerInstancesFinished(Utils.Pair<CrawlerInstanceExecutor, 
				Future<Boolean>>[] crawlerInsts, boolean exceptionThrown, String jobsPerHostsInfo, 
				int[] crawlerStats) 
			{
				for (Utils.Pair<CrawlerInstanceExecutor, Future<Boolean>> crawlerInst:
					crawlerInsts) {
					try {
						if (crawlerInst != null)
							crawlerInst.first.allInstancesFinished(exceptionThrown, 
								jobsPerHostsInfo, crawlerStats);
					}
					catch (Exception e) {
						ExceptionHandler.handle("Failed to notify crawlers that all instances " +
							"of class '" + crawlerClass.getName() + "' are finished!\n", e,
							CrawlerManager.class, null, getClass());
					}
				}
			}
			
			@SuppressWarnings("unchecked")
			public void run()
			{
				try {
					Utils.Pair<CrawlerInstanceExecutor, Future<Boolean>>[] crawlerInstExecs;
					String dbgInfo = "Executing crawler '" + crawlerClass.getName() + "' using ";
					
					awaitDependentCrawlers();
					if (remoteHostAddrs == null || remoteHostAddrs.length == 0) {
						crawlerInstExecs = new Utils.Pair[1];
						dbgInfo += "1 instance running on host 'localhost' ... ";
					}
					else {
						crawlerInstExecs = new Utils.Pair[remoteHostAddrs.length + 1];
						dbgInfo += (remoteHostAddrs.length + 1) +
									  " instances running on hosts '" + 
									  Utils.inetAddressesToString(remoteHostAddrs) + "' ... ";
					}
					//Start and execute local and remote crawler instances
					if (!startCrawlerInstances(crawlerInstExecs))
						return;
					//Wait for all instances to complete
					boolean exceptionThrown = joinCrawlerInstances(crawlerInstExecs);
					//Get crawler statistics
					Utils.Pair<String, int[]> stats = getCrawlerInstancesStats(crawlerInstExecs);
					//Notify all instances that all instances are finished
					DebugUtils.printDebugInfo(dbgInfo +	"' ... Done", CrawlerManager.class, null, 
						getClass());
					allCrawlerInstancesFinished(crawlerInstExecs, exceptionThrown, stats.first,
						stats.second);
				} 
				catch (Throwable e) {
					ExceptionHandler.handle("Failed to execute crawler of class '" + 
						crawlerClass.getName() + "'!\n", e, CrawlerManager.class, null, getClass());
				}
			}
		}
		
		
		Class<? extends CrawlerInstance> crawlerClass;
		Class<? extends CrawlerInstance>[] dependencies;
		InetAddress[] remoteHostAddrs;
		Future<?> crawlerExecFuture = null;
		
		public void verifyDependencies(Class<? extends CrawlerInstance> dependCrawlerClass)
			throws Exception
		{
			if (dependencies != null) {
				CrawlerController crawlerCtrlr; 
				
				for (Class<? extends CrawlerInstance> dependsOnCrawlerClass: dependencies) {
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
		
		public CrawlerController(Class<? extends CrawlerInstance> crawlerClass, 
			InetAddress[] remoteHostAddrs, Class<? extends CrawlerInstance>[] dependencies) 
		{
			this.crawlerClass = crawlerClass;
			this.dependencies = dependencies;
			this.remoteHostAddrs = remoteHostAddrs;
		}
		
		public void start()
		{
			synchronized (this) {
				if (crawlerExecFuture == null)
					crawlerExecFuture = thdPool.submit(new CrawlerExecutor());
			}
		}
		
		public void join()
		{
			try {
				Future<?> crawlerExecFuture;
				
				synchronized (this) {
					crawlerExecFuture = this.crawlerExecFuture;
				}
				if (crawlerExecFuture != null)
					crawlerExecFuture.get();
			} 
			catch (Throwable e) {
				ExceptionHandler.handle("Failed to await crawler-executor termination of " +
					"crawler class '" + crawlerClass.getName() + "'!\n", e, CrawlerManager.class, 
					null, getClass());
			}
		}
	}
	
	
	private static boolean isMasterNode;
	private static AtomicBoolean running = new AtomicBoolean(true);
	private static ExecutorService thdPool = Executors.newCachedThreadPool();
	private static DBConnector dbConn = null;
	private static Map<Class<? extends CrawlerInstance>, CrawlerController> crawlerCtrlrs = 
		new HashMap<Class<? extends CrawlerInstance>, CrawlerController>();
	
	private static CrawlerController getCrawlerCtrlr(Class<? extends CrawlerInstance> crawlerClass)
	{
		return crawlerCtrlrs.get(crawlerClass);
	}
	
	private static void crawlerMaster_run(boolean execCrawlerNow)
	{
		Utils.Pair<Integer, Integer> crawlTime = CrawlerConfig.getCrawlTime();
		Calendar calendar = Calendar.getInstance();
		
		while (running.get()) {
			calendar.setTime(new Date());
			if (execCrawlerNow || (calendar.get(Calendar.HOUR_OF_DAY) == crawlTime.first && 
				calendar.get(Calendar.MINUTE) <= crawlTime.second && 
				calendar.get(Calendar.MINUTE) + 5 >= crawlTime.second)) {
				try {
					if (dbConn == null)
						dbConn = DBConnector.getInstance();
					dbConn.connect();
				}
				catch (Exception e) {
					ExceptionHandler.handle("Failed to connect to database!\n", e, 
						CrawlerManager.class);
					dbConn = null;
					continue;
				}
				for (CrawlerController crawlerCtrlr: crawlerCtrlrs.values()) 
					crawlerCtrlr.start();
				for (Class<? extends CrawlerInstance> crawlerInst: crawlerCtrlrs.keySet()) {
					CrawlerController crawlerCtrlr = crawlerCtrlrs.get(crawlerInst);
					
					if (crawlerCtrlr != null) 
						crawlerCtrlr.join();
					crawlerCtrlrs.remove(crawlerInst);
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
				execCrawlerNow = false;
			}
			try {
				DebugUtils.printDebugInfo("Going to sleep now...", CrawlerManager.class);
				Thread.sleep(60 * 1000);
			}
			catch (Exception e) {}
			DebugUtils.printDebugInfo("Checking can-execute condition", CrawlerManager.class);
		}
		DebugUtils.printDebugInfo("Stopped", CrawlerManager.class);
	}
	
	private static void crawlerSlave_run()
	{
		RemoteObjectManager.setServerConnectionListener(
			new RemoteObjectManager.ServerConnectionListener() {
				
			private AtomicInteger connCnt = new AtomicInteger();
			
			public void established(InetAddress client) 
			{
				connCnt.incrementAndGet();
				try {
					dbConn.connect();
				} 
				catch (Exception e) {
					ExceptionHandler.handle("Failed to reconnect to database!", e, 
						CrawlerManager.class);
				}
			}
			
			public void closed(InetAddress client) 
			{
				if (connCnt.decrementAndGet() == 0)
					try {
						dbConn.disconnect();
					} catch (Exception e) {}
			}
		});
		Runtime.getRuntime().addShutdownHook(new Thread() {
			
			public void run()
			{
				CrawlerManager.shutdown();
			}
		});
		synchronized (running) {
			while (running.get()) {
				try {
					running.wait();
				} 
				catch (InterruptedException e) {}
			}
		}
	}
	
	@SafeVarargs
	public static void registerCrawler(Class<? extends CrawlerInstance> crawlerClass,
		InetAddress[] remoteHostAddrs, Class<? extends CrawlerInstance> ... dependsOnCrawlers)
	{
		if (!crawlerCtrlrs.containsKey(crawlerClass)) {
			crawlerCtrlrs.put(crawlerClass, new CrawlerController(crawlerClass, remoteHostAddrs, 
				dependsOnCrawlers));
			RemoteObjectManager.registerRemoteObject(CrawlerInstance.class, crawlerClass);
		}
		else
			throw new IllegalArgumentException("Crawler class '" + crawlerClass.getName() + 
					      "' already registered!");
	}
	
	@SafeVarargs
	public static void registerCrawler(Class<? extends CrawlerInstance> crawlerClass,
		Class<? extends CrawlerBase> ... dependsOnCrawlers)
	{
		registerCrawler(crawlerClass, null, dependsOnCrawlers);
	}
	
	public static void registerCrawler(Class<? extends CrawlerInstance> crawlerClass)
	{
		registerCrawler(crawlerClass, (InetAddress[])null);
	}
	
	public static boolean start(boolean isMasterNode) throws Exception
	{
		CrawlerManager.isMasterNode = isMasterNode;
		try {
			dbConn = DBConnector.getInstance();
			dbConn.connect();
		} 
		catch (Exception e) {
			ExceptionHandler.handle("Failed to connect to database!\n", e, CrawlerManager.class);
			return false;
		}
		if (isMasterNode)
			dbConn.clearLogs(CrawlerConfig.getDbgMaxLogs());
		ThreadMonitor.start();
		RemoteObjectManager.start(thdPool);
		CrawlerBase.setExecutionEnvironment(thdPool, dbConn, isMasterNode);
		return true;
	}
	
	public static void run(boolean execCrawlerNow)
	{
		//Current host is the distributed crawlers' master
		if (isMasterNode)
			crawlerMaster_run(execCrawlerNow);
		//Current host is a slave node
		else
			crawlerSlave_run();
	}
	
	public static void shutdown()
	{
		if (!isMasterNode) {
			synchronized (running) {
				if (!running.get())
					return;
				running.set(false);
				running.notifyAll();
			}
		}
		else 
			running.set(false);
		RemoteObjectManager.shutdown();
		ThreadMonitor.shutdown();
		thdPool.shutdown();
		try {
			thdPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
		} 
		catch (InterruptedException e) {
			ExceptionHandler.handle("Failed to shutdown thread pool!", e, CrawlerManager.class);
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
	}
}

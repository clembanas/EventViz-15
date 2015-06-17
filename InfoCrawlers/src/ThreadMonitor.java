/**
 * @author Bernhard Weber
 */
import java.lang.Thread.State;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sun.management.OperatingSystemMXBean;


/**
 * Utility class to monitor multiple threads.
 */
public class ThreadMonitor {
	
	private static int MONITOR_INTERVAL = 500; 				//milliseconds
	private static int WARN_NO_THD_PROGRESS_TIME = 300;		//seconds
	private static int THD_LIVENESS_INFO_INTERVAL = 3000;	//seconds
	private static int RUNTIME_INFO_INTERVAL = 60;			//seconds
	
	
	/**
	 * Available debug flags
	 */
	public static enum DebugFlag implements DebugUtils.DebugFlagBase {
		THD_LIVENESS_INFO(1),
		WARN_NO_THD_PROGRESS(2),
		RUNTIME_INFO(4);
		
		public final int value;
		
		DebugFlag(int value) 
		{
			this.value = value;
		}

		public int toInt() 
		{
			return value;
		}
	}
	
	
	/**
	 * Utility class which holds information about a certain thread. 
	 */
	private static class ThreadInfo {
		
		private Thread thd;
		private String thdDesc;
		private Thread.State lastState = Thread.State.NEW;
		private StackTraceElement lastStackElem = null;
		private long lastProgress;
		private long lastLivenessInfo = 0;
		private boolean warned = false;
		
		private boolean equals(Thread.State state, StackTraceElement[] stackTrace)
		{
			if (lastStackElem == null)
				return lastState == state || stackTrace.length > 0;
			return lastState == state || stackTrace.length == 0 || 
					   lastStackElem.equals(stackTrace[0]);
		}
		
		private void outputWarning(State thdState, StackTraceElement[] stackTrace, 
			long noProgressTime)
		{
			if (DebugUtils.canDebug(ThreadMonitor.class, DebugFlag.WARN_NO_THD_PROGRESS)) {
				if (stackTrace.length > 0) 
					DebugUtils.printDebugInfo("WARNING: Thread " + thd.getId() + " (" +
						thdDesc + ") is in state '" + thdState.toString() + "' in method '" +
						stackTrace[0].toString() + "' more than " + 
						(int)Math.round(noProgressTime/ 1000) + " sec (Trace: " + 
						Utils.stackTraceToString(stackTrace) + ")!", 
						ThreadMonitor.class, DebugFlag.WARN_NO_THD_PROGRESS);
				else
					DebugUtils.printDebugInfo("WARNING: Thread  " + thd.getId() + " (" +
						thdDesc + ") is in state '" + thdState.toString() + 
						"' in method 'unknown' more than " +
						(int)Math.round(noProgressTime/ 1000) + " sec (Trace: None)!", 
						ThreadMonitor.class, DebugFlag.WARN_NO_THD_PROGRESS);
			}
		}
		
		private void outputLivenessInfos(State thdState, StackTraceElement[] stackTrace)
		{
			if (DebugUtils.canDebug(ThreadMonitor.class, DebugFlag.THD_LIVENESS_INFO)) {
				if (stackTrace.length > 0) 
					DebugUtils.printDebugInfo("Liveness info: Thread " + thd.getId() + " (" +
						thdDesc + "): state '" + thdState.toString() + "'; method: '" +
						stackTrace[0].toString() + "'", ThreadMonitor.class,
						DebugFlag.THD_LIVENESS_INFO);
				else
					DebugUtils.printDebugInfo("Liveness info: Thread " + thd.getId() + " (" +
							thdDesc + "): state '" + thdState.toString() + "'; method 'unknown'", 
						ThreadMonitor.class, DebugFlag.THD_LIVENESS_INFO);
			}
		}

		public ThreadInfo(Thread thd, String thdDesc) 
		{
			this.thd = thd;
			this.thdDesc = thdDesc;
			lastProgress = System.currentTimeMillis();
		}
		
		public Thread.State getCurrentThdState()
		{
			return thd.getState();
		}
		
		public void checkAndUpdate(Thread.State currThdState)
		{
			StackTraceElement[] currStackTrace = thd.getStackTrace();
			
			if (equals(currThdState, currStackTrace)) {
				long noProgressTime = System.currentTimeMillis() - lastProgress;
						
				if (!warned && noProgressTime >= WARN_NO_THD_PROGRESS_TIME * 1000) {
					outputWarning(currThdState, currStackTrace, noProgressTime);
					warned = true;
				}
			}
			else {
				lastProgress = System.currentTimeMillis();
				warned = false;
			}
			lastState = currThdState;
			lastStackElem = currStackTrace.length == 0 ? null : currStackTrace[0];
			if (System.currentTimeMillis() - lastLivenessInfo >= THD_LIVENESS_INFO_INTERVAL * 
				1000) {
				outputLivenessInfos(currThdState, currStackTrace);
				lastLivenessInfo = System.currentTimeMillis();
			}
		}
	}

	/**
	 * Internally used thread which monitors all threads registered for monitoring.  
	 */
	private static class MonitoringThread extends Thread {
		
		private static final int MB = 1024*1024;

		private AtomicBoolean shutdownReq = new AtomicBoolean(false);
		
		private void outputRuntimeInfos()
		{
			if (DebugUtils.canDebug(ThreadMonitor.class, DebugFlag.RUNTIME_INFO)) {
				Runtime runtime = Runtime.getRuntime();
				OperatingSystemMXBean mxBean = ManagementFactory.getPlatformMXBean(
												   OperatingSystemMXBean.class);
				
				DebugUtils.printDebugInfo("Runtime info: Used mem: " + 
					(runtime.totalMemory() - runtime.freeMemory())/ MB + "MB; free mem: " + 
					runtime.freeMemory()/ MB + "MB; thds: " + Thread.activeCount() + 
					"; cpu load: " + (int)Math.round(mxBean.getProcessCpuLoad()) + 
					"; total cpu load: " + (int)Math.round(mxBean.getSystemCpuLoad()),	
					ThreadMonitor.class, DebugFlag.RUNTIME_INFO);
			}
		}
		
		public void run() 
		{
			long lastSysInfo = 0;
			ThreadInfo[] thdInfos = new ThreadInfo[0];
			Thread.State currThdState;
			
			while (!shutdownReq.get()) {
				synchronized (monitoredThds) {
					thdInfos = monitoredThds.values().toArray(thdInfos);
				}
				for (ThreadInfo thdInfo: thdInfos) {
					currThdState = thdInfo.getCurrentThdState();
					if (currThdState == Thread.State.TERMINATED) {
						synchronized (monitoredThds) {
							monitoredThds.remove(thdInfo.thd);
							break;
						}
					}
					thdInfo.checkAndUpdate(currThdState);
				}
				if (System.currentTimeMillis() - lastSysInfo > RUNTIME_INFO_INTERVAL * 1000) {
					outputRuntimeInfos();
					lastSysInfo = System.currentTimeMillis();
				}
				try {
					Thread.sleep(MONITOR_INTERVAL);
				}
				catch (Exception e) {}
			}
		}
		
		public void shutdown()
		{
			shutdownReq.getAndSet(true);
			try {
				join();
			} catch (InterruptedException e) {}
		}
	}

	
	private static MonitoringThread monitoringThd = new MonitoringThread();
	private static Map<Thread, ThreadInfo> monitoredThds = new HashMap<Thread, ThreadInfo>();
	
	
	public static void start()
	{
		if (DebugUtils.canDebug(ThreadMonitor.class, DebugFlag.WARN_NO_THD_PROGRESS, 
			DebugFlag.THD_LIVENESS_INFO, DebugFlag.RUNTIME_INFO)) {
			Runtime.getRuntime().traceMethodCalls(true);
			MONITOR_INTERVAL = CrawlerConfig.getDbgThdMonitorInterval(); 				
			WARN_NO_THD_PROGRESS_TIME = CrawlerConfig.getDbgWarnNoThdProgressTime();		
			THD_LIVENESS_INFO_INTERVAL = CrawlerConfig.getDbgThdLivenessInfoInterval();
			RUNTIME_INFO_INTERVAL = CrawlerConfig.getDbgRuntimeInfoInterval();
		}
	}
	
	public static void shutdown()
	{
		monitoringThd.shutdown();
		synchronized (monitoredThds) {
			monitoredThds.clear();
		}
	}
	
	public static void addThread(Thread thd, String thdDesc)
	{
		if (DebugUtils.canDebug(ThreadMonitor.class, DebugFlag.WARN_NO_THD_PROGRESS, 
			DebugFlag.THD_LIVENESS_INFO, DebugFlag.RUNTIME_INFO)) {
			synchronized (monitoredThds) {
				monitoredThds.put(thd, new ThreadInfo(thd, thdDesc));
				if (!monitoringThd.isAlive()) 
					monitoringThd.start();
			}
		}
	}
	
	public static void addThread(Thread thd)
	{
		addThread(thd, thd.getClass().getName());
	}
	
	public static void removeThread(Thread thd)
	{
		synchronized (monitoredThds) {
			monitoredThds.remove(thd);
		}
	}
}

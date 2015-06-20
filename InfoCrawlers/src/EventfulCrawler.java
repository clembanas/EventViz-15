/**
 * @author Robert Bierbauer
 *		   Integrated in the Crawler Framework by Bernhard Weber 
 */
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.evdb.javaapi.APIConfiguration;
import com.evdb.javaapi.data.Event;
import com.evdb.javaapi.data.Performer;
import com.evdb.javaapi.data.SearchResult;
import com.evdb.javaapi.data.request.EventSearchRequest;
import com.evdb.javaapi.operations.EventOperations;


public class EventfulCrawler extends CrawlerBase {
	
	//Constants which are loaded from config-file
	private int WORKER_THD_CNT;
	private String EVENTFUL_UNAME;
	private String EVENTFUL_PWORD;
	private String EVENTFUL_APIKEY;
	private String EVENTFUL_CATEGORY;
	private int MAX_DAYS;
	private int MAX_PAGE_SIZE;
	private int MAX_PAGES;
	
	
	/**
	 * Eventful specific job.
	 */
	protected static class EventfulJob extends JobBase {
		
		public int pageNum;
		
		public EventfulJob(int pageNum)
		{
			this.pageNum = pageNum;
		}
	}
	
	/**
	 * Eventful specific job controller.
	 */
	protected class EventfulJobController implements JobGroupController {
		
		private int pageCnt = 0;
		private AtomicInteger pageIdx = new AtomicInteger(0);
		private int totalEventCnt = 0;
		
		public EventfulJobController()
		{
			EventSearchRequest eventSrchReq = new EventSearchRequest();
			EventOperations eventOps = new EventOperations();
			SearchResult srchRes;
			
			eventSrchReq.setDateRange(dateRange);
			eventSrchReq.setCategory(EVENTFUL_CATEGORY);
			eventSrchReq.setPageSize(MAX_PAGE_SIZE);
			eventSrchReq.setPageNumber(1);
			eventSrchReq.setReadTimeout(20 * 1000);
			eventSrchReq.setConnectionTimeout(20 * 1000);
			try {
				srchRes = eventOps.search(eventSrchReq);
				totalEventCnt = srchRes.getTotalItems();
				pageCnt = Math.min(MAX_PAGES, srchRes.getPageCount());
				DebugUtils.printDebugInfo("Processing " + Math.min(totalEventCnt, 
					pageCnt * MAX_PAGE_SIZE) + " events on " + pageCnt + " pages (" +
					totalEventCnt + " events on " + srchRes.getPageCount() + 
					" pages available)", EventfulCrawler.class);
			}
			catch (Exception e) {
				ExceptionHandler.handle("Failed to retrieve job size information!", e, 
					EventfulCrawler.class, null, getClass());
			}
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
			return MAX_PAGE_SIZE;
		}

		public int getTotalJobCount() 
		{
			return totalEventCnt;
		}
	}
	
	
	private JobGroupController jobController = null;
	private String dateRange;
	private AtomicInteger[] statistics = new AtomicInteger[]{new AtomicInteger(0), 
															 new AtomicInteger(0), 
															 new AtomicInteger(0),
															 new AtomicInteger(0)};
	
	private void processEvents(List<Event> events) throws Exception
	{
		Utils.Pair<DBConnector.PrimaryKey, Boolean> cityID, locationID, eventID, bandID;
		
		for (Event event: events) {
			cityID = dbConnector.insertCity(event.getVenueCity(), event.getVenueRegion(), 
						 event.getVenueCountry());
			locationID = dbConnector.insertLocation(event.getVenueName(), 
							 event.getVenueLongitude(), event.getVenueLatitude(), cityID.first);
			eventID = dbConnector.insertEvent(event.getTitle(), event.getDescription(), 
						  event.getVenueType(), event.getStartTime(), event.getStopTime(),
						  event.getSeid(), locationID.first);
			if (eventID.second)
				statistics[0].incrementAndGet();
			if (locationID.second)
				statistics[1].incrementAndGet();
			if (cityID.second)
				statistics[2].incrementAndGet();
			for (Performer performer: event.getPerformers()) {
				bandID = dbConnector.insertBand(performer.getName());
				if (bandID.second)
					statistics[3].incrementAndGet();
				dbConnector.insertEventPerformer(eventID.first, bandID.first);
			}
		}
	}
	
	protected int getWorkerThdCount() 
	{
		return WORKER_THD_CNT;
	}
	
	protected JobGroupProvider getJobGroupProvider()
	{
		return new JobGroupProvider() {

			public JobBase[] getJobsOfGroup(int groupIdx, int groupCnt, int jobsPerGroup, 
				int totalJobCnt) throws Exception 
			{
				return new JobBase[]{new EventfulJob(groupIdx + 1)};
			}
		};
	}
	
	protected Class<? extends JobGroupController> getJobGroupControllerClass()
	{
		return EventfulJobController.class;
	}
	
	protected RemoteObjectManager.RemoteObjectCreator<JobGroupController> 
		getJobGroupControllerCreator()
	{
		return new RemoteObjectManager.RemoteObjectCreator<JobGroupController>() {

			public synchronized JobGroupController createRemoteObject() 
			{
				if (jobController == null)
					jobController = new EventfulJobController();
				return jobController;
			}
		};
	}

	protected void processJob(JobBase job) throws Exception 
	{
		EventSearchRequest eventSrchReq = new EventSearchRequest();
		EventOperations eventOps = new EventOperations();
		SearchResult srchRes;
		List<Event> events;
		
		eventSrchReq.setDateRange(dateRange);
		eventSrchReq.setCategory(EVENTFUL_CATEGORY);
		eventSrchReq.setPageSize(MAX_PAGE_SIZE);
		eventSrchReq.setPageNumber(((EventfulJob)job).pageNum);
		srchRes = eventOps.search(eventSrchReq);
		events = srchRes.getEvents();
		processEvents(events);
	}
	
	protected void started()
	{
		super.started();
		DebugUtils.printDebugInfo("Processing events for the next " + MAX_DAYS + " days (" + 
			dateRange + ")...", EventfulCrawler.class);
	}
	
	protected void jobStarted(JobBase job, int progress)
	{
		super.jobStarted(job, progress);
		DebugUtils.printDebugInfo("Processing " + Math.min(job.getJobsPerGroup(), 
			job.getTotalJobCount() - job.getGroupIndex() * job.getJobsPerGroup()) + 
			" events on page " + (job.getGroupIndex() + 1) + " (" +	progress + "%) ...", 
			EventfulCrawler.class);
	}
	
	protected void jobFinished(JobBase job,	int progress)
	{
		super.jobFinished(job, progress);
		DebugUtils.printDebugInfo("Processing " + Math.min(job.getJobsPerGroup(), 
			job.getTotalJobCount() - job.getGroupIndex() * job.getJobsPerGroup()) + 
			" events on page " + (job.getGroupIndex() + 1) + " (" +	progress + "%) ... DONE", 
			EventfulCrawler.class);
	}
	
	public EventfulCrawler() throws Exception
	{
		WORKER_THD_CNT = CrawlerConfig.getEventfulCrawlerWorkerThdCount();
		EVENTFUL_UNAME = CrawlerConfig.getEventfulCrawlerUname();
		EVENTFUL_PWORD = CrawlerConfig.getEventfulCrawlerPword();
		EVENTFUL_APIKEY = CrawlerConfig.getEventfulCrawlerApikey();
		EVENTFUL_CATEGORY = CrawlerConfig.getEventfulCrawlerCategory();
		MAX_DAYS = CrawlerConfig.getEventfulCrawlerMaxDays();
		MAX_PAGE_SIZE = CrawlerConfig.getEventfulCrawlerPageSize();
		MAX_PAGES = CrawlerConfig.getEventfulCrawlerMaxPages();
		
		
		SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd00");
		Date now = new Date();
		Calendar calendar = Calendar.getInstance();
		
		APIConfiguration.setEvdbUser(EVENTFUL_UNAME);
		APIConfiguration.setEvdbPassword(EVENTFUL_PWORD);
		APIConfiguration.setApiKey(EVENTFUL_APIKEY);
		calendar.setTime(now);
		calendar.add(Calendar.DATE, MAX_DAYS);
		dateRange = dateFmt.format(now) + "-" + dateFmt.format(calendar.getTime());
	}
	
	public int[] getStatistics()
	{
		int[] result = new int[statistics.length];
		
		for (int i = 0; i < statistics.length; ++i)
			result[i] = statistics[i].get();
		return result;
	}
	
	public String getSummary(int[] crawlerStats)
	{
		return "Added events: " + crawlerStats[0] + "; Added cities: " + crawlerStats[2] + 
					"; Added locations: " + crawlerStats[1] + "; Added bands: " +	crawlerStats[3];
	}
	
	public void allInstancesFinished(boolean exceptionThrown, String jobsPerHostsInfo, 
		int[] crawlerStats)
	{
		super.allInstancesFinished(exceptionThrown, jobsPerHostsInfo, crawlerStats);
		if (isMasterNode) 
			DebugUtils.printDebugInfo("\nSummary of added data:\n   Events: " + 
				crawlerStats[0] + "\n   Cities: " + crawlerStats[2] + "\n   Locations: " +	
				crawlerStats[1] + "\n   Bands: " + crawlerStats[3] + "\n", EventfulCrawler.class);
	}
}

/**
 * @author Robert Bierbauer
 *		   Integrated in the Crawler Framework by Bernhard Weber 
 */
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.evdb.javaapi.APIConfiguration;
import com.evdb.javaapi.data.Event;
import com.evdb.javaapi.data.Performer;
import com.evdb.javaapi.data.SearchResult;
import com.evdb.javaapi.data.request.EventSearchRequest;
import com.evdb.javaapi.operations.EventOperations;


public class EventfulCrawler extends JobBasedCrawler {
	
	//Constants which are loaded from config-file
	private int WORKER_THD_CNT;
	private String EVENTFUL_UNAME;
	private String EVENTFUL_PWORD;
	private String EVENTFUL_APIKEY;
	private String EVENTFUL_CATEGORY;
	private int MAX_DAYS;
	private int MAX_PAGE_SIZE;
	private int MAX_PAGES;
	
	
	public class EventWorkerJob extends WorkerJobBase {
		
		public int pageNum;
		
		public EventWorkerJob(int pageNum)
		{
			this.pageNum = pageNum;
		}
	}
	
	
	private String dateRange;
	private AtomicBoolean allPagesDone = new AtomicBoolean(false);
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

	protected Utils.Pair<WorkerJobBase, Object> getNextWorkerJob(Object customData) throws Exception 
	{
		if (allPagesDone.get())
			return null;
		
		Integer pageNum = (Integer)customData;
		
		if (pageNum == null) 
			pageNum = new Integer(1);
		else
			pageNum = pageNum + 1;
		return Utils.createPair((WorkerJobBase)new EventWorkerJob(pageNum), (Object)pageNum);
	}

	protected void processWorkerJob(WorkerJobBase job) throws Exception 
	{
		EventSearchRequest eventSrchReq = new EventSearchRequest();
		EventOperations eventOps = new EventOperations();
		SearchResult srchRes;
		List<Event> events;
		String dbgInfo = "";
		
		eventSrchReq.setDateRange(dateRange);
		eventSrchReq.setCategory(EVENTFUL_CATEGORY);
		eventSrchReq.setPageSize(MAX_PAGE_SIZE);
		eventSrchReq.setPageNumber(((EventWorkerJob)job).pageNum);
		srchRes = eventOps.search(eventSrchReq);
		if (((EventWorkerJob)job).pageNum > Math.min(MAX_PAGES, srchRes.getPageCount())) {
			allPagesDone.set(true);
			return;
		}
		events = srchRes.getEvents();
		if (DebugUtils.canDebug(EventfulCrawler.class)) {
			dbgInfo = "Processing " + events.size() + " events on page " + srchRes.getPageNumber() + 
						  " (" + (MAX_PAGE_SIZE * (srchRes.getPageNumber() - 1) + events.size()) + 
						  "/" + Math.min(srchRes.getTotalItems(), MAX_PAGE_SIZE * MAX_PAGES) + 
						  "; ~" + srchRes.getTotalItems() + " events on " + srchRes.getPageCount() + 
						  " pages available) ...";
			DebugUtils.printDebugInfo(dbgInfo, EventfulCrawler.class);
		}
		processEvents(events);
		dbConnector.logCrawlerProgress(EventfulCrawler.class, (int)(100.0/ 
			(float)Math.min(srchRes.getTotalItems(), MAX_PAGE_SIZE * MAX_PAGES) * 
			(float)(MAX_PAGE_SIZE * (srchRes.getPageNumber() - 1) + events.size())));
		DebugUtils.printDebugInfo(dbgInfo + " Done", EventfulCrawler.class);
	}
	
	protected void started()
	{
		super.started();
		
		SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd00");
		Date now = new Date();
		Calendar calendar = Calendar.getInstance();
		
		APIConfiguration.setEvdbUser(EVENTFUL_UNAME);
		APIConfiguration.setEvdbPassword(EVENTFUL_PWORD);
		APIConfiguration.setApiKey(EVENTFUL_APIKEY);
		calendar.setTime(now);
		calendar.add(Calendar.DATE, MAX_DAYS);
		dateRange = dateFmt.format(now) + "-" + dateFmt.format(calendar.getTime());
		DebugUtils.printDebugInfo("Processing events for the next " + MAX_DAYS + " days (" + 
			dateRange + ")...", EventfulCrawler.class);
	}
	
	protected void finished(boolean exceptionThrown)
	{
		super.finished(exceptionThrown);
		DebugUtils.printDebugInfo("\nSummary of added data:\n   Events: " + statistics[0].get() + 
			"\n   Cities: " + statistics[2].get() + "\n   Locations: " +	statistics[1].get() + 
			"\n   Bands: " + statistics[3].get() + "\n", EventfulCrawler.class);
		dbConnector.logCrawlerFinished(EventfulCrawler.class, "Added events: " + 
			statistics[0].get() + "; Added cities: " + statistics[2].get() + "; Added locations: " + 
			statistics[1].get() + "; Added bands: " + statistics[3].get());
		statistics = null;
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
	}
}

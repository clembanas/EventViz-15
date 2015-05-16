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


public class EventCrawler extends JobBasedCrawler {
	
	public static final int WORKER_THD_CNT = 10;
	public static final String EVENTFUL_UNAME = "RobertBierbauer";
	public static final String EVENTFUL_PWORD = "3v3ntful";
	public static final String EVENTFUL_APIKEY = "T79WLsPLQkRDJZxv";
	public static final String EVENTFUL_CATEGORY = "music";

	
	public class EventWorkerJob extends WorkerJobBase {
		
		public int pageNum;
		
		public EventWorkerJob(int pageNum)
		{
			this.pageNum = pageNum;
		}
	}
	
	
	private int maxDays;
	private int maxPageSize;
	private int maxPages;
	private String dateRange;
	private AtomicBoolean allPagesDone = new AtomicBoolean(false);
	private AtomicInteger[] statistics = new AtomicInteger[]{new AtomicInteger(0), 
															 new AtomicInteger(0), 
															 new AtomicInteger(0),
															 new AtomicInteger(0)};
	
	private void processEvents(List<Event> events) throws Exception
	{
		Utils.Pair<DBConnection.PrimaryKey, Boolean> cityID, locationID, eventID, bandID;
		
		for (Event event: events) {
			cityID = dbConnection.insertCity(event.getVenueCity(), event.getVenueRegion(), 
						 event.getVenueCountry());
			locationID = dbConnection.insertLocation(event.getVenueName(), 
							 event.getVenueLongitude(), event.getVenueLatitude(), cityID.first);
			eventID = dbConnection.insertEvent(event.getTitle(), event.getDescription(), 
						  event.getSeid(), locationID.first);
			if (eventID.second)
				statistics[0].incrementAndGet();
			if (locationID.second)
				statistics[1].incrementAndGet();
			if (cityID.second)
				statistics[2].incrementAndGet();
			for (Performer performer: event.getPerformers()) {
				bandID = dbConnection.insertBand(performer.getName());
				if (bandID.second)
					statistics[3].incrementAndGet();
				dbConnection.insertEventPerformer(eventID.first, bandID.first);
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
		eventSrchReq.setPageSize(maxPageSize);
		eventSrchReq.setPageNumber(((EventWorkerJob)job).pageNum);
		srchRes = eventOps.search(eventSrchReq);
		if (((EventWorkerJob)job).pageNum > Math.min(maxPages, srchRes.getPageCount())) {
			allPagesDone.set(true);
			return;
		}
		events = srchRes.getEvents();
		if (debug_canDebug(EventCrawler.class)) {
			dbgInfo = "Processing " + events.size() + " events on page " + srchRes.getPageNumber() + 
						  " (" + (maxPageSize * (srchRes.getPageNumber() - 1) + events.size()) + 
						  "/" + Math.min(srchRes.getTotalItems(), maxPageSize * maxPages) + "; ~" + 
						  srchRes.getTotalItems() + " events on " + srchRes.getPageCount() + 
						  " pages available) ...";
			debug_print(dbgInfo);
		}
		processEvents(events);
		dbConnection.logCrawlerProgress(EventCrawler.class, (int)(100.0/ 
			(float)Math.min(srchRes.getTotalItems(), maxPageSize * maxPages) * 
			(float)(maxPageSize * (srchRes.getPageNumber() - 1) + events.size())));
		debug_print(dbgInfo + " Done");
	}
	
	protected void started()
	{
		SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd00");
		Date now = new Date();
		Calendar calendar = Calendar.getInstance();
		
		APIConfiguration.setEvdbUser(EVENTFUL_UNAME);
		APIConfiguration.setEvdbPassword(EVENTFUL_PWORD);
		APIConfiguration.setApiKey(EVENTFUL_APIKEY);
		calendar.setTime(now);
		calendar.add(Calendar.DATE, maxDays);
		dateRange = dateFmt.format(now) + "-" + dateFmt.format(calendar.getTime());
		debug_print("Processing events for the next " + maxDays + " days (" + dateRange + ")...");
	}
	
	protected void finished()
	{
		debug_print("\nSummary of added data:\n   Events: " + statistics[0].get() + 
			"\n   Cities: " + statistics[1].get() + "\n   Locations: " +	statistics[2].get() + 
			"\n   Bands: " + statistics[3].get() + "\n");
		dbConnection.logCrawlerFinished(EventCrawler.class, "Added events: " + statistics[0].get() + 
			"; Added cities: " + statistics[1].get() + "; Added locations: " + statistics[2].get() + 
			"; Added bands: " + statistics[3].get());
		statistics = null;
	}
	
	public EventCrawler(Utils.Triple<Integer, Integer, Integer> settings)
	{
		maxDays = settings.first;
		maxPageSize = settings.second;
		maxPages = settings.third;
	}
}

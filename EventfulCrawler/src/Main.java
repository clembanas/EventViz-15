import com.evdb.javaapi.APIConfiguration;
import com.evdb.javaapi.EVDBAPIException;
import com.evdb.javaapi.EVDBRuntimeException;
import com.evdb.javaapi.data.Event;
import com.evdb.javaapi.data.Group;
import com.evdb.javaapi.data.Performer;
import com.evdb.javaapi.data.SearchResult;
import com.evdb.javaapi.data.request.EventSearchRequest;
import com.evdb.javaapi.operations.EventOperations;


public class Main {
	public static void main(String args[]){
		DatabaseConnection dbc = new DatabaseConnection();
		int count = 0;
		APIConfiguration.setEvdbUser("RobertBierbauer");
		APIConfiguration.setEvdbPassword("3v3ntful");
		APIConfiguration.setApiKey("T79WLsPLQkRDJZxv");

		EventOperations eo = new EventOperations();
		EventSearchRequest esr = new EventSearchRequest();

		esr.setDateRange("2015030300-2015120400");
		esr.setCategory("music");
		esr.setPageSize(100);
		SearchResult sr = null;
		try {
			for(int i = 1; i <= 2; i++){				
				esr.setPageNumber(i);
				sr = eo.search(esr);
				for(int j = 0; j<sr.getEvents().size(); j++){
					Event e = (Event)sr.getEvents().get(j);
					int cityID = dbc.insertCity(e.getVenueCity(), e.getVenueRegion(), e.getVenueCountry(), e.getVenuePostalCode(),  0, 0);
					int locationID = dbc.insertLocation(e.getVenueName(), e.getVenueLongitude(), e.getVenueLatitude(), cityID);
					int eventID = dbc.insertEvent(e.getTitle(), e.getDescription(), "", e.getSeid(), locationID);
					if(e.getPerformers().size() > 0){
						for(Performer p : e.getPerformers()){
							int bandID = dbc.insertBand(p.getName());
							dbc.insertEventPerformer(eventID, bandID);
						}
					}
				}
			}
		}catch(EVDBRuntimeException var){
			var.printStackTrace();
		} catch( EVDBAPIException var){
			var.printStackTrace();
		}
	}
}

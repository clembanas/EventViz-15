/**
 * @author Bernhard Weber
 */
import java.util.List;


/**
 * Crawler instance interface 
 */
public interface CrawlerInstance {

	//Executes the crawler and blocks until it is finished
	public boolean execute() throws Exception;
	
	//Retrieves the crawler statistics
	public int[] getStatistics(); 
	
	//Combine multiple statistics
	public int[] combineStatistics(List<int[]> crawlerStats);
	
	//Retrieves the crawler summary
	public String getSummary(int[] crawlerStats);
	
	//Notifies the crawler instance that all other instances of that crawler are finished
	public void allInstancesFinished(boolean exceptionThrown, int[] crawlerStats);
}

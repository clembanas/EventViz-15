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
	
	//Notifies the crawler instance that all other instances of that crawler are finished
	public void allInstancesFinished(boolean exceptionThrown, String jobsPerHostsInfo, 
		int[] crawlerStats);
	
	//Returns the count of jobs processed by the crawler
	public int getProcessedJobCount();
	
	//Retrieves the crawler statistics
	public int[] getStatistics(); 
	
	//Combine multiple statistics
	public int[] combineStatistics(List<int[]> crawlerStats);
	
	//Retrieves the crawler summary
	public String getSummary(int[] crawlerStats);
}

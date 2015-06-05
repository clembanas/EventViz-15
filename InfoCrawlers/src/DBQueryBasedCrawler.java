/**
 * @author Bernhard Weber
 */
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;


/**
 * Base class of crawlers which collects information based on the results of a DB query
 */
public abstract class DBQueryBasedCrawler extends JobBasedCrawler {
	
	/**
	 * Job whose associated data row has to be processed by a worker thread
	 */
	private class DBWorkerJob extends WorkerJobBase {
		
		public String[] dataRow = null;
		public int dataRowIdx;
		
		DBWorkerJob(ResultSet dataset, ResultSetMetaData datasetMeta) 
		{
			try {
				int colCount = datasetMeta.getColumnCount();
				
				dataRowIdx = dataset.getRow(); 
				dataRow = new String[colCount];
				for (int i = 0; i < colCount; ++i) {
					this.dataRow[i] = dataset.getString(i + 1);
					//Convert SQL-NULL entries to empty string
					if (dataRow[i] == null)
						dataRow[i] = new String("");
				}
			} 
			catch (Exception e) {
				ExceptionHandler.handle("Failed to create worker job for crawler '" + 
					DBQueryBasedCrawler.this.getClass().getName() + "'!", e, 
					DBQueryBasedCrawler.this.getClass(), DBQueryBasedCrawler.class, getClass());
			}
		}
	};
	
	
	private int datasetCnt = 0;
	private int datasetIdx = 0;
	private int lastEnqueueInfo = 0;

	protected abstract int getWorkerThdCount();
	protected abstract int getDatasetCount() throws Exception;
	protected abstract Utils.Pair<ResultSet, Object> getNextDataset(Object customData) 
		throws Exception;
	protected abstract void processDataRow(String[] dataRow, int dataRowIdx) throws Exception;
	
	@SuppressWarnings("unchecked")
	protected Utils.Pair<WorkerJobBase, Object> getNextWorkerJob(Object customData) throws Exception
	{
		Utils.Pair<ResultSet, Object> nextDataset = null;
		ResultSetMetaData datasetMeta = null;

		if (customData != null) {
			nextDataset = ((Utils.Pair<Utils.Pair<ResultSet, Object>, ResultSetMetaData>)
							  customData).first;
			datasetMeta = ((Utils.Pair<Utils.Pair<ResultSet, Object>, ResultSetMetaData>)
							  customData).second;
		}
		else
			datasetCnt = getDatasetCount();
		try {
			if (nextDataset == null || !nextDataset.first.next()) {
				if (nextDataset != null) {
					try {
						nextDataset.first.close();
					}
					catch (Exception e) {}
					nextDataset = getNextDataset(nextDataset.second);
				}
				else
					nextDataset = getNextDataset(null);
				if (nextDataset == null || nextDataset.first == null || !nextDataset.first.next())
					return null;
				datasetMeta = nextDataset.first.getMetaData();
			}
			datasetIdx++;
			if (lastEnqueueInfo <= (100.0/ (float)datasetCnt) * (float)datasetIdx - 1.0) {
				lastEnqueueInfo = (int)Math.ceil((100.0/ (float)datasetCnt) * (float)datasetIdx);
				DebugUtils.printDebugInfo(lastEnqueueInfo + "% of " + datasetCnt + 
					" datasets processed...", getClass(), DBQueryBasedCrawler.class);
				dbConnector.logCrawlerProgress(getClass(), lastEnqueueInfo);
			}
			return Utils.createPair((WorkerJobBase)new DBWorkerJob(nextDataset.first, datasetMeta), 
					   (Object)Utils.createPair(nextDataset, datasetMeta));
		}
		catch (Exception e) {
			if (nextDataset != null && nextDataset.first != null) {
				try {
					nextDataset.first.close();
				}
				catch (Exception e1) {}
			}
			throw e;
		}
	}
	
	protected void processWorkerJob(WorkerJobBase job) throws Exception
	{
		if (((DBWorkerJob)job).dataRow != null)
			processDataRow(((DBWorkerJob)job).dataRow, ((DBWorkerJob)job).dataRowIdx);
	}
}

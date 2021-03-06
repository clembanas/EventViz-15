/**
 * @author Bernhard Weber
 */
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;


/**
 * Base class of SPARQL based crawlers 
 */
public abstract class SparqlBasedCrawler extends DBQueryBasedCrawler {
	
	protected static final String DEF_ACCEPTED_LANG_ID = "en";
	protected static final String RESPROP_NAME = "rdfs:label";
	protected static final String RESPROP_TYPE = "rdf:type";
		

	/**
	 * Context per query and data row
	 */
	public class QueryContext {
		
		private QueryExecutor queryExec;
		private int queryIdx = 0;
		private String[] dataRow = null;
		private int dataRowIdx = 0;
		private String endpoint;
		private ResultSet resultSet;
		
		public QueryContext(QueryExecutor queryExec, String endpoint, String[] dataRow, 
			int dataRowIdx, int queryIdx)
		{
			this.queryExec = queryExec;
			this.endpoint = endpoint;
			this.dataRow = dataRow;
			this.dataRowIdx = dataRowIdx;
			this.queryIdx = queryIdx;
		}
		
		public void debug_print_dataRow(final String fmtStr, final String[] dataRow) 
			throws Exception
		{
			if (DebugUtils.canDebug(SparqlBasedCrawler.class)) 
				DebugUtils.printDebugInfo(String.format(fmtStr, 
					replaceWildcards(SparqlBasedCrawler.this.getDebugDSFmtStr(), dataRow, false)),
					SparqlBasedCrawler.this.getClass(), SparqlBasedCrawler.class, getClass());
		}
		
		public void debug_print_dataRow(final String fmtStr) throws Exception
		{
			debug_print_dataRow(fmtStr, dataRow);
		}

		public void debug_print_query(final String prefix, final String postfix) throws Exception
		{
			debug_print_dataRow(prefix + getQueryIdx() + ". query for " + getDataRowIdx() + 
				". data row '%s'" + postfix);
		}
		
		protected void handleQueryException(Exception e) 
		{
			try {
				ExceptionHandler.handle("Failed to execute " + getQueryIdx() + ". query for " + 
					getDataRowIdx() + ". data row '" +
					replaceWildcards(SparqlBasedCrawler.this.getDebugDSFmtStr(), dataRow, false) + 
					"'!", e, false, SparqlBasedCrawler.this.getClass(), SparqlBasedCrawler.class, 
					getClass());
			} 
			catch (Exception e1) {
				ExceptionHandler.handle(String.format("Failed to execute " + getQueryIdx() +	
					". query for " + getDataRowIdx() + ". data row '[%s]'!", e1.getMessage()), e, 
					false, SparqlBasedCrawler.this.getClass(), SparqlBasedCrawler.class, 
					getClass());
			}
		}
		
		public String escapeStr(String str)
		{
			if (str.startsWith("\""))
				str = str.substring(1);
			if (str.endsWith("\""))
				str = str.substring(0, str.length() - 1);
			return str.replace("'", "\\'").replace("\"", "\" AND \"");
		}
		
		public String escapeRegEx(String str)
		{
			if (str.startsWith("\""))
				str = str.substring(1);
			if (str.endsWith("\""))
				str = str.substring(0, str.length() - 1);
			
			StringBuilder res = new StringBuilder(str.length());
			char c;
			
			for (int i = 0; i < str.length(); ++i) {
				if ((c = str.charAt(i)) >= 128)
					res.append(String.format("\\u%04x", (int)c));
				else
					res.append(c);
			}
			return "\\\\Q" + res.toString().replace("\"", "\\\"") + "\\\\E";
		}
		
		public String replaceWildcards(String fmtStr, String[] dataRow, boolean allRequired) 
			throws Exception
		{
			String[] fmtStrParts = fmtStr.split("%(?=\\dcol\\_\\w*)");
			
			fmtStr = fmtStrParts[0];
			for (int i = 1, len = fmtStrParts.length; i < len; ++i) {
				String currPart = fmtStrParts[i];
				int colIdxLen = currPart.indexOf("col");
				int colIdx = Integer.valueOf(currPart.substring(0, colIdxLen));
				String currCol;
				
				currPart = currPart.substring(colIdxLen + 3);
				//Skip query if required data row column doesn't exist
				if (allRequired && (colIdx >= dataRow.length || dataRow[colIdx].isEmpty() || 
					dataRow[colIdx].equalsIgnoreCase("null")))
					return null;
				//Use empty string for invalid column index or NULL entry
				else if (colIdx >= dataRow.length)
					currCol = "";
				//Get appropriate column data
				else 
					currCol = dataRow[colIdx].trim();
				//No escaping
				if (currPart.startsWith("_noEsc"))
					fmtStr += currCol + currPart.substring(6);
				//Escape column data so that it is a valid SPARQL string
				else if (currPart.startsWith("_asStr"))
					fmtStr += escapeStr(currCol) + currPart.substring(6);
				//Escape column data so that it is a valid SPARQL lower case string
				else if (currPart.startsWith("_asLStr"))
					fmtStr += escapeStr(currCol.toLowerCase()) + currPart.substring(7);	
				//Escape column data so that it is a valid SPARQL RegEx
				else if (currPart.startsWith("_asRegEx"))
					fmtStr += escapeRegEx(currCol) + currPart.substring(8);	
				//Escape column data so that it is a valid SPARQL lower case RegEx
				else if (currPart.startsWith("_asLRegEx"))
					fmtStr += escapeRegEx(currCol.toLowerCase()) + currPart.substring(9);	
				//Invalid or missing escape specifier
				else
					throw new Exception("Invalid or missing escape specifier '" + currPart + "'!");
			}
			return fmtStr;
		}
		
		public String replaceWildcards(String fmtStr, String[] dataRow) throws Exception
		{
			return replaceWildcards(fmtStr, dataRow, true);
		}
		
		public String replaceWildcards(String fmtStr) throws Exception
		{
			return replaceWildcards(fmtStr, dataRow, true);
		}
		
		public String getEndpoint()
		{
			return endpoint;
		}
		
		public QueryExecutor getQueryExecutor() 
		{
			return queryExec;
		}
		
		public int getQueryIdx() 
		{
			return queryIdx;
		}
		
		public DBConnector getDBConnector()
		{
			return SparqlBasedCrawler.dbConnector; 
		}
		
		public String[] getDataRow() 
		{
			return dataRow;
		}

		public int getDataRowIdx() 
		{
			return dataRowIdx;
		}
		
		public void setResultSet(ResultSet resultSet)
		{
			this.resultSet = resultSet;
		}

		public ResultSet getResultSet() 
		{
			return resultSet;
		}
		
		public SparqlResourceCache getResourceCache()
		{
			return SparqlBasedCrawler.this.resCache;
		}
	}
	
	/**
	 * Base class of query preprocessor
	 */
	public static abstract class QueryPreProcBase {
		
		public abstract String execute(QueryContext queryContext) throws Exception;
	}
	
	/**
	 * Base class of query postprocessor
	 */
	public static abstract class QueryPostProcBase {
		
		private String buildResPropertyQueryStr(String resID, String[] propNames, String langID)
		{
			StringBuilder queryStrBuilder = new StringBuilder("SELECT DISTINCT * WHERE {\n");
			
			for (int i = 1; i <= propNames.length; ++i) {
				queryStrBuilder.append("   OPTIONAL {\n      <");
				queryStrBuilder.append(resID);
				queryStrBuilder.append("> ");
				queryStrBuilder.append(propNames[i - 1]);
				queryStrBuilder.append(" ?propValue" + i);
				queryStrBuilder.append(".\n      FILTER(!ISLITERAL(?propValue" + i + 
					") || LANG(?propValue" + i + ") = \"\" || LANGMATCHES(LANG(?propValue" + i + 
					"), \"" + langID + "\"))\n");
				queryStrBuilder.append("   }\n");
			}
			queryStrBuilder.append("}");
			return queryStrBuilder.toString();
		}

		private SparqlResourceCache.MultiPropValueList performResPropertyQuery(
			QueryContext queryContext, String resID, String[] propNames, String langID)
		{
			DebugUtils.printDebugInfo("Retrieving properties of resource '" + resID + "'...",
				SparqlBasedCrawler.class, null, getClass());
			try {
				ResultSet resSet = SparqlQuery.execute(queryContext.getEndpoint(), 
									   buildResPropertyQueryStr(resID, propNames, langID));
				SparqlResourceCache.PropertyList propList = new SparqlResourceCache.PropertyList();
				SparqlResourceCache.MultiPropValueList result;
				
				while (resSet.hasNext()) {
					QuerySolution querySol = resSet.next();
					
					for (int i = 1; i <= propNames.length; ++i) 
						propList.add(propNames[i - 1], querySol.get("propValue" + i));
				}
				DebugUtils.printDebugInfo("Retrieving properties of resource '" + resID + 
					"' ... Done (" + (propList.isEmpty() ? 0 : 
					ResultSetFactory.makeRewindable(resSet).size()) + " rows)",
					SparqlBasedCrawler.class, null, getClass());
				result = propList.lookup();
				queryContext.getResourceCache().addProperties(resID, propList);
				DebugUtils.printDebugInfo("Cache updated (Contains now " + 
					queryContext.getResourceCache().getSizeInBytes() + " Bytes)",
					SparqlBasedCrawler.class, null, getClass());
				return result;
			}
			catch (Exception e) {
				ExceptionHandler.handle("Failed to retrieve properties of resource " + resID + 
					"'!", e, SparqlBasedCrawler.class, null, getClass());
				return null;
			}
		}
		
		protected boolean checkLangID(final String expLangID, final String actLangID)
		{
			return expLangID == null || expLangID.isEmpty() || actLangID == null || 
					   actLangID.isEmpty() || expLangID.equalsIgnoreCase(actLangID);
		}
		
		protected boolean checkLangID(final String expLangID, final Literal literal)
		{
			return checkLangID(expLangID, literal.getLanguage());
		}
		
		protected boolean checkLangID(final Literal literal)
		{
			return checkLangID(DEF_ACCEPTED_LANG_ID, literal.getLanguage());
		}
		
		protected SparqlResourceCache.PropertyValue[] getResourceProperties(
			QueryContext queryContext, String resID, String propName)
		{
			SparqlResourceCache.PropertyValue[] propVals = queryContext.getResourceCache().
														 	   lookupProperties(resID, propName);
			
			if (propVals == null) {
				SparqlResourceCache.MultiPropValueList propValsByQuery;
				
				propValsByQuery = performResPropertyQuery(queryContext, resID, 
									  new String[]{propName}, DEF_ACCEPTED_LANG_ID);				
				return propValsByQuery == null ? null : propValsByQuery.get(propName);
			}
			return propVals;
		}
		
		protected SparqlResourceCache.MultiPropValueList getResourceProperties(
			QueryContext queryContext, String resID, String ... propNames)
		{
			SparqlResourceCache.MultiPropValueList propVals = 
				queryContext.getResourceCache().lookupProperties(resID, propNames);
			
			if (propVals == null) 
				return performResPropertyQuery(queryContext, resID, propNames, 
						   DEF_ACCEPTED_LANG_ID);
			return propVals;
		}
		
		protected String getResourceName(QueryContext queryContext, 
			SparqlResourceCache.PropertyValue propValue)
		{
			if (propValue == null)
				return null;
			if (propValue.isLiteral())
				return propValue.getValue();
			else if (propValue.isResource())
				return getResourceName(queryContext, propValue.getValue());
			return null;			
		}
		
		protected String getResourceName(QueryContext queryContext, String resID)
		{
			SparqlResourceCache.PropertyValue[] propVals = getResourceProperties(queryContext, 
															   resID, RESPROP_NAME);
			
			return propVals == null || propVals.length == 0 ? null : propVals[0].getValue();			
		}
		
		protected String getResourceName(QueryContext queryContext, RDFNode nameNode)
		{
			if (nameNode == null)
				return null;
			if (nameNode.isLiteral()) {
				if (checkLangID(nameNode.asLiteral()))
					return nameNode.asLiteral().getString();
				return null;
			}
			if (nameNode.isResource())
				return getResourceName(queryContext, nameNode.asResource().getURI());
			DebugUtils.printDebugInfo("Warning: Name-node is of unexpected type!", 
				SparqlBasedCrawler.class, null, getClass());
			return null;
		}
		
		protected abstract boolean processQueryResult(QueryContext queryContext, ResultSet resSet) 
			throws Exception;
		
		public boolean execute(QueryContext queryContext) throws Exception
		{
			ResultSet resSet = queryContext.getResultSet();
			
			if (!resSet.hasNext()) {
				queryContext.debug_print_query("", " returned no results!");
				return false;
			}
	        else if (processQueryResult(queryContext, resSet)) {
		        queryContext.debug_print_query("Executing ", " ... Done (" +
		        	ResultSetFactory.makeRewindable(resSet).size() + " rows)");
		        return true;
	        }
	        else {
	        	queryContext.debug_print_query("", " results skipped by postprocessor!");
		        return false;
	        }
		}
	}
	
	/**
	 * Default query preprocessor
	 */
	public static class DefQueryPreProc extends QueryPreProcBase {

		public String execute(QueryContext queryContext) throws Exception
		{
			return queryContext.replaceWildcards(queryContext.getQueryExecutor().getQueryFmtStr());
		}
	}
	
	/**
	 * Default query postprocessor
	 */
	public static class DefQueryPostProc extends QueryPostProcBase {
		
		protected boolean processQueryResult(QueryContext queryContext, ResultSet resSet) 
			throws Exception
		{
			return true;
		}
	}
	
	/**
	 * Executes a SPARQL query using a certain query context
	 */
	public static class QueryExecutor {
		
		private String queryFmtStr;
		public QueryPreProcBase preprocessor = null;
		public QueryPostProcBase postprocessor = null;

		protected ResultSet executeSparqlQuery(String endpoint, String query)
		{
			return SparqlQuery.execute(endpoint, query);
		}
		
		public QueryExecutor(String queryFmtStr, QueryPreProcBase preprocessor, 
				QueryPostProcBase postprocessor)
		{
			this.queryFmtStr = queryFmtStr;
			if (preprocessor == null)
				preprocessor = new DefQueryPreProc();
			this.preprocessor = preprocessor;
			if (postprocessor == null)
				postprocessor = new DefQueryPostProc();
			this.postprocessor = postprocessor;
		}
		
		public QueryExecutor(String queryFmtStr, QueryPreProcBase preprocessor)
		{
			this(queryFmtStr, preprocessor, null);
		}
		
		public QueryExecutor(String queryFmtStr, QueryPostProcBase postprocessor)
		{
			this(queryFmtStr, null, postprocessor);
		}
		
		public QueryExecutor(String queryFmtStr)
		{
			this(queryFmtStr, null, null);
		}
		
		public boolean execute(QueryContext queryContext)
		{
			try {
				queryContext.debug_print_query("Executing ", "...");
				
				//Replace wildcards in SPARQL query by entries from associated data row
				String queryStr = preprocessor.execute(queryContext);
				
				if (queryStr == null) {
					queryContext.debug_print_query("", " skipped by preprocessor!");
					return false;
				}
				
				//Execute SPARQL query
				queryContext.setResultSet(executeSparqlQuery(queryContext.getEndpoint(), queryStr));
				return postprocessor.execute(queryContext);
			}
			catch (Exception e) {
				queryContext.handleQueryException(e);
				return false;
			}
		}
		
		public String getQueryFmtStr() 
		{
			return queryFmtStr;
		}
	}
	

	private String[] endpoints;
	private QueryExecutor[] queryExecutors;
	private String debugDSFmtStr;
	protected SparqlResourceCache resCache;
	
	protected void processDataRow(String[] dataRow, int dataRowIdx) throws Exception 
	{
		for (String endpoint: endpoints) {
			for (int j = 0, queryLen = queryExecutors.length; j < queryLen; ++j) {
				if (queryExecutors[j].execute(new QueryContext(queryExecutors[j], endpoint, dataRow, 
					dataRowIdx,	j + 1)))
					return;
			}
		}
	}
	
	public SparqlBasedCrawler(String[] endpoints, QueryExecutor[] queryHandlers, 
		String debugDSFmtStr) throws Exception
	{
		resCache = new SparqlResourceCache(RESPROP_NAME);
		this.endpoints = endpoints;
		this.queryExecutors = queryHandlers;
		this.debugDSFmtStr = debugDSFmtStr;
		SparqlQuery.init();
	}
	
	public String[] getEndpoints()
	{
		return endpoints;
	}
	
	public String getDebugDSFmtStr()
	{
		return debugDSFmtStr;
	}
	
	public void allInstancesFinished(boolean exceptionThrown, String jobsPerHostsInfo, 
		int[] crawlerStats)
	{
		super.allInstancesFinished(exceptionThrown, jobsPerHostsInfo, crawlerStats);
		if (resCache != null)
			resCache.clear();
		resCache = null;
	}
}

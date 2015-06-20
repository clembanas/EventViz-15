/**
 * @author Bernhard Weber
 */
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.rdf.model.RDFNode;


/**
 * Utility class encapsulating the SPARQL query-execution process.
 */
public class SparqlQuery {
	
	public static String[] DEF_PREFIXES = new String[] {
			"owl: <http://www.w3.org/2002/07/owl#>",
			"xsd: <http://www.w3.org/2001/XMLSchema#>",
			"rdfs: <http://www.w3.org/2000/01/rdf-schema#>",
			"rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>",
			"foaf: <http://xmlns.com/foaf/0.1/>",
			"dc: <http://purl.org/dc/elements/1.1/>",
			": <http://dbpedia.org/resource/>",
			"dbpedia2: <http://dbpedia.org/property/>",
			"dbpedia: <http://dbpedia.org/>",
			"dbpedia-owl: <http://dbpedia.org/ontology/>",
			"skos: <http://www.w3.org/2004/02/skos/core#>",
			"geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>"
		};
	public static final int LIMIT_NONE = 0;
	private static int DEF_LIMIT;
	private static int MAX_QUERY_RETRIES;
	private static int QUERY_RETRY_DELAY;		//msec
	
	
	/**
	 * Available debug flags
	 */
	public static enum DebugFlag implements DebugUtils.DebugFlagBase {
		QUERIES(1),
		QUERY_RESULTS(2),
		NO_QUERY_STRING(4),
		NO_QUERY_PREFIXES(8);
		
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

	
	private static Map<String, String> defPrefixCache = genPrefixCache(DEF_PREFIXES);	

	private static void debug_queryResult(String query, ResultSet resSet)
	{
		if (resSet.hasNext()) {
			List<String> varNames = resSet.getResultVars();
			int varCnt = varNames.size();
			String[] entries = new String[varCnt];
			DebugUtils.TableDebugger tableDebugger = new DebugUtils.TableDebugger();
  				
  			tableDebugger.setHeader(varNames);
			do {
				QuerySolution querySol = resSet.next();	
				
				entries = new String[varCnt];
				for (int i = 0; i < varCnt; ++i) {
					RDFNode currNode = querySol.get(varNames.get(i));
					
					if (currNode == null)
						entries[i] = "null";
					else if (currNode.isLiteral())
						entries[i] = currNode.asLiteral().getString() + " [L]";
					else if (currNode.isResource())
						entries[i] = currNode.asResource().getURI() + " [R]";
					else if (currNode.isURIResource())
						entries[i] = currNode.asResource().getURI() + " [U]";
					else 
						entries[i] = currNode + " [?]";
				}
				tableDebugger.addRow(entries);
			}
			while (resSet.hasNext());
				DebugUtils.printDebugInfo("Results of query '" + 
					(DebugUtils.canDebug(SparqlQuery.class, DebugFlag.NO_QUERY_STRING) ? "..." : 
					query) + "':\n" + tableDebugger, SparqlQuery.class);
		}
		else 
			DebugUtils.printDebugInfo("Query '" + 
				(DebugUtils.canDebug(SparqlQuery.class, DebugFlag.NO_QUERY_STRING) ? "..." : 
				query) + "' returned no results!", SparqlQuery.class);
	}
	
	private static Map<String, String> genPrefixCache(String[] prefixes) 
	{
		Map<String, String> prefixCache = new TreeMap<String, String>(
												   String.CASE_INSENSITIVE_ORDER);
		
		for (String prefix: prefixes) {
			int prefixNameEnd = prefix.indexOf(": <");
			
			if (prefixNameEnd == -1)
				throw new IllegalArgumentException("Invalid SPARQL prefix '" + prefix + "'!");
			prefixCache.put(prefix.substring(0, prefixNameEnd + 1), 
				prefix.substring(prefixNameEnd + 3, prefix.length() - 1));
		}
		return prefixCache;
	}
	
	private static String expandPrefixByCache(String prefixedStr, Map<String, String> prefixCache) 
	{
		int prefixNameEnd = prefixedStr.indexOf(":");
		String expandPrefix;
		
		if (prefixNameEnd == -1)
			return prefixedStr;
		expandPrefix = prefixCache.get(prefixedStr.substring(0, prefixNameEnd + 1).trim());
		if (expandPrefix != null)
			return expandPrefix + prefixedStr.substring(prefixNameEnd + 1);
		return prefixedStr;
	}
	
	private static ResultSet executeQuery(String svc, String query)
	{
		QueryExecution queryExec = QueryExecutionFactory.sparqlService(svc, query);
		int retries = 0;
		long retryQueryDelay = 0;
		
		try {
			queryExec.setTimeout(60 * 1000, 90 * 1000);
			while (true) {
				try {
					ResultSet resSet = queryExec.execSelect();
		        
			  		if (DebugUtils.canDebug(SparqlQuery.class, DebugFlag.QUERY_RESULTS)) {
			  			ResultSetRewindable rewindResSet = ResultSetFactory.makeRewindable(resSet);
			  			
			  			debug_queryResult(query, rewindResSet);
			  			rewindResSet.reset();
			  			return rewindResSet;
			  		}
			  		return ResultSetFactory.makeRewindable(resSet);
				}
				catch (QueryExceptionHTTP e) {
					if (retries < MAX_QUERY_RETRIES && ( 
						 e.getResponseCode() == 503 ||  //Service temporarily unavailable
						 e.getResponseCode() == 405     //Query not allowed
						)) {
						retries++;
						retryQueryDelay += QUERY_RETRY_DELAY;
						try {
							Thread.sleep(retryQueryDelay);
						} catch (InterruptedException e1) {}
					}
					else 
						throw e;
				}
			}
		}
		finally {
			queryExec.close();
		}
	}
	
	public static void init()
	{
		DEF_LIMIT = CrawlerConfig.getSparqlBasedCrawlerDefQueryLimit();
		MAX_QUERY_RETRIES = CrawlerConfig.getSparqlBasedCrawlerMaxQueryRetries();
		QUERY_RETRY_DELAY = CrawlerConfig.getSparqlBasedCrawlerQueryRetryDelay();
	}

	public static ResultSet execute(String svc, String query, String[] prefixes)
	{
		if (DEF_LIMIT != LIMIT_NONE)
			query += " LIMIT " + DEF_LIMIT;
		if (DebugUtils.canDebug(SparqlQuery.class, DebugFlag.NO_QUERY_PREFIXES)) {
			if (DebugUtils.canDebug(SparqlQuery.class, DebugFlag.QUERIES)) 
				DebugUtils.printDebugInfo("Executing query '" + 
					(DebugUtils.canDebug(SparqlQuery.class, DebugFlag.NO_QUERY_STRING) ? "..." : 
					query) + "' on '" +	svc + "'...", SparqlQuery.class);
			query = (prefixes.length == 0 ? "" : "PREFIX " +
						StringUtils.join(prefixes, " PREFIX ")) + "\n" + query;
		}
		else {
			query = (prefixes.length == 0 ? "" : "PREFIX " +
						StringUtils.join(prefixes, " PREFIX ")) + "\n" + query;
			if (DebugUtils.canDebug(SparqlQuery.class, DebugFlag.QUERIES)) 
				DebugUtils.printDebugInfo("Executing query '" + 
					(DebugUtils.canDebug(SparqlQuery.class, DebugFlag.NO_QUERY_STRING) ? "..." : 
					query) + "' on '" +	svc + "'...", SparqlQuery.class);
		}
		return executeQuery(svc, query);
	}

	public static ResultSet execute(String svc, String query)
	{
		return execute(svc, query, DEF_PREFIXES);
	}
	
	public static String expandPrefix(String prefixedStr, String[] prefixes) 
	{
		return expandPrefixByCache(prefixedStr, genPrefixCache(prefixes));
	}
	
	public static String expandPrefix(String prefixedStr) 
	{
		return expandPrefixByCache(prefixedStr, defPrefixCache);
	}
	
	public static boolean isEqualResource(String[] prefixes, String resID1, String resID2) 
	{
		Map<String, String> prefixCache = genPrefixCache(prefixes);
		
		return expandPrefixByCache(resID1, prefixCache).trim().equalsIgnoreCase(
				   expandPrefixByCache(resID2, prefixCache).trim());
	}
	
	public static boolean isEqualResource(String resID1, String resID2) 
	{
		return expandPrefixByCache(resID1, defPrefixCache).trim().equalsIgnoreCase(
				   expandPrefixByCache(resID2, defPrefixCache).trim());
	}
	
	public static boolean isAnyEqualResource(String[] prefixes, String resID1, String ... resIDs)
	{
		for (String resID2: resIDs) {
			if (isEqualResource(prefixes, resID1, resID2))
				return true;
		}
		return false;
	}
	
	public static boolean isAnyEqualResource(String resID1, String ... resIDs) 
	{
		for (String resID2: resIDs) {
			if (isEqualResource(resID1, resID2))
				return true;
		}
		return false;
	}
}

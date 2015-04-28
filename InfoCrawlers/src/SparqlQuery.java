/**
 * @author Bernhard Weber
 */
import java.util.List;

import org.apache.commons.lang3.StringUtils;

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
	
	public static final int LIMIT_NONE = 0;
	
	public static boolean DEBUG = true;
	public static boolean DEBUG_QUERIES = true;
	public static boolean DEBUG_RESULTS = true;
	public static boolean DEBUG_NO_QUERY_STR = true;
	public static boolean DEBUG_NO_PREFIXES = true;
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
	public static int DEF_LIMIT = 100;


	private static void debug_print(final String info)
	{
		if (DEBUG) 
			DebugUtils.debug_printf("[SparqlQuery (Thread %s)]: %s\n", 
				Thread.currentThread().getId(),	info);
	}
	
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
			debug_print("Results of query '" + (DEBUG_NO_QUERY_STR ? "..." : query) + "':\n" + 
				tableDebugger);
		}
		else 
			debug_print("Query '" + (DEBUG_NO_QUERY_STR ? "..." : query) + 
				"' returned no results!");
	}

	public static ResultSet execute(String svc, String query, String[] prefixes)
	{
		//Add prefixes to query
		if (DEF_LIMIT != LIMIT_NONE)
			query += " LIMIT " + DEF_LIMIT;
		if (DEBUG_NO_PREFIXES) {
			if (DEBUG_QUERIES) 
				debug_print("Executing query '" + (DEBUG_NO_QUERY_STR ? "..." : query) + "' on '" + 
					svc + "'...");
			query = (DEF_PREFIXES.length == 0 ? "" : "PREFIX " + 
					StringUtils.join(DEF_PREFIXES, " PREFIX ")) +
					(prefixes.length == 0 ? "" : "PREFIX " +
					StringUtils.join(prefixes, " PREFIX ")) + "\n" + query;
		}
		else {
			query = (DEF_PREFIXES.length == 0 ? "" : "PREFIX " + 
						StringUtils.join(DEF_PREFIXES, " PREFIX ")) +
						(prefixes.length == 0 ? "" : "PREFIX " +
						StringUtils.join(prefixes, " PREFIX ")) + "\n" + query;
			if (DEBUG_QUERIES) 
				debug_print("Executing query '" + (DEBUG_NO_QUERY_STR ? "..." : query) + "' on '" + 
					svc + "'...");
		}
		
		//Execute query
		QueryExecution queryExec = QueryExecutionFactory.sparqlService(svc, query);
		
		try {
			ResultSet resSet = queryExec.execSelect();
        
	  		if (DEBUG_RESULTS) {
	  			ResultSetRewindable rewindResSet = ResultSetFactory.makeRewindable(resSet);
	  			
	  			debug_queryResult(query, rewindResSet);
	  			rewindResSet.reset();
	  			return rewindResSet;
	  		}
	  		return resSet;
		}
		finally {
			queryExec.close();
		}
	}

	public static ResultSet execute(String svc, String query)
	{
		return execute(svc, query, new String[]{});
	}
}

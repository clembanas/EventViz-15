/**
 * @author Bernhard Weber
 */
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.rdf.model.RDFNode;


public class CityInfoCrawler extends SparqlBasedCrawler {
	
	private static final QueryExecutor[] QUERIES = new QueryExecutor[] {
		new QueryExecutor(
				"SELECT DISTINCT ?city ?geolat ?geolong ?country ?country2 ?region ?subDev " +
				"?partOf ?prefect WHERE {\n" +
				"   {\n" +
				"      ?city rdfs:label ?city_label; \n" +
				"          rdf:type ?type;\n" +
				"          geo:lat ?geolat; \n" +
				"          geo:long ?geolong. \n" +
				"      ?city_label <bif:contains> '\"%1col_asStr\"'.\n" +
				"   }\n" +
				"   UNION \n" +
				"   {\n" +
				"       ?city dbpedia2:commonName ?city_label; \n" +
				"          rdf:type ?type;\n" +
				"          geo:lat ?geolat; \n" +
				"          geo:long ?geolong. \n" +
				"       ?city_label <bif:contains> '\"%1col_asStr\"'.\n" +
				"   }\n" +
				"   OPTIONAL { ?city dbpedia-owl:country ?country. }\n" +
				"   OPTIONAL { ?city dbpedia2:country ?country2. }\n" +
				"   OPTIONAL { ?city dbpedia-owl:region ?region. }\n" +
				"   OPTIONAL { ?city dbpedia2:subdivisionName ?subDev. }\n" +
				"   OPTIONAL { ?city dbpedia-owl:isPartOf ?partOf. }       \n" +
				"   OPTIONAL { ?city dbpedia-owl:prefecture ?prefect. }\n" +
				"   FILTER regex(str(?type), \"City|Settlement\", \"i\")\n" +
				"   FILTER regex(lcase(str(?city_label)), \"^%1col_asLRegEx$|^%1col_asLRegEx, " +
				"%3col_asLRegEx$|^%1col_asLRegEx (%3col_asLRegEx)$\", \"i\")\n" +
				"}\n", 
				CityInfoQPP.getInstance()
		),
		new QueryExecutor(
				"SELECT DISTINCT ?city ?geolat ?geolong ?country ?country2 ?region ?subDev " +
				"?partOf ?prefect WHERE {\n" +
				"   {\n" +
				"      ?city rdfs:label ?city_label; \n" +
				"          rdf:type ?type;\n" +
				"          geo:lat ?geolat; \n" +
				"          geo:long ?geolong. \n" +
				"      ?city_label <bif:contains> '\"%1col_asStr\"'.\n" +
				"   }\n" +
				"   UNION \n" +
				"   {\n" +
				"       ?city dbpedia2:commonName ?city_label; \n" +
				"          rdf:type ?type;\n" +
				"          geo:lat ?geolat; \n" +
				"          geo:long ?geolong. \n" +
				"       ?city_label <bif:contains> '\"%1col_asStr\"'.\n" +
				"   }\n" +
				"   OPTIONAL { ?city dbpedia-owl:country ?country. }\n" +
				"   OPTIONAL { ?city dbpedia2:country ?country2. }\n" +
				"   OPTIONAL { ?city dbpedia-owl:region ?region. }\n" +
				"   OPTIONAL { ?city dbpedia2:subdivisionName ?subDev. }\n" +
				"   OPTIONAL { ?city dbpedia-owl:isPartOf ?partOf. }       \n" +
				"   OPTIONAL { ?city dbpedia-owl:prefecture ?prefect. }\n" +
				"   FILTER regex(str(?type), \"City|Settlement\", \"i\")\n" +
				"   FILTER regex(lcase(str(?city_label)), \"^%1col_asLRegEx.*\", \"i\")\n" +
				"}\n", 
				CityInfoQPP.getInstance()
		)
	};
	private static final String DEBUG_DS_FMT = "City '%1col_noEsc' ('%3col_noEsc') in " +
												    "'%2col_noEsc' (ID: %0col_noEsc)";
	private int PAGE_SIZE;
	private int WORKER_THD_CNT;
	private int DB_UPDATE_INTERVAL;
	
	
	public static class CityInfoQPP extends QueryPostProcBase {
		
		protected static final String RESSET_VAR_CITY = "city";
		protected static final String RESSET_VAR_LATITUDE = "geolat";
		protected static final String RESSET_VAR_LONGITUDE = "geolong";
		protected static final String[] RESSET_VARS_REGION_COUNTRY = new String[]{"country", 
			"region", "subDev", "partOf", "prefect"};
		protected static final String[] RESSET_TYPE_COUNTRY = new String[]{
			"dbpedia-owl:country", "dbpedia2:country"};
		protected static final String[] RESSET_TYPES_REGION = new String[]{
			"dbpedia-owl:region", "dbpedia-owl:island"};
		protected static final Map<String, String> RESSET_TYPES_OF_VARS = Utils.createStrMap(
			"country", "dbpedia-owl:country", "country2", "dbpedia2:country", 
			"region", "dbpedia-owl:region",	"subDev", "dbpedia2:subdivisionName", 
			"partOf", "dbpedia-owl:isPartOf", "prefect", "dbpedia-owl:prefecture");
		protected static final String[] RESPROP_COUNTRY_OF_REGION = new String[]{
			"dbpedia-owl:country", "dbpedia2:country", "dbpedia-owl:isPartOf", 
			"dbpedia2:subdivisionName"};
		
		
		public static class CityInfos {
			
			public String cityResID = null;
			public Double longitude = null;
			public Double latitude = null;
			public String regionName = null;
			public String regionResID = null;
			public String countryName = null;
			public String countryResID = null;
			
			public void reset()
			{
				regionName = null;
				regionResID = null;
				countryName = null;
				countryResID = null;
			}
			
			public boolean hasValidName(boolean forRegion, boolean forCountry)
			{
				return (!forRegion || regionName != null) && (!forCountry || countryName != null);
			}
			
			public boolean store(QueryContext queryContext)
			{
				try {
					queryContext.getDBConnector().updateCity(
						DBConnector.PrimaryKey.create(queryContext.getDataRow()[0]), latitude, 
						longitude, regionName == null ? queryContext.getDataRow()[2] : regionName, 
						countryName == null ? queryContext.getDataRow()[3] : countryName, 
						cityResID, regionResID, countryResID);
					updatedCityCnt.incrementAndGet();
					return true;
				}
				catch (Exception e) {
					ExceptionHandler.handle("Failed to store city information of city with ID '" + 
						queryContext.getDataRow()[0] + "'!", e, CityInfoCrawler.class, null,
						getClass());
					return false;
				}
			}
		}
		
		
		private static CityInfoQPP instance = new CityInfoQPP();
		private static AtomicInteger updatedCityCnt = new AtomicInteger(0);
		
		private boolean isValidRegion(QueryContext queryContext, 
			SparqlResourceCache.PropertyValue[] typePropVals)
		{
			for (SparqlResourceCache.PropertyValue typeProp: typePropVals) {
				if (SparqlQuery.isAnyEqualResource(typeProp.asResource(), RESSET_TYPES_REGION))
					return true;
			}
			return false;
		}
		
		private boolean isValidCountry(QueryContext queryContext, 
			SparqlResourceCache.PropertyValue[] typePropVals)
		{
			for (SparqlResourceCache.PropertyValue typeProp: typePropVals) {
				if (SparqlQuery.isAnyEqualResource(typeProp.asResource(), RESSET_TYPE_COUNTRY))
					return true;
			}
			return false;
		}
		
		private void processRegCtryAsLiteral(QueryContext queryContext, boolean regResIDRequired, 
			boolean ctryResIDRequired, String expRegName, String expCtryName, 
			String regCtryVarType, String regCtryVarVal, CityInfos cityInfos)
		{
			if (cityInfos.regionName == null && !regResIDRequired &&
				((expRegName != null && expRegName.equalsIgnoreCase(regCtryVarVal)) ||
				(expRegName == null && 
				SparqlQuery.isAnyEqualResource(regCtryVarType, RESSET_TYPES_REGION)))) 
				cityInfos.regionName = regCtryVarVal;
			else if (cityInfos.countryName == null && !ctryResIDRequired &&
				((expCtryName != null && expCtryName.equalsIgnoreCase(regCtryVarVal)) ||
				(expCtryName == null && 
				SparqlQuery.isAnyEqualResource(regCtryVarType, RESSET_TYPE_COUNTRY)))) 
				cityInfos.countryName = regCtryVarVal;
		}
		
		private void performRegCtryLookup(QueryContext queryContext, String expRegName, 
			String expCtryName, String resID, CityInfos cityInfos)
		{
			SparqlResourceCache.MultiPropValueList propVals;
			SparqlResourceCache.PropertyValue[] typePropVals;
			String resName;
			
			propVals = getResourceProperties(queryContext, resID, RESPROP_TYPE, RESPROP_NAME);
			if (propVals == null) 
				return;
			resName = propVals.getHeadAsLiteral(RESPROP_NAME);
			typePropVals = propVals.get(RESPROP_TYPE);
			if (resName == null || typePropVals == null)
				return;
			if (cityInfos.countryName == null && isValidCountry(queryContext, typePropVals)) {
				cityInfos.countryName = resName;
				cityInfos.countryResID = resID;
			}
			else if (cityInfos.regionName == null && isValidRegion(queryContext, typePropVals)) {
				cityInfos.regionName = resName;
				cityInfos.regionResID = resID;
			}
		}
		
		private boolean performCtryByRegLookup(QueryContext queryContext, boolean ctryResIDRequired, 
			String expCtryName, CityInfos cityInfos)
		{
			SparqlResourceCache.MultiPropValueList propVals;
			SparqlResourceCache.PropertyValue[] regCtryVals;
			
			propVals = getResourceProperties(queryContext, cityInfos.regionResID, 
						   RESPROP_COUNTRY_OF_REGION);
			if (propVals == null) 
				return false;
			for (String regCtryProp: RESPROP_COUNTRY_OF_REGION) {
				regCtryVals = propVals.get(regCtryProp);
				if (regCtryVals == null) 
					continue;
				for (SparqlResourceCache.PropertyValue regCtryVal: regCtryVals) {
					if (regCtryVal.isLiteral()) 
						processRegCtryAsLiteral(queryContext, true, ctryResIDRequired, null, 
							expCtryName, regCtryProp, regCtryVal.asLiteral(), cityInfos); 
					else if (regCtryVal.isResource()) 
						performRegCtryLookup(queryContext, null, expCtryName, 
							regCtryVal.asResource(), cityInfos); 
					if (cityInfos.hasValidName(true, true) && (expCtryName == null || 
						expCtryName.equalsIgnoreCase(cityInfos.countryName)))
						return true;
				}
			}
			return false;
		}
		
		private CityInfos findFirstRow(QueryContext queryContext, boolean regResIDRequired, 
			boolean ctryResIDRequired, String expRegName, String expCtryName, boolean regIsOpt, 
			boolean ctryIsOpt)
		{
			CityInfos cityInfos = new CityInfos();
			ResultSetRewindable resSet = ResultSetFactory.makeRewindable(
			  							     queryContext.getResultSet());
			QuerySolution querySol;
			RDFNode currNode;
			
			do {
				querySol = resSet.next();
				for (String regCtryVar: RESSET_VARS_REGION_COUNTRY) {
					currNode = querySol.get(regCtryVar);
					if (currNode == null)
						continue;
					if (currNode.isLiteral()) 
						processRegCtryAsLiteral(queryContext, regResIDRequired,
							ctryResIDRequired, expRegName, expCtryName, 
							RESSET_TYPES_OF_VARS.get(regCtryVar),
							currNode.asLiteral().getString(), cityInfos); 
					else if (currNode.isResource()) 
						performRegCtryLookup(queryContext, expRegName, expCtryName, 
							currNode.asResource().getURI(), cityInfos); 
					if (cityInfos.hasValidName(true, true))
						break;
				}
				if (cityInfos.hasValidName(!regIsOpt, !ctryIsOpt) && 
					(expRegName == null || 
					expRegName.equalsIgnoreCase(cityInfos.regionName)) &&
					(expCtryName == null || 
				    expCtryName.equalsIgnoreCase(cityInfos.countryName))) {
					cityInfos.cityResID = querySol.get(RESSET_VAR_CITY).asResource().
											  getURI();
					cityInfos.longitude = querySol.get(RESSET_VAR_LONGITUDE).asLiteral().
											  getDouble();
					cityInfos.latitude = querySol.get(RESSET_VAR_LATITUDE).asLiteral().
											 getDouble();
					return cityInfos;
				}
				cityInfos.reset();
			}
			while (resSet.hasNext());
			resSet.reset();
			return null;
		}
		
		private CityInfos loadCityInfosByRegionCountry(QueryContext queryContext, 
				String expRegName, String expCtryName)
		{
			CityInfos cityInfos;
			
			//Use first entry which contains a valid country and region resource
			if ((cityInfos = findFirstRow(queryContext, true, true, expRegName, expCtryName, false, 
				false)) != null)
				return cityInfos;
			//Use first entry which has a valid region resource
			cityInfos = findFirstRow(queryContext, true, false, expRegName, expCtryName, false, 
						   true);
			if (cityInfos != null) {
				//Lookup country via region -> Accept country by resource or, if none found, by 
				//literal (i.e. name)
				if (performCtryByRegLookup(queryContext, true, expCtryName, cityInfos) || 
					performCtryByRegLookup(queryContext, false, expCtryName, cityInfos))
					return cityInfos;
			}			
			//Use first entry which has a valid country resource and a region literal
			if ((cityInfos = findFirstRow(queryContext, false, true, expRegName, expCtryName, false, 
				false)) != null)
				return cityInfos;
			//Use first entry which has a valid region resource
			if ((cityInfos = findFirstRow(queryContext, true, false, expRegName, expCtryName, false, 
				true)) != null)
				return cityInfos;
			//Use first entry which has a valid country resource
			if ((cityInfos = findFirstRow(queryContext, false, true, expRegName, expCtryName, true, 
				false)) != null)
				return cityInfos;
			//Use first entry having a literal for country and region
			if ((cityInfos = findFirstRow(queryContext, false, false, expRegName, expCtryName, 
				false, false)) != null)
				return cityInfos;
			//Perform lookups using reduced expect-data
			if (expRegName != null && expCtryName != null) {
				if ((cityInfos = loadCityInfosByRegion(queryContext, expRegName)) != null || 
					(cityInfos = loadCityInfosByCountry(queryContext, expCtryName)) != null)
					return cityInfos;
			}
			//Use first row having the reight region and country name
			if ((cityInfos = findFirstRow(queryContext, false, false, expRegName, expCtryName, true, 
				true)) != null)
				return cityInfos;
			//Perform lookups using no expect-data
			if ((expRegName != null || expCtryName != null) && 
				(cityInfos = loadCityInfos(queryContext)) != null)
				return cityInfos;
			return null;
		}
		
		private CityInfos loadCityInfos(QueryContext queryContext)
		{
			return loadCityInfosByRegionCountry(queryContext, null, null);
		}
		
		private CityInfos loadCityInfosByRegion(QueryContext queryContext, String expRegName)
		{
			return loadCityInfosByRegionCountry(queryContext, expRegName, null);
		}
		
		private CityInfos loadCityInfosByCountry(QueryContext queryContext, String expCtryName)
		{
			return loadCityInfosByRegionCountry(queryContext, null, expCtryName);
		}
		
		protected boolean processQueryResult(QueryContext queryContext, ResultSet resSet) 
			throws Exception
		{
			CityInfos cityInfos;
			
			//No country and region infos in DB
			if (queryContext.getDataRow()[2].isEmpty() && queryContext.getDataRow()[3].isEmpty()) 
				cityInfos = loadCityInfos(queryContext);
			//No country, but region infos in DB			
			else if (queryContext.getDataRow()[3].isEmpty()) 
				cityInfos = loadCityInfosByRegion(queryContext, queryContext.getDataRow()[2]);
			//No region, but country infos in DB			
			else if (queryContext.getDataRow()[2].isEmpty()) 
				cityInfos = loadCityInfosByCountry(queryContext, queryContext.getDataRow()[3]);
			//Country and region infos in DB
			else
				cityInfos = loadCityInfosByRegionCountry(queryContext, queryContext.getDataRow()[2],
								queryContext.getDataRow()[3]);
			return cityInfos != null && cityInfos.store(queryContext);
		}
		
		public static CityInfoQPP getInstance()
		{
			return instance;
		}
		
		public static int getUpdatedCityCount()
		{
			return updatedCityCnt.get();
		}
	}


	protected int getWorkerThdCount() 
	{
		return WORKER_THD_CNT;
	}
	
	protected int getPageSize() 
	{
		return PAGE_SIZE;
	}
	
	protected int getDatasetCount() throws Exception
	{
		return dbConnector.getIncompleteCitiesCount(DB_UPDATE_INTERVAL);
	}
	
	protected java.sql.ResultSet getDataset(int pageIdx, int pageSize) throws Exception
	{
		return dbConnector.getIncompleteCities(DB_UPDATE_INTERVAL, pageIdx, pageSize);
	}
	
	public CityInfoCrawler() throws Exception
	{
		super(CrawlerConfig.getSparqlBasedCrawlerDBPediaEndpoints(), QUERIES, DEBUG_DS_FMT);
		PAGE_SIZE = CrawlerConfig.getCityInfoCrawlerPageSize();
		WORKER_THD_CNT = CrawlerConfig.getCityInfoCrawlerWorkerThdCount();
		DB_UPDATE_INTERVAL = CrawlerConfig.getCityInfoCrawlerUpdateInterval();
	}
	
	public int[] getStatistics()
	{
		int[] result = new int[4];
		
		result[0] = CityInfoQPP.getUpdatedCityCount();
		if (isMasterNode) {
			result[1] = (int)resCache.getLookupCount();
			result[2] = (int)resCache.getLookupMissCount();
			result[3] = (int)resCache.getMaxLoadInBytes();
		}
		return result;
	}
	
	public String getSummary(int[] crawlerStats) 
	{
		return "Updated cities: " + crawlerStats[0] +	"; Cache misses: " + crawlerStats[2] +
				   "; Cache lookups: " + crawlerStats[1] + "; Max cache load: " + crawlerStats[3] + 
				   " Bytes";
	}
	
	public void allInstancesFinished(boolean exceptionThrown, int[] crawlerStats)
	{
		super.allInstancesFinished(exceptionThrown, crawlerStats);
		if (isMasterNode) {
			if (!exceptionThrown)
				dbConnector.updateCityCrawlerTS();
			DebugUtils.printDebugInfo("\n   Summary:\n      Updated cities: " + crawlerStats[0] +
				"\n      Cache misses: " + crawlerStats[2] + "\n      Cache lookups: " + 
				crawlerStats[1] +	"\n      Max cache load: " + crawlerStats[3] + " Bytes\n",	
				CityInfoCrawler.class);
		}
	}
}

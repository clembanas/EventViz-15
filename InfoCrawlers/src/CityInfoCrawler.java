/**
 * @author Bernhard Weber
 */
import java.util.Map;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.rdf.model.RDFNode;


public class CityInfoCrawler extends SparqlCrawlerBase {
	
	public static final int WORKER_THD_CNT = 10;
	public static final String SVC = "http://dbpedia.org/sparql";
	public static final QueryExecutor[] QUERIES = new QueryExecutor[] {
		new QueryExecutor(
				"SELECT DISTINCT ?city ?geolat ?geolong ?country ?region ?subDev ?partOf " +
				"?prefect WHERE {\n" +
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
				"   OPTIONAL { ?city dbpedia-owl:region ?region. }\n" +
				"   OPTIONAL { ?city dbpedia2:subdivisionName ?subDev. }\n" +
				"   OPTIONAL { ?city dbpedia-owl:isPartOf ?partOf. }       \n" +
				"   OPTIONAL { ?city dbpedia-owl:prefecture ?prefect. }\n" +
				"   FILTER regex(str(?type), \"City|Settlement\", \"i\")\n" +
				"   FILTER regex(str(?city_label), \"^%1col_asRegEx$|^%1col_asRegEx, " +
				"%3col_asRegEx$|^%1col_asRegEx (%3col_asRegEx)$\", \"i\")\n" +
				"}\n", 
				new CityInfoStrictNameQPP()
		),
		new QueryExecutor(
				"SELECT DISTINCT ?city ?geolat ?geolong ?country ?region ?subDev ?partOf " +
				"?prefect WHERE {\n" +
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
				"   OPTIONAL { ?city dbpedia-owl:region ?region. }\n" +
				"   OPTIONAL { ?city dbpedia2:subdivisionName ?subDev. }\n" +
				"   OPTIONAL { ?city dbpedia-owl:isPartOf ?partOf. }       \n" +
				"   OPTIONAL { ?city dbpedia-owl:prefecture ?prefect. }\n" +
				"   FILTER regex(str(?type), \"City|Settlement\", \"i\")\n" +
				"   FILTER regex(str(?city_label), \"^%1col_asRegEx.*\", \"i\")\n" +
				"}\n", 
				new CityInfoRelaxedNameQPP()
		)
	};
	public static final String DEBUG_DS_FMT = "City '%1col_noEsc' ('%3col_noEsc') in " +
												   "'%2col_noEsc' (ID: %0col_noEsc)";
	
	
	public static class CityInfoStrictNameQPP extends QueryPostProcBase {
		
		protected static final String RESSET_VAR_CITY = "city";
		protected static final String RESSET_VAR_LATITUDE = "geolat";
		protected static final String RESSET_VAR_LONGITUDE = "geolong";
		protected static final String[] RESSET_VARS_REGION_COUNTRY = new String[]{"country", 
			"region", "subDev", "partOf", "prefect"};
		protected static final String RESSET_TYPE_COUNTRY = "dbpedia-owl:Country";
		protected static final String RESSET_TYPE_REGION = "dbpedia-owl:Region";
		protected static final Map<String, String> RESSET_TYPES_OF_VARS = Utils.createStrMap(
			"country", RESSET_TYPE_COUNTRY, "region", "dbpedia-owl:region", 
			"subDev", "dbpedia2:subdivisionName", "partOf", "dbpedia-owl:isPartOf",
			"prefect", "dbpedia-owl:prefecture");
		protected static final String[] RESPROP_COUNTRY_OF_REGION = new String[]{
			RESSET_TYPE_COUNTRY, "dbpedia-owl:isPartOf", "dbpedia2:subdivisionName"};
		
		
		public static class CityData {
			
			public String cityResID = null;
			public Float longitude = null;
			public Float latitude = null;
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
		}
		
		public static class CityInfos {
			
			private QueryContext queryContext;
			public CityData cityData = null;
			
			
			public CityInfos(QueryContext queryContext) 
			{
				this.queryContext = queryContext;
			}
			
			public CityInfos(QueryContext queryContext, CityData cityData) 
			{
				this.queryContext = queryContext;
				this.cityData = cityData;
			}

			public boolean store()
			{
				return true;
			}
		}
		
		
		private boolean isValidRegion(QueryContext queryContext, 
			SparqlResourceCache.PropertyValue[] typePropVals)
		{
			for (SparqlResourceCache.PropertyValue typeProp: typePropVals) {
				if (RESSET_TYPE_REGION.equalsIgnoreCase(typeProp.asResource()))
					return true;
			}
			return false;
		}
		
		private boolean isValidCountry(QueryContext queryContext, 
			SparqlResourceCache.PropertyValue[] typePropVals)
		{
			for (SparqlResourceCache.PropertyValue typeProp: typePropVals) {
				if (RESSET_TYPE_COUNTRY.equalsIgnoreCase(typeProp.asResource()))
					return true;
			}
			return false;
		}
		
		private boolean processRegCtryAsLiteral(QueryContext queryContext, boolean regResIDRequired, 
			boolean ctryResIDRequired, String expRegName, String expCtryName, boolean regIsOpt, 
			boolean ctryIsOpt, String regCtryVarType, String regCtryVarVal, CityData cityData)
		{
			if (cityData.regionName == null && !regResIDRequired &&
				((expRegName != null && expRegName.equalsIgnoreCase(regCtryVarVal)) ||
				(expRegName == null && regCtryVarType.equalsIgnoreCase(RESSET_TYPE_REGION)))) 
				cityData.regionName = regCtryVarVal;
			else if (cityData.countryName == null && !ctryResIDRequired &&
				((expCtryName != null && expCtryName.equalsIgnoreCase(regCtryVarVal)) ||
				(expCtryName == null && regCtryVarType.equalsIgnoreCase(RESSET_TYPE_COUNTRY)))) 
				cityData.countryName = regCtryVarVal;
			return cityData.hasValidName(!regIsOpt, !ctryIsOpt);
		}
		
		private boolean performRegCtryLookup(QueryContext queryContext, String expRegName, 
			String expCtryName, boolean regIsOpt, boolean ctryIsOpt, String resID, 
			CityData cityData)
		{
			SparqlResourceCache.MultiPropValueList propVals;
			SparqlResourceCache.PropertyValue[] typePropVals;
			String resName;
			
			propVals = getResourceProperties(queryContext, resID, RESPROP_TYPE, 
						   RESPROP_NAME);
			if (propVals == null) 
				return false;
			resName = propVals.getHeadAsLiteral(RESPROP_NAME);
			typePropVals = propVals.get(RESPROP_TYPE);
			if (resName == null || typePropVals == null)
				return false;
			if (cityData.countryName == null && isValidCountry(queryContext, typePropVals)) {
				cityData.countryName = resName;
				cityData.countryResID = resID;
			}
			else if (cityData.regionName == null && isValidRegion(queryContext, typePropVals)) {
				cityData.regionName = resName;
				cityData.regionResID = resID;
			}
			return cityData.hasValidName(!regIsOpt, !ctryIsOpt) && 
					   (expRegName == null || expRegName.equalsIgnoreCase(cityData.regionName)) && 
					   (expRegName == null || expCtryName.equalsIgnoreCase(cityData.countryName));
		}
		
		private boolean performCtryByRegLookup(QueryContext queryContext, boolean ctryResIDRequired, 
			String expCtryName, CityData cityData)
		{
			SparqlResourceCache.MultiPropValueList propVals;
			SparqlResourceCache.PropertyValue[] regCtryVals;
			
			propVals = getResourceProperties(queryContext, cityData.regionResID, 
						   RESPROP_COUNTRY_OF_REGION);
			if (propVals == null) 
				return false;
			for (String regCtryProp: RESPROP_COUNTRY_OF_REGION) {
				regCtryVals = propVals.get(regCtryProp);
				if (regCtryVals == null) 
					continue;
				for (SparqlResourceCache.PropertyValue regCtryVal: regCtryVals) {
					if (regCtryVal.isLiteral()) {
						if (!processRegCtryAsLiteral(queryContext, true, ctryResIDRequired, 
							null, expCtryName, false, false, regCtryProp, regCtryVal.asLiteral(), 
							cityData)) 
							continue;
					}
					else if (regCtryVal.isResource()) {
						if (!performRegCtryLookup(queryContext, null, expCtryName, false, false, 
							regCtryVal.asResource(), cityData)) 
							continue;
					}
					else 
						continue;
					return true;
				}
			}
			return false;
		}
		
		private CityData findFirstRow(QueryContext queryContext, boolean regResIDRequired, 
			boolean ctryResIDRequired, String expRegName, String expCtryName, boolean regIsOpt, 
			boolean ctryIsOpt)
		{
			CityData cityData = new CityData();
			ResultSetRewindable resSet = ResultSetFactory.makeRewindable(
			  							     queryContext.getResultSet());
			QuerySolution querySol;
			RDFNode currNode;
			String resID;
			
			do {
				cityData.reset();
				resID = "?";
				try {
					querySol = resSet.next();
					for (String regCtryVar: RESSET_VARS_REGION_COUNTRY) {
						currNode = querySol.get(regCtryVar);
						if (currNode != null && currNode.isLiteral()) {
							if (!processRegCtryAsLiteral(queryContext, regResIDRequired, 
								ctryResIDRequired, expRegName, expCtryName, regIsOpt, ctryIsOpt, 
								RESSET_TYPES_OF_VARS.get(regCtryVar),
								currNode.asLiteral().getString(), cityData)) 
								continue;
						}
						else if (currNode != null && currNode.isResource()) {
							resID = currNode.asResource().getURI();
							if (!performRegCtryLookup(queryContext, expRegName, expCtryName, 
								regIsOpt, ctryIsOpt, resID, cityData)) 
								continue;
						}
						else
							continue;
						resSet.reset();
						cityData.cityResID = querySol.get(RESSET_VAR_CITY).asResource().
												 getURI();
						cityData.longitude = querySol.get(RESSET_VAR_LONGITUDE).asLiteral().
												 getFloat();
						cityData.latitude = querySol.get(RESSET_VAR_LATITUDE).asLiteral().
												getFloat();
						return cityData;
					}
				}
				catch (Exception e) {
					queryContext.handleException(e, "Failed to retrieve country and/ or region " +
						"information of resource '" + resID + "'!");
				}
			}
			while (resSet.hasNext());
			resSet.reset();
			return null;
		}
		
		protected boolean processQueryResult(QueryContext queryContext, ResultSet resSet) 
			throws Exception
		{
			CityInfos cityInfos;
			
			//No country and region infos in DB
			if (queryContext.getDataRow()[3].isEmpty() && queryContext.getDataRow()[4].isEmpty()) 
				cityInfos = loadCityInfos(queryContext);
			//No country, but region infos in DB			
			else if (queryContext.getDataRow()[3].isEmpty()) 
				cityInfos = loadCityInfosByRegion(queryContext, queryContext.getDataRow()[4]);
			//No region, but country infos in DB			
			else if (queryContext.getDataRow()[4].isEmpty()) 
				cityInfos = loadCityInfosByCountry(queryContext, queryContext.getDataRow()[3]);
			//Country and region infos in DB
			else
				cityInfos = loadCityInfos(queryContext);//, queryContext.getDataRow()[3], 
								//queryContext.getDataRow()[4]); FIXME
			return cityInfos != null && cityInfos.store();
		}
		
		public CityInfos loadCityInfosByRegionCountry(QueryContext queryContext, 
				String expRegName, String expCtryName)
		{
			CityData cityData;
			CityInfos cityInfos;
			
			//Use first entry which contains a valid country and region resource
			if ((cityData = findFirstRow(queryContext, true, true, expRegName, expCtryName, false, 
				false)) != null)
				return new CityInfos(queryContext, cityData);
			//Use first entry which has a valid region resource
			cityData = findFirstRow(queryContext, true, false, expRegName, expCtryName, false, 
						   true);
			if (cityData != null) {
				//Lookup country via region -> Accept country by resource or, if none found, by 
				//literal (i.e. name)
				if (performCtryByRegLookup(queryContext, true, expCtryName, cityData) || 
					performCtryByRegLookup(queryContext, false, expCtryName, cityData))
					return new CityInfos(queryContext, cityData);
			}			
			//Use first entry which has a valid country resource and a region literal
			if ((cityData = findFirstRow(queryContext, false, true, expRegName, expCtryName, false, 
				false)) != null)
				return new CityInfos(queryContext, cityData);
			//Use first entry which has a valid region resource
			if ((cityData = findFirstRow(queryContext, true, false, expRegName, expCtryName, false, 
				true)) != null)
				return new CityInfos(queryContext, cityData);
			//Use first entry which has a valid country resource
			if ((cityData = findFirstRow(queryContext, false, true, expRegName, expCtryName, true, 
				false)) != null)
				return new CityInfos(queryContext, cityData);
			//Use first entry having a literal for country and region
			if ((cityData = findFirstRow(queryContext, false, false, expRegName, expCtryName, false, 
				false)) != null)
				return new CityInfos(queryContext, cityData);
			//Perform lookups using reduced expect-data
			if (expRegName != null && expCtryName != null) {
				if ((cityInfos = loadCityInfosByRegion(queryContext, expRegName)) != null || 
					(cityInfos = loadCityInfosByCountry(queryContext, expCtryName)) != null)
					return cityInfos;
			}
			//Use first row
			if ((cityData = findFirstRow(queryContext, false, false, expRegName, expCtryName, true, 
				true)) != null)
				return new CityInfos(queryContext, cityData);
			//Perform lookups using no expect-data
			if ((expRegName != null || expCtryName != null) && 
				(cityInfos = loadCityInfos(queryContext)) != null)
				return cityInfos;
			return null;
		}
		
		public CityInfos loadCityInfos(QueryContext queryContext)
		{
			return loadCityInfosByRegionCountry(queryContext, null, null);
		}
		
		public CityInfos loadCityInfosByRegion(QueryContext queryContext, String expRegName)
		{
			return loadCityInfosByRegionCountry(queryContext, expRegName, null);
		}
		
		public CityInfos loadCityInfosByCountry(QueryContext queryContext, String expCtryName)
		{
			return loadCityInfosByRegionCountry(queryContext, null, expCtryName);
		}
	}
	
	public static class CityInfoRelaxedNameQPP extends QueryPostProcBase {
		
		protected boolean processQueryResult(QueryContext queryContext, ResultSet resSet) 
			throws Exception
		{
			return false;
		}
	}
	
	
	protected Utils.Pair<java.sql.ResultSet, Object> getNextDataset(Object customData) 
		throws Exception 
	{
		if (customData != null)
			return null;
		return new Utils.Pair<java.sql.ResultSet, Object>(DBConnection.getIncompleteCities(), 
					   new Object());
	}

	protected int getWorkerThdCount() 
	{
		return WORKER_THD_CNT;
	}

	public CityInfoCrawler() 
	{
		super(SVC, QUERIES, DEBUG_DS_FMT);
	}
}

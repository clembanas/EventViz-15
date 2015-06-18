/**
 * @author Bernhard Weber
 */
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;


/**
 * Retrieves information about bands and their members via DBPedia.
 */
public class BandInfoCrawler extends SparqlBasedCrawler {
	
	private static final QueryExecutor[] QUERIES = new QueryExecutor[] {
		//Find city by name, region and (optional) by country
		new QueryExecutor(
				"SELECT DISTINCT ?band ?members_curr ?members_past ?members_former ?artist_name " +
				"   ?alt_artist_name WHERE {\n" +
				"   {\n" +
				"       ?band rdfs:label ?band_label;\n" +
				"           rdf:type ?type.\n" +
				"	    ?band_label <bif:contains> '\"%1col_asStr\"'.\n" +
				"   }\n" +
				"   UNION \n" +
				"   {\n" +
				"       ?band dbpedia2:commonName ?band_label;\n" +
				"           rdf:type ?type.\n" +
				"	    ?band_label <bif:contains> '\"%1col_asStr\"'.\n" +
				"   }\n" +
				"   OPTIONAL { ?band dbpedia2:currentMembers ?members_curr. }\n" +
				"   OPTIONAL { ?band dbpedia-owl:formerBandMember ?members_past. }\n" +
				"   OPTIONAL { ?band dbpedia2:pastMembers ?members_former. }\n" +
				"   OPTIONAL { \n" +
				"       ?band rdfs:label ?artist_name;\n" +
				"           rdf:type ?artist_type.\n" +
				"       FILTER regex(str(?artist_type), \"org/Person|ontology/Person\" , \"i\")\n" +
				"       FILTER(LANG(?artist_name) = \"\" || LANGMATCHES(LANG(?artist_name), " +
				"           \"en\"))\n" +
				"   }\n" +
				"   OPTIONAL { \n" +
				"       {\n" +
				"           ?band dbpedia2:alternativeNames ?alt_artist_name;\n" +
				"               rdf:type ?artist_type.\n" +
				"       }\n" +
				"       UNION \n" +
				"       {\n" +
				"           ?band dbpedia-owl:alias ?alt_artist_name;\n" +
				"               rdf:type ?artist_type.\n" +
				"       }\n" +				
				"       FILTER regex(str(?artist_type), \"org/Person|ontology/Person\" , \"i\")\n" +
				"   }\n" +
				"   FILTER regex(str(?type), \"Music|Artist|Band|Person\", \"i\")\n" +
				"	FILTER regex(lcase(str(?band_label)), \"^%1col_asLRegEx$\")\n" +
				"}",
				new BandInfoQPP() 
		)
	};
	private static final String DEBUG_DS_FMT = "Band '%1col_noEsc' (ID: %0col_noEsc)";
	private int PAGE_SIZE;
	private int WORKER_THD_CNT;
	private int DB_UPDATE_INTERVAL;
	
	
	/**
	 * Query postprocessor of band specific queries
	 */
	public static class BandInfoQPP extends QueryPostProcBase {
		
		public static final String RESSET_VAR_BAND = "band";
		public static final String RESSET_VAR_CURR_MEMBERS = "members_curr";
		public static final String RESSET_VAR_PAST_MEMBERS = "members_past";
		public static final String RESSET_VAR_FORMER_MEMBERS = "members_former";
		public static final String RESSET_VAR_ARTIST = "artist_name";
		public static final String RESSET_VAR_ALT_ARTIST = "alt_artist_name";
		public static final String RESPROP_ALT_ARTIST = "dbpedia2:alternativeNames";
		
		
		/**
		 * Specifies the possible values for the band member type attribute  
		 */
		public static enum BandMemberType {
			CURR_MEMBER('C'), PAST_MEMBER('P'), BAND_IS_ARTIST('A'); 
			
			private final char c;
			private BandMemberType(char c)	{ this.c = c; }
			public String toString() { return String.valueOf(c); }
			public char toChar() { return c; }
		};
		
		/**
		 * Information about a single artist
		 */
		public static class Artist {
			
			public String name, altName;
			public BandMemberType memberType;
			public String resID;
			
			public Artist(String name, String altName, BandMemberType memberType, String resID)
			{
				this.name = name;
				this.altName = altName;
				this.memberType = memberType;
				this.resID = resID;
			}
		}
		
		/**
		 * Processes and stores band information
		 */
		public class BandInfos {
		
			public QueryContext queryContext;
			public String bandResID = null;
			public Map<String, Artist> artists = new TreeMap<String, Artist>(
														 String.CASE_INSENSITIVE_ORDER);
			
			private void addArtist(String name, String altName, BandMemberType memberType, 
				String resID)
			{
				Artist artist = artists.get(name);
						
				if (artist != null && (artist.memberType == BandMemberType.BAND_IS_ARTIST ||
					memberType == BandMemberType.BAND_IS_ARTIST)) {
					artist.memberType = BandMemberType.BAND_IS_ARTIST;
					if (artist.resID == null)
						artist.resID = resID;
					if (altName != null)
						artist.altName = altName;
				}
				else 
					artists.put(name, new Artist(name, altName, memberType, resID));
			}
			
			private void addArtist(String name, String altName, BandMemberType memberType)
			{
				addArtist(name, altName, memberType, null);
			}
			
			private void addArtistByResID(String resID, String altName, BandMemberType memberType)
			{
				String name = queryContext.getResourceCache().lookupName(resID);
				
				if (name == null) {
					SparqlResourceCache.MultiPropValueList props = getResourceProperties(
																	   queryContext, 
																	   resID, RESPROP_NAME, 
																	   RESPROP_ALT_ARTIST);
					
					if (altName == null)
						altName = props == null ? null : getResourceName(queryContext, 
									  props.getHead(RESPROP_ALT_ARTIST));
					name = props.getHeadAsLiteral(RESPROP_NAME);
					if (name != null)
						addArtist(name, altName, memberType, resID);
				}
				else 
					addArtist(name, altName, memberType, resID);
			}
			
			public BandInfos(QueryContext queryContext) 
			{ 
				this.queryContext = queryContext; 
			}
			
			public void processBandMember(RDFNode nameNode, RDFNode altNameNode, 
				BandMemberType memberType)
			{
				String altName = null;
				
				if (altNameNode != null) 
					altName = getResourceName(queryContext, altNameNode);
				if (nameNode != null) {
					if (nameNode.isResource())
						addArtistByResID(nameNode.asResource().getURI(), altName, memberType);
					else if (nameNode.isLiteral()) {
						if (checkLangID(nameNode.asLiteral()))
							addArtist(nameNode.asLiteral().getString(), altName, memberType);
					}
					else
						DebugUtils.printDebugInfo("Warning: Member-name-node is of unexpected " +
							"type!", BandInfoCrawler.class, null, getClass());
				}
			}
			
			public void processBandMember(RDFNode nameNode, BandMemberType memberType)
			{
				processBandMember(nameNode, null, memberType);
			}
			
			public boolean store()
			{
				String bandID = queryContext.getDataRow()[0];
				DBConnector dbConn = queryContext.getDBConnector();
				
				try {
					//Store band artists
					for (Artist artist: artists.values()) {
						dbConn.insertBandArtist(DBConnector.PrimaryKey.create(bandID), artist.name, 
							artist.altName,	artist.memberType.toChar(), 
							(artist.memberType == BandMemberType.BAND_IS_ARTIST && 
							artist.resID == null ? bandResID : artist.resID));
						addedArtistCnt.incrementAndGet();
					}
					//Update band's DBpedia resource identifier
					dbConn.updateBand(DBConnector.PrimaryKey.create(bandID), bandResID);
					updatedBandCnt.incrementAndGet();
				}
				catch (Exception e) {
					ExceptionHandler.handle("Failed to store band information of band with ID '" + 
						bandID + "'!", e, BandInfoCrawler.class, null, getClass());
					return false;
				}
				return true;
			}
		}
		
		
		private static AtomicInteger updatedBandCnt = new AtomicInteger(0);
		private static AtomicInteger addedArtistCnt = new AtomicInteger(0);

		protected boolean processQueryResult(QueryContext queryContext, ResultSet resSet) 
			throws Exception
		{
			BandInfos bandInfos = new BandInfos(queryContext);
			RDFNode currNode;
			
			do {
				QuerySolution querySol = resSet.next();	

				//Extract band's DBPedia resource identifier
				if (bandInfos.bandResID == null && 
					(currNode = querySol.get(RESSET_VAR_BAND)) != null && currNode.isResource())
					bandInfos.bandResID = currNode.asResource().getURI();
				//Process artist nodes
				bandInfos.processBandMember(querySol.get(RESSET_VAR_CURR_MEMBERS), 
					BandMemberType.CURR_MEMBER);
				bandInfos.processBandMember(querySol.get(RESSET_VAR_PAST_MEMBERS), 
					BandMemberType.PAST_MEMBER);
				bandInfos.processBandMember(querySol.get(RESSET_VAR_FORMER_MEMBERS), 
					BandMemberType.PAST_MEMBER);
				bandInfos.processBandMember(querySol.get(RESSET_VAR_ARTIST), 
					querySol.get(RESSET_VAR_ALT_ARTIST), BandMemberType.BAND_IS_ARTIST);
			}
			while (resSet.hasNext());
			return bandInfos.store();
		}

		public static int getUpdatedBandCount()
		{
			return updatedBandCnt.get();
		}

		public static int getAddedArtistCount() 
		{
			return addedArtistCnt.get();
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
		return dbConnector.getIncompleteBandsCount(DB_UPDATE_INTERVAL);
	}
	
	protected java.sql.ResultSet getDataset(int pageIdx, int pageSize) throws Exception 
	{
		return dbConnector.getIncompleteBands(DB_UPDATE_INTERVAL, pageIdx, pageSize);
	}

	public BandInfoCrawler() throws Exception
	{
		super(CrawlerConfig.getSparqlBasedCrawlerDBPediaEndpoints(), QUERIES, DEBUG_DS_FMT);
		PAGE_SIZE = CrawlerConfig.getBandInfoCrawlerPageSize();
		WORKER_THD_CNT = CrawlerConfig.getBandInfoCrawlerWorkerThdCount();
		DB_UPDATE_INTERVAL = CrawlerConfig.getBandInfoCrawlerUpdateInterval();
	}
	
	public int[] getStatistics()
	{
		int[] result = new int[5];
		
		result[0] = BandInfoQPP.getUpdatedBandCount();
		result[1] = BandInfoQPP.getAddedArtistCount();
		result[2] = (int)resCache.getLookupCount();
		result[3] = (int)resCache.getLookupMissCount();
		if (isMasterNode) 
			result[4] = (int)resCache.getMaxLoadInBytes();
		return result;
	}

	public String getSummary(int[] crawlerStats) 
	{
		return "Updated bands: " + crawlerStats[0] + "; Added artists: " + crawlerStats[1] + 
					"; Cache misses: " + crawlerStats[3] + "; Cache lookups: " + crawlerStats[2] + 
					"; Max cache load: " + crawlerStats[4] + " Bytes";	
	}
	
	public void allInstancesFinished(boolean exceptionThrown, String jobsPerHostsInfo, 
		int[] crawlerStats)
	{
		super.allInstancesFinished(exceptionThrown, jobsPerHostsInfo, crawlerStats);
		if (isMasterNode) {
			if (!exceptionThrown)
				dbConnector.updateBandCrawlerTS();
			DebugUtils.printDebugInfo("\n   Summary:\n      Updated bands: " + crawlerStats[0] +
				"\n      Added artists: " + crawlerStats[1] + "\n      Cache misses: " + 
				crawlerStats[3] + "\n      Cache lookups: " + crawlerStats[2] + 
				"\n      Max cache load: " + crawlerStats[4] + " Bytes\n",	BandInfoCrawler.class);
		}
	}
}

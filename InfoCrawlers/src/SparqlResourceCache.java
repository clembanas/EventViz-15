/**
 * @author Bernhard Weber
 */
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.hp.hpl.jena.rdf.model.RDFNode;


/**
 * Cache used to temporary store RDF node related information.
 * Each SPARQL crawler has its own cache (except multiple instances explicitly share on cache). 
 */
public class SparqlResourceCache {

	public static final int MAX_CACHE_SIZE = 5000000; 		//Maximum cache size in Bytes
	
	
	/**
	 * Result of cache lookups using multiple properties. 
	 */
	public static class MultiPropValueList {
		
		private Map<String, PropertyValue[]> properties;
		
		private MultiPropValueList(Map<String, PropertyValue[]> properties)
		{
			this.properties = properties;
		}
		
		PropertyValue[] get(String propName)
		{
			return properties.get(propName);
		}
		
		PropertyValue getHead(String propName)
		{
			PropertyValue[] propVals = properties.get(propName);
			
			return propVals == null || propVals.length == 0 ? null : propVals[0];
		}
		
		String getHeadValue(String propName)
		{
			PropertyValue[] propVals = properties.get(propName);
			
			return propVals == null || propVals.length == 0 ? null : propVals[0].value;
		}
		
		String getHeadAsLiteral(String propName)
		{
			PropertyValue[] propVals = properties.get(propName);
			
			return propVals == null || propVals.length == 0 ? null : propVals[0].asLiteral();
		}
		
		String getHeadAsResource(String propName)
		{
			PropertyValue[] propVals = properties.get(propName);
			
			return propVals == null || propVals.length == 0 ? null : propVals[0].asResource();
		}
	}

	/**
	 * Represents a resource property
	 */
	public static class PropertyValue {
		
		public enum ValueType {LITERAL, RESOURCE, UNKNOWN};
		
		private ValueType type;
		private String value;
		
		private PropertyValue(String name) 
		{
			type = ValueType.LITERAL;
			value = name;
		}
		
		private PropertyValue(RDFNode node)
		{
			if (node.isResource() || node.isURIResource()) {
				type = ValueType.RESOURCE;
				value = node.asResource().getURI();
			}
			else if (node.isLiteral()) {
				type = ValueType.LITERAL;
				value = node.asLiteral().getString();
			}
			else {
				type = ValueType.UNKNOWN;
				value = node.toString();
			}
		}
		
		public ValueType getValueType()
		{
			return type;
		}
		
		public boolean isResource()
		{
			return type == ValueType.RESOURCE;
		}
		
		public boolean isLiteral()
		{
			return type == ValueType.LITERAL;
		}
		
		public String getValue()
		{
			return value;
		}
		
		public String asLiteral()
		{
			return type == ValueType.LITERAL ? value : null;
		}
		
		public String asResource()
		{
			return type == ValueType.RESOURCE ? value : null;
		}
		
		public boolean equals(Object obj)
		{
			if (obj == null)
				return false;
			if (this == obj)
				return true;
			if (obj.getClass() == this.getClass())
				return value.equalsIgnoreCase(((PropertyValue)obj).value);
			if (obj.getClass() == String.class)
				return value.equalsIgnoreCase((String)obj);
			return false;
		}
		
		public int hashCode()
		{
			return value.hashCode();
		}
		
		public long getSizeInBytes()
		{
			return value.length() + 1;
		}
	}
	
	/**
	 * List of resource properties
	 */
	public static class PropertyList {
		
		private long sizeInBytes = 0;
		private Map<String, Set<PropertyValue>> properties = 
					new TreeMap<String, Set<PropertyValue>>(String.CASE_INSENSITIVE_ORDER);
		
		private void addToPropValsSet(Set<PropertyValue> destSet, 
			Collection<? extends PropertyValue> propVals)
		{
			for (PropertyValue propVal: propVals) {
				if (destSet.add(propVal)) 
					sizeInBytes += propVal.getSizeInBytes();
			}
		}
		
		public PropertyValue[] lookup(String propName)
		{
			Set<PropertyValue> propValues = properties.get(propName);
				
			return propValues == null ? null : 
					   propValues.toArray(new PropertyValue[propValues.size()]);
		}
		
		public MultiPropValueList lookup(String ... propNames)
		{
			Map<String, PropertyValue[]> result = new TreeMap<String, PropertyValue[]>(
														  String.CASE_INSENSITIVE_ORDER);
				
			if (propNames.length == 0) {
				for (Map.Entry<String, Set<PropertyValue>> entry: properties.entrySet()) 
						result.put(entry.getKey(), entry.getValue().toArray(new PropertyValue[
						    entry.getValue().size()]));
			}
			else {
				for (String propName: propNames) {
					Set<PropertyValue> propValues = properties.get(propName);
					
					if (propValues != null) 
						result.put(propName, propValues.toArray(new PropertyValue[
						    propValues.size()]));
				}
			}
			return result.isEmpty() ? null : new MultiPropValueList(result);
		}
		
		public boolean hasPropertyValue(String propName, String ... propValues)
		{
			Set<PropertyValue> currPropValues = properties.get(propName);
			
			if (currPropValues != null) {
				for (String propValue: propValues) {
					if (currPropValues.contains(propValue))
						return true;
				}
			}
			return false;
		}
		
		public void add(String propName, PropertyValue ... propValues)
		{
			List<PropertyValue> propValList = new ArrayList<PropertyValue>(
													  propValues.length);
			Set<PropertyValue> currPropValues = properties.get(propName);
			
			for (PropertyValue propVal: propValues) {
				if (propVal != null && propVal.getValue() != null && 
					!propVal.getValue().isEmpty())
					propValList.add(propVal);
			}
			if (currPropValues == null) { 
				currPropValues = new HashSet<PropertyValue>(propValList.size());
				properties.put(propName, currPropValues);
			}
			addToPropValsSet(currPropValues, propValList);
		}
		
		public void add(String propName, RDFNode ... propValueNodes)
		{
			PropertyValue[] propVals = new PropertyValue[propValueNodes.length];
			
			for (int i = 0; i < propValueNodes.length; ++i) {
				if (propValueNodes[i] != null)
					propVals[i] = new PropertyValue(propValueNodes[i]);
			}
			add(propName, propVals);
		}
		
		public void add(PropertyList propList)
		{
			for (Map.Entry<String, Set<PropertyValue>> entry: propList.properties.entrySet()) {
				Set<PropertyValue> propVals = properties.get(entry.getKey());
				
				if (propVals != null)
					addToPropValsSet(propVals, entry.getValue());
				else {
					properties.put(entry.getKey(), entry.getValue());
					sizeInBytes += propList.sizeInBytes;
				}
			}
		}
		
		public boolean isEmpty()
		{
			return properties.isEmpty();
		}
		
		public long getSizeInBytes()
		{
			return sizeInBytes;
		}
	}
	
	/**
	 * Resource cache item
	 */
	private static class CacheItem {
		
		private PropertyList properties = null;
		
		public PropertyValue[] lookup(String propName)
		{
			return properties == null ? null : properties.lookup(propName);
		}
		
		public MultiPropValueList lookup(String ... propNames)
		{
			return properties == null ? null : properties.lookup(propNames);
		}
		
		public boolean hasPropertyValue(String propName, String ... propValue)
		{
			return properties == null ? null : properties.hasPropertyValue(propName, propValue);
		}		
		
		public void add(String propName, PropertyValue ... propValues)
		{
			if (properties == null)
				properties = new PropertyList();
			properties.add(propName, propValues);				
		}
		
		public void add(String propName, RDFNode ... propValueNodes)
		{
			if (properties == null)
				properties = new PropertyList();
			properties.add(propName, propValueNodes);		
		}
		
		public void add(PropertyList propList)
		{
			if (properties == null)
				properties = propList;
			else {
				properties.add(propList);
			}
		}
		
		public long getSizeInBytes()
		{
			return properties == null ? 0 : properties.getSizeInBytes();
		}
	}
	
	
	private long sizeInBytes = 0;
	private String RESPROP_NAME;
	private ReadWriteLock rwLock = new ReentrantReadWriteLock();
	private Map<String, CacheItem> items = new TreeMap<String, CacheItem>(
				 								   String.CASE_INSENSITIVE_ORDER);
	private AtomicLong lookupCnt = new AtomicLong(0);
	private AtomicLong lookupMissCnt = new AtomicLong(0);
	private int maxLoad = 0;
	
	private CacheItem addItem(String resID)
	{
		CacheItem item = items.get(resID);
		
		if (item == null) {
			item = new CacheItem();
			items.put(resID, item);
		}
		return item;
	}
	
	private void shrink(CacheItem addedItem)
	{
		while (sizeInBytes > MAX_CACHE_SIZE) {
			Iterator<Entry<String, CacheItem>> iter = items.entrySet().iterator();
			Entry<String, CacheItem> entry;
			
			if (iter.hasNext()) {
				entry = iter.next();
				if (entry.getValue() == addedItem) {
					if (!iter.hasNext())
						break;
					entry = iter.next();
				}
				sizeInBytes -= entry.getValue().getSizeInBytes();
				iter.remove();
			}
			else
				break;
		}
	}
	
	public SparqlResourceCache(String resPropName)
	{
		RESPROP_NAME = resPropName;
	}
	
	public String lookupName(String resID)
	{
		rwLock.readLock().lock();
		try {
			CacheItem item = items.get(resID);
			
			lookupCnt.incrementAndGet();
			if (item != null) {
				PropertyValue[] props = item.lookup(RESPROP_NAME);
				
				if (props != null && props.length > 0) 
					return props[0].value;
			}
			lookupMissCnt.incrementAndGet();
			return null;
		}
		finally {
			rwLock.readLock().unlock();
		}
	}
	
	public PropertyValue[] lookupProperties(String resID, String propName)
	{
		rwLock.readLock().lock();
		try {
			CacheItem item = items.get(resID);
			PropertyValue[] props = null;
			
			lookupCnt.incrementAndGet();
			if (item == null || (props = item.lookup(propName)) == null)
				lookupMissCnt.incrementAndGet();
			return props;
		}
		finally {
			rwLock.readLock().unlock();
		}
	}
	
	public MultiPropValueList lookupProperties(String resID, String ... propNames)
	{
		rwLock.readLock().lock();
		try {
			CacheItem item = items.get(resID);
			MultiPropValueList propValList = null;
			
			lookupCnt.incrementAndGet();
			if (item == null || (propValList = item.lookup(propNames)) == null)
				lookupMissCnt.incrementAndGet();
			return propValList;
		}
		finally {
			rwLock.readLock().unlock();
		}
	}
	
	public boolean hasPropertyValue(String resID, String propName, String ... propValues)
	{
		rwLock.readLock().lock();
		try {
			CacheItem item = items.get(resID);
			
			return item != null && item.hasPropertyValue(propName, propValues);
		}
		finally {
			rwLock.readLock().unlock();
		}
	}

	public void addName(String resID, String name)
	{
		addProperties(resID, RESPROP_NAME, new PropertyValue(name));
	}
	
	public void addProperties(String resID, String propName, PropertyValue ... propValues)
	{
		rwLock.writeLock().lock();
		try {
			CacheItem addedItem = addItem(resID);
			long prevSize = addedItem.getSizeInBytes();
			
			addedItem.add(propName, propValues);
			sizeInBytes += addedItem.getSizeInBytes() - prevSize;
			maxLoad = (int)Math.max(maxLoad, sizeInBytes);
			shrink(addedItem);
		}
		finally {
			rwLock.writeLock().unlock();
		}
	}
	
	public void addProperties(String resID, String propName, RDFNode ... propValueNodes)
	{
		rwLock.writeLock().lock();
		try {
			CacheItem addedItem = addItem(resID);
			long prevSize = addedItem.getSizeInBytes();
			
			addedItem.add(propName, propValueNodes);
			sizeInBytes += addedItem.getSizeInBytes() - prevSize;
			maxLoad = (int)Math.max(maxLoad, sizeInBytes);
			shrink(addedItem);
		}
		finally {
			rwLock.writeLock().unlock();
		}
	}
	
	public void addProperties(String resID, PropertyList propList)
	{
		if (propList.isEmpty())
			return;
		rwLock.writeLock().lock();
		try {
			CacheItem addedItem = addItem(resID);
			long prevSize = addedItem.getSizeInBytes();
			
			addedItem.add(propList);
			sizeInBytes += addedItem.getSizeInBytes() - prevSize;
			maxLoad = (int)Math.max(maxLoad, sizeInBytes);
			shrink(addedItem);
		}
		finally {
			rwLock.writeLock().unlock();
		}
	}
	
	public void clear()
	{
		rwLock.writeLock().lock();
		try {
			sizeInBytes = 0;
			items.clear();
		}
		finally {
			rwLock.writeLock().unlock();
		}
	}
	
	public long getSizeInBytes()
	{
		rwLock.readLock().lock();
		try {
			return sizeInBytes;
		}
		finally {
			rwLock.readLock().unlock();
		}
	}
	
	public long getLookupCount()
	{
		return lookupCnt.get();
	}
	
	public long getLookupMissCount()
	{
		return lookupMissCnt.get();
	}
	
	public int getMaxLoadInBytes()
	{
		return maxLoad;
	}
}

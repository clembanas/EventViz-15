/**
 * @author Bernhard Weber
 */
import java.net.InetAddress;
import java.util.Map;
import java.util.TreeMap;


public class Utils {

	public static class Pair<First, Second>{
		public First first;
		public Second second;
		
		public Pair() 
		{
			this.first = null;
			this.second = null;
		}

		public Pair(First first, Second second)
		{
			this.first = first;
			this.second = second;
		}
	}
	
	public static class Triple<First, Second, Third>{
		public First first;
		public Second second;
		public Third third;
		
		public Triple() 
		{
			this.first = null;
			this.second = null;
			this.third = null;
		}

		public Triple(First first, Second second, Third third)
		{
			this.first = first;
			this.second = second;
			this.third = third;
		}
	}
	
	
	public static <First, Second> Pair<First, Second> createPair(First first, Second second)
	{
		return new Pair<First, Second>(first, second);
	}
	
	public static <First, Second, Third> 
		Triple<First, Second, Third> createTriple(First first, Second second, Third third)
	{
		return new Triple<First, Second, Third>(first, second, third);
	}
	
	@SafeVarargs
	public static <First, Second, MapType extends Map<First, Second>> 
		MapType createMap(Class<MapType> MapClass, Pair<First, Second> ... pairs) 
	{
		try {
			MapType result = MapClass.newInstance();
			
			for (Pair<First, Second> pair: pairs)
				result.put(pair.first, pair.second);
			return result;
		} 
		catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage());
		} 
	}
	
	@SuppressWarnings("unchecked")
	public static <First, Second, MapType extends Map<First, Second>> 
		MapType createMap(Class<MapType> MapClass, Object ... pairs) 
	{
		try {
			MapType result = MapClass.newInstance();
			
			if (pairs.length % 2 != 0)
				throw new IllegalArgumentException("Expects an even count of arguments!");
			for (int i = 0; i < pairs.length; i += 2) 
				result.put((First)pairs[i], (Second)pairs[i + 1]);
			return result;
		} 
		catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage());
		} 
	}

	@SafeVarargs
	public static Map<String, String> createStrMap(Pair<String, String> ... pairs)
	{
		Map<String, String> result = new TreeMap<String, String>(
											 String.CASE_INSENSITIVE_ORDER);
		
		for (Pair<String, String> pair: pairs)
			result.put(pair.first, pair.second);
		return result;
	}
	
	public static Map<String, String> createStrMap(String ... pairs)
	{
		Map<String, String> result = new TreeMap<String, String>(
											 String.CASE_INSENSITIVE_ORDER);
		if (pairs.length % 2 != 0)
			throw new IllegalArgumentException("Expects an even count of arguments!");
		for (int i = 0; i < pairs.length; i += 2) 
			result.put(pairs[i], pairs[i + 1]);
		return result;
	}
	
	public static Class<?> getRootClass(Class<?> _class)
	{
		Class<?> superClass = _class.getSuperclass();
		
		while (superClass != null) {
			_class = superClass;
			superClass = _class.getSuperclass();
		}
		return _class;
	}
	
	public static String classPathToString(Class<?> derivedClass, Class<?> baseClass, 
		Class<?> subClass)
	{
		StringBuilder strBuilder = new StringBuilder(derivedClass.getName());
		
		if (baseClass != null && !baseClass.equals(derivedClass)) {
			strBuilder.append("[~");
			strBuilder.append(baseClass.getName());
			strBuilder.append("]");
		}
		if (subClass != null) {
			strBuilder.append("::");
			strBuilder.append(subClass.getName());
		}
		return strBuilder.toString();
	}
	
	public static String objectsToString(Object ... args)
	{
		StringBuilder strBuilder = new StringBuilder();
		
		if (args == null)
			return "null";
		strBuilder.append("[");
		for (Object arg: args) {
			if (strBuilder.length() > 1) 
				strBuilder.append(", ");
			if (arg == null) 
				strBuilder.append("null");
			else {
				strBuilder.append(arg.getClass().getName());
				strBuilder.append(": '");
				strBuilder.append(arg.toString());
				strBuilder.append("'");
			}
		}
		strBuilder.append("]");
		return strBuilder.toString();
	}
	
	public static String inetAddressesToString(InetAddress ... inetAddrs)
	{
		StringBuilder strBuilder = new StringBuilder();
		
		for (InetAddress inetAddr: inetAddrs) {
			if (strBuilder.length() > 1) 
				strBuilder.append("; ");
			try {
				strBuilder.append(inetAddr.getHostName());
				strBuilder.append(" (");
				strBuilder.append(inetAddr.getHostAddress());
				strBuilder.append(")");
			}
			catch (Exception e) {
				strBuilder.append(inetAddr.getHostAddress());
			}
		}
		return strBuilder.toString();
	}
	
	public static String stackTraceToString(StackTraceElement[] stackTraceElems)
	{
		StringBuilder strBuilder = new StringBuilder();
		
		for (StackTraceElement elem: stackTraceElems) {
			if (strBuilder.length() > 0)
				strBuilder.append("\n");
			strBuilder.append(elem.toString());
		}
		return strBuilder.toString();
	}
	
	public static String wrapString(String s, int maxLen, String delim)
	{
		StringBuilder strBuilder = new StringBuilder();
		int idx;
		
		for (String line: s.split("\\n")) {
			while (line.length() > maxLen) {
				for (idx = maxLen; idx >= 0 && 
					!String.valueOf(line.charAt(idx)).matches("\\p{Punct}|\\p{Space}"); --idx);
				if (idx < 0)
					idx = maxLen;
				strBuilder.append(line.substring(0, idx + 1));
				line = line.substring(idx + 1);
				strBuilder.append(delim);
			}
			strBuilder.append(line);
		}
		return strBuilder.toString();
	}
	
	public static String wrapString(String s, int maxLen)
	{
		return wrapString(s, maxLen, "\n");
	}
	
	public static String replaceEach(String fmt, String val, String valReplaceFmt, Object ... by)
	{
		StringBuilder strBuilder = new StringBuilder(val.length());
		int lastIdx = 0;
		int currIdx;
		int objIdx = 0;
		
		while ((currIdx = fmt.indexOf(val, lastIdx)) > 0) {
			strBuilder.append(fmt.substring(lastIdx, currIdx));
			strBuilder.append(String.format(valReplaceFmt, by[objIdx] == null ? "null" :
				by[objIdx].toString()));
			objIdx++;
			lastIdx = currIdx + val.length();
		}
		if (lastIdx < fmt.length())
			strBuilder.append(fmt.substring(lastIdx));
		return strBuilder.toString();
	}
	
	public static String replaceEach(String fmt, String val, Object ... by)
	{
		return replaceEach(fmt, val, "%s", by);
	}
	
	public static String truncate(String s, int maxLen)
	{
		return s == null ? null : (s.length() > maxLen ? s.substring(0, maxLen) : s);
	}
	
	public static String trimAndTrunc(String s, int maxLen)
	{
		if (s == null)
			return null;
		s = s.trim();
		return s.length() > maxLen ? s.substring(0, maxLen) : s;
	}
}

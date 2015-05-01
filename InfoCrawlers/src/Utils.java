/**
 * @author Bernhard Weber
 */
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
	
	
	public static <First, Second> Pair<First, Second> createPair(First first, Second second)
	{
		return new Pair<First, Second>(first, second);
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
		
		for (int i = 0; i < pairs.length; i += 2) 
			result.put(pairs[i], pairs[i + 1]);
		return result;
	}
}

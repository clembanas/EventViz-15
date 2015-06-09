/**
 * @author Bernhard Weber
 */
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DebugUtils {
	
	/**
	 * Interface which Debug-Flag enumerations must implement 
	 */
	public static interface DebugFlagBase {
		
		public int toInt();
	}
	
	/**
	 * Multiple Debug Flags 
	 */
	public static class DebugFlags {
		
		private int value;
		
		DebugFlags() 
		{
			value = 0;
		}
		
		DebugFlags(int flags) 
		{
			value = flags;
		}
		
		public DebugFlags(DebugFlags dbgFlags, int flags) 
		{
			if (dbgFlags != null)
				value = flags | dbgFlags.value;
			else
				value = flags;
		}

		public boolean matches(DebugFlagBase ... flags) 
		{
			for (DebugFlagBase flag: flags) {
				if (flag == null || (value & flag.toInt()) != 0)
					return true;
			}
			return false;
		}		
		
		public int asInt()
		{
			return value;
		}
	}

	/**
	 * Used to output data formatted as a table. 
	 */
	public static class TableDebugger {

		public static final int MAX_ENTRY_LEN = 40;
		public static final int ROWIDX_ENTRY_LEN = 10;
		
		private String rowFmtStr = "%" + MAX_ENTRY_LEN + "." + MAX_ENTRY_LEN + "s";
		private String rowNullFmtStr = "%-" + MAX_ENTRY_LEN + "." + MAX_ENTRY_LEN + "s";
		private String rowIdxFmtStr = "| %" + ROWIDX_ENTRY_LEN + "." + ROWIDX_ENTRY_LEN + "s | ";
		private int maxEntryLen = MAX_ENTRY_LEN;
		private int rowIdxEntryLen = ROWIDX_ENTRY_LEN;
		private String[] header = null;
		private List<String[]> rows = new ArrayList<String[]>();
		
		private Utils.Pair<Integer, String> getFirstLine(String entry)
		{
			String currRowFmtStr = rowFmtStr;
			
			if (entry == null) {
				entry = "NULL";
				currRowFmtStr = rowNullFmtStr;
			}
			
			int entryLen = entry.length();
			return Utils.createPair((int)Math.ceil(entryLen/ (float)maxEntryLen), 
					   String.format(currRowFmtStr, entry.substring(0, 
					   Math.min(maxEntryLen, entryLen))));
		}
		
		private String getNextLine(String entry, int lineIdx)
		{
			String currRowFmtStr = rowFmtStr;
			
			if (entry == null) {
				entry = "NULL";
				currRowFmtStr = rowNullFmtStr;
			}
			
			int entryLen = entry.length();
			if (lineIdx * maxEntryLen > entryLen)
				return String.format(currRowFmtStr, "");
			return String.format(currRowFmtStr, entry.substring(lineIdx * maxEntryLen, 
					   Math.min(lineIdx * maxEntryLen + maxEntryLen, entryLen)));
		}
		
		private void appendRowSeparator(StringBuilder strBuilder, int colCnt, char sep)
		{
			strBuilder.ensureCapacity(strBuilder.length() + colCnt * 3 + 2);
			for (int i = 0; i < colCnt * (maxEntryLen + 3) + rowIdxEntryLen + 3; ++i)
				strBuilder.append(sep);
			strBuilder.append(sep);
			strBuilder.append("\n");
		}

		private void wrapRow(StringBuilder strBuilder, String rowIdx, String[] row, 
			Character rowDelim)
		{
			int lineCnt = 0;
			
			strBuilder.append(String.format(rowIdxFmtStr, rowIdx));
			for (String entry: row) {
				Utils.Pair<Integer, String> firstLine = getFirstLine(entry);
				
				lineCnt = Math.max(firstLine.first, lineCnt);
				strBuilder.append(firstLine.second);
				strBuilder.append(" | ");
			}
			strBuilder.append("\n");
			for (int i = 1; i < lineCnt; ++i) {
				strBuilder.append(String.format(rowIdxFmtStr, ""));
				for (String entry: row) {
					String nextLine = getNextLine(entry, i);
					
					strBuilder.append(nextLine);
					strBuilder.append(" | ");
				}
				strBuilder.append("\n");
			}
			if (rowDelim != null)
				appendRowSeparator(strBuilder, row.length, rowDelim.charValue());
		}
		
		public TableDebugger() {}
		
		public TableDebugger(int maxEntryLen, int rowIdxEntryLen)
		{
			rowFmtStr = "%" + maxEntryLen + "." + maxEntryLen + "s";
			rowNullFmtStr = "%-" + maxEntryLen + "." + maxEntryLen + "s";
			rowIdxFmtStr = "| %" + rowIdxEntryLen + "." + rowIdxEntryLen + "s | ";
			this.rowIdxEntryLen = rowIdxEntryLen;
			this.maxEntryLen = maxEntryLen;
		}
		
		public TableDebugger(int maxEntryLen)
		{
			this(maxEntryLen, ROWIDX_ENTRY_LEN);
		}
		
		public void setHeader(String[] entries)
		{
			header = entries;
		}
		
		public void setHeader(List<String> entries)
		{
			header = entries.toArray(new String[entries.size()]);
		}
		
		public void addRow(String[] entries)
		{
			rows.add(entries);
		}
		
		public void addRow(List<String> entries)
		{
			rows.add(entries.toArray(new String[entries.size()]));
		}
		
		public String toString()
		{
			StringBuilder strBuilder = new StringBuilder();
			int rowIdx = 0;
			
			//Output header
			if (header != null) 
				wrapRow(strBuilder, "Index", header, '-');
			//Output data rows
			for (String[] row: rows) 
				wrapRow(strBuilder, String.valueOf(rowIdx++), row, null);
			return strBuilder.toString();
		}
	}
	
	
	private static Map<Class<?>, DebugFlags> dbgClassFlags = new HashMap<Class<?>, DebugFlags>();
	private static Map<Class<?>, Class<?>> dbgClasses = new HashMap<Class<?>, Class<?>>();
	private static SimpleDateFormat dateFmt = new SimpleDateFormat("HH:mm:ss.SSS ");
	
	public static void debugClass(Class<?> _class, int flags)
	{
		Class<?> rootClass = Utils.getRootClass(_class);
		
		dbgClasses.put(_class, rootClass);
		dbgClassFlags.put(rootClass, new DebugFlags(dbgClassFlags.get(rootClass), flags));
	}
	
	public static void debugClass(Class<?> _class)
	{
		Class<?> rootClass = Utils.getRootClass(_class);
		DebugFlags dbgFlags = dbgClassFlags.get(rootClass);
		
		dbgClasses.put(_class, rootClass);
		dbgClassFlags.put(rootClass, dbgFlags == null ? new DebugFlags() : dbgFlags);
	}
	
	public static boolean canDebug(Class<?> derivedClass, Class<?> baseClass, Class<?> subClass, 
		DebugFlagBase firstFlag, DebugFlagBase ... remainFlags)
	{
		Class<?> rootClass = dbgClasses.get(baseClass != null ? baseClass : derivedClass);
		
		if (rootClass == null)
			return false;
		if (firstFlag == null)
			return true;
		
		DebugFlags dbgFlags = dbgClassFlags.get(rootClass);
		return dbgFlags.matches(firstFlag) || dbgFlags.matches(remainFlags);
	}
	
	public static boolean canDebug(Class<?> derivedClass, Class<?> baseClass, 
		DebugFlagBase firstFlag, DebugFlagBase ... remainFlags)
	{
		return canDebug(derivedClass, baseClass, null, firstFlag, remainFlags);
	}
	
	public static boolean canDebug(Class<?> derivedClass, DebugFlagBase firstFlag, 
		DebugFlagBase ... remainFlags)
	{
		return canDebug(derivedClass, null, null, firstFlag, remainFlags);
	}
	
	public static boolean canDebug(Class<?> derivedClass, Class<?> baseClass, Class<?> subClass)
	{
		return canDebug(derivedClass, baseClass, subClass, null);
	}
	
	public static boolean canDebug(Class<?> derivedClass, Class<?> baseClass)
	{
		return canDebug(derivedClass, baseClass, (Class<?>)null, null);
	}
	
	public static boolean canDebug(Class<?> derivedClass)
	{
		return canDebug(derivedClass, (Class<?>)null, (Class<?>)null, null);
	}
	
	public static void printDebugInfo(final String info, Class<?> derivedClass,	Class<?> baseClass, 
		Class<?> subClass, boolean canLog, DebugFlagBase firstFlag, DebugFlagBase ... remainFlags)
	{
		if (canDebug(derivedClass, baseClass, subClass, firstFlag, remainFlags)) {
			String classPath = Utils.classPathToString(derivedClass, baseClass, subClass);
			
			System.out.println(dateFmt.format(new Date()) + "[" + classPath + " (Thread " + 
				Thread.currentThread().getId() + ")]: " + info);
			if (canLog) {
				try {
					DBConnector.getInstance().logDebugInfo(classPath, 
						Thread.currentThread().getId(), info);
				}
				catch (Exception e1) {}
			}
		}
	}
	
	public static void printDebugInfo(final String info, Class<?> derivedClass,	Class<?> baseClass, 
		Class<?> subClass, DebugFlagBase firstFlag, DebugFlagBase ... remainFlags)
	{
		printDebugInfo(info, derivedClass, baseClass, subClass, true, firstFlag, remainFlags);
	}
	
	public static void printDebugInfo(final String info, Class<?> derivedClass, 
		Class<?> baseClass, boolean canLog, DebugFlagBase firstFlag, DebugFlagBase ... remainFlags)
	{
		printDebugInfo(info, derivedClass, baseClass, null, canLog, firstFlag, remainFlags);
	}
	
	public static void printDebugInfo(final String info, Class<?> derivedClass, 
		Class<?> baseClass, DebugFlagBase firstFlag, DebugFlagBase ... remainFlags)
	{
		printDebugInfo(info, derivedClass, baseClass, null, firstFlag, remainFlags);
	}
	
	public static void printDebugInfo(final String info, Class<?> derivedClass, boolean canLog, 
		DebugFlagBase firstFlag, DebugFlagBase ... remainFlags)
	{
		printDebugInfo(info, derivedClass, null, null, canLog, firstFlag, remainFlags);
	}

	public static void printDebugInfo(final String info, Class<?> derivedClass,
		DebugFlagBase firstFlag, DebugFlagBase ... remainFlags)
	{
		printDebugInfo(info, derivedClass, null, null, true, firstFlag, remainFlags);
	}
	
	public static void printDebugInfo(final String info, Class<?> derivedClass,	Class<?> baseClass, 
		Class<?> subClass)
	{
		printDebugInfo(info, derivedClass, baseClass, subClass, true, null);
	}
	
	public static void printDebugInfo(final String info, Class<?> derivedClass, Class<?> baseClass, 
		boolean canLog)
	{
		printDebugInfo(info, derivedClass, baseClass, (Class<?>)null, canLog, (DebugFlagBase)null);
	}
	
	public static void printDebugInfo(final String info, Class<?> derivedClass, Class<?> baseClass)
	{
		printDebugInfo(info, derivedClass, baseClass, (Class<?>)null, true, (DebugFlagBase)null);
	}
	
	public static void printDebugInfo(final String info, Class<?> derivedClass, boolean canLog)
	{
		printDebugInfo(info, derivedClass, (Class<?>)null, (Class<?>)null, canLog, 
			(DebugFlagBase)null);
	}

	public static void printDebugInfo(final String info, Class<?> derivedClass)
	{
		printDebugInfo(info, derivedClass, (Class<?>)null, (Class<?>)null, true,
			(DebugFlagBase)null);
	}
}

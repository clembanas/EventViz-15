/**
 * @author Bernhard Weber
 */
import java.util.ArrayList;
import java.util.List;


public class DebugUtils {

	public static class TableDebugger {

		public static final int MAX_ENTRY_LEN = 40;
		public static final int ROWIDX_ENTRY_LEN = 10;
		
		private String rowFmtStr = new String("%" + MAX_ENTRY_LEN + "." + MAX_ENTRY_LEN + "s");
		private String rowIdxFmtStr = new String("| %" + ROWIDX_ENTRY_LEN + "." + 
											  ROWIDX_ENTRY_LEN + "s | ");
		private int maxEntryLen = MAX_ENTRY_LEN;
		private int rowIdxEntryLen = ROWIDX_ENTRY_LEN;
		private String[] header = null;
		private List<String[]> rows = new ArrayList<String[]>();
		
		private Utils.Pair<Integer, String> getFirstLine(String entry)
		{
			int entryLen = entry.length();
			
			return new Utils.Pair<Integer, String>((int)Math.ceil(entryLen/ (float)maxEntryLen), 
						   String.format(rowFmtStr, entry.substring(0, 
						   Math.min(maxEntryLen, entryLen))));
		}
		
		private String getNextLine(String entry, int lineIdx)
		{
			int entryLen = entry.length();
			
			if (lineIdx * maxEntryLen > entryLen)
				return String.format(rowFmtStr, "");
			return String.format(rowFmtStr, entry.substring(lineIdx * maxEntryLen, 
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
			rowFmtStr = new String("%" + maxEntryLen + "." + maxEntryLen + "s");
			rowIdxFmtStr = new String("| %" + rowIdxEntryLen + "." + rowIdxEntryLen + "s | ");
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
	
	public static void debug_print(final String info)
	{
		System.out.println(info);
	}
	
	public static void debug_printf(final String fmt, Object ... infos)
	{
		System.out.printf(fmt, infos);
	}
}

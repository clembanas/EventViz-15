/**
 * @author Bernhard Weber
 */

public class ExceptionHandler {
	
	private static String stackTraceToStr(StackTraceElement[] stackTraceElems)
	{
		StringBuilder strBuilder = new StringBuilder();
		
		for (StackTraceElement elem: stackTraceElems) {
			if (strBuilder.length() > 0)
				strBuilder.append("\n");
			strBuilder.append(elem.toString());
		}
		return strBuilder.toString();
	}

	public static void handle(final Exception e, final String info, final boolean printTrace)
	{
		System.err.println("\n-------------------------- Exception --------------------------\n" +
			info + " Error: '" + e.getMessage() + "' [" + e.getClass().getName() + "] in " + 
			e.getStackTrace()[0] + "!");
		if (printTrace) {
			System.err.println();
			e.printStackTrace();
		}
		System.err.println("---------------------------------------------------------------\n");
		try {
			DBConnection.getInstance().logException(e, info, stackTraceToStr(e.getStackTrace()));
		}
		catch (Exception e1) {}
	}
	
	public static void handle(final Exception e, final String info)
	{
		handle(e, info, true);
	}
}

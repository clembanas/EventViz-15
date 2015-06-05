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
	
	public static void handle(final String info, final Exception e, final boolean printTrace,
		Class<?> derivedClass, Class<?> baseClass, Class<?> subClass)
	{
		String classPath = Utils.classPathToString(derivedClass, baseClass, subClass);
		
		System.err.println("\n-------------------------- Exception --------------------------\n" +
			"[" + classPath + " (Thread " + Thread.currentThread().getId() + ")]: " + info + 
			" Error: '" + e.getMessage() + "' [" + e.getClass().getName() + "] in " + 
			e.getStackTrace()[0] + "!");
		if (printTrace) {
			System.err.println();
			e.printStackTrace();
		}
		System.err.println("---------------------------------------------------------------\n");
		try {
			DBConnector dbConn = DBConnector.getInstance();
			
			if (dbConn.isConnected())
				dbConn.logException(classPath, Thread.currentThread().getId(), info, e, 
					stackTraceToStr(e.getStackTrace()));
		}
		catch (Exception e1) {}
	}
	
	public static void handle(final String info, final Exception e, Class<?> derivedClass, 
		Class<?> baseClass, Class<?> subClass)
	{
		handle(info, e, true, derivedClass, baseClass, subClass);
	}
	
	public static void handle(final String info, final Exception e, Class<?> derivedClass, 
		Class<?> baseClass)
	{
		handle(info, e, true, derivedClass, baseClass, null);
	}
	
	public static void handle(final String info, final Exception e, Class<?> _class)
	{
		handle(info, e, true, _class, null, null);
	}
}

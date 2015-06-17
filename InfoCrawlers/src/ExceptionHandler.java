import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Bernhard Weber
 */

public class ExceptionHandler {
	
	private static SimpleDateFormat dateFmt = new SimpleDateFormat("HH:mm:ss.SSS ");
	
	public static void handle(final String info, final Exception e, final boolean printTrace,
		Class<?> derivedClass, Class<?> baseClass, Class<?> subClass, boolean canLog)
	{
		String classPath = Utils.classPathToString(derivedClass, baseClass, subClass);
		
		System.err.println("\n-------------------------- Exception --------------------------\n" +
			dateFmt.format(new Date()) + "[" + classPath + " (Thread " + 
			Thread.currentThread().getId() + ")]: " + info + " Error: '" + e.getMessage() + "' [" + 
			e.getClass().getName() + "] " + (e.getCause() != null ? "(caused by: " + 
			e.getCause().getMessage() + " [" + e.getCause().getClass().getName() + "]) in " : 
			"in ") + e.getStackTrace()[0] + "!");
		if (printTrace) {
			System.err.println();
			e.printStackTrace();
		}
		System.err.println("---------------------------------------------------------------\n");
		if (canLog) {
			try {
				DBConnector.getInstance().logException(classPath, Thread.currentThread().getId(), 
					info, e, Utils.stackTraceToString(e.getStackTrace()));
			}
			catch (Exception e1) {}
		}
	}
	
	public static void handle(final String info, final Exception e, final boolean printTrace,
		Class<?> derivedClass, Class<?> baseClass, Class<?> subClass)
	{
		handle(info, e, true, derivedClass, baseClass, subClass, true);
	}
	
	public static void handle(final String info, final Exception e, Class<?> derivedClass, 
		Class<?> baseClass, Class<?> subClass, boolean canLog)
	{
		handle(info, e, true, derivedClass, baseClass, subClass, canLog);
	}
	
	public static void handle(final String info, final Exception e, Class<?> derivedClass, 
		Class<?> baseClass, Class<?> subClass)
	{
		handle(info, e, true, derivedClass, baseClass, subClass, true);
	}
	
	public static void handle(final String info, final Exception e, Class<?> derivedClass, 
		Class<?> baseClass, boolean canLog)
	{
		handle(info, e, true, derivedClass, baseClass, null, canLog);
	}
	
	public static void handle(final String info, final Exception e, Class<?> derivedClass, 
		Class<?> baseClass)
	{
		handle(info, e, true, derivedClass, baseClass, null, true);
	}
	
	public static void handle(final String info, final Exception e, Class<?> _class, boolean canLog)
	{
		handle(info, e, true, _class, null, null, canLog);
	}
	
	public static void handle(final String info, final Exception e, Class<?> _class)
	{
		handle(info, e, true, _class, null, null, true);
	}
}

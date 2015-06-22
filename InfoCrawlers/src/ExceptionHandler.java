import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Bernhard Weber
 */

public class ExceptionHandler {
	
	private static SimpleDateFormat dateFmt = new SimpleDateFormat("HH:mm:ss.SSS ");
	private static String logFileName = null;
	
	private static void writeDBLog(String classPath, String info, Throwable e)
	{
		try {
			DBConnector.getInstance().logException(classPath, Thread.currentThread().getId(), 
				info, e, Utils.stackTraceToString(e.getStackTrace()));
		}
		catch (Exception e1) {}
	}
	
	private static void writeFileLog(String log)
	{
		try {
			synchronized (ExceptionHandler.class) {
				if (logFileName == null) {
					logFileName = CrawlerConfig.getDbgExceptionLogFile();
					if (logFileName == null)
						logFileName = "";
					else
						new File(logFileName).getAbsoluteFile().getParentFile().mkdirs();
				}
				if (!logFileName.isEmpty()) {
					OutputStream out = new FileOutputStream(logFileName, true);
					
					try {
						out.write(log.getBytes("UTF-8"));
						out.flush();
					}
					finally {
						out.close();
					}
				}
			}
		}
		catch (Throwable e) {}
	}
	
	public static void handle(final String info, final Throwable e, final boolean printTrace,
		Class<?> derivedClass, Class<?> baseClass, Class<?> subClass, boolean canLog)
	{
		String classPath = Utils.classPathToString(derivedClass, baseClass, subClass);
		String log = "\n-------------------------- Exception --------------------------\n" +
						dateFmt.format(new Date()) + "[" + classPath + " (Thread " + 
						Thread.currentThread().getId() + ")]: " + info + " Error: '" + 
						e.getMessage() + "' [" + e.getClass().getName() + "] " + 
						(e.getCause() != null ? "(caused by: " + e.getCause().getMessage() + " [" + 
						e.getCause().getClass().getName() + "]) in " : "in ") + 
						e.getStackTrace()[0] + "!\n";
		
		if (printTrace)
			log += "\n" + Utils.stackTraceToString(e.getStackTrace()) + "\n";
		log += "---------------------------------------------------------------\n\n";
		System.err.print(log);
		
		if (canLog) {
			writeDBLog(classPath, info, e);
			writeFileLog(log);
		}
	}
	
	public static void handle(final String info, final Throwable e, final boolean printTrace,
		Class<?> derivedClass, Class<?> baseClass, Class<?> subClass)
	{
		handle(info, e, true, derivedClass, baseClass, subClass, true);
	}
	
	public static void handle(final String info, final Throwable e, Class<?> derivedClass, 
		Class<?> baseClass, Class<?> subClass, boolean canLog)
	{
		handle(info, e, true, derivedClass, baseClass, subClass, canLog);
	}
	
	public static void handle(final String info, final Throwable e, Class<?> derivedClass, 
		Class<?> baseClass, Class<?> subClass)
	{
		handle(info, e, true, derivedClass, baseClass, subClass, true);
	}
	
	public static void handle(final String info, final Throwable e, Class<?> derivedClass, 
		Class<?> baseClass, boolean canLog)
	{
		handle(info, e, true, derivedClass, baseClass, null, canLog);
	}
	
	public static void handle(final String info, final Throwable e, Class<?> derivedClass, 
		Class<?> baseClass)
	{
		handle(info, e, true, derivedClass, baseClass, null, true);
	}
	
	public static void handle(final String info, final Throwable e, Class<?> _class, boolean canLog)
	{
		handle(info, e, true, _class, null, null, canLog);
	}
	
	public static void handle(final String info, final Throwable e, Class<?> _class)
	{
		handle(info, e, true, _class, null, null, true);
	}
}

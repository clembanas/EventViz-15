/**
 * @author Bernhard Weber
 */

public class ExceptionHandler {

	protected static void handle(final Exception e, final String info, final boolean printTrace)
	{
		System.err.println("\n-------------------------- Exception --------------------------\n" +
			info + " Error: '" + e.getMessage() + "' [" + e.getClass().getName() + "] in " + 
			e.getStackTrace()[0] + "!");
		if (printTrace) {
			System.err.println();
			e.printStackTrace();
		}
		System.err.println("---------------------------------------------------------------\n");
	}
	
	protected static void handle(final Exception e, final String info)
	{
		handle(e, info, true);
	}
}

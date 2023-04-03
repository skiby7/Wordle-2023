package CommonUtils;

public class PrettyPrinter {
	public static final String BLACK = "\033[30m";
	public static final String RED = "\033[31m";
	public static final String GREEN = "\033[32m";
	public static final String YELLOW = "\033[33m";
	public static final String BLUE = "\033[34m";
	public static final String MAGENTA = "\033[35m";
	public static final String CYAN = "\033[36m";
	public static final String WHITE = "\033[37m";
	public static final String BOLD = "\033[1m";
	public static final String ITALICS = "\033[2m";
	public static final String RESET = "\033[0m";

	public static void prettyPrintln(String string) {
		 System.out.println(string.replaceAll("@BK", BLACK)
					 			  .replaceAll("@R", RED)
				 				  .replaceAll("@G", GREEN)
					 			  .replaceAll("@Y", YELLOW)
			 					  .replaceAll("@BL", BLUE)
				 				  .replaceAll("@M", MAGENTA)
				 				  .replaceAll("@C", CYAN)
				 				  .replaceAll("@W", WHITE)
				 				  .replaceAll("@BO", BOLD)
				 				  .replaceAll("@I", ITALICS)
				 				  .replaceAll("@0", RESET));
	}
	public static void prettyPrint(String string) {
		 System.out.print(string.replaceAll("@BK", BLACK)
								.replaceAll("@R", RED)
								.replaceAll("@G", GREEN)
								.replaceAll("@Y", YELLOW)
								.replaceAll("@BL", BLUE)
								.replaceAll("@M", MAGENTA)
								.replaceAll("@C", CYAN)
								.replaceAll("@W", WHITE)
								.replaceAll("@BO", BOLD)
								.replaceAll("@I", ITALICS)
								.replaceAll("@0", RESET));
	}
}

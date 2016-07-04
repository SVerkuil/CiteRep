package citerep;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Main runnable class Start a worker and connect to the CiteRep web interface
 * 
 * @author Steven
 *
 */
public class Run {

	// What is the version number of this software?
	// Used to keep the web-interface in sync with this client
	public static final String version = "1.0";

	// When did we start this program?
	private static long startTime = System.currentTimeMillis();

	// How much workers does this program handle?
	// NOTE: this is typically 1, added for future use
	private static long workerCount = 1;

	// What is the default verbose logging level?
	public static int verbose = 1;

	// Base directory for file handing
	public static final String FilePath = "./";

	// Additional prefix to output in RUN log
	protected static String runLogPrefix = "";

	/**
	 * Start the main application.
	 * 
	 * @param args
	 *            First argument is the url, second argument the passphrase,
	 *            third is human readable name
	 */
	public static void main(String[] args) {

		// Check if all arguments are present
		if (args.length < 2) {
			log("Run as CitRep [url] [passphrase] [name ''?] [verbose 1-3 (1)?]");
		} else {

			// Obtain arguments from input
			String url = args[0];
			String passphrase = args[1];
			String name = "";

			// Check if worker name identifier is set, if so, store it
			if (args.length > 2) {
				name = args[2];
			}

			// Check if worker name identifier is set, if so, store it
			if (args.length > 3) {
				verbose = Math.min(3, Math.max(1, Integer.parseInt(args[3])));
			}

			// Create worker object
			Worker worker = new Worker(url, passphrase, name);
			Thread t = new Thread(worker);
			worker.setThread(t);
			t.start();
		}

		if (verbose < 3) {

			// Attempt to disable logging from PDFBOX
			String[] loggers = { "org.apache.pdfbox.util.PDFStreamEngine",
					"org.apache.pdfbox.pdmodel.font.PDSimpleFont", "org.apache.pdfbox.pdmodel.font.PDTrueTypeFont",
					"org.apache.pdfbox.pdmodel.font.PDFont", "org.apache.pdfbox.pdmodel.font.FontManager",
					"org.apache.pdfbox.pdfparser.PDFObjectStreamParser", "org.apache.fontbox.ttf.CmapSubtable" };
			for (String logger : loggers) {
				Logger logpdfengine = LogManager.getLogManager().getLogger(logger);
				if (logpdfengine != null) {
					logpdfengine.setLevel(Level.OFF);
				}
			}
		}
	}

	/**
	 * Log error message to system out
	 * 
	 * @param msg
	 *            The message to log
	 * @param verbose
	 *            At which verbosity level should we actually proceed with
	 *            logging?
	 */
	public static void error(String msg, int verbose) {
		if (Run.verbose >= verbose) {
			error(msg);
		}
	}

	/**
	 * Log error message to system out
	 * 
	 * @param msg
	 *            The message to log
	 */
	public static void error(String msg) {
		if (msg != null) {
			if (!msg.trim().equals("")) {
				System.err.println(((runLogPrefix.equals("")) ? "" : "[" + runLogPrefix + "] ") + msg.trim());
			}
		}
	}

	/**
	 * 
	 * Advanced message log, only log if verbose level is high enough Logs
	 * messages only if this.verbose > verbose
	 * 
	 * @param msg
	 *            The message to log
	 * @param verbose
	 *            The verbosity level at which to log the message
	 */
	public static void log(String msg, int verbose) {
		if (Run.verbose >= verbose) {
			log(msg);
		}
	}

	/**
	 * Write string contents to a file relative to FilePath
	 * 
	 * @param filename
	 *            The filename of the file to create
	 * @param contents
	 *            The content which will be written to the file
	 */
	public static void writeFile(String filename, String contents) {
		PrintWriter writer;
		try {

			// Create the directory structure
			File dirs = new File(FilePath + filename).getParentFile();
			if (!dirs.exists()) {
				dirs.mkdirs();
			}

			// Write file contents
			writer = new PrintWriter(FilePath + filename, "UTF-8");
			writer.print(contents);
			writer.close();

		} catch (Exception e) {

			// Something went wrong, log the error
			log("[DEBUG] Could not write file " + FilePath + filename, 3);
			e.printStackTrace();
		}
	}

	// Helper2
	public static String readFile(String path) {
		return readFile(path, Charset.defaultCharset());
	}

	// Helper
	public static String readFile(File path) {
		return readFile(path.getAbsolutePath(), Charset.defaultCharset());
	}

	/**
	 * Read file into string
	 * 
	 * @param path
	 *            The file to read
	 * @param encoding
	 *            The encoding to use
	 * @return The file as a string or empty string
	 */
	public static String readFile(String path, Charset encoding) {
		byte[] encoded = null;
		try {

			// Read all bytes from file
			encoded = Files.readAllBytes(Paths.get(path));
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Return the string contents, or empty string
		return (encoded == null) ? "" : new String(encoded, encoding);
	}

	/**
	 * Alias for log(msg,3)
	 * 
	 * @param msg
	 *            The message to log
	 */
	public static void logDebug(String msg) {
		log(msg, 3);
	}

	/**
	 * Log an empty line
	 */
	public static void log() {
		log("");
	}

	/**
	 * Simple message log
	 * 
	 * @param msg
	 *            Log message to system out
	 */
	public static void log(String msg) {
		if (msg != null) {
			if (!msg.trim().equals("")) {
				System.out.println(((runLogPrefix.equals("")) ? "" : "[" + runLogPrefix + "] ") + msg.trim());
			}
		}
	}

	public static void setLogPrefix(String prefix) {
		runLogPrefix = prefix;
	}

	/**
	 * Returns Globally Unique worker name (Changes after each call!)
	 * 
	 * @return (String) GUUID Worker Name
	 */
	public static String getWorkerGUUID() {
		return getWorkerGUUID("");
	}

	/**
	 * Returns Globally Unique worker uuid (Changes after each call!)
	 * 
	 * @return (String) GUUID Worker Name
	 */
	public static String getWorkerGUUID(String name) {
		String uuid = "";

		// Try to read the computer name or hostname
		Map<String, String> env = System.getenv();
		if (env.containsKey("COMPUTERNAME")) {
			uuid = env.get("COMPUTERNAME");
		} else if (env.containsKey("HOSTNAME")) {
			uuid = env.get("HOSTNAME");
		} else {
			uuid = "REMOTE";
		}

		// Add start time and worker count
		uuid = uuid.replace("@", "");
		return (uuid + "@" + startTime + "@" + (workerCount++)).toUpperCase() + "@" + name;
	}
}

package task;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import citerep.Run;
import citerep.TaskRunner;

/**
 * 
 * NormalizeTask
 * 
 * This class is directly called from IdentifyTask This saves us an additional
 * roundtrip over the network.
 * 
 * @author Steven
 * 
 */
public class NormalizeTask extends Task {

	/*
	 * Basic replacements
	 */

	private static boolean isInitialized = false;

	// Abbreviations (FROM -> TO)
	private static HashMap<String, String> abbr = new HashMap<String, String>();

	// All known abbreviations
	private static TreeSet<String> allAbbreviations = new TreeSet<String>();
	private static TreeSet<String> allAbbreviationWords = new TreeSet<String>();
	private static String[] allAbbr = null;

	// Known journals
	private static HashMap<String, String> known = new HashMap<String, String>();

	// Compiled file of known journals
	public static final String journalFile = "CiteRepData/journals.txt";

	/*
	 * File containing known abbreviations These abbreviations were extracted
	 * from journals.txt by comparison and manual correction
	 */
	public static final String abbreviationFile = "CiteRepData/abbreviations.txt";

	public NormalizeTask(TaskRunner runner) {
		super(runner);
	}

	/**
	 * Get all known abbreviations in string array
	 * 
	 * @return Known abbreviations
	 */
	public static String[] getAllAbbreviations() {
		init();
		return allAbbr;
	}

	/**
	 * Checks if input is either already abbreviated, or can be abbreviated
	 * 
	 * @param in
	 *            The word to check
	 * @return true if this word is an abbreviation, or can be abbreviated
	 */
	public static boolean canAbbreviate(String in) {
		init();
		return allAbbreviationWords.contains(in.trim());
	}

	/**
	 * Generate SimpleJournal without abbreviating
	 * 
	 * @param journal
	 *            The journal to simplify
	 * @return The simplified journal
	 */
	public static String simpleJournal(String journal) {
		return simpleJournal(journal, false);
	}

	/**
	 * Return String in which all journal abbreviations are replaced with their
	 * form without a dot preceding. e.g. "proc." and "proc," becomes "proc" in
	 * this string
	 * 
	 * @param journal
	 * @return
	 */
	public static String removeDotsFromAbbreviations(String journal) {
		init();

		// Basic corrections
		journal = journal.toLowerCase().replace("in:", "").replace("&", " and ").replace(":", "")
				.replaceAll("\\([^\\)]+\\)", "").replaceAll("([a-z]+)\\- ([a-z]+)\\s?", "$1$2 ").trim();

		// This changes notations such as j. am. assoc. to j am assoc
		String[] parts = journal.split(" ");
		for (String p : parts) {
			String clean = p.replace(".", "").trim();
			if (allAbbreviationWords.contains(clean)) {
				journal = journal.replace(p, clean);
			}
		}

		// This changes notations such as j.am.assoc. to j am assoc
		for (String p : parts) {
			String[] parts2 = p.split("\\.");
			for (String pp : parts2) {
				String clean = pp.trim();
				if (allAbbreviationWords.contains(clean)) {
					journal = journal.replace(pp + ".", clean + " ");
				}
			}

		}

		return journal.replaceAll("\\s\\s+", " ").trim();
	}

	/**
	 * Make journal into simple notation
	 * 
	 * @param journal
	 *            The journal to turn into simple notation
	 * @return The simple notation journal
	 */
	public static String simpleJournal(String journal, Boolean parseAbbr) {
		init();

		// Basic fixes
		String j = (" " + journal + " ").toLowerCase().replace("&", " ").replace("\\([^\\)]+\\)", " ")
				.replaceAll("[^a-z ]+", " ").replace(" of the ", " ").replace(" ofthe ", "").replace(" of ", " ")
				.replace(" no ", " ").replace(" pp ", " ").replace(" nd ", " ").replace(" th ", " ")
				.replace(" ed ", " ").replace(" and ", " ").replace(" edition ", " ").replace(" vols ", " ")
				.replace(" volume ", " ").replace(" volumes ", " ").replace(" journal ", " ").replace(" j ", " ")
				.replace(" proceedings ", " ").replace(" proc ", " ").replace(" vol ", " ").replace(" part ", " ")
				.replace(" parts ", " ").replaceAll("\\s\\s+", " ").trim().replaceAll(" http.*$", "");

		// Also parse abbreviations?
		// NOTE only do this when we have at least 2 words in the source
		String ret = "";
		if (parseAbbr && journal.contains(" ")) {
			String[] parts = j.split(" ");
			String res = "";
			for (String s : parts) {
				if (abbr.containsKey(s)) {
					res = res + " " + abbr.get(s);
				} else {
					res = res + " " + s;
				}
			}
			ret = res;
		} else {
			ret = j;
		}

		return ret.trim(); // Remove whitespace and return
	}

	/**
	 * Load standard abbreviations in main memory
	 */
	private static synchronized void init() {

		if (!isInitialized) {

			// Load known abbreviations
			File file = new File(Run.FilePath + abbreviationFile);
			List<String> lines = null;
			try {
				lines = Files.readAllLines(file.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}

			// If the file exists, store its abbreviations
			if (file.exists()) {
				for (String line : lines) {
					String[] parts = line.split("\t");
					allAbbreviations.add(parts[0]);
					allAbbreviationWords.add(parts[0]);
					if (parts.length == 2) { // Mapping FROM(abbr) -> TO
						abbr.put(parts[0], parts[1]);
						allAbbreviationWords.add(parts[1]);
					} else if (parts.length > 2) { // TO(abbr) <- FROM FROM FROM
						for (int i = 1; i < parts.length; i++) {
							abbr.put(parts[i], parts[0]);
							allAbbreviationWords.add(parts[i]);
						}
					}
				}
				allAbbr = allAbbreviations.toArray(new String[allAbbreviations.size()]);
			}

			// Load known journals
			file = new File(Run.FilePath + journalFile);
			lines = null;
			try {
				lines = Files.readAllLines(file.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}

			// We are now initialized
			// (otherwise calls to SimpleJournal() fail)
			// Only the abbreviations need to be initialized for this, which is
			// the case
			isInitialized = true;

			// If the file exists, store it in map
			if (file.exists()) {
				for (String line : lines) {
					String[] parts = line.split("\t");
					known.putIfAbsent(parts[0], parts[0]); // We can match the
															// exact journal
					known.putIfAbsent(simpleJournal(parts[0]), parts[0]); // Or
																			// we
																			// match
																			// the
																			// simplified
																			// journal
					known.putIfAbsent(simpleJournal(parts[0], true), parts[0]); // Or
																				// we
																				// match
																				// the
																				// abbreviated
																				// journal

					// known.putIfAbsent(journalSoundex(parts[0]),parts[0]);
					// Or we match the soundex (disabled, gives too much
					// noise)
					if (parts.length > 1) {
						for (int i = 1; i < parts.length; i++) {
							known.putIfAbsent(parts[i], parts[0]); // We can
																	// match the
																	// exact
																	// journal
							known.putIfAbsent(simpleJournal(parts[i]), parts[0]); // Or
																					// we
																					// match
																					// the
																					// simplified
																					// journal
							known.putIfAbsent(simpleJournal(parts[i], true), parts[0]); // Or
																						// we
																						// match
																						// the
																						// abbreviated
																						// journal
						}
					}
				}
			}
		}
	}

	/**
	 * Normalize the journal, without using the database
	 * 
	 * @param journal
	 *            The journal to normalize
	 * @return The normalized journal
	 */
	public static String normalizeJournal(String journal) {
		return normalizeJournal(journal, false);
	}

	/**
	 * This class is directly called from IdentifyTask This saves us an
	 * additional round trip over the network.
	 */
	public static String normalizeJournal(String journal, boolean databaseOnly) {
		init();

		// "In" is often a prefix "in proc ..."
		journal = journal.toLowerCase().replaceAll("[^a-z0-9 ]", "").trim().replaceAll("^in ", "");

		/**
		 * We assume to have a FULL database of all journals This database is
		 * carefully crafted and prepared for each journal several alternate
		 * spellings are stored Therefore all journals should be found in the
		 * database
		 */

		// Search for exact match
		if (journal.trim().equals("")) {
			return "";

		} // If the journal is empty, return empty
		if (known.containsKey(journal)) {
			return known.get(journal).replaceAll("^[a-z]{1}$", "");
		}

		// Search for simplified match
		String search = simpleJournal(journal);
		if (search.equals("")) {
			return "";

		} // If the simplefied version is empty, return emtpy
		if (known.containsKey(search)) {
			return known.get(search).replaceAll("^[a-z]{1}$", "");
		}

		// Search for abbreviated match
		search = simpleJournal(journal, true);
		if (known.containsKey(search)) {
			return known.get(search).replaceAll("^[a-z]{1}$", "");
		}

		// Search for match without journal/proceedings prefix
		search = simpleJournal(journal.replaceAll("^journal", "").replaceAll("^j ", "").replaceAll("^proc", "")
				.replaceAll("^proceedings", ""), true);

		if (known.containsKey(search)) {
			return known.get(search).replaceAll("^[a-z]{1}$", "");
		}

		// Search for prefix with "journal of?"
		search = simpleJournal("j " + journal, true);
		if (known.containsKey(search)) {
			return known.get(search).replaceAll("^[a-z]{1}$", "");
		}

		// Search for prefix with "proceedings of?"
		search = simpleJournal("proc " + journal, true);
		if (known.containsKey(search)) {
			return known.get(search).replaceAll("^[a-z]{1}$", "");
		}

		// Search for text "in proc ...... [^a-z ]" which clearly denotes a
		// proceedings
		Pattern p = Pattern.compile("in proc ([a-z0-9 ]+) ?.*$");
		Matcher m = p.matcher("in " + journal);
		if (m.find()) {
			return (m.group(1));
		}

		// The characters 1,i,l are sometimes mixed up, search without them
		search = simpleJournal(journal.replaceAll("[il1]", "i"), true);
		if (known.containsKey(search)) {
			return known.get(search).replaceAll("^[a-z]{1}$", "");
		}
		search = simpleJournal(journal.replaceAll("[il1]", "l"), true);
		if (known.containsKey(search)) {
			return known.get(search).replaceAll("^[a-z]{1}$", "");
		}

		// There can be some mess before the journal, e.g. xxxx j journal
		search = simpleJournal(journal.replace(" journal ", " j ").replaceAll("^.+ j ", "j ").trim());
		if (known.containsKey(search)) {
			return known.get(search).replaceAll("^[a-z]{1}$", "");
		}
		search = simpleJournal(journal.replace(" journal ", " j ").replaceAll("^.+ j ", "j ").trim(), true);
		if (known.containsKey(search)) {
			return known.get(search).replaceAll("^[a-z]{1}$", "");
		}

		// Remove edition information
		search = removeNumbering(search);
		if (known.containsKey(search)) {
			return known.get(search).replaceAll("^[a-z]{1}$", "");
		}
		search = simpleJournal(search, true);
		if (known.containsKey(search)) {
			return known.get(search).replaceAll("^[a-z]{1}$", "");
		}

		// The journal
		String j = "";

		// If we still do not have the journal, check if it can be considered to
		// be a journal
		// If this is the case, leave it be, otherwise discard it
		if (!databaseOnly) {

			float countJWords = (float) IdentifyTask.countJournalIdentifiersInText(journal);
			float countWords = (float) journal.split(" ").length;
			String tmp = simpleJournal(journal, true);
			if ((countJWords / countWords) > 0.3f) { // 30% of the text is a
														// journal identifier
				j = tmp;
				j = j.replaceAll("^in", "").trim(); // Sometimes there is a
													// leading in...
				if (j.startsWith("on")) { // This is often a title
					j = "";
				}
				if (j.startsWith("and")) { // This is often a title
					j = "";
				}
			}

			// If it starts with this, it is probably a journal
			if (tmp.startsWith("proc") || tmp.startsWith("jour")) {
				j = tmp;
			}
		}

		return j.trim().replaceAll("^[a-z]{1}$", "");
	}

	public static String removeNumbering(String search) {
		return (" " + search + " ").replace(" first ", " ").replaceAll(" [0-9]+th ", " ").replace(" second ", " ")
				.replace(" third ", " ").replace(" fourth ", " ").replace(" fifth ", " ").replace(" sixth ", " ")
				.replace(" seventh ", " ").replace(" eighth ", " ").replace(" ninth ", " ").replace(" tenth ", " ")
				.replace(" eleventh ", " ").replace(" twelfth ", " ").replace(" thirteenth ", " ")
				.replace(" fourteenth ", " ").replace(" fifteenth ", " ").replace(" seventeenth ", " ")
				.replace(" eighteenth ", " ").replace(" nineteenth ", " ").replace(" twentieth ", " ")
				.replace(" twentieth ", " ").replace(" thirtieth ", " ").replace(" fortieth ", " ")
				.replace(" fiftieth ", " ").replace(" sixtieth ", " ").replace(" seventieth ", " ")
				.replace(" eightieth ", " ").replace(" ninetieth ", " ").replace(" twenty ", " ")
				.replace(" thirty ", " ").replace(" fourty ", " ").replace(" fifty ", " ").replace(" sixty ", " ")
				.replace(" seventy ", " ").replace(" eighty ", " ").replace(" ninety ", " ").replace(" edition ", " ")
				.replace(" ed ", " ").replaceAll(" [ixv]+ ", " ").replaceAll("\\s\\s+", " ").trim();
	}
}

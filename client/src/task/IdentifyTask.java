package task;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

import org.json.JSONArray;

import citerep.Run;
import citerep.TaskRunner;
import citerep.Worker;
import correction.FormulaCorrection;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import refParse.RefParseAutomatic;

/**
 * IdentifyTask
 * 
 * @author Steven
 *
 */
public class IdentifyTask extends Task {

	// If we find less than this percentage of a journal for each citation
	// using refParse, we use a secondary approach. Allow 30% to not have a
	// journal
	protected static final float threshold = 0.7f;

	// The Current worker object
	protected static Worker worker;

	// These types are relevant for us when looking at the journal as outputted
	// by refParse
	// Note that the journal is not always correctly identified by this third
	// party app, and
	// hence we look at more than one token returned by the automatic parser
	protected static final String[] tagTypes = { "journal", "journalOrPublisher", "proceedingsVolumeTitle",
			"volumeReference", "volumeTitle", "publisher", "title", "author" };

	// These invalidatators should NOT be present in any journal
	// If found in a journal, we mark it as NOT being a journal
	protected final static String[] invalidators = { " van de ", "university of ", "last modified ", " retrieved ",
			" patent ", " we have ", " bounded by ", "this is because", "web site", " website ", " was seen ",
			" seen in ", " which is ", " isbn ", " university ", " et al ", "?" };

	// These regexes should be taken out from the final journal
	// Assume the input is trimmed
	protected final static String[] cleanup = { "\\(.+\\)?$", "^reprinted from", "paper for the", "paper at the",
			"^[a-zA-Z\\s\\.]+:\\s[A-Za-z\\s\\-0-9\\+\\:]+\\.", "^.+journal of ", "^.{80,}\\.", "^in\\s", "^janu?a?r?i?",
			"^febr?u?a?r?i?", "^marc?h?", "^apri?l?", "^may", "^june?", "^july?", "^augu?s?t?", "^sept?e?m?b?e?r?",
			"^octo?b?e?r?", "^nove?m?b?e?r?", "^dece?m?b?e?r?", "^vol\\.?\\s", "^ed\\.?\\-?\\s", "^in ",
			"^of [a-zA-Z\\- ]+[\\.\\-]{1}" };

	/**
	 * Create new Identify task
	 * 
	 * @param runner
	 */
	public IdentifyTask(TaskRunner runner) {
		super(runner);
		worker = getWorker();
	}

	/**
	 * Perform Journal identification on citation array Parses the citation
	 * array into an array with journals
	 * 
	 * NOTE: This is disabled at the moment, and called directly from
	 * CitationTask
	 */
	@Override
	public void perform() {
		getWorker().notifyThreadClosed();
	}

	/**
	 * Fix string encoding
	 * 
	 * @param str
	 *            The string to fix
	 * @return Fixed string, without printable characters
	 */
	public static String fixEncoding(String str) {
		// Fix page numbering such as 104Ã¢€“115 to 104-115
		str = str.replaceAll("(\\s[0-9]+)[^A-Za-z0-9\\s\\-\\:\\.\\,]+([0-9]+)", "$1-$2");

		// The input must be as correct and clean as possible to obtain the best
		// results.
		// The RefParse classifier works with eliminating tokens and guessing
		// which remaining fit
		// Therefore, if the input string is clean, the output will be
		// significantly better
		return str.replaceAll("\\P{Print}", "");
	}

	/**
	 * Get journals from an array of citations
	 * 
	 * @param citations
	 *            The citations to get the journals from
	 * @return An string array with journals
	 */
	public static String[] getJournalsFromCitations(JSONArray citations) {
		String[] tmp = new String[citations.length()];
		for (int i = 0; i < citations.length(); i++) {
			tmp[i] = citations.getString(i);
		}
		return getJournalsFromCitations(tmp);
	}

	/**
	 * Get journals from citations
	 * 
	 * @param citations
	 *            The citations to go through
	 * @return The parsed citations as string array
	 */
	public static String[] getJournalsFromCitations(String[] citations) {
		RefParseAutomatic refParse = init(); // Setup refParse
		String[] journals = new String[citations.length];

		// Return empty list of journals if pdf is empty
		if (citations.length == 1) {
			if (citations[0].equals(CitationTask.pdfEmpty)) {
				return new String[0];
			}
		}

		// Create XML to be processed by RefParse
		String xml = "";
		for (String cit : citations) {
			xml = xml + "<bibRef>" + fixEncoding(cit) + "</bibRef>";
		}

		MutableAnnotation myBibRefs = null;
		try {

			// Feed XML into RefParse Library, store results as
			// QueriableAnnotation. Give not more than 512KB of data
			boolean canProcess = (xml.length() < (1024 * 512));

			// Check if RefParse can process the data
			// Giving RefParse too much data hangs the system
			if (canProcess) {
				InputStream in = new ByteArrayInputStream(xml.getBytes());
				myBibRefs = SgmlDocumentReader.readDocument(in);
				in.close();
				refParse.process(myBibRefs, new Properties());
				refParse = null;
			}

			int index = 0;
			String[][] candidates = new String[citations.length][tagTypes.length + 1];

			// Check if the XML was too large (RefParse will cause memory
			// overflow copying arrays)
			if (!canProcess) {
				Run.error("XML file too large for RefParse, XML is " + xml.length() + "bytes");

				// Check if the process function succeeded
			} else if (myBibRefs.getAnnotations("bibRef").length != citations.length) {
				Run.error("RefParse external library did not correctly parse citation input");
				Run.error("-> Given " + citations.length + " citations as input, returned "
						+ myBibRefs.getAnnotations().length);
			} else {

				// Loop over QueriableAnnotation
				for (QueriableAnnotation an : myBibRefs.getAnnotations("bibRef")) {
					String[] relevant = new String[tagTypes.length + 2];

					for (QueriableAnnotation sub : an.getAnnotations()) {
						// Find relevant tags, store them in order
						for (int t = 0; t < tagTypes.length; t++) {
							if (sub.getType().equals(tagTypes[t])) {

								// Override previous values, keep the last
								relevant[t] = sub.toString();
							}
						}
					}
					// Add the original citation to the end relevant option list
					relevant[tagTypes.length] = citations[index];

					// Now the 'relevant' variable contains tokens which have
					// the highest probability of containing the journal
					// Run the selection procedure to select the correct token
					// from the array
					candidates[index++] = relevant;
				}
			}

			// Select the journals in the candidates
			journals = selectFromCandidates(candidates);
			myBibRefs.clear();

		} catch (Exception e) {
			FatalError("Could not parse citations");
			e.printStackTrace();
		}

		// Normalize input
		ArrayList<String> list = new ArrayList<String>();
		for (int i = 0; i < journals.length; i++) {
			if (journals[i] != null) {
				if (!journals[i].trim().equals("")) {
					String normal = NormalizeTask.normalizeJournal(journals[i]);
					if (!normal.equals("")) {
						list.add(normal);
					}
				}
			}
		}

		// Now it can well be that the normalizer has thrown away a lot of stuff
		// because it is not an actual journal
		// In this last attempt we try one more time to find journals based on
		// the database of known journals
		// We split the citation on [.,;] text [,.;] and run each part through
		// the normalizer, if only one part
		// matches, we use that as the journal. If two or more matches, we
		// ignore it. This method is less preferable
		// because journal abbreviations often contain a dot, but trial and
		// error has shown that the journal citations
		// that do not parse correctly often do NOT have a dot in the journal
		// notation, and hence this method might help
		if ((((float) list.size() / (float) citations.length) < threshold) || list.size() == 0) {

			// Holder for new list
			ArrayList<String> newlist = new ArrayList<String>();
			for (int j = 0; j < citations.length; j++) {

				// Remove dots from abbreviations, substitute year by delimiter,
				// split on known delimiters
				String[] parts = NormalizeTask.removeDotsFromAbbreviations(citations[j]).replaceAll("[0-9]{4}", ";")
						.split("[\\.\\,\\;]{1}");
				if (parts.length > 1) {
					for (int i = 0; i < parts.length; i++) {
						String part = parts[i].trim().replaceAll("^in ", "");
						if (part.startsWith("proc")) { // A part starting with
														// this is almost
														// certain to be a
														// proceeding
							newlist.add(NormalizeTask.simpleJournal(part));
						} else if (part.startsWith("jour")) { // A part starting
																// with this is
																// almost
																// certain to be
																// a journal
							newlist.add(NormalizeTask.simpleJournal(part));
						} else if (part.length() > 3) {
							String attempt = NormalizeTask.normalizeJournal(part, true);

							// The form "a and b" is often an author
							if (attempt.length() > 2 && !attempt.matches("^[a-z]{1}\\sand\\s[a-z]{1}$")) {

								newlist.add(attempt);
							}
						}
					}
				}
			}
			if (newlist.size() > list.size()) {
				list = newlist;
			}
		}

		// Remove all empty entries and remove numbering from journal titles
		// Also if the result is a single word, which is known to be an
		// abbreviation, ignore it
		ArrayList<String> nonempty = new ArrayList<String>();
		for (String j : list) {
			String journal = NormalizeTask.removeNumbering(j.trim());
			String test = NormalizeTask.simpleJournal(journal, false);
			if (!test.contains(" ")) {
				if (NormalizeTask.canAbbreviate(test)) {
					journal = ""; // Journal was something like "proceedings" or
									// another single abbreviation word
				}
			}
			if (!journal.equals("")) {
				nonempty.add(journal);
			}
		}

		// Fill string array
		String[] normalized = new String[nonempty.size()];
		for (int i = 0; i < nonempty.size(); i++) {
			normalized[i] = nonempty.get(i);
		}

		xml = "";
		myBibRefs.clear();
		myBibRefs.clearAttributes();
		return normalized;
	}

	/**
	 * Count the number of journal identifiers in a text
	 * 
	 * @param search
	 *            The text to count identifiers in
	 * @return The number of found identifiers
	 */
	public static int countJournalIdentifiersInText(String search) {
		int count = 0;
		search = " " + search.toLowerCase().replaceAll("[^a-z]", " ") + " ";
		for (String word : NormalizeTask.getAllAbbreviations()) {
			if (search.contains(" " + word + " ")) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Return a list of journals from a list of candidates. for each citation,
	 * the first not emtpy array item has the highest probability of being the
	 * journal
	 */
	public static String[] selectFromCandidates(String[][] candidates) {
		String[] journals = new String[candidates.length];

		// This counter contains how relevant each option is
		int[] relevancy = new int[tagTypes.length];
		int[] wordCount = new int[tagTypes.length];

		// Calculate the counts by iterating over all options
		for (String[] tokens : candidates) {
			for (int idx = 0; idx < tagTypes.length; idx++) {
				if (tokens[idx] != null) {
					if (!tokens[idx].equals("")) {

						// This token contains info, so this field is relevant
						relevancy[idx] = relevancy[idx] + 1;

						// Check if token contains one of the special words
						// if so, it is even more relevant :)
						String search = " "
								+ tokens[idx].toLowerCase().replace(".", "").replace(",", "").replace(";", "") + " ";
						wordCount[idx] = wordCount[idx] + countJournalIdentifiersInText(search);
					}
				}
			}
		}

		// Both this approach and the refParse parser heavily rely
		// on citations being consistent, meaning that all citations
		// in a document follow the same structure. The refParse paper
		// proves that this 100% the case for their test-set, and so
		// we assume this is true. Therefore if there is an error in refParse
		// the error will be consistent, and hence for a list of citations
		// the errorous citation will be in the same place. We select the place
		// with the highest probability, and if there are two places with the
		// same
		// probability, we select the first (note that the list was sorted on
		// relevance
		// in the first place).
		int highestWordCount = 0;
		int matchCount = 0;
		int index = -1;
		for (int idx = 0; idx < relevancy.length; idx++) {
			if (highestWordCount < wordCount[idx]) {
				highestWordCount = wordCount[idx];
				matchCount = relevancy[idx];
				index = idx;
			}
		}

		// We should select the journal from the candidate list
		if (index > -1 && highestWordCount > 0) {
			String[] journalInput = new String[matchCount];
			int journalIndex = 0;
			for (String[] entry : candidates) {
				if (entry[index] != null) {
					if (!entry[index].equals("")) {
						journalInput[journalIndex++] = entry[index];
					}
				}
			}
			journals = selectFromCandidates(journalInput);
		}

		// If more than 50% of the list contains single word entries
		// and the list is longer than 10 items, it is most probably
		// an error in the extract task and we are listing words, not journals
		int counter = 0;
		if (journals.length > 10) {
			for (String j : journals) {
				if (j != null) {
					if (!j.contains(" ")) {
						counter++;
					}
				}
			}
			if (((float) counter / (float) journals.length) > 0.5f) {
				journals = new String[0];
			}
		}

		return journals;
	}

	/*
	 * Return a list of journals from a list of candidates.
	 */
	public static String[] selectFromCandidates(String[] candidates) {
		ArrayList<String> journals = new ArrayList<String>();

		// Pre-process candidates
		ArrayList<String> preprocess = new ArrayList<String>();
		boolean allHasComma = true;
		int countJournalStart = 0;
		int countJournalEnd = 0;
		for (String candidate : candidates) {

			// First remove leading and tailing comma's and trim input
			String txt = candidate.replaceAll("^\\.\\,\\;\\s(.+)\\s\\.\\,\\;$", "$1").replace(";", ",").trim();

			preprocess.add(txt);

			// Check if every entry contains a comma
			if (!txt.contains(",")) {
				allHasComma = false;
			}

			// If this entry has a comma, check if journal is at end or start
			if (txt.contains(",")) {
				String[] parts = txt.split(",");
				if (countJournalIdentifiersInText(parts[0]) > 0) {
					countJournalStart++;
				}
				if (countJournalIdentifiersInText(parts[parts.length - 1]) > 0) {
					countJournalEnd++;
				}
			}

		}

		// The following is true for the input candidates
		// - Not every item has to contain a journal
		// - The journal in an item can have additional text before or after it
		// - items are consistently formatted
		for (String txt : preprocess) {

			// If there is a comma, we probably have more info than just the
			// journal
			if (txt.contains(",")) {

				String[] parts = txt.split(",");
				if (countJournalEnd > countJournalStart) {
					journals.add(parts[parts.length - 1].trim());
				} else if (countJournalStart > countJournalEnd) {
					journals.add(parts[0].trim());
				}

				// If there is no comma, and not all have a comma, we see this
				// as the final journal
			} else if (!allHasComma) {
				journals.add(txt.trim());
			}

		}

		// Remove everything from journals that contains an invalidator, and
		// clean it
		ArrayList<String> cleaned = new ArrayList<String>();
		int totalLength = 0;
		for (String j : journals) {
			boolean isValid = true;

			// Check if string contains an invalidator
			for (String invalid : invalidators) {
				if ((" " + j + " ").contains(invalid)) {
					isValid = false;
				}
			}

			// If input is a formula, it is not a journal
			if (FormulaCorrection.isFormula(j)) {
				isValid = false;
			}

			// If it is valid, store it.
			if (isValid) {
				String toAdd = j;
				for (String reg : cleanup) {
					toAdd = toAdd.trim().replaceAll(reg, "").trim();
				}

				// Add if not empty and contains letters
				if (!toAdd.trim().matches("^[^a-zA-Z]+$") && !toAdd.trim().equals("")) {
					cleaned.add(toAdd.trim());
					totalLength = totalLength + toAdd.trim().length();
				}
			}
		}

		// It can be the case that there are still errors in the data
		// The most obvious case being additional text before the journal
		// (tested by taking a random sample). We can fix this by looking
		// at the average string length and apply a special fix for the strings
		// that are longer than the average. For the fix we look at the
		// identifiers
		// And keep only the section after the identifier. If no identifier is
		// found
		// we keep the original text without modification.
		ArrayList<String> list = new ArrayList<String>();
		float avgLength = (float) totalLength / (float) cleaned.size();
		for (String cit : cleaned) {
			String fixed = cit;
			if (cit.length() > avgLength) {
				fixed = "";
				String[] parts = cit.split(" ");
				int found = -1;
				search: {
					for (int i = 0; i < parts.length; i++) {
						for (String m : NormalizeTask.getAllAbbreviations()) {
							if (parts[i].replace(".", "").trim().toLowerCase().matches(m)) {
								found = i;
								break search;
							}
						}
					}
				}

				// We have found a word and it is more than only the last word
				if (found > -1 && found != (parts.length - 1)) {
					for (int j = found; j < parts.length; j++) {
						fixed = fixed + " " + parts[j];
					}
				} else {
					fixed = cit;
				}
			}
			list.add(fixed.trim());
		}

		// Make final string array
		String[] result = new String[list.size()];
		int i = 0;
		for (String j : list) {
			result[i++] = j;
		}
		return result;
	}

	/**
	 * Please note that a task runs inside a thread. The RefParseAutomatic
	 * library is not thread-safe. Therefore, create a new object for each
	 * instance.
	 * 
	 * @return
	 */
	public static RefParseAutomatic init() {
		RefParseAutomatic refParse = null;
		try {
			refParse = new RefParseAutomatic();
			File myRefParseDataFolder = new File("RefParseData/");
			refParse.setDataProvider(new AnalyzerDataProviderFileBased(myRefParseDataFolder));
		} catch (Exception e) {
			FatalError(
					"Could not initialize Reference Parse Data for RefParse library, make sure /RefParseData/ folder exists");
		}
		return refParse;
	}

	/**
	 * This method outputs a fatal error And attempts to disconnect the current
	 * worker
	 */
	public static void FatalError(String msg) {
		Run.error(msg);
		if (worker != null) {
			worker.disconnect();
		}
	}

}

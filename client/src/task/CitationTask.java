package task;

import org.json.JSONArray;
import org.json.JSONObject;

import citerep.CiteRepResponse;
import citerep.TaskRunner;
import correction.BigTextCorrection;
import correction.BlockListCorrection;
import correction.Correction;
import correction.NumberedListCorrection;
import correction.TrimCorrection;

/**
 * CitationTask
 * 
 * @author Steven
 *
 */
public class CitationTask extends Task {

	// Keywords that identify the start of a reference section in a paper
	// (lowercase)
	public static final String[] delimiterReferences = { "references", "bronvermelding", "bronverwijzing",
			"bibliography", "bronnen", "reference list", "bibliografie", "referenties", "bronverwijzingen", "literatur",
			"literatuur", "literature", "literature cited", "literaturhinweise", "resource guide", "literatuurlijst",
			"references and notes", "references & notes" };

	// Keywords that identify the end of a reference section in a paper
	// Can optionally be prefixed with a chapter number, but should be at start
	// of a string
	public static final String[] delimiterAppendix = { "appendix", "bijlage", "bijlagen", "chapter", "afbeeldingen",
			"appendices", "summary", "motivation", "table", "figure", "fig.", "samenvatting", "summary", "section",
			"authors", "index", "acknowledgement", "notes", "noten" };

	// Keywords (lowercase) that might never exist and invalidate the entire
	// reference section
	public static final String[] referenceInvalidators = { "1. introduction", "1 introduction", "1 inleiding",
			"1. inleiding", ".........", "-------" };

	// List of corrections used to reparse citation array
	// Order is important, if correction meeds treshold, it will be executed
	public static Correction[] correctionList = {

			// Trim arbitrary text from start/end of string
			new TrimCorrection(),

			// Correct if numbered lists are incorrectly splitted
			new NumberedListCorrection(),

			// Correct if [author0-9] lists are incorrectly splitted
			new BlockListCorrection(),

			// Layout big text blobs, or return empty result set
			new BigTextCorrection() };

	// We count these special characters and sentences
	// to determine if a stringis a citation
	public static final char[] specialChars = { ']', '[', ')', '(', '-', '.', ',', '/', '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', '0' };

	public static final String[] specialStrings = { "pp", "vol", "part", "and", "no", "et al", " p.", "online",
			"available", "http://" };

	// Use this delimiter if PDF is empty (image)
	public static final String pdfEmpty = "@PDF_IS_EMPTY@";

	// The treshold below is used to find the first piece of text that has
	// not this amount of special characters vs normal characters in a line
	public static final float tresholdSpecialChars = 0.07f;

	// The Threshold which determines after how often we have found a citation
	// starting delimiter
	// we should ignore this approach and feed it to the secondChance method
	public static final int allowMaxDelimiterCount = 10;

	public CitationTask(TaskRunner runner) {
		super(runner);
	}

	/**
	 * Perform Text Extraction on PDF url Dowloads the PDF and extracts text
	 */
	@Override
	public void perform() {

		// Obtain paper ID and Plain Text from task
		JSONArray parameters = getJson().getJSONArray("param");
		int paperID = parameters.getInt(0);
		String plain = parameters.getString(1);

		// This object will contain the citations that were found
		JSONArray citations = new JSONArray();

		// This is the response object that will be send back to the server
		CiteRepResponse response = new CiteRepResponse(this);
		response.put("paperID", paperID);

		String[] papers = plain.split(ExtractTask.pdfSeparator);
		for (String paper : papers) {

			// Attempt to obtain citations
			JSONArray cits = getCitationsFromPlainText(paper);

			// If we have found citations, put them into the array
			if (cits.length() != 0) {
				for (int i = 0; i < cits.length(); i++) {
					citations.put(cits.get(i));
				}

			}
		}

		// These are the identified citations
		response.put("paperCit", citations);

		// Now also perform the identify and normalize task
		String[] input = new String[citations.length()];
		for (int i = 0; i < citations.length(); i++) {
			input[i] = citations.getString(i);
		}

		// Find the journals and normalize them
		String[] journals = IdentifyTask.getJournalsFromCitations(input);
		JSONArray result = new JSONArray();
		for (String jour : journals) {
			JSONObject jobj = new JSONObject();
			jobj.put("j", jour);
			String n = NormalizeTask.normalizeJournal(jour);
			if (!n.equals("")) {
				jobj.put("n", NormalizeTask.simpleJournal(n, true));
			} else {
				jobj.put("n", NormalizeTask.simpleJournal(jour, true));
			}
			result.put(jobj);
		}

		// Complete and send response back
		response.put("paperJour", result);
		complete(response);
		getWorker().notifyThreadClosed();
	}

	/**
	 * Find citation section in free text
	 * 
	 * @param plain
	 *            The free text to search for citation section
	 * @return (JSONArray) The citation section, separated as good as possible
	 */
	public static JSONArray getCitationsFromPlainText(String text) {

		// The text parser from PDFbox adds a delimiter between each paragraph
		// and line
		// Use these delimiters to split and search for citations
		String[] lines_raw = text.split(ExtractTask.delimiterParagraph);
		JSONArray citations = new JSONArray();
		boolean processCorrections = true;

		// If the text is empty, return special-token at once (PDF is empty)
		if (text.replace(ExtractTask.delimiterLine, "").replace(ExtractTask.delimiterParagraph, "").replace("\n", "")
				.replace("\r", "").trim().length() < 10) {
			citations.put(pdfEmpty);

		} else {

			// Did we find the reference section yet?
			Boolean hasFoundReferences = false;

			// Count how many delimiters inidicating a reference section exist
			// Generally only take the last delimiter
			int foundReferenceDelimiterCount = 0;
			int totalReferenceSections = 0;
			for (String line : lines_raw) {
				String part = line.toLowerCase();

				// Check reference section delimiter is found
				for (String title : delimiterReferences) {
					if (StringHasSentenceEndsWith(part, title)) {
						// Run.log("--FOUND DELIM ["+title+"] in: "+part);
						foundReferenceDelimiterCount++;
					}
				}
			}

			totalReferenceSections = foundReferenceDelimiterCount;

			// If we have found many citation sections, this method did not work
			if (foundReferenceDelimiterCount <= allowMaxDelimiterCount) {

				// Loop over the lines
				parse: {
					for (String line : lines_raw) {

						// The part of the text we are looking at
						String part = line.toLowerCase();

						// We have found the reference section
						if (!hasFoundReferences) {

							// Check reference section is found
							for (String title : delimiterReferences) {
								if (StringHasSentenceEndsWith(part, title)) {
									// Run.log("FOUND DELIM: "+title);
									hasFoundReferences = ((--foundReferenceDelimiterCount) == 0);

									// Use the stuff after "references" for the
									// rest of the process
									if (hasFoundReferences) {

										String[] tmp = FixTitleInString(part, title)
												.split(title + ExtractTask.delimiterLine, 2);
										part = tmp[1];
									}

								}
							}
						}

						// Cleanup the text
						part = part.replace(ExtractTask.delimiterLine, " ").replace("\t", " ").replace("\r", " ")
								.replace("\n", " ").replaceAll("\\s\\s+", " ").trim();

						// We have found a reference
						// Throw away short sentences such as numbers and
						// markup.
						if (hasFoundReferences && (part.replace(" ", "").length() > 10)) {

							// Check if we have reached the end of the reference
							// section
							for (String end : delimiterAppendix) {
								if (part.matches("^([0-9\\. ]*)" + end + "(.*)$")) {
									// Run.log("FOUND END DELIM "+end+" in:
									// "+part);
									break parse;
								}
							}
							citations.put(part);
						}
					}
				}

			}

			// Because there was more than 1 reference section found it could be
			// that
			// We have processed the wrong section, so calculate a second chance
			// as well
			// This will work well with numbered lists
			if (totalReferenceSections > 1) {
				JSONArray second = secondChance(text);
				/*
				 * if(DEBUG) { Run.log("SecondChance() "+second.length()); }
				 */
				if (second.length() > citations.length()) {
					hasFoundReferences = true;
					processCorrections = false;
					citations = second;
				}
			}

			// We have not found the reference section
			// Check it again for numbered lists
			if (!hasFoundReferences) {
				citations = secondChance(text);
				/*
				 * if(DEBUG) { Run.log("SecondChance() "+citations.length()); }
				 */
				processCorrections = false; // Do not process any corrections
			}

		}

		// If there are citations, clean them up
		if (citations.length() > 0 && processCorrections) {
			citations = AttemptCleanup(citations, text, lines_raw);
		}

		return citations;

	}

	/**
	 * Private helper to prepare title search in string
	 * 
	 * @param part
	 *            The part to prepare
	 * @return The prepared string
	 */
	private static String FixTitleInString(String part, String title) {

		// Make input lowercase and allow title to be suffixed with a :
		part = part.toLowerCase().replace(title + ":", title);
		part = part.replaceAll(title + "\\s+" + ExtractTask.delimiterLine, title + ExtractTask.delimiterLine);

		// It could also be that the title has multiple words as indicator, for
		// instance "reference list"
		// And that the whitesapace in this title is actually indexed as a line
		// character, fix this
		part = part.replaceAll(title.replace(" ", ExtractTask.delimiterLine), title);
		return part;
	}

	/**
	 * Check if a string ends with a delimiter
	 * 
	 * @param part
	 *            The string to search in
	 * @param title
	 *            The title to search for
	 * @return true if string has a sentence ending with title
	 */
	public static boolean StringHasSentenceEndsWith(String part, String title) {
		boolean contains = false;
		part = FixTitleInString(part, title);
		if (part.contains(title + ExtractTask.delimiterLine)) {

			// Check if char before match is either the line beginning or a
			// whitespace
			// This way we do not match for instance "preferences" when looking
			// for "references"
			// int length = (title+ExtractTask.delimiterLine).length();
			int start = part.indexOf(title + ExtractTask.delimiterLine);
			if (start > 0) {
				contains = (!(String.valueOf(part.charAt(start - 1))).matches("^[A-Za-z]$"));
				if (!contains) {
					// Run.log("char: ["+part.charAt(part.length()-length-2)+"]
					// in ["+part+"]");
				}
			} else {
				contains = true;
			}
			contains = true;
		}

		return contains;
	}

	/**
	 * Attempt cleanup of JSON array
	 * 
	 * @param arr
	 *            The array to cleanup
	 * @return The cleaned array
	 */
	private static JSONArray AttemptCleanup(JSONArray arr, String plain, String[] lines) {
		// First look through all the corrections and determine if cleanup is
		// needed
		corr: {
			for (Correction correction : correctionList) {

				// Check if this correction applies
				if (correction.applies(arr, plain, lines)) {
					arr = correction.fix(arr);

					if (!correction.allowOtherCorrections()) {
						break corr;
					}
				}
			}
		}

		return arr;
	}

	/**
	 * Attempt once again to obtain references from free-text We known that the
	 * previous procedure failed, therefore take more risks this time
	 * 
	 * @param raw
	 *            The raw text to clean
	 * @return The cleaned array
	 */
	private static JSONArray secondChance(String raw) {

		// Run the numbered list correction on the entire plain text
		NumberedListCorrection correction = new NumberedListCorrection();
		return correction.fix(raw);
	}

	/**
	 * Check if a string is a citation by counting the amount of special
	 * characters Also the string must have letters in it (not only be numbers
	 * and equations)
	 * 
	 * @param line
	 *            The line to look at
	 * @return true if this line is a citation
	 */
	public static boolean stringIsCitation(String line) {
		boolean match = false;

		// Line must have a-z characters in it
		if (line.trim().length() > 0 && line.matches("^.+[A-Za-z]+.+$")) {
			int count = countSpecialCharacters(line);
			match = ((((float) count) / ((float) line.length())) >= tresholdSpecialChars);
		}
		return match;
	}

	/*
	 * Local helper to count amount of special characters in a string
	 */
	private static int countSpecialCharacters(String s) {
		int counter = 0;
		s = s.toLowerCase();

		// Count special characters
		for (char m : specialChars) {
			for (int i = 0; i < s.length(); i++) {
				if (s.charAt(i) == m) {
					counter++;
				}
			}
		}

		// Count special strings
		for (String c : specialStrings) {
			if (s.contains(c)) {
				counter += c.length();
			}
		}
		return counter;
	}

}

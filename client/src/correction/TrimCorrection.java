package correction;

import org.json.JSONArray;
import task.CitationTask;

/**
 * TrimCorrection
 * 
 * @author Steven
 *
 */
public class TrimCorrection extends Correction {

	// Simply always apply this correction, tests have shown that it will
	// never remove too much text. Make it part of the overall algorithm
	public static float threshold = 0.00f;

	// We check for these character sequences to find when a citation has
	// actually ended
	protected final String[] specialStrings = { "was born in", "received the", "the m.s.c.", "the m.sc.",
			"m.s.c. degree", "m.sc. degree", "from the university", "at the department", "this appendix",
			"the proof of" };

	/**
	 * Check if this correction applies to given JSONArray
	 * 
	 * @param arr
	 *            The JSON array to use
	 * @return True if this correction applies
	 */
	public boolean applies(JSONArray arr, String plain, String[] lines) {
		boolean applies = true;
		super.threshold = threshold;

		/*
		 * Please see paper: we made this correction part of our overall
		 * algorithm by always enabling it.
		 * 
		 * //Check if we meet the threshold int charcountAll =
		 * plain.replace(ExtractTask.delimiterLine, "")
		 * .replace(ExtractTask.delimiterParagraph, "")
		 * .replace(ExtractTask.pdfSeparator, "").trim().length(); int
		 * charCountCits = 0; for(int i=0;i<arr.length();i++) { charCountCits +=
		 * arr.getString(i).length(); }
		 * 
		 * //This correction applies if citation part takes up more than 20% of
		 * all text in a document if((((float)charCountCits) /
		 * ((float)charcountAll)) >= threshold && arr.length()>1) { applies =
		 * true; }
		 * 
		 * 
		 * //Correction also applies if a stopper is found (special string)
		 * for(int i=0;i<arr.length();i++) { for(String check:specialStrings) {
		 * if(arr.getString(i).contains(check)) { applies = true; } } }
		 */

		return applies;
	}

	/**
	 * Fix the input JSONArray based on the algorithm for this fix
	 * 
	 * @param array
	 *            Input to fix
	 * @return Fixed output
	 */
	public JSONArray fix(JSONArray array) {
		array = super.fix(array);

		// The resulting JSON array
		JSONArray result = new JSONArray();

		// ----- Option 1, arbitrary text BEFORE the citation list -----
		// It could well be the case that there is additional text before the
		// reference list
		// It is then often the case that there is a sentence which ends with a
		// reference delimiter
		// We find it, and discard everything that comes before.
		boolean foundTextBefore = false;
		for (int i = 0; i < array.length(); i++) {
			String line = array.getString(i);
			if (!foundTextBefore) {
				for (String end : CitationTask.delimiterReferences) {
					if (line.endsWith(end)) {
						foundTextBefore = true;
					}
				}
			} else {
				result.put(line);
			}
		}
		if (foundTextBefore) {
			array = result;
		}

		// Clear the resulting array, results of this step are in array
		result = new JSONArray();

		// ----- Option 2, arbitrary text AFTER the citation list -----
		// It could be the case that there is bad text AFTER the reference list
		// In that case the closing delimiter was not found and other text was
		// appended
		// We look for a big piece of text without much special characters such
		// as ][)(,.-:/
		// Also if a string has a number in the first two positions, probably
		// because of [1],[2] etc, skip it
		boolean foundTextAfter = false;
		int previousMatchingLine = -2;
		for (int i = 0; i < array.length(); i++) {
			String line = array.getString(i);

			if (!foundTextAfter) {
				result.put(line);
				if (line.length() > 2) {
					if (i > (array.length() / 4) // We are looking at text at
													// the END of the reference
													// section, so search in the
													// last one fourth
							&& !line.trim().matches("^[0-9\\[\\(\\-]{1}.*")) { // Line
																				// does
																				// not
																				// start
																				// with
																				// a
																				// number
																				// (or
																				// part
																				// of
																				// list)
						if (!CitationTask.stringIsCitation(line)) {
							if (previousMatchingLine == i - 1) {
								foundTextAfter = true;
								result.remove(i - 1);
							}
							previousMatchingLine = i;
						}
					}
				}
			}
		}
		if (foundTextAfter) {
			array = result;
		}

		// Clear the resulting array, results of this step are in array
		result = new JSONArray();

		// It could also be that at the end of a reference section another text
		// is appended but this text
		// does contain many special characters, so it is not truncated.
		// However, there are clear indicators
		// which aid in finding this plain text, if a text contains them, trim
		// from the beginning of that sentence
		// markers are texts such are 'was born in', 'he received the', 'm.sc.
		// degree', 'at the department'
		boolean foundTextMidLine = false;
		for (int i = 0; i < array.length(); i++) {
			String line = array.getString(i);

			// We have not yet found text mid line
			if (!foundTextMidLine) {

				// Check if this line contains word sequences we are looking for
				findspecial: {
					for (String find : specialStrings) {
						String search = find.replace("\\.", "");
						String lookup = line.toLowerCase().replace(find, search);

						// If so, only add the sentences before that word
						// occurred
						if (lookup.contains(search)) {
							foundTextMidLine = true;
							String[] parts = lookup.split("\\.");
							String add = "";
							for (String p : parts) {
								if (!p.toLowerCase().contains(search)) {
									add = add + p + ".";
								} else {
									line = add;
									break findspecial;
								}
							}
						}
					}
				}

				// Double check the last sentence that is added
				boolean foundinline = false;
				if (foundTextMidLine) {
					for (String find : specialStrings) {
						if (line.toLowerCase().contains(find)) {
							foundinline = true;
						}
					}
				}

				// only add if no keywords are left in it
				if (!foundinline) {
					result.put(line);
				}
			}
		}
		if (foundTextMidLine) {
			array = result;
		}

		return array;
	}

}

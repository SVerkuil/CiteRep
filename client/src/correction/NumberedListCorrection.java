package correction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import task.CitationTask;
import task.ExtractTask;

/**
 * NumberedListCorrection
 * 
 * @author Steven
 *
 */
public class NumberedListCorrection extends Correction {

	// The pattern that defines this correction
	protected final String pattern = "^\\[?\\(?f?([0-9\\-]+)g?\\]?\\)?\\.?\\s?[^0-9]+.+$";

	// The pattern which we use to replace a number with
	// ExtractTask.delimiterLine+number
	protected final String patternReplace = "(\\[?\\(?f?([0-9]+)g?\\]?\\)?\\.?)\\s";

	// At least this amount of sentences must match pattern
	public static float threshold = 0.1f;

	// If this correction applies, stop the other corrections from happening
	protected Boolean allowOtherCorrections = false;

	// The latest plain text (store it)
	String plain = "";

	/**
	 * Check if this correction applies to given JSONArray
	 * 
	 * @param arr
	 *            The JSON array to use
	 * @return True if this correction applies
	 */
	public boolean applies(JSONArray arr, String plain, String[] lines) {
		this.plain = plain;
		super.threshold = threshold;
		super.allowOtherCorrections = this.allowOtherCorrections;

		// Helper variables
		boolean applies = false;
		boolean allMatchRegex = true;
		int matchCount = 0;

		// Check if correction applies
		for (int i = 0; i < arr.length(); i++) {
			String line = arr.getString(i).trim();
			if (line.matches(pattern)) {
				matchCount++;
			} else {
				allMatchRegex = false;
			}
		}

		if (
		// Either 50% of the lines equals threshold
		(((float) matchCount / (float) arr.length()) >= threshold) ||
				// There is only one line, and it matches regex
				(allMatchRegex && arr.length() == 1)) {
			applies = true;
		}

		return applies;
	}

	/**
	 * Fix the input JSONArray based on the algorithm for this fix
	 * 
	 * @param array
	 *            Input to fix
	 * @return Fixed output
	 */
	public JSONArray fix(JSONArray input) {
		input = super.fix(input);
		JSONArray output = fix(this.plain); // Always work on full plain text,
											// columns might be misaligned and
											// lists mixed up

		// If the output is less than 50% of the input size (or emtpy for that
		// matter)
		// Probably we accedently substitued some numbered list for author based
		// sorted list
		// And hence it is better to return the original input
		if ((output.length() * 2) < input.length()) {
			return input;
		}

		// We fixed something, so return it
		return output;

	}

	/**
	 * Fix the input JSONArray based on the algorithm for this fix Please note,
	 * this is also called in CitationTask.secondChance()
	 * 
	 * @param String
	 *            input to fix
	 * @return Fixed output
	 */
	public JSONArray fix(String raw) {
		return fix(raw, 0, 0);
	}

	/**
	 * Fix the input JSONArray based on the algorithm for this fix Please note,
	 * this is also called in CitationTask.secondChance()
	 * 
	 * @param String
	 *            input to fix
	 * @param start
	 *            The start number for the numbered list
	 * @param ignore
	 *            The number of times to ignore the first occurrence of the
	 *            start number
	 * @return Fixed output
	 */
	public JSONArray fix(String raw, int start, int ignore) {
		JSONArray result = new JSONArray();
		int ignoreValue = ignore;

		// Numbered lists are of great help
		// Check if we can find [delimiterParagraph|delimiterLine] followed by
		// numbering [1],[2], (1),(2) or 1, 2
		// if so, use that as delimiters, split sentences again. There may be at
		// most 2 other numbers starting with
		// the above delimiter+number logic before we expect the next
		// incrementing number to be present.

		// Use this regex to find number
		Pattern p = Pattern.compile(pattern);

		// Cleanup the raw input
		String input = raw.replace(ExtractTask.delimiterParagraph, ExtractTask.delimiterLine);

		// Common parse error where 1 is parsed as i or l
		input = input.replace("[i", "[1").replace("i]", "1]").replace("[l", "[1").replace("l]", "1]");

		// Split on delimiter
		String[] lines = input.split(ExtractTask.delimiterLine);

		// Arraylist to hold results
		ArrayList<ArrayList<String>> list = new ArrayList<ArrayList<String>>();

		int prevnumber = start; // Counter per list (if paper contains multiple
								// lists with numbers 1-xxx)

		int totalnumber = start; // Counter for total list, if paper contains
									// multiple lists with consecutive numbers
									// 1-x x-y y-z etc)
		int skipcount = 0;
		ArrayList<String> curlist = new ArrayList<String>();
		for (String line : lines) {

			// Skip completely empty lines
			line = line.trim();
			if (line.replaceAll("[A-Za-z0-9]", "").trim().equals("")) {
				continue;
			}

			// Check if this sentence starts with an incrementing number
			Matcher m = p.matcher(line);

			// If we have found a number (from list) at start of the line
			if (m.find()) {

				// Obtain the integer value from this number
				// Sometimes citations are concatenated (x-y), take last num (y)
				// in this case
				int number = -1;
				String group = m.group(1);
				String num = group;
				if (group.contains("-")) {
					String[] parts = group.split("\\-", 2);
					num = parts[1];
				}
				try {
					number = Integer.parseInt(num);
				} catch (Exception e) {
				}

				// Check if it is an increment from the previous number, if so,
				// add it
				if (number == prevnumber + 1 || number == totalnumber + 1) {

					if (--ignore < 0) { // Sometimes there are multiple 1-x
										// reference lists in a doc
										// If this is a subsequent scan, ignore
										// the first occurrence
						curlist.add(line.trim());
						prevnumber = number;
						totalnumber = number;
						skipcount = 0;
					}
				} else {

					// Otherwise, add text to the previous citation entry
					if (!curlist.isEmpty()) {
						curlist.set(curlist.size() - 1, curlist.get(curlist.size() - 1) + " " + line);
					}
					skipcount++;
				}
			} else {

				// Otherwise, add text to the previous citation entry
				if (!curlist.isEmpty()) {
					curlist.set(curlist.size() - 1, curlist.get(curlist.size() - 1) + " " + line);
				}
				skipcount++;
			}

			// If we have found 9 lines without incrementing number, stop
			if (skipcount == 9) {

				if (curlist.size() >= 2) { // List must have at least 3
											// references

					// Validate the list, sometimes there are other numbered
					// lists in documents, which are not citations at all
					boolean validateList = true;
					int isCitCounter = 0; // Count presumed citations
					int isFormCounter = 0; // Count presumed formulas
					checkList: {
						for (int i = 0; i < curlist.size(); i++) {
							String cit = curlist.get(i);

							// Sometimes a list of numbers is seen as a
							// citation.
							// All citations are assumed to have letters in it
							// Does not have letters, this is probably a list of
							// pure numbers
							if (!cit.matches("^.+[A-Za-z]+.+$")) {
								validateList = false;
								break checkList;
							}

							// Also check if this string passes the citation
							// check
							// Which means there must be a minimum threshold of
							// special characters in the string
							if (CitationTask.stringIsCitation(cit)) {
								isCitCounter++;
							}

							// Also check if it is not a formula.
							if (FormulaCorrection.isFormula(cit)) {
								isFormCounter++;
							}

							// Also check if there are invalid words in the list
							for (String invalid : CitationTask.referenceInvalidators) {
								if (cit.toLowerCase().contains(invalid)) {
									validateList = false;
								}
							}
						}
					}

					// We require half of the list to pass our citation test
					if (isCitCounter <= (curlist.size() / 2)) {
						validateList = false;
					}

					// If more than one third of the list is a formula, discard
					// it
					if (isFormCounter >= (curlist.size() / 3)) {
						validateList = false;
					}

					// If all checks passed, store this list
					if (validateList) {
						list.add(curlist);
					}
				}
				curlist = new ArrayList<String>();
				skipcount = 0;
				prevnumber = start;
			}
		}

		// If we have citation in this list (a document can have multiple
		// citation lists)
		// Then store it to the main list
		if (curlist.size() > 0) {
			list.add(curlist);
		}

		// Empty the main list into the JSON object
		for (ArrayList<String> lst : list) {
			for (String cit : lst) {
				result.put(cit.toLowerCase());
			}
		}

		// Now it could be that pages are mixed up because of column layout.
		// We have for sure found the list 1-x but might have missed x-y because
		// it is
		// before 1-x in the text (due to column parse errors). Therefore scan
		// again
		if (start != totalnumber) {
			JSONArray test = fix(raw, totalnumber, ignoreValue);
			if (test.length() > 0) {
				for (int i = 0; i < test.length(); i++) {
					result.put(test.getString(i));
				}
			}
		}

		// Check if there is another numbered list which starts with startValue
		// Some documents have a numbered citation list at the end of each
		// chapter
		// Hence there are multiple lists numbered 1-xxx in a single document
		if (ignoreValue == 0) {
			boolean search = true;
			int ignoreCount = ignoreValue;
			while (search) {
				JSONArray test = fix(raw, totalnumber, ++ignoreCount);
				if (test.length() > 0) {
					for (int i = 0; i < test.length(); i++) {
						result.put(test.getString(i));
					}
				} else {
					search = false; // Stop searching
				}
			}

		}

		// It can be the case that the result contains duplicate items because
		// multiple lists overlap
		// Therefore deduplicate it
		HashSet<String> dedup = new HashSet<String>();
		JSONArray fin = new JSONArray();
		for (int i = 0; i < result.length(); i++) {
			String cit = result.getString(i).trim();
			if (!dedup.contains(cit)) {
				fin.put(cit);
				dedup.add(cit);
			}
		}

		return fin;

	}

}

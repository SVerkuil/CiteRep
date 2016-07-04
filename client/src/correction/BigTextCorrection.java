package correction;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;

/**
 * BigTextCorrection
 * 
 * @author Steven
 *
 */
public class BigTextCorrection extends Correction {

	// The pattern that defines this correction
	protected String pattern = "^.{1000,}$";

	// Patterns for year matching
	protected String yearPatternBraces = "\\([0-9]{4}\\)";
	protected String yearPatternPlain = "[0-9]{4}";

	// At least this amount of sentences must match pattern
	// Set to 0.001 means that if at least one sentence has 1000 chars
	// We proceed with this correction
	public static float threshold = 0.05f;

	/**
	 * Check if this correction applies to given JSONArray
	 * 
	 * @param arr
	 *            The JSON array to use
	 * @return True if this correction applies
	 */
	public boolean applies(JSONArray arr) {
		super.pattern = this.pattern;
		super.threshold = threshold;
		return super.applies(arr);
	}

	/**
	 * Fix the input JSONArray based on the algorithm for this fix The
	 * BigTextCorrection fixes entries with more than 1000 chars
	 * 
	 * @param array
	 *            Input to fix
	 * @return Fixed output
	 */
	public JSONArray fix(JSONArray array) {
		array = super.fix(array);
		JSONArray result = new JSONArray();

		// Assuming this correction applies, we have a large piece
		// of text. Concatenate all text and attempt to split again
		String bigText = "";
		for (int i = 0; i < array.length(); i++) {
			bigText = bigText + " " + array.getString(i);
		}
		bigText = bigText.trim();

		// We have a big blob of text, with no numbering
		// We assume each citation has a publication year in it, and use it as
		// delimiter
		// Since there can be multiple years, first attempt to split with
		// surrounding ()
		// Allow split with (year) to differ at most 30% before falling back to
		// plain year without ()
		String[] partsWithBraces = bigText.split(yearPatternBraces);
		String[] partsWithoutBraces = bigText.split(yearPatternPlain);
		String[] parts = {};
		ArrayList<String> years = new ArrayList<String>();

		// Split on year with bracec e.g. (2012)
		if (((float) partsWithBraces.length / (float) partsWithoutBraces.length) > 0.3f) {
			parts = partsWithBraces;
			Matcher m = Pattern.compile("(" + yearPatternBraces + ")").matcher(bigText);
			while (m.find()) {
				years.add(m.group());
			}

			// Split on year without braces e.g. 2012
		} else {
			parts = partsWithoutBraces;
			Matcher m = Pattern.compile("(" + yearPatternPlain + ")").matcher(bigText);
			while (m.find()) {
				years.add(m.group());
			}
		}

		// Now, if the format were to be [author] (year) text [author] (year)
		// text
		// The author of a citation would be at the end of the previous string.
		// This also means that the first split only contains [author], we check
		// this by validating
		// That the author is indeed a name, and hence no numbers are found in
		// this string
		if (parts.length > 0 && years.size() == (parts.length - 1)) {

			if (!parts[0].matches(".*[0-9]+.*$")) {

				// List with all citations and current citation
				String citation = parts[0];

				// Loop over all the lines structured [text1] [author2]
				for (int i = 1; i < parts.length; i++) {

					// Add back the year which was used to split earlier
					citation = citation + years.get((i - 1));

					// Backtrack on string
					boolean search = true;
					int curPos = parts[i].length();
					while (--curPos >= 0 && search) {
						search = (!String.valueOf(parts[i].charAt(curPos)).matches("^[0-9\\)\\(\\]\\[\\'\\\"]+$"));
					}

					// Backtrack succeeded, finish citation and start new
					if ((curPos) > 0 && (curPos + 3 < parts[i].length())) {

						// Correct for --curpos, substring starts at -1 and we
						// include the char itself
						String lastPart = parts[i].substring(0, curPos + 3);
						String authors = parts[i].substring(curPos + 3);

						// Store the resulting citation
						result.put((citation + " " + lastPart).trim());
						citation = authors;

						// Backtrack failed, we probably will concatenate
						// multiple
						// references together
					} else {
						citation = citation + " " + parts[i];
					}

				}
				result.put((citation).trim());
			}
		}

		// Note, this check will return the original input if the fix can not be
		// performend
		// Please note that we still have a big blob of 1000 characters
		return (result.length() == 0) ? array : result;
	}

}

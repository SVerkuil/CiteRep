package correction;

import org.json.JSONArray;

/**
 * FormulaCorrection
 * 
 * Please note this correction is left out of the correction chain However, its
 * isFormula() method is called from IdentifyTask
 * 
 * @author Steven
 *
 */
public class FormulaCorrection extends Correction {

	// If our indexer has marked more than 20% of all lines as formulas
	// in a document as citations, we enable this correction
	public static float threshold = 0.2f;

	// These characters are often used in formulas
	protected static final char[] formulaCharacters = { ',', '(', ')', '=', '{', '}', '%', '*', '-', '$', '+', '/',
			'x' };

	// These words are often used in chemistry and math formulas
	protected static final String[] formulaStrings = { "thyl", "amino", "hoxy", "chlor", "oxy", "iso", "dehyde", "meth",
			"propa", "tography", "sin", "cos", "tan", "for all", "index", "mm", "cm3", "mm3", "gr", "x(", "y(", "t(",
			"(t)", "end for", ":=", "return", "value", "end if", "else", "set" };

	// The treshold below is used to find the first piece of text that has
	// not this amount of special characters vs normal characters in a line
	public static final float tresholdSpecialChars = 0.1f;

	/**
	 * Check if this correction applies to given JSONArray
	 * 
	 * @param arr
	 *            The JSON array to use
	 * @return True if this correction applies
	 */
	public boolean applies(JSONArray arr, String[] lines) {
		int formulacounter = 0;
		super.threshold = threshold;

		// Check if number of presumed formulas reaches threshold value
		for (int i = 0; i < arr.length(); i++) {
			if (isFormula(arr.getString(i))) {
				formulacounter++;
			}
		}

		return (((float) formulacounter) / (float) arr.length() > threshold);
	}

	/**
	 * Returns true if amount of characters in string matching formula
	 * characters is above threshold
	 * 
	 * @param line
	 *            The line to check
	 * @return True if this string is likely a formula
	 */
	public static boolean isFormula(String line) {
		int counter = 0;

		// Check how often specific characters occur
		for (char m : formulaCharacters) {
			for (int i = 0; i < line.length(); i++) {
				if (line.charAt(i) == m) {
					counter++;
				}
			}
		}

		// Check how often specific substring occurs
		for (String substring : formulaStrings) {
			int idx = 0;
			while ((idx = line.indexOf(substring, idx)) != -1) {
				idx++;
				counter += substring.length();
			}
		}

		// Check if the threshold is met
		return (((float) counter / (float) line.length()) >= tresholdSpecialChars);
	}

}

package correction;

import org.json.JSONArray;

import citerep.Run;

/**
 * Superclass for a Correction
 * 
 * @author Steven
 *
 */
public class Correction {

	// Pattern for regex based corrections
	protected String pattern = "";

	// Threshold value for corrections
	public float threshold = 0.75f;

	// Allow other corrections after this correction?
	protected Boolean allowOtherCorrections = true;

	/**
	 * The threshold, which percentage of all citations must match the pattern
	 * for this correction to apply?
	 * 
	 * @return The percentage
	 */
	public float threshold() {
		return threshold;
	}

	/**
	 * Obtain the pattern that defines this correction
	 * 
	 * @return The regex pattern
	 */
	protected String getPattern() {
		return pattern;
	}

	/**
	 * Fix the input JSONArray based on the algorithm for this fix
	 * 
	 * @param array
	 *            Input to fix
	 * @return Fixed output
	 */
	public JSONArray fix(JSONArray array) {
		Run.log("[DEBUG] Applying correction " + this.getClass().getName(), 3);
		return array;
	}

	/**
	 * Check if this correction applies to given JSONArray
	 * 
	 * @param arr
	 *            The JSON array to use
	 * @return True if this correction applies
	 */
	public boolean applies(JSONArray arr, String plain, String[] lines) {
		return applies(arr, lines);
	}

	/**
	 * Check if this correction applies to given JSONArray
	 * 
	 * @param arr
	 *            The JSON array to use
	 * @return True if this correction applies
	 */
	public boolean applies(JSONArray arr, String[] lines) {
		return applies(arr);
	}

	/**
	 * Check if this correction applies to given JSONArray
	 * 
	 * @param arr
	 *            The JSON array to use
	 * @return True if this correction applies
	 */
	public boolean applies(JSONArray arr) {
		int counter = 0;

		// For each correction, match pattern in line
		for (int i = 0; i < arr.length(); i++) {
			String line = arr.getString(i);
			if (line.matches(pattern)) {
				counter++;
			}
		}

		// If counter surpasses threshold, this correction applies
		return (((float) counter / (float) arr.length()) >= threshold);
	}

	public boolean allowOtherCorrections() {
		return allowOtherCorrections;
	}

}

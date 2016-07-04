package correction;

import org.json.JSONArray;

/**
 * BlockListCorrection
 * 
 * @author Steven
 *
 */
public class BlockListCorrection extends Correction {

	// The pattern that defines this correction
	protected final String pattern = "^\\[[A-Za-z0-9]*[a-zA-Z]+[A-Za-z0-9]*\\].*$";

	// If many sentences start with [a-z0-9], use this correction
	public static float threshold = 0.1f;

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
	 * Fix the input JSONArray based on the algorithm for this fix
	 * 
	 * @param array
	 *            Input to fix
	 * @return Fixed output
	 */
	public JSONArray fix(JSONArray array) {
		array = super.fix(array);
		JSONArray result = new JSONArray();

		// Check if there are citations
		if (array.length() > 0) {
			String citation = array.getString(0);

			// Loop over the citations
			for (int i = 1; i < array.length(); i++) {
				String line = array.getString(i).trim();

				// If we have a line with characters
				if (line.length() > 0) {

					// If the line starts with [, store it
					if (line.charAt(0) == '[') {
						result.put(citation);
						citation = line;

						// Otherwise append
					} else {
						citation = citation + " " + line;
					}
				}
			}
			result.put(citation);
		}

		if (result.length() == 0) {
			result = array;
		}
		return result;
	}
}

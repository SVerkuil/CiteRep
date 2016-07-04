package test;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import citerep.Run;

public class Test extends Run {

	public static String url = "";
	public static String passphrase = "";
	public static String name = "";

	/**
	 * Run CiteRep in Test Mode
	 */
	public static void main(String[] args) {

		// Process PDF documents in /input
		(new PdfTest()).perform();

		// Run benchmark using CiteRepTest set
		(new CiteRepTest()).perform();

	}

	/**
	 * Implemented by extending classes
	 */
	public void perform() {
	}

	/**
	 * Run benchmark on JSON string input
	 * 
	 * @param input
	 */
	public static void benchmark(String input) {
		benchmark(new JSONArray(input));
	}

	/**
	 * Method to benchmark citerep results against standard Expects JSON array of
	 * objects. Each object must contain - citrep_normalized Containing an array
	 * of normalized items - standard_normalized The array to check citerep
	 * against
	 * 
	 * @param json
	 *            The json to benchmark
	 */
	public static void benchmark(JSONArray json) {

		// Counters
		float precOverall = 0.0f;
		float recOverall = 0.0f;
		float totalSize = 0.0f;

		for (int i = 0; i < json.length(); i++) {

			// The paper to process
			JSONObject paper = json.getJSONObject(i);

			// This set contains citrep results
			HashMap<String, Integer> citrep = new HashMap<String, Integer>();
			JSONArray tmp = null;
			int citSize = 0;
			if (paper.has("citrep_normalized")) {
				tmp = paper.getJSONArray("citrep_normalized");
				for (int j = 0; j < tmp.length(); j++) {
					String jour = tmp.getString(j).replaceAll("^the ", "");
					int count = 1;
					if (citrep.containsKey(jour)) {
						count = citrep.get(jour) + 1;
					}
					citrep.put(jour, count);
					citSize++;
				}
			}

			// This set contains the golden standard for this paper
			HashMap<String, Integer> standard = new HashMap<String, Integer>();
			tmp = paper.getJSONArray("standard_normalized");
			int standSize = 0;
			for (int j = 0; j < tmp.length(); j++) {
				String jour = tmp.getString(j).replaceAll("^the ", "");
				int count = 1;
				if (standard.containsKey(jour)) {
					count = standard.get(jour) + 1;
				}
				standard.put(jour, count);
				standSize++;
			}

			// Count how many items are correct in set intersection
			Set<String> intersect = new HashSet<String>(citrep.keySet());
			intersect.retainAll(standard.keySet()); // Intersect now contains
													// keys that are both in
													// citerep and in standard
			int intersectSize = 0; // Now check how many actually match (e.g.
									// citerep can reference a journal 3 times,
									// and standard 5 times)
			Iterator<String> it = intersect.iterator();

			// Process intersection
			while (it.hasNext()) {
				String key = it.next();
				int counta = citrep.get(key);
				int countb = standard.get(key);
				intersectSize += Math.min(counta, countb);
			}

			// Calculate precision, recall, f1
			float precision = 0.0f;
			if (citSize != 0) {
				precision = ((float) intersectSize / (float) citSize);
			}
			float recall = 0;

			if (standSize > 0) {
				recall = ((float) intersectSize / (float) standSize);
				totalSize += 1.0f;
			}

			// Save overall stats
			precOverall += precision;
			recOverall += recall;

		}

		// Calculate overall values
		float finalPrec = (precOverall / totalSize);
		float finalRec = (recOverall / totalSize);
		float fscoreOverall = (2 * finalPrec * finalRec) / (finalPrec + finalRec);

		// Output overall statistics
		DecimalFormat df = new DecimalFormat("#.###");
		Run.log("------------ Results based on " + ((int) totalSize) + " papers ------------");
		Run.log("prec: " + df.format(finalPrec) + ", rec: " + df.format(finalRec) + ", f-score: "
				+ df.format(fscoreOverall));

		System.out.println("");
	}

}

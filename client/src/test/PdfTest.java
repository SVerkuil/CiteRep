package test;

import java.io.File;
import org.json.JSONArray;

import citerep.Run;
import task.CitationTask;
import task.ExtractTask;
import task.IdentifyTask;
import task.NormalizeTask;

/**
 * Run CiteRep on individual PDF files
 * 
 * @author Steven
 *
 */
public class PdfTest extends Test {

	@Override
	public void perform() {

		Run.log("--- Processing PDF files in /input ---");

		// Read file into string
		File dir = new File(Run.FilePath + "input/");
		if (dir.isDirectory()) {

			// Process each file ending on .pdf
			for (String file : dir.list()) {
				String path = Run.FilePath + "input/" + file;
				if (path.endsWith(".pdf")) {

					// Load file if exists
					File pdf = new File(path);
					if (pdf.exists()) {

						// Extract text
						String plain = ExtractTask.pdfToText(pdf);
						String result = "-=-=-=-=-= PLAIN TEXT =-=-=-=-=-\n" + plain;

						// Extract citations
						JSONArray cits = CitationTask.getCitationsFromPlainText(plain);
						result = result + "\n-=-=-=-=-= CITATIONS =-=-=-=-=-\n" + cits.toString(3);

						String[] input = new String[cits.length()];
						for (int i = 0; i < cits.length(); i++) {
							input[i] = cits.getString(i);
						}

						// Identify journals
						String[] journals = IdentifyTask.getJournalsFromCitations(input);
						result = result + "\n-=-=-=-=-= IDENTIFY =-=-=-=-=-\n" + String.join("\n", journals);

						// Normalize journals
						JSONArray normal = new JSONArray();
						for (String jour : journals) {
							String n = NormalizeTask.normalizeJournal(jour);
							if (!n.equals("")) {
								normal.put(NormalizeTask.simpleJournal(n, true));
							} else {
								normal.put(NormalizeTask.simpleJournal(jour, true));
							}
						}
						result = result + "\n-=-=-=-=-= NORMALIZED =-=-=-=-=-\n" + normal.toString(3);

						//Write PDF file
						Run.writeFile("input/" + file + ".citerep.txt", result);
						Run.log("Processed PDF: " + file);

					}

				}

			}
		}

	}

}

package test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import citerep.Run;
import task.CitationTask;
import task.ExtractTask;
import task.IdentifyTask;
import task.NormalizeTask;

/**
 * Benchmark CiteRep using own TestSet
 * 
 * @author Steven
 *
 */
public class CiteRepTest extends Test {

	// Path for file caching (so it does not have to download each file on every
	// run)
	String cacheFilePath = Run.FilePath + "TestSet/";

	@Override
	public void perform() {
		
		Run.log("--- Benchmark CiteRep on CiteRepDataSet ---");

		// Load benchmark file
		File file = new File(Run.FilePath + "TestSet/_documents.csv");

		// Load lines from documents
		List<String> lines = null;
		try {
			lines = Files.readAllLines(file.toPath());

		} catch (IOException e) {
		}

		// Process
		if (file.exists()) {

			// Benchmark these papers
			JSONArray papers = new JSONArray();

			for (String line : lines) {

				// Paper object
				JSONObject paper = new JSONObject();
				String plain_pdf = "";
				JSONArray golden = new JSONArray();
				String title = "";

				// 0 = unique id,
				// 1 = web url (document source),
				// 2 = pdf (direct download link)
				String[] parts = line.split("\t");

				// Check if all is there
				if (parts.length == 3) {

					// Obtain plain text and golden standard
					File file_plain = new File(cacheFilePath + parts[0] + ".pdf.txt");
					File file_golden = new File(cacheFilePath + parts[0] + ".golden.txt");
					title = "id: " + parts[0];

					// If plain text exists

					if (file_plain.exists()) {
						plain_pdf = readFile(file_plain);

						// Attempt to Download and store PDF on file-system
					} else {

						try {

							// Download PDF
							InputStream input = new URL(parts[2].trim()).openStream();
							FileOutputStream outputStream = new FileOutputStream(
									new File(cacheFilePath + parts[0] + ".pdf"));
							int read = 0;
							byte[] bytes = new byte[1024];
							while ((read = input.read(bytes)) != -1) {
								outputStream.write(bytes, 0, read);
							}
							outputStream.close();
							input.close();
						} catch (Exception e) {
							e.printStackTrace();
						}

						// Convert PDF to plain text
						plain_pdf = ExtractTask.pdfToText(parts[2]);
						writeFile("../benchmark/" + parts[0] + ".pdf.txt", plain_pdf);

						// Create golden standard (empty file if not exists)
						if (!file_golden.exists()) {
							writeFile("../benchmark/" + parts[0] + ".golden.txt", "");
						}
					}

					// Obtain golden standard
					String[] goldenLines = new String[0];
					try {
						String input = readFile(file_golden).trim();
						goldenLines = input.split("\r?\n");
					} catch (Exception e) {
						e.printStackTrace();
					}
					for (String l : goldenLines) {
						String put = l.trim();
						if (!put.equals("")) {
							golden.put(l);
						}
					}
				}

				// Obtain citations from plain text
				JSONArray cits = CitationTask.getCitationsFromPlainText(plain_pdf);
				String[] journals = IdentifyTask.getJournalsFromCitations(cits);
				JSONArray citerep = new JSONArray();
				for (int i = 0; i < journals.length; i++) {
					String jour = journals[i];
					String n = NormalizeTask.normalizeJournal(jour);
					if (!n.equals("")) {
						citerep.put(NormalizeTask.simpleJournal(n, true));
					} else {
						citerep.put(NormalizeTask.simpleJournal(jour, true));
					}
				}
				paper.put("citrep_normalized", citerep);

				// Use abbreviated syntax for checking equality
				JSONArray goldenNormalized = new JSONArray();
				for (int i = 0; i < golden.length(); i++) {
					String jour = golden.getString(i);
					String n = NormalizeTask.normalizeJournal(jour);
					if (!n.equals("")) {
						goldenNormalized.put(NormalizeTask.simpleJournal(n, true));
					} else {
						goldenNormalized.put(NormalizeTask.simpleJournal(jour, true));
					}
				}
				paper.put("standard_normalized", goldenNormalized);

				// Add paper to papers array
				paper.put("title", title);
				papers.put(paper);

			}

			// Run benchmark
			benchmark(papers);

		} else {
			Run.error("Please create file: " + file.getAbsolutePath());
		}
	}
}

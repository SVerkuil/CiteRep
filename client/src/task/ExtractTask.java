package task;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.json.JSONArray;

import citerep.CiteRepResponse;
import citerep.CiteRepTextStripper;
import citerep.Run;
import citerep.TaskRunner;

/**
 * ExtractTask
 * 
 * @author Steven
 *
 */
public class ExtractTask extends Task {

	// If there are multiple PDF documents as input, use this string seperator
	// Note: you need to keep this in sync with serverside code!
	public static final String pdfSeparator = "---[@#!PDF_SPLIT!#@]---";

	// ASCII delimiter for "Start of Text" and "Start of Line"
	public static final String delimiterParagraph = Character.toString((char) 1);
	public static final String delimiterLine = Character.toString((char) 2);

	// Should we store the raw contents to the filesystem aswell?
	public static boolean storeToFileSystem = false;

	public ExtractTask(TaskRunner runner) {
		super(runner);
	}

	/**
	 * Return text from PDF file
	 * 
	 * @param url
	 * @return
	 */
	public static String pdfToText(File f) {
		String plaintext = "";
		if (f.exists()) {
			try {

				// Initialize text stripper, set whitespacing thresholds
				CiteRepTextStripper textStripper = new CiteRepTextStripper();
				textStripper.setParagraphStart(delimiterParagraph);
				textStripper.setLineSeparator(delimiterLine);
				textStripper.setAddMoreFormatting(true);
				textStripper.setIndentThreshold(80.0f);
				textStripper.setDropThreshold(15.0f);

				try {
					PDDocument pddDocument = PDDocument.load(f);
					plaintext = textStripper.getText(pddDocument);
					pddDocument.close();
					pddDocument = null;
				} catch (Exception e) {
					Run.error("Could not extract text");
				}
			} catch (Exception e) {
			}

		}
		return plaintext;
	}

	/**
	 * Return text from PDF url
	 * 
	 * @param url
	 * @return
	 */
	public static String pdfToText(String url) {
		String plaintext = "";
		try {

			// Initialize text stripper, set whitespacing thresholds
			CiteRepTextStripper textStripper = new CiteRepTextStripper();
			textStripper.setParagraphStart(delimiterParagraph);
			textStripper.setLineSeparator(delimiterLine);
			textStripper.setAddMoreFormatting(true);
			textStripper.setIndentThreshold(80.0f);
			textStripper.setDropThreshold(15.0f);

			// Check if last part of url is .pdf
			if (url.length() > 4 && url.substring(url.length() - 4, url.length()).toLowerCase().equals(".pdf")) {

				try {
					InputStream input = new URL(url.trim()).openStream();
					PDDocument pddDocument = PDDocument.load(input);
					plaintext = textStripper.getText(pddDocument);
					pddDocument.close();
					pddDocument = null;
					input.close();
					input = null;
				} catch (Exception e) {
					Run.error("Could not extract text");
				}
			}
		} catch (Exception e) {
		}
		return plaintext;
	}

	/**
	 * Perform Text Extraction on PDF url Dowloads the PDF and extracts text
	 */
	@Override
	public void perform() {

		// Obtain paper ID and URLS from task
		JSONArray parameters = getJson().getJSONArray("param");
		int paperID = parameters.getInt(0);
		JSONArray urls = parameters.getJSONArray(1);

		try {

			// Initialize server response
			CiteRepResponse response = new CiteRepResponse(this);
			response.put("paperID", paperID);
			Boolean hasSeperator = false;
			String plaintext = "";

			// Loop over all urls, a publication can have multiple documents
			// Not all documents have to be pdf, and pdf's can be appendices /
			// images
			for (int i = 0; i < urls.length(); i++) {
				String url = urls.getString(i);

				// Check if last part of url is .pdf
				if (url.length() > 4 && url.substring(url.length() - 4, url.length()).toLowerCase().equals(".pdf")) {

					// Add raw text to plaintext array
					plaintext = plaintext + (hasSeperator ? pdfSeparator : "") + pdfToText(url);
					hasSeperator = true;
				}
			}

			// Check if we also want to store the output to the local filesyste
			if (storeToFileSystem) {
				Run.writeFile("extract/" + paperID + ".txt", plaintext);
			}

			// Send server response
			response.put("paperTxt", plaintext);
			complete(response);

		} catch (Exception e) {
			Run.log("Could not complete Task");
			e.printStackTrace();
			Run.log(e.getMessage(), 2);
		}

		getWorker().notifyThreadClosed();
	}
}

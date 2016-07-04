package citerep;

import java.io.IOException;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * CiteRepTextStripper
 * 
 * This class can easily be extended in the future It now serves as an empty
 * shell to the apache PDFTextStripper class
 * 
 * @author Steven
 *
 */
public class CiteRepTextStripper extends PDFTextStripper {

	// Singleton instance for the text stripper
	private static CiteRepTextStripper instance;

	/**
	 * Obtain text stripper instance
	 * 
	 * @return The TextStripper instance
	 * @throws IOException
	 *             If no instance could be made
	 */
	public static CiteRepTextStripper getInstance() throws IOException {
		if (instance == null) {
			instance = new CiteRepTextStripper();
		}
		return instance;
	}

	/**
	 * Initialize TextStripper
	 * 
	 * @throws IOException
	 */
	public CiteRepTextStripper() throws IOException {
		super();
	}

}

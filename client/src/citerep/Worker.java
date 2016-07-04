package citerep;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.body.MultipartBody;

/**
 * Main worker class Connect using this->connect() Start using this->run()
 * 
 * @author Steven
 *
 */
public class Worker implements Runnable {

	// API remote url
	protected String url = "";

	// How often do we retry after no HTTP response before giving up?
	protected final int httpMaxRetry = 3;

	// How much attempts have we made currently?
	protected int httpRetries = 0;

	// Amount of concurrent treads
	protected int concurrent = 0;
	protected int concurrentMax = 4;

	// API passphrase
	protected String passphrase = "";

	// Globally Unique Worker name
	protected String uuid = "";

	// Human readable name (can be empty if not set)
	protected String name = "";

	// Is this worker connected to remote?
	// This will fire up heartbeat messages at regular intervals
	protected Boolean isConnected = false;

	// How often do we send heartbeat messages (in ms)?
	// Please also edit server-side when changed
	private final int interval = 5000;

	// Last JSON response
	protected JSONObject resp;

	// Main Thread
	private Thread thread;

	/**
	 * Create a worker for a url & pwd combination
	 * 
	 * @param url
	 *            The url to connect to
	 * @param pwd
	 *            The password to use
	 */
	public Worker(String url, String pwd, String name) {

		// Store parameters
		this.url = url;
		this.passphrase = pwd;
		this.name = name;

		// Store UUID
		this.uuid = Run.getWorkerGUUID(this.name);

		// Make sure url ends with a tailing '/'
		if (!url.substring(url.length() - 1).equals("/")) {
			this.url = this.url + "/";
		}
	}

	/**
	 * Connect to remote web interface
	 * 
	 * @return True if connected, otherwise false
	 */
	public boolean connect() {
		if ((resp = query("login", Run.version)) != null) {
			this.isConnected = true;
		}
		return this.isConnected;
	}

	/**
	 * Disconnect worker
	 */
	public void disconnect() {
		this.isConnected = false;
	}

	/**
	 * Obtain JSONObject with response
	 * 
	 * @param action
	 *            The action to query remotely
	 * @return JSONObject, or null if failed
	 */
	public JSONObject query(String action) {
		return this.query(action, "");
	}

	/**
	 * Obtain JSONObject with response
	 * 
	 * @param action
	 *            The action to query remotely
	 * @return JSONObject, or null if failed
	 */
	public JSONObject query(String action, JSONObject json) {
		return this.query(action, json.toString());
	}

	/**
	 * Obtain JSONObject with response
	 * 
	 * @param action
	 *            The action to query remotely
	 * @param input
	 *            The input string to send to remote action
	 * @return JSONObject, or null if failed
	 */
	public JSONObject query(String action, String input) {
		JSONObject response = null;
		MultipartBody body = null;
		long start = System.currentTimeMillis();
		Run.log("Send Query to: " + this.url + action, 3);

		try {

			// Obtain multipartbody
			body = Unirest.post(this.url + action).field("p", this.passphrase).field("w", this.uuid).field("i", input);

			// Attempt to obtain response from webserver
			response = body.asJson().getBody().getObject();

			Run.log("Received response from: " + this.url + action + " (" + (System.currentTimeMillis() - start)
					+ "ms)", 3);

			// Check if status is present and is "OK"
			if (!response.getString("s").equals("OK")) {
				String error = "[ERROR] Response: " + response.getString("s");
				response = null;
				throw new Exception(error);
			} else {
				httpRetries = 0;
			}

		} catch (IOException n) {
			Run.log("IOException", 3);

			if (httpRetries < httpMaxRetry) {
				Run.log("[ERROR] Server did not response, retrying attempt " + (++httpRetries), 2);
				query(action, input);

			} else {
				Run.log("[ERROR] Server did not respond after " + (httpRetries) + " attempts, exiting worker", 2);
				disconnect();
			}

		} catch (JSONException j) {
			Run.log("JSONException", 3);

			// JSON was invalid
			Run.log("[ERROR] Invalid JSON, skipped task", 2);

		} catch (Exception e) {

			// Attempt to obtain raw response from HTTP body
			String rawResponse = "";
			try {
				rawResponse = body.asString().getBody();
			} catch (Exception f) {
			}

			// Sometimes HTTP response might be an object, wich does not parse
			// to array
			// But the response succeeded nevertheless
			if (rawResponse.equals("{\"s\":\"OK\"}")) {
				Run.log("[ERROR] Something went wrong while querying /" + action);
				Run.log("[ERROR] Request: " + input, 3);
				if (!rawResponse.equals("")) {
					Run.log("[ERROR] Raw response: " + rawResponse, 3);
				}
				Run.log(e.getMessage());

				// HTTP response failed, so on serverside task is not completed.
				// Uncompleted tasks will be auto rescheduled, so leave this
				// case alone and continue.
				// Stop if we hit max http retries (server is probably offline)
				if (httpRetries < httpMaxRetry) {
					Run.log("[ERROR] Server did not response, retrying attempt " + (++httpRetries), 2);
					query(action, input);

				} else {
					Run.log("[ERROR] Server did not respond after " + (httpRetries) + " attempts, exiting worker", 2);
					disconnect();
				}
			} else {
				Run.log("Exception", 3);
				e.printStackTrace();
			}
		}

		return response;
	}

	/**
	 * A task thread was created
	 */
	public void notifyThreadStarted() {
		setConcurrent(1);
	}

	/**
	 * A task thread was closed
	 */
	public void notifyThreadClosed() {
		setConcurrent(-1);
		thread.interrupt();
	}

	/**
	 * Update counter of number of concurrent tasks
	 * 
	 * @param relative
	 *            The relative difference
	 */
	private synchronized void setConcurrent(int relative) {
		concurrent = concurrent + relative;
	}

	/**
	 * Own thread
	 * 
	 * @param t
	 *            The thread this worker is running in
	 */
	public void setThread(Thread t) {
		this.thread = t;
	}

	/**
	 * Run the worker Attempts to connect to remote source
	 */
	@Override
	public void run() {

		// Attempt to connect the worker to remote source
		if (!this.connect()) {
			Run.log("[ERROR] Attempt to start worker while not connected to remote host");
		} else {
			Run.log("[WORKER] \"" + uuid + "\" successfully connected!");

			// While connected, run this loop
			while (this.isConnected) {
				try {

					// Check if we have no more than 3 active tasks
					// A PDF file can be 160MB, hence we don't want infinite
					// downloads
					// since server ram will be limited :)
					if (this.concurrent < this.concurrentMax) {

						// Obtain a task from the remote host and start it
						// setGarbageCount(1);
						(new Thread(new TaskRunner(this, "heartbeat"))).start();
					}

					// Wait for x seconds before sending next heartbeat
					Thread.sleep(this.interval);

				} catch (InterruptedException e) {
					// Continue instantly

					// Something went wrong, stop the worker
				} catch (Exception e) {
					Run.log("[ERROR] Worker Thread was killed");
					Run.log(e.getMessage());
					this.isConnected = false;
				}

			}

			// Log that a worker has stopped
			Run.log("[WORKER] \"" + uuid + "\" Stopped!");
		}

	}

}

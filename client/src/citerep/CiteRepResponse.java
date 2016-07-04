package citerep;

import task.Task;
import org.json.JSONObject;

/**
 * CiteRepResponse
 * 
 * @author Steven
 *
 */
public class CiteRepResponse extends JSONObject {

	// The task to which to respond
	private Task task;

	/**
	 * Create new response Response will be send to dashboard
	 * 
	 * @param t
	 *            The task to respond to
	 */
	public CiteRepResponse(Task t) {
		this.task = t;

		// Store UUID
		put("uuid", t.uuid());
	}

	/**
	 * Send response to dashboard
	 */
	public void send() {

		// Count CPU time and send response via worker
		long completionTime = task.getDurationInMilliSeconds();
		put("cputime", completionTime);
		task.getWorker().query("completed", this);

		// Log task compleded
		Run.log("[TASK] " + task.uuid() + " completed in " + completionTime + "ms", 2);
	}
}

package task;

import org.json.JSONObject;

import citerep.CiteRepResponse;
import citerep.Run;
import citerep.TaskRunner;
import citerep.Worker;

/**
 * Abstract Task
 * 
 * @author Steven
 *
 */
public abstract class Task {

	// Associated TaskRunner
	protected TaskRunner runner;

	// Start time of the task
	private final long startTime = System.currentTimeMillis();

	/**
	 * Create task
	 * 
	 * @param runner
	 *            The runner which created the task
	 */
	public Task(TaskRunner runner) {
		this.runner = runner;
		Run.log("[TASK] " + uuid() + " created", 2);
	}

	/**
	 * Get readable action description
	 * 
	 * @return Human readable action
	 */
	public String getAction() {
		return this.runner.action;
	}

	/**
	 * Get associated JSON with task
	 * 
	 * @return The JSON associated
	 */
	protected JSONObject getJson() {
		return this.runner.task;
	}

	/**
	 * Get associated Worker with task
	 * 
	 * @return The Worker associated
	 */
	public Worker getWorker() {
		return this.runner.worker;
	}

	/**
	 * Obtain UUID for this task
	 * 
	 * @return The task UUID
	 */
	public String uuid() {
		try {
			return getJson().getString("uuid");
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * How long is this task running?
	 * 
	 * @return Time in millisec
	 */
	public long getDurationInMilliSeconds() {
		return System.currentTimeMillis() - startTime;
	}

	/**
	 * Perform a given task based on input
	 * 
	 * @param input
	 *            The input to this task
	 * @return The JSONArray result
	 */
	public void perform() {
	}

	/**
	 * Mark task as completed, send response to dashboard
	 * 
	 * @param resp
	 *            The response to send
	 */
	protected void complete(CiteRepResponse resp) {
		resp.send();
	}

}

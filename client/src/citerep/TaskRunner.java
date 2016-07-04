package citerep;

import org.json.JSONObject;

import task.CitationTask;
import task.ExtractTask;

/**
 * Main class for creating tasks
 * 
 * @author Steven
 *
 */
public class TaskRunner implements Runnable {

	// Worker this task belongs to
	public Worker worker;

	// The action that was performed
	public String action;

	// Json object that describes the task to do
	public JSONObject task;

	/**
	 * Create a new task to perform
	 * 
	 * @param Worker,
	 *            the Worker object this task belongs to
	 * @param action,
	 *            the action that was performed
	 */
	public TaskRunner(Worker worker, String action) {
		this.worker = worker;
		this.action = action;
		this.task = worker.query(action);
	}

	/**
	 * Run the task, performing computing work This is a thread
	 */
	@Override
	public void run() {

		// Check if we can actually do something
		if (task != null) {

			// This was a heartbeat, so extract further tasks from it
			// Perform these tasks within this Thread
			if (action.equals("heartbeat")) {
				String todo = task.getString("task");
				String log = todo.equals("") ? "Nothing to do" : todo;
				Run.log("[HEARTBEAT] Response: " + log, 2);

				// We should start processing PDF documents
				if (todo.equals("extract")) {
					worker.notifyThreadStarted();
					ExtractTask extract = new ExtractTask(this);
					extract.perform();
					extract = null;

					// We should find citations inside plain text
				} else if (todo.equals("citation")) {
					worker.notifyThreadStarted();
					CitationTask cit = new CitationTask(this);
					cit.perform();
					cit = null;

					// There is nothing to do
				} else {
					//
				}

			}
		}
		task = null;
	}
}

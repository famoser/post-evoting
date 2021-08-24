/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.plugin;

public class ExecutionListenerImpl implements ExecutionListener {

	private int error;
	private String progress;
	private String message;

	public void onError(int error) {
		this.error = error;
	}

	public int getError() {
		return error;
	}

	@Override
	public void onProgress(String progress) {
		this.progress = progress;

	}

	public String getProgress() {
		return progress;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public void onMessage(String message) {
		this.message = message;
	}

}

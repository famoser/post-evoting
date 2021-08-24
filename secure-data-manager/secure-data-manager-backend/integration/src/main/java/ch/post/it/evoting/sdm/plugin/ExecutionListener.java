/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.plugin;

public interface ExecutionListener {

	void onProgress(String progress);

	void onError(int result);

	void onMessage(String message);
}

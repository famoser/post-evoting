/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.service.impl.progress;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class ProgressData<T> {
	private final URI uri;
	private final CompletableFuture<T> future;
	private T progressStatus;

	ProgressData(URI uri, CompletableFuture<T> future) {
		this.uri = uri;
		this.future = future;
	}

	ProgressData(URI uri, CompletableFuture<T> future, T progressStatus) {
		this.uri = uri;
		this.future = future;
		this.progressStatus = progressStatus;
	}

	public URI getUri() {
		return uri;
	}

	public CompletableFuture<T> getFuture() {
		return future;
	}

	public T getProgressStatus() {
		return progressStatus;
	}

	public void setProgressStatus(T status) {
		this.progressStatus = status;
	}
}

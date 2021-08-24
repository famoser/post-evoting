/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.status;

/**
 * Identifies the possible status in a synchronization operation
 */
public enum SynchronizeStatus {

	SYNCHRONIZED("Synchronized", true),
	FAILED("Error synchronizing", false),
	PENDING("Pending to synchronize", false);

	private final String status;
	private final Boolean isSynchronized;

	SynchronizeStatus(String status, Boolean sync) {
		this.status = status;
		isSynchronized = sync;
	}

	public String getStatus() {
		return status;
	}

	/**
	 * Gets isSynchronized.
	 *
	 * @return Value of isSynchronized.
	 */
	public Boolean getIsSynchronized() {
		return isSynchronized;
	}
}

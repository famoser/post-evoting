/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election;

/**
 * A bean representing the BallotBoxContextData.json file, saved as an output of the Create Ballot Boxes command
 */
public class BallotBoxContextData {

	private ElectionEvent electionEvent;

	private String id;

	private String keystore;

	private String passwordKeystore;

	public ElectionEvent getElectionEvent() {
		return electionEvent;
	}

	public void setElectionEvent(ElectionEvent electionEvent) {
		this.electionEvent = electionEvent;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getKeystore() {
		return keystore;
	}

	public void setKeystore(String keystore) {
		this.keystore = keystore;
	}

	public String getPasswordKeystore() {
		return passwordKeystore;
	}

	public void setPasswordKeystore(String passwordKeystore) {
		this.passwordKeystore = passwordKeystore;
	}
}

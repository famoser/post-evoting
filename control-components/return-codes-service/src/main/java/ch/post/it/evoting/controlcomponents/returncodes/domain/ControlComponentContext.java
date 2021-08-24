/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.domain;

import static ch.post.it.evoting.domain.Validations.validateUUID;

public class ControlComponentContext {

	private final String electionEventId;
	private final String verificationCardSetId;
	private final String controlComponentId;

	public ControlComponentContext(String electionEventId, String verificationCardSetId, String controlComponentId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		this.electionEventId = electionEventId;
		this.verificationCardSetId = verificationCardSetId;
		this.controlComponentId = controlComponentId;
	}

	public String getVerificationCardSetId() {
		return verificationCardSetId;
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public String getControlComponentId() {
		return controlComponentId;
	}
}

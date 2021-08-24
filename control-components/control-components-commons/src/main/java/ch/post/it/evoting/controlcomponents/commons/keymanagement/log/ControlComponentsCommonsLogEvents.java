/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement.log;

import ch.post.it.evoting.logging.api.domain.LogEvent;

/**
 * Log events for this context.
 */
public enum ControlComponentsCommonsLogEvents implements LogEvent {

	// @formatter:off

	CCM_J_ELECTION_KEY_PAIR_GENERATED("GENELK", "000", "CCM_j election key pair successfully generated."),
	CCM_J_ELECTION_KEY_PAIR_SIGNED("GENELKSIGN", "000", "CCM_j election key pair successfully signed."),
	CCM_J_ELECTION_KEY_PAIR_STORED("GENELKSTORE", "000", "CCM_j election key pair successfully stored."),
	CCR_J_RETURN_CODES_KEY_PAIRS_GENERATED("GENCCKEYS", "000", "CCR_j Choice Return Codes Encryption key pair and CCR_j Return Codes Generation key pair successfully generated."),
	CCR_J_RETURN_CODES_KEY_PAIRS_SIGNED("GENCCKEYSSIGN", "000", "CCR_j Choice Return Codes Encryption key pair and CCR_j Return Codes Generation key pair successfully signed."),
	CCR_J_RETURN_CODES_KEY_PAIRS_STORED("GENCCKEYSSTORE", "000", "CCR_j Choice Return Codes Encryption key pair and CCR_j Return Codes Generation key pair successfully stored."),
	CONFIRMATION_KEY_EXPONENTIATED("GENPVCC", "000", "Confirmation Key successfully exponentiated to the Voter Vote Cast Return Code Generation secret key."),
	CONTROL_COMPONENT_SIGNING_CERTIFICATED_GENERATED("GENCCCRT", "000", "Control Component Signing Certificate successfully generated."),
	ERROR_CONTROL_COMPONENT_SIGNING_CERTIFICATED("GENCCCRT", "X34", "Error generating the Control Component Signing Certificate."),
	ERROR_GENERATION_CCM_J_ELECTION_KEY_PAIR("GENELK", "X38", " Error generating the CCM_j election key pair."),
	ERROR_GENERATION_CCR_J_RETURN_CODES_KEY_PAIRS("GENCCKEYS", "X35", " Error generating the CCR_j Choice Return Codes Encryption key pair and CCR_j Return Codes Generation key pair."),
	ERROR_LONG_RETURN_CODES_SHARE_SIGNED("PCCOMPSIGN", "X75", "Error while signing the CCR_j long Return Codes Share (either the CCR_j long Choice Return Codes shares or the CCR_j long Vote Cast Return Code share) and the corresponding exponentiation proof."),
	ERROR_PARAMETERS_VALIDATION("GENPV", "X44", "The parameter must not be null or empty."),
	ERROR_STORING_CCM_J_ELECTION_KEY_PAIR("GENELKSTORE", "X39", "Error storing the CCM_j election key pair."),
	ERROR_STORING_CCR_J_RETURN_CODES_KEY_PAIRS("GENCCKEYSSTORE", "X36", " Error storing the CCR_j Choice Return Codes Encryption key pair and CCR_j Return Codes Generation key pair."),
	KEY_PAIR_GENERATED_STORED("GENCCSK", "000", "Key pair successfully generated and stored."),
	PARTIAL_CHOICE_RETURN_CODES_EXPONENTIATED("GENPCC", "000", "Partial Choice Return Codes successfully exponentiated to the Voter Choice Return Code Generation secret key."),
	PROOF_KNOWLEDGE_CCR_J_CHOICE_RETURN_CODES_ENCRYPTION_SECRET_KEY("PDECPROOF", "000", "Proof of knowledge of the CCR_j Choice Return Codes encryption secret key successfully computed."),
	PROOF_KNOWLEDGE_VOTER_RETURN_CODE_GENERATION_SECRET_KEY("PCCOMPPROOF", "000", "Proof of knowledge of the Voter Return Code Generation secret key (Voter Choice Return Code Generation secret key or Voter Vote Cast Return Code Generation secret key) successfully computed.");

	// @formatter:on

	private final String layer;

	private final String action;

	private final String outcome;

	private final String info;

	ControlComponentsCommonsLogEvents(final String action, final String outcome, final String info) {
		this.layer = "";
		this.action = action;
		this.outcome = outcome;
		this.info = info;
	}

	@Override
	public String getAction() {
		return action;
	}

	@Override
	public String getOutcome() {
		return outcome;
	}

	@Override
	public String getInfo() {
		return info;
	}

	@Override
	public String getLayer() {
		return layer;
	}
}

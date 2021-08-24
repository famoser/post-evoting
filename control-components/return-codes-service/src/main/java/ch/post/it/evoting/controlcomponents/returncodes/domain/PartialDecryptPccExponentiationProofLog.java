/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.domain;

public class PartialDecryptPccExponentiationProofLog implements ReturnCodes {

	private final String eventType;
	private final ControlComponentContext context;
	private final PartialDecryptPccExponentiationProof exponentiationProof;

	public PartialDecryptPccExponentiationProofLog(ControlComponentContext context, PartialDecryptPccExponentiationProof exponentiationProof) {
		this.eventType = "PDECPROOF";
		this.context = context;
		this.exponentiationProof = exponentiationProof;
	}

	public String getEventType() {
		return eventType;
	}

	public ControlComponentContext getContext() {
		return context;
	}

	public PartialDecryptPccExponentiationProof getExponentiationProof() {
		return exponentiationProof;
	}
}

/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.domain;

public class LongChoiceReturnCodesShareExponentiationProofLog implements ReturnCodes {

	private final String eventType;
	private final ControlComponentContext context;
	private final LongChoiceReturnCodesShareExponentiationProof exponentiationProof;

	public LongChoiceReturnCodesShareExponentiationProofLog(ControlComponentContext context,
			LongChoiceReturnCodesShareExponentiationProof exponentiationProof) {
		this.eventType = "LCCPROOF";
		this.context = context;
		this.exponentiationProof = exponentiationProof;
	}

	public String getEventType() {
		return eventType;
	}

	public ControlComponentContext getContext() {
		return context;
	}

	public LongChoiceReturnCodesShareExponentiationProof getExponentiationProof() {
		return exponentiationProof;
	}
}

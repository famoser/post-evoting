/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.domain;

public class LongVoteCastReturnCodesShareExponentiationProofLog implements ReturnCodes {

	private final String eventType;
	private final ControlComponentContext context;
	private final LongVoteCastReturnCodesShareExponentiationProof exponentiationProof;

	public LongVoteCastReturnCodesShareExponentiationProofLog(ControlComponentContext context,
			LongVoteCastReturnCodesShareExponentiationProof exponentiationProof) {
		this.eventType = "LVCCPROOF";
		this.context = context;
		this.exponentiationProof = exponentiationProof;
	}

	public String getEventType() {
		return eventType;
	}

	public ControlComponentContext getContext() {
		return context;
	}

	public LongVoteCastReturnCodesShareExponentiationProof getExponentiationProof() {
		return exponentiationProof;
	}
}

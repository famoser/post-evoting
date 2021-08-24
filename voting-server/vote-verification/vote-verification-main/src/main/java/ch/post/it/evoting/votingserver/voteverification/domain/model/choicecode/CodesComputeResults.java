/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.domain.model.choicecode;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.domain.returncodes.ReturnCodesExponentiationResponsePayload;

public class CodesComputeResults {

	private final Map<BigInteger, ZpGroupElement> combinedPartialChoiceCodes;

	private List<ReturnCodesExponentiationResponsePayload> computationResults;

	public CodesComputeResults(Map<BigInteger, ZpGroupElement> combinedPartialChoiceCodes,
			List<ReturnCodesExponentiationResponsePayload> computeResults) {
		this.combinedPartialChoiceCodes = combinedPartialChoiceCodes;
		this.setComputationResults(computeResults);
	}

	public Map<BigInteger, ZpGroupElement> getCombinedPartialChoiceCodes() {
		return combinedPartialChoiceCodes;
	}

	public List<ReturnCodesExponentiationResponsePayload> getComputationResults() {
		return computationResults;
	}

	public void setComputationResults(List<ReturnCodesExponentiationResponsePayload> computationResults) {
		this.computationResults = computationResults;
	}

}

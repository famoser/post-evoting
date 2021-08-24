/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.domain.model.choicecode;

import java.util.List;

import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.domain.returncodes.ChoiceCodesVerificationDecryptResPayload;

public class CodesDecryptionResults {

	private List<ZpGroupElement> combinedZpGroupElementLists;

	private List<ChoiceCodesVerificationDecryptResPayload> decryptResult;

	public CodesDecryptionResults(List<ZpGroupElement> combineZpGroupElementLists, List<ChoiceCodesVerificationDecryptResPayload> decryptResult) {
		this.setCombinedZpGroupElementLists(combineZpGroupElementLists);
		this.setDecryptResult(decryptResult);
	}

	public List<ZpGroupElement> getCombinedZpGroupElementLists() {
		return combinedZpGroupElementLists;
	}

	public void setCombinedZpGroupElementLists(List<ZpGroupElement> combineZpGroupElementLists) {
		this.combinedZpGroupElementLists = combineZpGroupElementLists;
	}

	public List<ChoiceCodesVerificationDecryptResPayload> getDecryptResult() {
		return decryptResult;
	}

	public void setDecryptResult(List<ChoiceCodesVerificationDecryptResPayload> decryptResult) {
		this.decryptResult = decryptResult;
	}

}

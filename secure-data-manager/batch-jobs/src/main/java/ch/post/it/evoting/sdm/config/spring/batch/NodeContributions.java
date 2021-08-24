/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch;

import java.util.List;

import ch.post.it.evoting.domain.returncodes.ChoiceCodeGenerationDTO;
import ch.post.it.evoting.domain.returncodes.ReturnCodeGenerationRequestPayload;
import ch.post.it.evoting.domain.returncodes.ReturnCodeGenerationResponsePayload;

public class NodeContributions {

	private List<ChoiceCodeGenerationDTO<ReturnCodeGenerationResponsePayload>> nodeContributionResponse;

	private ReturnCodeGenerationRequestPayload nodeContributionRequest;

	public NodeContributions(List<ChoiceCodeGenerationDTO<ReturnCodeGenerationResponsePayload>> nodeContributionResponse,
			ReturnCodeGenerationRequestPayload nodeContributionRequest) {
		this.nodeContributionResponse = nodeContributionResponse;
		this.nodeContributionRequest = nodeContributionRequest;
	}

	public List<ChoiceCodeGenerationDTO<ReturnCodeGenerationResponsePayload>> getNodeContributionResponse() {
		return nodeContributionResponse;
	}

	public void setNodeContributionResponse(List<ChoiceCodeGenerationDTO<ReturnCodeGenerationResponsePayload>> nodeContributionResponse) {
		this.nodeContributionResponse = nodeContributionResponse;
	}

	public ReturnCodeGenerationRequestPayload getNodeContributionRequest() {
		return nodeContributionRequest;
	}

	public void setNodeContributionRequest(ReturnCodeGenerationRequestPayload nodeContributionRequest) {
		this.nodeContributionRequest = nodeContributionRequest;
	}

}

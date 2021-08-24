/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch.readers;

import java.io.IOException;
import java.util.List;

import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.domain.returncodes.ChoiceCodeGenerationDTO;
import ch.post.it.evoting.domain.returncodes.ReturnCodeGenerationRequestPayload;
import ch.post.it.evoting.domain.returncodes.ReturnCodeGenerationResponsePayload;
import ch.post.it.evoting.sdm.config.commands.voters.NodeContributionsPath;
import ch.post.it.evoting.sdm.config.spring.batch.NodeContributions;

public class NodeContributionsReader implements ItemReader<NodeContributions> {

	private final List<NodeContributionsPath> nodeContributionsPaths;

	@Autowired
	private ObjectMapper objectMapper;

	private Integer index = 0;

	public NodeContributionsReader(List<NodeContributionsPath> nodeContributionsPaths) {
		this.nodeContributionsPaths = nodeContributionsPaths;
	}

	@Override
	public NodeContributions read() throws IOException {
		if (index < nodeContributionsPaths.size()) {
			NodeContributionsPath nodeContributionsPath = nodeContributionsPaths.get(index);

			List<ChoiceCodeGenerationDTO<ReturnCodeGenerationResponsePayload>> nodeContributionResponse = objectMapper
					.readValue(nodeContributionsPath.getOutput().toFile(),
							new TypeReference<List<ChoiceCodeGenerationDTO<ReturnCodeGenerationResponsePayload>>>() {
							});

			ReturnCodeGenerationRequestPayload nodeContributionRequest = objectMapper
					.readValue(nodeContributionsPath.getInput().toFile(), new TypeReference<ReturnCodeGenerationRequestPayload>() {
					});

			index++;
			return new NodeContributions(nodeContributionResponse, nodeContributionRequest);
		} else {
			return null;
		}
	}

}

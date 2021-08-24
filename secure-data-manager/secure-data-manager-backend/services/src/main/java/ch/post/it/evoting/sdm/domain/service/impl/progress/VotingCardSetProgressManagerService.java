/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.service.impl.progress;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.sdm.domain.model.config.VotingCardGenerationJobStatus;

/**
 * This implementation contains a call to the configuration engine for progressManager
 */
@Service
public class VotingCardSetProgressManagerService extends GenericProgressManagerService<VotingCardGenerationJobStatus> {

	@Value("${CONFIG_GENERATOR_URL}")
	private String configServiceBaseUrl;

	@PostConstruct
	void setup() {
		setServiceUrl(configServiceBaseUrl);
	}

	@Override
	protected VotingCardGenerationJobStatus defaultData(final String jobId) {
		return new VotingCardGenerationJobStatus(jobId);
	}

	@Override
	protected List<VotingCardGenerationJobStatus> doCall(final URI uri) {
		final VotingCardGenerationJobStatus[] response = restClient.getForObject(uri, VotingCardGenerationJobStatus[].class);
		return Arrays.asList(response);
	}
}


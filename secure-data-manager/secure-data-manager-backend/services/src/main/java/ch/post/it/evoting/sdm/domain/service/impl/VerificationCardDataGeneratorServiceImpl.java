/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import ch.post.it.evoting.sdm.commons.domain.CreateVerificationCardIdsInput;
import ch.post.it.evoting.sdm.domain.service.VerificationCardDataGeneratorService;
import ch.post.it.evoting.sdm.domain.service.exception.VerificationCardDataGeneratorServiceException;

@Service
public class VerificationCardDataGeneratorServiceImpl implements VerificationCardDataGeneratorService {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerificationCardDataGeneratorServiceImpl.class);
	private static final String PRECOMPUTE_URL_PATH = "/precompute";
	@Autowired
	private RestTemplate restClient;
	@Value("${CONFIG_GENERATOR_URL}")
	private String configServiceBaseUrl;

	@Override
	public BufferedReader precompute(String electionEventId, String verificationCardSetId, int numberOfVotingCards) throws IOException {

		CreateVerificationCardIdsInput createVerificationCardIdsInput = new CreateVerificationCardIdsInput();
		createVerificationCardIdsInput.setVerificationCardSetId(verificationCardSetId);
		createVerificationCardIdsInput.setNumberOfVerificationCardIds(numberOfVotingCards);
		createVerificationCardIdsInput.setElectionEventId(electionEventId);

		final String targetUrl = configServiceBaseUrl + PRECOMPUTE_URL_PATH;

		LOGGER.info("Starting pre-computation of verification card set {}...", verificationCardSetId);
		ResponseEntity<Resource> response;
		try {
			response = restClient.postForEntity(targetUrl, createVerificationCardIdsInput, Resource.class);
		} catch (RestClientException e) {
			LOGGER.error("Error performing precompute request to endpoint '{}'", targetUrl);
			throw new VerificationCardDataGeneratorServiceException(e);
		}
		if (response.getStatusCode().is2xxSuccessful()) {
			final Resource body = response.getBody();
			if (body == null) {
				throw new VerificationCardDataGeneratorServiceException("Malformed response contains no body.");
			}
			return new BufferedReader(new InputStreamReader(body.getInputStream()));
		} else {
			throw new VerificationCardDataGeneratorServiceException(String.format("Unexpected response status code %s", response.getStatusCode()));
		}
	}
}

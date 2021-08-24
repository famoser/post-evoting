/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.util.List;

import org.bouncycastle.cms.CMSException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.RandomService;
import ch.post.it.evoting.domain.election.EncryptionParameters;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.domain.model.electionevent.ElectionEventRepository;
import ch.post.it.evoting.sdm.domain.model.generator.DataGeneratorResponse;
import ch.post.it.evoting.sdm.domain.model.status.Status;
import ch.post.it.evoting.sdm.domain.service.ElectionEventDataGeneratorService;
import ch.post.it.evoting.sdm.infrastructure.service.ConfigurationEntityStatusService;
import ch.post.it.evoting.sdm.utils.EncryptionParametersLoader;

/**
 * This is a service for handling election event entities.
 */
@Service
public class ElectionEventService {

	private static final int OMEGA = 1200;

	private final ElectionEventDataGeneratorService electionEventDataGeneratorService;
	private final ConfigurationEntityStatusService configurationEntityStatusService;
	private final ElectionEventRepository electionEventRepository;
	private final PathResolver pathResolver;
	private final ObjectMapper mapper;
	private final EncryptionParametersLoader encryptionParametersLoader;

	@Autowired
	public ElectionEventService(final ElectionEventDataGeneratorService electionEventDataGeneratorService,
			final ConfigurationEntityStatusService configurationEntityStatusService, final ElectionEventRepository electionEventRepository,
			final PathResolver pathResolver, final ObjectMapper objectMapper, final EncryptionParametersLoader encryptionParametersLoader) {

		this.electionEventDataGeneratorService = electionEventDataGeneratorService;
		this.configurationEntityStatusService = configurationEntityStatusService;
		this.electionEventRepository = electionEventRepository;
		this.pathResolver = pathResolver;
		this.mapper = objectMapper;
		this.encryptionParametersLoader = encryptionParametersLoader;
	}

	/**
	 * Creates an election event based on the given id and if everything ok, it sets its status to ready.
	 *
	 * @param electionEventId identifies the election event to be created.
	 * @return an object containing the result of the creation.
	 */
	public DataGeneratorResponse create(final String electionEventId) throws IOException {
		// Load the election encryption parameters.
		final EncryptionParameters encryptionParameters;
		final Path electionEventPath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR, electionEventId);
		try {
			encryptionParameters = encryptionParametersLoader.load(electionEventPath);
		} catch (CertificateException | CMSException | GeneralCryptoLibException e) {
			throw new IllegalStateException(String.format("Failed to load the encryption parameters located at: %s", electionEventPath), e);
		}
		final BigInteger p = new BigInteger(encryptionParameters.getP());
		final BigInteger q = new BigInteger(encryptionParameters.getQ());
		final BigInteger g = new BigInteger(encryptionParameters.getG());
		final GqGroup group = new GqGroup(p, q, g);

		// Create the setup key pair. Implements the GenEncryptionKeysPO algorithm.
		final ElGamalMultiRecipientKeyPair setUpKeyPair = ElGamalMultiRecipientKeyPair.genKeyPair(group, OMEGA, new RandomService());

		// Persist the setup key pair.
		final Path outputPath = electionEventPath.resolve(Constants.CONFIG_DIR_NAME_OFFLINE);
		Files.createDirectories(outputPath);
		mapper.writeValue(outputPath.resolve(Constants.SETUP_SECRET_KEY_FILE_NAME).toFile(), setUpKeyPair.getPrivateKey());

		// Create election event data.
		final DataGeneratorResponse result = electionEventDataGeneratorService.generate(electionEventId);
		if (result.isSuccessful()) {
			configurationEntityStatusService.update(Status.READY.name(), electionEventId, electionEventRepository);
		}

		return result;
	}

	/**
	 * This method returns election event alias based on the given id
	 */
	public String getElectionEventAlias(String electionEventId) {

		return electionEventRepository.getElectionEventAlias(electionEventId);
	}

	/**
	 * Get all available election events
	 *
	 * @return the election events list
	 */
	public List<String> getAllElectionEventIds() {

		return electionEventRepository.listIds();
	}

}

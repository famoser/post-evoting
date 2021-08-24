/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.service.ElGamalService;
import ch.post.it.evoting.cryptoprimitives.GroupVector;
import ch.post.it.evoting.cryptoprimitives.hashing.HashService;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.domain.UUIDGenerator;
import ch.post.it.evoting.domain.election.Ballot;
import ch.post.it.evoting.domain.mixnet.MixnetShufflePayload;
import ch.post.it.evoting.domain.mixnet.exceptions.FailedValidationException;
import ch.post.it.evoting.domain.mixnet.generators.MixnetShufflePayloadGenerator;
import ch.post.it.evoting.sdm.application.exception.CheckedIllegalStateException;
import ch.post.it.evoting.sdm.application.exception.ResourceNotFoundException;
import ch.post.it.evoting.sdm.application.service.writers.VotePrimeFactorsFileRepository;
import ch.post.it.evoting.sdm.commons.CryptolibPayloadSignatureService;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.sdm.domain.model.mixing.MixingKeys;
import ch.post.it.evoting.sdm.infrastructure.mixnetpayload.MixnetFinalPayloadFileRepository;
import ch.post.it.evoting.sdm.infrastructure.mixnetpayload.MixnetShufflePayloadFileRepository;
import ch.post.it.evoting.sdm.infrastructure.service.ConfigurationEntityStatusService;

class MixOfflineFacadeTest {

	private static final int NUM_VOTES = 10;

	private static String electionEventId;
	private static String ballotBoxId = "ballotBoxId";
	private static String administrationBoardPrivateKey;
	private static Ballot ballot;
	private static String ballotId;

	private final Random random = new SecureRandom();

	private MixnetShufflePayload lastMixnetShufflePayload;
	private BallotBoxService ballotBoxService;
	private MixnetShufflePayloadFileRepository shufflepayloadFileRepository;
	private FactorizeService factorizeService;
	private VotePrimeFactorsFileRepository votePrimeFactorsFileRepository;
	private BallotService ballotService;
	private MixOfflineFacade mixFacade;
	private MixDecryptService mixDecryptService;
	private MixDecryptService.Result result;
	private MixingKeys mixingKeys;
	private CryptolibPayloadSignatureService signatureService;
	private MixnetShufflePayloadService mixnetShufflePayloadService;

	@BeforeAll
	static void setUpSuite() throws IOException {
		electionEventId = UUIDGenerator.genValidUUID();
		ballotId = UUIDGenerator.genValidUUID();
		ballotBoxId = UUIDGenerator.genValidUUID();

		//Ballot options representation must be valid group elements
		String ballotJson = readFileContent("src/test/resources/mixing/ballotForMixing.json");
		ballot = new ObjectMapper().readValue(ballotJson, Ballot.class);

		administrationBoardPrivateKey = readFileContent("src/test/resources/mixing/adminBoardPrivateKey.pem");
	}

	private static String readFileContent(String pathToFile) throws IOException {
		return new String(Files.readAllBytes(Paths.get(pathToFile)), StandardCharsets.UTF_8);
	}

	@BeforeEach
	void setUp() throws GeneralCryptoLibException, IOException {
		ballotBoxService = mock(BallotBoxService.class);
		BallotBoxRepository ballotBoxRepository = mock(BallotBoxRepository.class);
		shufflepayloadFileRepository = mock(MixnetShufflePayloadFileRepository.class);
		MixnetFinalPayloadFileRepository finalPayloadFileRepository = mock(MixnetFinalPayloadFileRepository.class);
		ConfigurationEntityStatusService configurationEntityStatusService = mock(ConfigurationEntityStatusService.class);
		factorizeService = mock(FactorizeService.class);
		votePrimeFactorsFileRepository = mock(VotePrimeFactorsFileRepository.class);
		ballotService = mock(BallotService.class);
		mixDecryptService = mock(MixDecryptService.class);
		mixnetShufflePayloadService = mock(MixnetShufflePayloadService.class);

		//Have to use real group because of cryptolib properties
		GqGroup group = GroupTestData.getLargeGqGroup();
		int numVotes = random.nextInt(10) + 2;
		int voteSize = random.nextInt(10) + 1;
		int nodeId = random.nextInt(3) + 1;
		lastMixnetShufflePayload = new MixnetShufflePayloadGenerator(group).genPayload(numVotes, voteSize, nodeId);
		result = new MixDecryptResultGenerator(group).genMixDecryptResult(numVotes, voteSize);

		ElGamalEncryptionParameters encryptionParameters = new ElGamalEncryptionParameters(group.getP(), group.getQ(),
				group.getGenerator().getValue());
		String encodedelectoralBoardPrivateKey = generateEncodedElGamalPrivateKey(encryptionParameters);

		mixingKeys = new MixingKeys(administrationBoardPrivateKey, encodedelectoralBoardPrivateKey, "");

		HashService hashService = new HashService();
		signatureService = mock(CryptolibPayloadSignatureService.class);
		AdminBoardService adminBoardService = mock(AdminBoardService.class);

		mixFacade = new MixOfflineFacade(ballotBoxService, ballotBoxRepository, shufflepayloadFileRepository, finalPayloadFileRepository,
				configurationEntityStatusService, factorizeService, votePrimeFactorsFileRepository, ballotService, mixDecryptService, hashService,
				signatureService, adminBoardService, mixnetShufflePayloadService);
	}

	private String generateEncodedElGamalPrivateKey(ElGamalEncryptionParameters encryptionParameters) throws GeneralCryptoLibException {
		ElGamalService elGamalService = new ElGamalService();
		ElGamalKeyPair electoralBoardKeyPair = elGamalService.generateKeyPair(encryptionParameters, NUM_VOTES);
		String electoralBoardPrivateKeyJson = electoralBoardKeyPair.getPrivateKeys().toJson();
		byte[] electoralBoardPrivateKeyJsonBytes = electoralBoardPrivateKeyJson.getBytes(StandardCharsets.UTF_8);
		byte[] electoralBoardPrivateKeyJsonBytesBase64Encoded = Base64.getEncoder().encode(electoralBoardPrivateKeyJsonBytes);
		return new String(electoralBoardPrivateKeyJsonBytesBase64Encoded, StandardCharsets.ISO_8859_1);
	}

	@Test
	void mixDecryptMixesFactorizesAndPersists() throws ResourceNotFoundException {
		when(ballotBoxService.getBallotId(ballotBoxId)).thenReturn(ballotId);
		when(ballotBoxService.isDownloaded(ballotBoxId)).thenReturn(true);
		when(ballotBoxService.hasDownloadedBallotBoxConfirmedVotes(electionEventId, ballotId, ballotBoxId)).thenReturn(true);
		when(shufflepayloadFileRepository.getPayload(eq(electionEventId), eq(ballotId), eq(ballotBoxId), anyInt()))
				.thenReturn(lastMixnetShufflePayload);
		when(mixDecryptService.mixDecryptOffline(any(), any(), any(), any(), any())).thenReturn(result);
		when(ballotService.getBallot(electionEventId, ballotId)).thenReturn(ballot);
		when(mixnetShufflePayloadService.areOnlinePayloadSignaturesValid(any(), any(), any())).thenReturn(true);

		assertDoesNotThrow(() -> mixFacade.mixOffline(electionEventId, ballotBoxId, mixingKeys));

		verify(mixDecryptService).mixDecryptOffline(any(), any(), any(), any(), any());
		verify(factorizeService).factorize((GroupVector<GqElement, GqGroup>) any(), anyList(), anyInt());
		verify(votePrimeFactorsFileRepository).saveDecompressedVotes(any(), anyString(), anyString(), anyString(), any());
	}

	@Test
	void mixFacadeThrowsForInvalidUUID() {
		assertThrows(FailedValidationException.class, () -> mixFacade.mixOffline("", ballotBoxId, mixingKeys));
		assertThrows(FailedValidationException.class, () -> mixFacade.mixOffline(electionEventId, "", mixingKeys));
	}

	@Test
	void mixFacadeThrowsWhenBallotBoxIsNotDownloaded() throws ResourceNotFoundException {
		when(ballotBoxService.isDownloaded(ballotBoxId)).thenReturn(false);
		assertThrows(CheckedIllegalStateException.class, () -> mixFacade.mixOffline(electionEventId, ballotBoxId, mixingKeys));
	}
}

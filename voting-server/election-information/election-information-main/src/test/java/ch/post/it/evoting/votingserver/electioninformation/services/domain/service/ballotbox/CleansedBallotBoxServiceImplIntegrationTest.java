/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.service.ballotbox;

import static ch.post.it.evoting.votingserver.electioninformation.services.domain.model.util.Constants.BALLOT_BOX_ID;
import static ch.post.it.evoting.votingserver.electioninformation.services.domain.model.util.Constants.ELECTION_EVENT_ID;
import static ch.post.it.evoting.votingserver.electioninformation.services.domain.model.util.Constants.TENANT_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.naming.InvalidNameException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Ciphertext;
import ch.post.it.evoting.cryptoprimitives.hashing.HashService;
import ch.post.it.evoting.domain.election.Ballot;
import ch.post.it.evoting.domain.election.model.ballotbox.BallotBoxIdImpl;
import ch.post.it.evoting.domain.election.model.vote.CiphertextEncryptedVote;
import ch.post.it.evoting.domain.election.model.vote.EncryptedVote;
import ch.post.it.evoting.domain.election.payload.sign.CryptolibPayloadSigner;
import ch.post.it.evoting.domain.election.payload.sign.PayloadSigner;
import ch.post.it.evoting.domain.mixnet.MixnetInitialPayload;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.CleansedBallotBoxServiceException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.sign.CryptoTestData;
import ch.post.it.evoting.votingserver.commons.sign.SignedTestVotingDataService;
import ch.post.it.evoting.votingserver.commons.sign.TestCertificateGenerator;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxInformation;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxInformationRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.CleansedBallotBox;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.CleansedBallotBoxRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionPublicKey;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionPublicKeyRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.tenant.EiTenantSystemKeys;

@RunWith(MockitoJUnitRunner.class)
public class CleansedBallotBoxServiceImplIntegrationTest {

	private static final short ORIGINAL_VOTE_SET_MAX_SIZE = CleansedBallotBox.CHUNK_SIZE;
	private static final short TESTING_VOTE_SET_MAX_SIZE = 10;
	private static final String BALLOT_JSON = "ballot.json";
	private static final BallotBoxIdImpl ballotBoxId = new BallotBoxIdImpl(TENANT_ID, ELECTION_EVENT_ID, BALLOT_BOX_ID);
	private static final ElectionPublicKeyRepository ELECTION_PUBLIC_KEY_REPOSITORY = mock(ElectionPublicKeyRepository.class);
	private static final CleansedBallotBoxRepository cleansedBallotBoxRepository = mock(CleansedBallotBoxRepository.class);
	private static final BallotBoxInformationRepository ballotBoxInformationRepository = mock(BallotBoxInformationRepository.class);
	private static final EiTenantSystemKeys eiTenantSystemKeys = mock(EiTenantSystemKeys.class);

	private static Field maxSizeField;
	private static TestCertificateGenerator testCertificateGenerator;
	private static AsymmetricServiceAPI asymmetricService;

	@Spy
	private static PayloadSigner payloadSigner;
	@InjectMocks
	private final CleansedBallotBoxServiceImpl cleansedBallotBoxService = new CleansedBallotBoxServiceImpl();
	@Mock
	private HashService hashService;
	@Spy
	private AsymmetricServiceAPI ballotBoxAsymmetricService;

	@BeforeClass
	public static void setUpAll() throws GeneralCryptoLibException, IOException, InvalidNameException, ResourceNotFoundException {

		// Initialise actual services.
		asymmetricService = new AsymmetricService();
		payloadSigner = new CryptolibPayloadSigner(asymmetricService);
		testCertificateGenerator = TestCertificateGenerator.createDefault();

		// Initialise mocks.
		setUpEiTenantSystemKeysMock();
		setUpBallotBoxInformationRepositoryMock();
		setUpElectoralAuthorityRepositoryMock();
	}

	@BeforeClass
	public static void reduceVoteSetMaxValueForTesting() throws NoSuchFieldException, IllegalAccessException {
		// Get the private static final field.
		maxSizeField = CleansedBallotBox.class.getDeclaredField("CHUNK_SIZE");
		// Make it accessible.
		maxSizeField.setAccessible(true);
		// Make it non-final.
		Field modifiersField = Field.class.getDeclaredField("modifiers");
		modifiersField.setAccessible(true);
		modifiersField.setInt(maxSizeField, maxSizeField.getModifiers() & ~Modifier.FINAL);
		// Alter its value.
		maxSizeField.setShort(null, TESTING_VOTE_SET_MAX_SIZE);
	}

	@AfterClass
	public static void restoreVoteSetMaxValue() throws IllegalAccessException {
		// Restore the vote set's original maximum size value.
		maxSizeField.setShort(null, ORIGINAL_VOTE_SET_MAX_SIZE);
	}

	/**
	 * Get a ballot definition from a JSON file.
	 *
	 * @return the ballot definition
	 */
	private static Ballot getBallotDefinition() throws IOException {
		return new ObjectMapper()
				.readValue(CleansedBallotBoxServiceImplIntegrationTest.class.getClassLoader().getResourceAsStream(BALLOT_JSON), Ballot.class);
	}

	private static void setUpEiTenantSystemKeysMock() throws InvalidNameException, GeneralCryptoLibException {

		KeyPair signingKeyPair = asymmetricService.getKeyPairForSigning();
		X509Certificate signingCertificate = testCertificateGenerator.createCACertificate(signingKeyPair, "Signing certificate");

		// Set up the certificate chain for signing.
		X509Certificate[] certificateChain = { signingCertificate, testCertificateGenerator.getRootCertificate() };
		when(eiTenantSystemKeys.getSigningCertificateChain(anyString())).thenReturn(certificateChain);

		// Set up the signing key.
		when(eiTenantSystemKeys.getSigningPrivateKey(anyString())).thenReturn(signingKeyPair.getPrivate());
	}

	private static void setUpBallotBoxInformationRepositoryMock() throws ResourceNotFoundException, IOException {
		String jsonString;
		try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("ballot_box_information-empty.json");
				Scanner scanner = new Scanner(is)) {
			assertNotNull(is);
			jsonString = scanner.useDelimiter("\\A").next();
			assertFalse(jsonString.isEmpty());
		}
		BallotBoxInformation bbi = mock(BallotBoxInformation.class);
		when(bbi.getJson()).thenReturn(jsonString);
		when(ballotBoxInformationRepository.findByTenantIdElectionEventIdBallotBoxId(anyString(), anyString(), anyString())).thenReturn(bbi);
	}

	private static void setUpElectoralAuthorityRepositoryMock() throws GeneralCryptoLibException, ResourceNotFoundException {
		ElGamalKeyPair electoralAuthorityKeyPair = CryptoTestData.generateElGamalKeyPair(1);

		String elGamalPublicKeyJson = electoralAuthorityKeyPair.getPublicKeys().toJson();
		String base64PublicKey = Base64.getEncoder().encodeToString(elGamalPublicKeyJson.getBytes(StandardCharsets.UTF_8));

		JsonObject jsonObject = Json.createObjectBuilder().add("publicKey", base64PublicKey).build();

		ElectionPublicKey entity = mock(ElectionPublicKey.class);
		when(entity.getJson()).thenReturn(jsonObject.toString());
		when(ELECTION_PUBLIC_KEY_REPOSITORY.findByTenantIdElectionEventIdElectoralAuthorityId(anyString(), anyString(), anyString()))
				.thenReturn(entity);

	}

	@Test
	public void testPayloadWithValidVoteSet()
			throws ResourceNotFoundException, GeneralCryptoLibException, IOException, InvalidNameException, CleansedBallotBoxServiceException {
		assertEquals(TESTING_VOTE_SET_MAX_SIZE, CleansedBallotBox.CHUNK_SIZE);

		SignedTestVotingDataService votingDataService = new SignedTestVotingDataService(getBallotDefinition(), asymmetricService,
				testCertificateGenerator);

		List<Ciphertext> votes = votingDataService.generateVotes(TESTING_VOTE_SET_MAX_SIZE);
		Stream<EncryptedVote> voteStream = votes.stream().map(CiphertextEncryptedVote::new);
		when(cleansedBallotBoxRepository.getVoteSet(any(), anyInt(), anyInt())).thenReturn(voteStream);

		final MixnetInitialPayload mixnetInitialPayload = cleansedBallotBoxService.getMixnetInitialPayload(ballotBoxId);

		// Ensure the payload contains the expected number of votes.
		assertEquals(TESTING_VOTE_SET_MAX_SIZE, mixnetInitialPayload.getEncryptedVotes().size());
	}

}

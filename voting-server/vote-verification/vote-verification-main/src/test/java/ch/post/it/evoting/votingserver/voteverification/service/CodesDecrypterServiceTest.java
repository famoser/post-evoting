/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.service;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.proofs.ProofsServiceAPI;
import ch.post.it.evoting.cryptolib.certificates.utils.CryptographicOperationException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.service.ElGamalService;
import ch.post.it.evoting.cryptolib.mathematical.groups.activity.GroupElementsCompressor;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofVerifierAPI;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;
import ch.post.it.evoting.domain.ObjectMappers;
import ch.post.it.evoting.domain.election.EncryptionParameters;
import ch.post.it.evoting.domain.election.VoteVerificationContextData;
import ch.post.it.evoting.domain.election.model.messaging.PayloadVerificationException;
import ch.post.it.evoting.domain.election.payload.verify.PayloadVerifier;
import ch.post.it.evoting.domain.returncodes.ChoiceCodesVerificationDecryptResPayload;
import ch.post.it.evoting.domain.returncodes.ReturnCodesInput;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.domain.model.platform.PlatformCAEntity;
import ch.post.it.evoting.votingserver.commons.domain.model.platform.PlatformCARepository;
import ch.post.it.evoting.votingserver.commons.tracking.TrackIdInstance;
import ch.post.it.evoting.votingserver.voteverification.domain.model.choicecode.CodesDecryptionResults;
import ch.post.it.evoting.votingserver.voteverification.domain.model.content.VerificationContent;
import ch.post.it.evoting.votingserver.voteverification.domain.model.content.VerificationContentRepository;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verification.Verification;
import ch.post.it.evoting.votingserver.voteverification.infrastructure.remote.OrchestratorClient;

import okhttp3.ResponseBody;
import retrofit2.Call;

@RunWith(MockitoJUnitRunner.class)
public class CodesDecrypterServiceTest {

	private static final String TEST_ROOT_CA_CERTIFICATE = "-----BEGIN CERTIFICATE-----MIIDbzCCAlegAwIBAgIUXbBGcIUMqgpjO1tuS4id9XahygkwDQYJKoZIhvcNAQELBQAwXzEWMBQGA1UEAwwNU2N5dGwgUm9vdCBDQTEWMBQGA1UECwwNT25saW5lIFZvdGluZzEVMBMGA1UECgwMT3JnYW5pemF0aW9uMQkwBwYDVQQHDAAxCzAJBgNVBAYTAkVTMB4XDTE4MDcxMTEyMzMxNVoXDTE5MDcxMTEyMzMxNlowXzEWMBQGA1UEAwwNU2N5dGwgUm9vdCBDQTEWMBQGA1UECwwNT25saW5lIFZvdGluZzEVMBMGA1UECgwMT3JnYW5pemF0aW9uMQkwBwYDVQQHDAAxCzAJBgNVBAYTAkVTMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAi0mRnBYdh+BpA7MpZ1jatMiD6GR/8qT9nN76S3OtA16/Ho148U1GO1A2mOjB+QFPbo8/H3OeluvmQKLpkiYePgk3+XnPyu1jY12AJ4NLCqizi98HuJk9HFEEicWjscn8Xyh1XoiVvtq/lvEKUzzeX1ehDn316YkbXbaz9xtMOMMAs4TaUeEZaa5Vf0UQ00/M54+R/p8yrVkHpm8ffhWGSMAyhVU9DQh9vdgctHfNJ8io4H+5vPAVMYg7/bD76ColRgKq12vHacJKB6LjAleofvwpf7biw1PKrl1hQhMlyy6tIqdYP20MDHz5eE9BRi/E+x9C8qjyKBpDJ1g/fFBQFwIDAQABoyMwITAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwIBBjANBgkqhkiG9w0BAQsFAAOCAQEAVJs8AJzYG9n6jZRy4pxkiXz4/qEp91GFO1CLnvMcjqdkHot/rJVSmXPMuOrYGCWB12JLWcgmv4ez4JtLoULXrSqt7hqPrGJiCMNqp9UUEBlOzERDywR8vO54P7PWZyDXc5GW8EQDX5A26BJfNoXXDP4ajBGJLEaawyklWk2mLcMZ38F6RK+2gMighdO5QdEZyVqLurSqK/Zhqf1VnS9CO8tkH4JJWQbvPi/TujZKZRr9SVFtMmYzNxubX130AJwj81I2y5umrLHCw+eD31fSud5gLiHWIqbLcaYFethFpAnUbonLsAqvc/0pqU76oeCP+umNUZYyWllzPd0GGH1+MQ==-----END CERTIFICATE-----";

	@InjectMocks
	private final CodesDecrypterService codesDecrypterService = new CodesDecrypterService();

	@Mock
	private TrackIdInstance trackIdInstance;

	@Mock
	private VerificationContentRepository verificationContentRepository;

	@Mock
	private OrchestratorClient ccOrchestratorClient;

	@Mock
	private ProofsServiceAPI proofsService;

	@Mock
	private PlatformCARepository platformCARepository;

	@Mock
	private PayloadVerifier payloadVerifier;

	private String exponentiatedGamma;

	@Before
	public void setUp() throws ResourceNotFoundException, GeneralCryptoLibException, IOException, IllegalArgumentException, IllegalAccessException,
			NoSuchFieldException, SecurityException, PayloadVerificationException {

		GroupElementsCompressor<ZpGroupElement> groupElementsCompressor = new GroupElementsCompressor<>();

		Field groupElementsCompressorField = codesDecrypterService.getClass().getDeclaredField("groupElementsCompressor");
		groupElementsCompressorField.setAccessible(true);
		groupElementsCompressorField.set(codesDecrypterService, groupElementsCompressor);

		exponentiatedGamma = IOUtils.toString(this.getClass().getResourceAsStream("/exponentiatedGamma.json"), StandardCharsets.UTF_8);

		when(trackIdInstance.getTrackId()).thenReturn("trackId");

		ElGamalServiceAPI elgamalService = new ElGamalService();
		String pString = "16370518994319586760319791526293535327576438646782139419846004180837103527129035954742043590609421369665944746587885814920851694546456891767644945459124422553763416586515339978014154452159687109161090635367600349264934924141746082060353483306855352192358732451955232000593777554431798981574529854314651092086488426390776811367125009551346089319315111509277347117467107914073639456805159094562593954195960531136052208019343392906816001017488051366518122404819967204601427304267380238263913892658950281593755894747339126531018026798982785331079065126375455293409065540731646939808640273393855256230820509217411510058759";
		String qString = "8185259497159793380159895763146767663788219323391069709923002090418551763564517977371021795304710684832972373293942907460425847273228445883822472729562211276881708293257669989007077226079843554580545317683800174632467462070873041030176741653427676096179366225977616000296888777215899490787264927157325546043244213195388405683562504775673044659657555754638673558733553957036819728402579547281296977097980265568026104009671696453408000508744025683259061202409983602300713652133690119131956946329475140796877947373669563265509013399491392665539532563187727646704532770365823469904320136696927628115410254608705755029379";
		String gString = "2";
		BigInteger p = new BigInteger(pString);
		BigInteger q = new BigInteger(qString);
		BigInteger g = new BigInteger(gString);
		ElGamalEncryptionParameters encryptionParameters = new ElGamalEncryptionParameters(p, q, g);
		ElGamalKeyPair testKeyPair = elgamalService.generateKeyPair(encryptionParameters, 1);
		String publicKeyString = testKeyPair.getPublicKeys().toJson();

		ChoiceCodesVerificationDecryptResPayload payloadMock = new ChoiceCodesVerificationDecryptResPayload();
		payloadMock.setDecryptContributionResult(Arrays.asList(exponentiatedGamma, exponentiatedGamma, exponentiatedGamma));
		payloadMock.setPublicKeyJson(publicKeyString);
		payloadMock.setExponentiationProofJson(
				new Proof(new Exponent(BigInteger.ONE, BigInteger.ONE), Collections.singletonList(new Exponent(BigInteger.ONE, BigInteger.ONE)))
						.toJson());
		String serializedPayload = ObjectMappers.toJson(Collections.singletonList(payloadMock));
		ResponseBody responseBody = ResponseBody.create(okhttp3.MediaType.parse(MediaType.APPLICATION_JSON), serializedPayload);

		Call<ResponseBody> callMock = Mockito.mock(Call.class);
		when(callMock.execute()).thenReturn(retrofit2.Response.success(responseBody));

		when(ccOrchestratorClient.getChoiceCodeNodesDecryptContributions(any(), any(), any(), any(), any(), any(), any())).thenReturn(callMock);

		VoteVerificationContextData voteVerificationContextData = new VoteVerificationContextData();
		voteVerificationContextData
				.setNonCombinedChoiceCodesEncryptionPublicKeys(Base64.getEncoder().encodeToString(publicKeyString.getBytes(StandardCharsets.UTF_8)));
		voteVerificationContextData.setEncryptionParameters(new EncryptionParameters(pString, qString, gString));
		VerificationContent verificationContent = new VerificationContent();
		verificationContent.setJson(ObjectMappers.toJson(voteVerificationContextData));
		when(verificationContentRepository.findByTenantIdElectionEventIdVerificationCardSetId(any(), any(), any())).thenReturn(verificationContent);
		Verification verification = new Verification();
		verification.setVerificationCardSetId("1");

		PlatformCAEntity platformCAEntity = new PlatformCAEntity();
		platformCAEntity.setCertificateContent(TEST_ROOT_CA_CERTIFICATE);
		when(platformCARepository.getRootCACertificate()).thenReturn(platformCAEntity);
		when(payloadVerifier.isValid(any(), any())).thenReturn(true);

		ProofVerifierAPI proofVerifier = Mockito.mock(ProofVerifierAPI.class);

		when(proofsService.createProofVerifierAPI(any())).thenReturn(proofVerifier);
		when(proofVerifier.verifyExponentiationProof(any(), any(), any())).thenReturn(true);

	}

	@Test
	public void testDecryptPartialCodesWithMoreKeysThanCodes()
			throws ResourceNotFoundException, CryptographicOperationException, GeneralCryptoLibException, SecurityException,
			IllegalArgumentException {

		ZpGroupElement zpGroupElement = ZpGroupElement.fromJson(exponentiatedGamma);

		BigInteger g = new BigInteger("2");
		BigInteger p = zpGroupElement.getP();
		BigInteger q = zpGroupElement.getQ();

		BigInteger expectedValue1 = new BigInteger("246913575308642");
		BigInteger expectedValue2 = new BigInteger("4115226213991770781893");

		ZpSubgroup group = new ZpSubgroup(g, p, q);

		List<ZpGroupElement> elements = asList(new ZpGroupElement(new BigInteger("11111111"), group),
				new ZpGroupElement(new BigInteger("22222222"), group), new ZpGroupElement(new BigInteger("33333333"), group));

		ReturnCodesInput input = new ReturnCodesInput();
		input.setReturnCodesInputElements(Collections.singletonList(elements.get(0).getValue()));

		CodesDecryptionResults decryptPartialCodes = codesDecrypterService
				.decryptPartialCodes("100", "9914226cae8a48c796015dd74f7c0fa3", "123", "123", elements, input);

		List<ZpGroupElement> results = decryptPartialCodes.getCombinedZpGroupElementLists();

		Assert.assertEquals(p, results.get(0).getP());
		Assert.assertEquals(q, results.get(0).getQ());
		Assert.assertEquals(expectedValue1, results.get(0).getValue());
		Assert.assertEquals(expectedValue2, results.get(1).getValue());
	}
}

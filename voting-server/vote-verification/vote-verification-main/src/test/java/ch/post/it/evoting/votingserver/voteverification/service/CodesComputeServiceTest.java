/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.utils.CryptographicOperationException;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofVerifierAPI;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;
import ch.post.it.evoting.domain.election.model.messaging.PayloadVerificationException;
import ch.post.it.evoting.domain.election.payload.verify.PayloadVerifier;
import ch.post.it.evoting.domain.returncodes.ReturnCodesExponentiationResponsePayload;
import ch.post.it.evoting.domain.returncodes.ReturnCodesInput;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.domain.model.platform.PlatformCAEntity;
import ch.post.it.evoting.votingserver.commons.domain.model.platform.PlatformCARepository;
import ch.post.it.evoting.votingserver.commons.tracking.TrackIdInstance;
import ch.post.it.evoting.votingserver.voteverification.domain.model.choicecode.CodesComputeResults;
import ch.post.it.evoting.votingserver.voteverification.infrastructure.remote.OrchestratorClient;

import okhttp3.ResponseBody;
import retrofit2.Call;

@RunWith(MockitoJUnitRunner.class)
public class CodesComputeServiceTest {

	private static final String TEST_ROOT_CA_CERTIFICATE = "-----BEGIN CERTIFICATE-----MIIDbzCCAlegAwIBAgIUXbBGcIUMqgpjO1tuS4id9XahygkwDQYJKoZIhvcNAQELBQAwXzEWMBQGA1UEAwwNU2N5dGwgUm9vdCBDQTEWMBQGA1UECwwNT25saW5lIFZvdGluZzEVMBMGA1UECgwMT3JnYW5pemF0aW9uMQkwBwYDVQQHDAAxCzAJBgNVBAYTAkVTMB4XDTE4MDcxMTEyMzMxNVoXDTE5MDcxMTEyMzMxNlowXzEWMBQGA1UEAwwNU2N5dGwgUm9vdCBDQTEWMBQGA1UECwwNT25saW5lIFZvdGluZzEVMBMGA1UECgwMT3JnYW5pemF0aW9uMQkwBwYDVQQHDAAxCzAJBgNVBAYTAkVTMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAi0mRnBYdh+BpA7MpZ1jatMiD6GR/8qT9nN76S3OtA16/Ho148U1GO1A2mOjB+QFPbo8/H3OeluvmQKLpkiYePgk3+XnPyu1jY12AJ4NLCqizi98HuJk9HFEEicWjscn8Xyh1XoiVvtq/lvEKUzzeX1ehDn316YkbXbaz9xtMOMMAs4TaUeEZaa5Vf0UQ00/M54+R/p8yrVkHpm8ffhWGSMAyhVU9DQh9vdgctHfNJ8io4H+5vPAVMYg7/bD76ColRgKq12vHacJKB6LjAleofvwpf7biw1PKrl1hQhMlyy6tIqdYP20MDHz5eE9BRi/E+x9C8qjyKBpDJ1g/fFBQFwIDAQABoyMwITAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwIBBjANBgkqhkiG9w0BAQsFAAOCAQEAVJs8AJzYG9n6jZRy4pxkiXz4/qEp91GFO1CLnvMcjqdkHot/rJVSmXPMuOrYGCWB12JLWcgmv4ez4JtLoULXrSqt7hqPrGJiCMNqp9UUEBlOzERDywR8vO54P7PWZyDXc5GW8EQDX5A26BJfNoXXDP4ajBGJLEaawyklWk2mLcMZ38F6RK+2gMighdO5QdEZyVqLurSqK/Zhqf1VnS9CO8tkH4JJWQbvPi/TujZKZRr9SVFtMmYzNxubX130AJwj81I2y5umrLHCw+eD31fSud5gLiHWIqbLcaYFethFpAnUbonLsAqvc/0pqU76oeCP+umNUZYyWllzPd0GGH1+MQ==-----END CERTIFICATE-----";
	@InjectMocks
	private final CodesComputeService codesComputeService = new CodesComputeService();
	@Mock
	private TrackIdInstance trackIdInstance;
	@Mock
	private OrchestratorClient ccOrchestratorClient;
	@Mock
	private PlatformCARepository platformCARepository;
	@Mock
	private PayloadVerifier payloadVerifier;
	private String exponentiatedGamma;

	@Before
	public void setUp() throws ResourceNotFoundException, GeneralCryptoLibException, IOException, IllegalArgumentException, SecurityException,
			PayloadVerificationException {

		exponentiatedGamma = IOUtils.toString(this.getClass().getResourceAsStream("/exponentiatedGamma.json"), StandardCharsets.UTF_8);

		when(trackIdInstance.getTrackId()).thenReturn("trackId");

		ReturnCodesExponentiationResponsePayload payload = new ReturnCodesExponentiationResponsePayload();
		payload.setVoterVoteCastReturnCodeGenerationPublicKeyJson("derivedKeyJson");
		payload.setExponentiationProofJson(
				new Proof(new Exponent(BigInteger.ONE, BigInteger.ONE), Collections.singletonList(new Exponent(BigInteger.ONE, BigInteger.ONE)))
						.toJson());

		Map<BigInteger, BigInteger> primeToComputedPrime = new HashMap<>();
		primeToComputedPrime.put(new BigInteger("617"), new BigInteger("953"));
		primeToComputedPrime.put(new BigInteger("9421"), new BigInteger("9739"));
		payload.setPccOrCkToLongReturnCodeShare(primeToComputedPrime);

		List<ReturnCodesExponentiationResponsePayload> payloadList = Arrays.asList(payload, payload);

		byte[] serializedPayload;

		try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
			oos.writeObject(payloadList);
			serializedPayload = bos.toByteArray();
		}

		ResponseBody responseBody = ResponseBody.create(okhttp3.MediaType.parse(MediaType.APPLICATION_OCTET_STREAM), serializedPayload);

		Call<ResponseBody> callMock = Mockito.mock(Call.class);
		when(callMock.execute()).thenReturn(retrofit2.Response.success(responseBody));

		when(ccOrchestratorClient.getChoiceCodeNodesComputeContributions(any(), any(), any(), any(), any(), any(), any())).thenReturn(callMock);

		PlatformCAEntity platformCAEntity = new PlatformCAEntity();

		platformCAEntity.setCertificateContent(TEST_ROOT_CA_CERTIFICATE);
		when(platformCARepository.getRootCACertificate()).thenReturn(platformCAEntity);
		when(payloadVerifier.isValid(any(), any())).thenReturn(true);

		ProofVerifierAPI proofVerifier = Mockito.mock(ProofVerifierAPI.class);
	}

	@Test
	public void testComputeCodesSuccessful()
			throws ResourceNotFoundException, CryptographicOperationException, GeneralCryptoLibException, SecurityException,
			IllegalArgumentException {

		ZpGroupElement zpGroupElement = ZpGroupElement.fromJson(exponentiatedGamma);

		BigInteger g = new BigInteger("2");
		BigInteger p = zpGroupElement.getP();
		BigInteger q = zpGroupElement.getQ();

		ZpSubgroup group = new ZpSubgroup(g, p, q);

		ReturnCodesInput returnCodesInput = new ReturnCodesInput();
		returnCodesInput.setReturnCodesInputElements(Arrays.asList(new BigInteger("617"), new BigInteger("9421")));

		CodesComputeResults result = codesComputeService
				.computePartialCodes("100", "9914226cae8a48c796015dd74f7c0fa3", "123", "123", group, returnCodesInput);

		Assert.assertEquals(new BigInteger("908209"), result.getCombinedPartialChoiceCodes().get(new BigInteger("617")).getValue());
		Assert.assertEquals(new BigInteger("94848121"), result.getCombinedPartialChoiceCodes().get(new BigInteger("9421")).getValue());
	}

}

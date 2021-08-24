/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.service;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.utils.CryptographicOperationException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.returncode.VoterCodesService;
import ch.post.it.evoting.domain.election.model.confirmation.TraceableConfirmationMessage;
import ch.post.it.evoting.domain.returncodes.CastCodeAndComputeResults;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.voteverification.domain.model.choicecode.CodesComputeResults;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verification.Verification;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verification.VerificationRepository;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset.VerificationSetEntity;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset.VerificationSetRepository;

@RunWith(MockitoJUnitRunner.class)
public class CastCodesServiceImplTest {

	private final static String TENANT_ID = "1";
	private final static String ELECTION_EVENT_ID = "2";
	private final static String VERIFICATION_CARD_ID = "3";
	private final static String VERIFICATION_CARD_SET_ID = "5";
	private final static String VOTE_CAST_CODE = Base64.getEncoder().encodeToString("789".getBytes(StandardCharsets.UTF_8));
	private final static String VOTE_CAST_SIGNATURE_BASE64 = Base64.getEncoder().encodeToString("789".getBytes(StandardCharsets.UTF_8));
	@InjectMocks
	CastCodeService sut = new CastCodesServiceImpl();
	@Mock
	private VerificationRepository verificationRepository;
	// The service used to calculate long choice codes
	@Mock
	private VoterCodesService voterCodesService;
	// The service used to retrieve short choice codes
	@Mock
	private ShortCodesService shortCodesService;
	// The verification set repository that will give us the verification card
	// set for the voter verification card
	@Mock
	private VerificationSetRepository verificationSetRepository;
	@Mock
	private CodesComputeService codesComputeService;

	@Test
	public void testRetrieveCastCode_Successful()
			throws ResourceNotFoundException, CryptographicOperationException, GeneralCryptoLibException, ClassNotFoundException, IOException {

		Verification verificationMock = new Verification();
		verificationMock.setVerificationCardSetId(VERIFICATION_CARD_SET_ID);

		VerificationSetEntity verificationSetMock = new VerificationSetEntity();
		verificationSetMock.setJson(prepareVerificationSetMockJSON());

		when(verificationRepository.findByTenantIdElectionEventIdVerificationCardId(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_ID))
				.thenReturn(verificationMock);
		when(verificationSetRepository.findByTenantIdElectionEventIdVerificationCardSetId(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID))
				.thenReturn(verificationSetMock);
		when(voterCodesService.generateLongReturnCode(eq(ELECTION_EVENT_ID), eq(VERIFICATION_CARD_ID), any(), any()))
				.thenReturn(prepareLongCodeMock());
		when(shortCodesService.retrieveShortCodes(eq(TENANT_ID), eq(ELECTION_EVENT_ID), eq(VERIFICATION_CARD_ID), any()))
				.thenReturn(prepareShortCodeMock());
		Map<BigInteger, ZpGroupElement> codes = new HashMap<>();
		codes.put(new BigInteger("789"), new ZpGroupElement(new BigInteger("788"), new BigInteger("789"), new BigInteger("1")));
		CodesComputeResults computeResults = new CodesComputeResults(codes, null);
		when(codesComputeService.computePartialCodes(any(), any(), any(), any(), any(), any())).thenReturn(computeResults);

		TraceableConfirmationMessage castCodeMessage = new TraceableConfirmationMessage();
		castCodeMessage.setConfirmationKey(VOTE_CAST_CODE);
		CastCodeAndComputeResults castCodeMessageResult = sut.retrieveCastCode(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_ID, castCodeMessage);

		assertThat(castCodeMessageResult.getVoteCastCode(), is(VOTE_CAST_CODE));
	}

	@Test(expected = ResourceNotFoundException.class)
	public void testRetrieveCastCode_ShortCodesResourceNotFound()
			throws ResourceNotFoundException, CryptographicOperationException, GeneralCryptoLibException, ClassNotFoundException, IOException {

		Verification verificationMock = new Verification();
		verificationMock.setVerificationCardSetId(VERIFICATION_CARD_SET_ID);

		VerificationSetEntity verificationSetMock = new VerificationSetEntity();
		verificationSetMock.setJson(prepareVerificationSetMockJSON());

		when(verificationRepository.findByTenantIdElectionEventIdVerificationCardId(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_ID))
				.thenReturn(verificationMock);
		when(verificationSetRepository.findByTenantIdElectionEventIdVerificationCardSetId(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID))
				.thenReturn(verificationSetMock);
		when(voterCodesService.generateLongReturnCode(eq(ELECTION_EVENT_ID), eq(VERIFICATION_CARD_ID), any(), any()))
				.thenReturn(prepareLongCodeMock());
		Map<BigInteger, ZpGroupElement> codes = new HashMap<>();
		codes.put(new BigInteger("789"), new ZpGroupElement(new BigInteger("788"), new BigInteger("789"), new BigInteger("1")));
		CodesComputeResults computeResults = new CodesComputeResults(codes, null);
		when(codesComputeService.computePartialCodes(any(), any(), any(), any(), any(), any())).thenReturn(computeResults);
		when(shortCodesService.retrieveShortCodes(eq(TENANT_ID), eq(ELECTION_EVENT_ID), eq(VERIFICATION_CARD_ID), any()))
				.thenThrow(ResourceNotFoundException.class);
		TraceableConfirmationMessage castCodeMessage = new TraceableConfirmationMessage();
		castCodeMessage.setConfirmationKey(VOTE_CAST_CODE);
		sut.retrieveCastCode(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_ID, castCodeMessage);
	}

	private byte[] prepareLongCodeMock() {
		return new byte[] {};
	}

	private List<String> prepareShortCodeMock() {
		List<String> shortCodes = new ArrayList<>();
		shortCodes.add(VOTE_CAST_CODE);
		return shortCodes;
	}

	private String prepareVerificationSetMockJSON() throws GeneralCryptoLibException {
		List<ZpGroupElement> keyElements = new ArrayList<>();
		keyElements.add(new ZpGroupElement(new BigInteger("2"), new BigInteger("7"), new BigInteger("3")));
		ZpSubgroup zpSubgroup = new ZpSubgroup(new BigInteger("2"), new BigInteger("7"), new BigInteger("3"));
		String elGamalPkJson = new ElGamalPublicKey(keyElements, zpSubgroup).toJson();
		String elGamalPkJson_Base64 = Base64.getEncoder().encodeToString(elGamalPkJson.getBytes(StandardCharsets.UTF_8));

		JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
		jsonObjectBuilder.add(CastCodesServiceImpl.PUBLIC_KEY, elGamalPkJson_Base64);

		return jsonObjectBuilder.build().toString();
	}

}

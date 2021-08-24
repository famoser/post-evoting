/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.model.rule;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.service.certificate.X509CertificateService;

/**
 * Test class for the verification of signature within the vote
 */
@RunWith(MockitoJUnitRunner.class)
public class VerifySignatureRuleTest {

	@InjectMocks
	public VerifySignatureRule rule = new VerifySignatureRule();
	@Rule
	public ExpectedException expected = ExpectedException.none();
	@Mock
	AsymmetricServiceAPI asymmetricServiceAPI;

	@Mock
	X509CertificateService certificateFactory;
	@Mock
	private Logger LOGGER;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this.getClass());
	}

	@Test
	public void test() throws GeneralCryptoLibException, CertificateException {
		Vote vote = new Vote();
		vote.setTenantId("100");
		vote.setElectionEventId("f86b9967f5f14a01b9f70c6277c38329");
		vote.setVotingCardId("14d96e761e571a08769dc4337a2ce98b");
		vote.setBallotBoxId("b972e500fe554ed7b24e54814e9b3c85");
		vote.setBallotId("0edd4256df7d4b17a148f218080abea9");
		vote.setCredentialId("0edc375a02584cb09f35a9b956bc768f");
		String authTokenString = "token";
		vote.setSignature(Base64.getEncoder().encodeToString("signature".getBytes(StandardCharsets.UTF_8)));
		vote.setCertificate("certificate");

		vote.setAuthenticationToken(authTokenString);
		doReturn(true).when(asymmetricServiceAPI).verifySignature(any(), any(), (byte[]) any());
		doReturn(mock(X509Certificate.class)).when(certificateFactory).generateCertificate(any());
		ValidationError result = rule.execute(vote);
		assertEquals(result.getValidationErrorType(), ValidationErrorType.SUCCESS);
	}
}

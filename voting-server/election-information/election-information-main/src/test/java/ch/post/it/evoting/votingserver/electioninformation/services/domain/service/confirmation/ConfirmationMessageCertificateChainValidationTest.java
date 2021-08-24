/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.service.confirmation;

import static ch.post.it.evoting.votingserver.electioninformation.services.domain.model.util.Constants.ELECTION_EVENT_ID;
import static ch.post.it.evoting.votingserver.electioninformation.services.domain.model.util.Constants.TENANT_ID;
import static ch.post.it.evoting.votingserver.electioninformation.services.domain.model.util.Constants.VOTING_CARD_ID;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.cert.X509Certificate;

import javax.json.Json;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.bean.CertificateParameters;
import ch.post.it.evoting.cryptolib.certificates.bean.X509CertificateType;
import ch.post.it.evoting.cryptolib.certificates.bean.X509DistinguishedName;
import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.domain.election.model.authentication.AuthenticationToken;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.votingserver.commons.beans.confirmation.ConfirmationInformation;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.beans.validation.CertificateValidationService;
import ch.post.it.evoting.votingserver.commons.util.CryptoUtils;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionInformationContent;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionInformationContentRepository;

@RunWith(MockitoJUnitRunner.class)
public class ConfirmationMessageCertificateChainValidationTest {

	public static final String COMMON_NAME = "commonName";

	private static final String ELECTION_ROOT_CA = "electionRootCA";
	private static final String CREDENTIALS_CA = "credentialsCA";

	@InjectMocks
	private final ConfirmationMessageCertificateChainValidation validation = new ConfirmationMessageCertificateChainValidation();

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Mock
	private ElectionInformationContentRepository electionInformationContentRepository;

	@Mock
	private ConfirmationInformation confirmationInformation;

	@Mock
	private AuthenticationToken authenticationToken;

	@Mock
	private ElectionInformationContent electionInformationContent;

	@Mock
	private CertificateValidationService certificateValidationService;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this.getClass());
	}

	@Test
	public void testValidationOK() throws ResourceNotFoundException, GeneralCryptoLibException {

		when(electionInformationContentRepository.findByTenantIdElectionEventId(anyString(), anyString())).thenReturn(electionInformationContent);

		final CryptoAPIX509Certificate certificate = getCertificate();
		String certificateAsString = new String(certificate.getPemEncoded(), StandardCharsets.UTF_8);

		final String json = getJson(certificateAsString);
		when(electionInformationContent.getJson()).thenReturn(json);
		when(confirmationInformation.getCertificate()).thenReturn(certificateAsString);

		doReturn(true).when(certificateValidationService)
				.validateCertificateChain(any(X509Certificate.class), any(X509DistinguishedName.class), eq(X509CertificateType.CERTIFICATE_AUTHORITY),
						any(X509Certificate[].class), any(X509DistinguishedName[].class), any(X509Certificate.class));

		doReturn(true).when(certificateValidationService)
				.validateCertificateChain(any(X509Certificate.class), any(X509DistinguishedName.class), eq(X509CertificateType.SIGN),
						any(X509Certificate[].class), any(X509DistinguishedName[].class), any(X509Certificate.class));

		final ValidationError execute = validation
				.execute(TENANT_ID, ELECTION_EVENT_ID, VOTING_CARD_ID, confirmationInformation, authenticationToken);
		assertEquals(ValidationErrorType.SUCCESS, execute.getValidationErrorType());

	}

	@Test
	public void testValidationFailsInCA() throws ResourceNotFoundException, GeneralCryptoLibException {

		when(electionInformationContentRepository.findByTenantIdElectionEventId(anyString(), anyString())).thenReturn(electionInformationContent);

		final CryptoAPIX509Certificate certificate = getCertificate();
		String certificateAsString = new String(certificate.getPemEncoded(), StandardCharsets.UTF_8);

		final String json = getJson(certificateAsString);
		when(electionInformationContent.getJson()).thenReturn(json);

		doReturn(false).when(certificateValidationService)
				.validateCertificateChain(any(X509Certificate.class), any(X509DistinguishedName.class), eq(X509CertificateType.CERTIFICATE_AUTHORITY),
						any(X509Certificate[].class), any(X509DistinguishedName[].class), any(X509Certificate.class));

		final ValidationError execute = validation
				.execute(TENANT_ID, ELECTION_EVENT_ID, VOTING_CARD_ID, confirmationInformation, authenticationToken);
		assertEquals(ValidationErrorType.FAILED, execute.getValidationErrorType());

	}

	@Test
	public void testValidationFailsInChainValidation() throws ResourceNotFoundException, GeneralCryptoLibException {

		when(electionInformationContentRepository.findByTenantIdElectionEventId(anyString(), anyString())).thenReturn(electionInformationContent);

		final CryptoAPIX509Certificate certificate = getCertificate();
		String certificateAsString = new String(certificate.getPemEncoded(), StandardCharsets.UTF_8);

		final String json = getJson(certificateAsString);
		when(electionInformationContent.getJson()).thenReturn(json);
		when(confirmationInformation.getCertificate()).thenReturn(certificateAsString);

		doReturn(true).when(certificateValidationService)
				.validateCertificateChain(any(X509Certificate.class), any(X509DistinguishedName.class), eq(X509CertificateType.CERTIFICATE_AUTHORITY),
						any(X509Certificate[].class), any(X509DistinguishedName[].class), any(X509Certificate.class));

		doReturn(false).when(certificateValidationService)
				.validateCertificateChain(any(X509Certificate.class), any(X509DistinguishedName.class), eq(X509CertificateType.SIGN),
						any(X509Certificate[].class), any(X509DistinguishedName[].class), any(X509Certificate.class));

		final ValidationError execute = validation
				.execute(TENANT_ID, ELECTION_EVENT_ID, VOTING_CARD_ID, confirmationInformation, authenticationToken);
		assertEquals(ValidationErrorType.FAILED, execute.getValidationErrorType());

	}

	@Test
	public void testValidationThrowsCryptoLibException() throws ResourceNotFoundException, GeneralCryptoLibException {

		when(electionInformationContentRepository.findByTenantIdElectionEventId(anyString(), anyString())).thenReturn(electionInformationContent);

		final CryptoAPIX509Certificate certificate = getCertificate();
		String certificateAsString = new String(certificate.getPemEncoded(), StandardCharsets.UTF_8);

		final String json = getJson(certificateAsString);
		when(electionInformationContent.getJson()).thenReturn(json);

		doThrow(GeneralCryptoLibException.class).when(certificateValidationService)
				.validateCertificateChain(any(X509Certificate.class), any(X509DistinguishedName.class), eq(X509CertificateType.CERTIFICATE_AUTHORITY),
						any(X509Certificate[].class), any(X509DistinguishedName[].class), any(X509Certificate.class));

		final ValidationError execute = validation
				.execute(TENANT_ID, ELECTION_EVENT_ID, VOTING_CARD_ID, confirmationInformation, authenticationToken);
		assertEquals(ValidationErrorType.FAILED, execute.getValidationErrorType());

	}

	@Test
	public void testValidationThrowsResourceNotFoundException() throws ResourceNotFoundException {

		when(electionInformationContentRepository.findByTenantIdElectionEventId(anyString(), anyString()))
				.thenThrow(new ResourceNotFoundException("exception"));

		final ValidationError execute = validation
				.execute(TENANT_ID, ELECTION_EVENT_ID, VOTING_CARD_ID, confirmationInformation, authenticationToken);
		assertEquals(ValidationErrorType.FAILED, execute.getValidationErrorType());

	}

	private CryptoAPIX509Certificate getCertificate() throws GeneralCryptoLibException {
		final KeyPair keyPairForSigning = CryptoUtils.getKeyPairForSigning();
		return CryptoUtils.createCryptoAPIx509Certificate(COMMON_NAME, CertificateParameters.Type.SIGN, keyPairForSigning);

	}

	private String getJson(String certificate) {

		return Json.createObjectBuilder().add(CREDENTIALS_CA, certificate).add(ELECTION_ROOT_CA, certificate).build().toString();

	}

}

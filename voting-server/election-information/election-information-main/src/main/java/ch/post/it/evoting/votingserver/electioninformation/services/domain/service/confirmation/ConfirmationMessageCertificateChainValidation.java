/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.service.confirmation;

import java.security.cert.X509Certificate;

import javax.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.bean.X509CertificateType;
import ch.post.it.evoting.cryptolib.certificates.bean.X509DistinguishedName;
import ch.post.it.evoting.cryptolib.certificates.factory.CryptoX509Certificate;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.domain.election.model.authentication.AuthenticationToken;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.votingserver.commons.beans.confirmation.ConfirmationInformation;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.beans.validation.CertificateValidationService;
import ch.post.it.evoting.votingserver.commons.util.JsonUtils;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.confirmation.ConfirmationMessageValidation;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionInformationContent;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionInformationContentRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.platform.EiCertificateValidationService;

/**
 * Implements the credential ID certificate chain validation of the confirmation message.
 */
public class ConfirmationMessageCertificateChainValidation implements ConfirmationMessageValidation {

	private static final String ELECTION_ROOT_CA = "electionRootCA";

	private static final String CREDENTIALS_CA = "credentialsCA";

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmationMessageCertificateChainValidation.class);

	@Inject
	private ElectionInformationContentRepository electionInformationContentRepository;

	@Inject
	@EiCertificateValidationService
	private CertificateValidationService certificateValidationService;

	/**
	 * Validates the credential ID certificate chain.
	 *
	 * @param tenantId                - the tenant identifier.
	 * @param electionEventId         - the election event identifier.
	 * @param votingCardId            - the voting card identifier.
	 * @param confirmationInformation - the confirmation information to be validated.
	 * @param authenticationToken     - the authentication token.
	 * @return A ValidationError describing whether the validation was successful or not.
	 */
	@Override
	public ValidationError execute(String tenantId, String electionEventId, String votingCardId, ConfirmationInformation confirmationInformation,
			AuthenticationToken authenticationToken) {
		ValidationError result = new ValidationError();

		try {
			// Get election information content from repository.
			ElectionInformationContent electionInformationContent = electionInformationContentRepository
					.findByTenantIdElectionEventId(tenantId, electionEventId);
			String electionInformationContentJson = electionInformationContent.getJson();

			// Get election root CA and its subject distinguished name.
			String electionRootCaPem = JsonUtils.getJsonObject(electionInformationContentJson).getString(ELECTION_ROOT_CA);
			X509Certificate electionRootCa = (X509Certificate) PemUtils.certificateFromPem(electionRootCaPem);
			X509DistinguishedName electionRootCaSubjectDn = new CryptoX509Certificate(electionRootCa).getSubjectDn();

			// Verify that election root CA is a valid certificate authority.
			X509Certificate[] electionRootCaCertChain = {};
			X509DistinguishedName[] electionRootCaSubjectDnChain = {};

			boolean electionRootCaIsValid = certificateValidationService
					.validateCertificateChain(electionRootCa, electionRootCaSubjectDn, X509CertificateType.CERTIFICATE_AUTHORITY,
							electionRootCaCertChain, electionRootCaSubjectDnChain, electionRootCa);

			if (electionRootCaIsValid) {
				// Get credential ID certificate and its subject distinguished name.
				String credentialCertificatePem = confirmationInformation.getCertificate();
				X509Certificate credentialIdCertificate = (X509Certificate) PemUtils.certificateFromPem(credentialCertificatePem);
				X509DistinguishedName credentialIdCertificateSubjectDn = new CryptoX509Certificate(credentialIdCertificate).getSubjectDn();

				// Get credentials CA (an intermediate certificate) and its subject distinguished name.
				String credentialsCaPem = JsonUtils.getJsonObject(electionInformationContentJson).getString(CREDENTIALS_CA);
				X509Certificate credentialsCa = (X509Certificate) PemUtils.certificateFromPem(credentialsCaPem);
				X509DistinguishedName credentialsCaSubjectDn = new CryptoX509Certificate(credentialsCa).getSubjectDn();

				// Construct intermediate certificate chain.
				X509Certificate[] intermediateCertChain = { credentialsCa };
				X509DistinguishedName[] intermediateCertSubjectDnChain = { credentialsCaSubjectDn };

				// Validate credential ID certificate chain.
				boolean certificateChainIsValid = certificateValidationService
						.validateCertificateChain(credentialIdCertificate, credentialIdCertificateSubjectDn, X509CertificateType.SIGN,
								intermediateCertChain, intermediateCertSubjectDnChain, electionRootCa);
				if (certificateChainIsValid) {
					result.setValidationErrorType(ValidationErrorType.SUCCESS);
				} else {
					result.setValidationErrorType(ValidationErrorType.FAILED);
					result.setErrorArgs(new String[] { "Credential ID certificate chain validation failed." });
				}
			} else {
				result.setValidationErrorType(ValidationErrorType.FAILED);
				result.setErrorArgs(new String[] { "Election root CA is not a valid certificate authority." });
			}
		} catch (GeneralCryptoLibException e1) {
			LOGGER.error("Cryptographic error:", e1);
			result.setValidationErrorType(ValidationErrorType.FAILED);
			result.setErrorArgs(new String[] { "Cryptographic error:" + ExceptionUtils.getRootCauseMessage(e1) });
		} catch (ResourceNotFoundException e2) {
			LOGGER.error("Certificate not found: ", e2);
			result.setValidationErrorType(ValidationErrorType.FAILED);
			result.setErrorArgs(new String[] { "Certificate not found: " + ExceptionUtils.getRootCauseMessage(e2) });
		}

		return result;
	}

}

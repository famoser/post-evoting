/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.service.confirmation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.factory.CryptoX509Certificate;
import ch.post.it.evoting.domain.election.model.authentication.AuthenticationToken;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.votingserver.commons.beans.confirmation.ConfirmationInformation;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.confirmation.ConfirmationMessageValidation;

/**
 * This class implements the credential id validation of the confirmation message.
 */
public class ConfirmationMessageCredentialIdValidation implements ConfirmationMessageValidation {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmationMessageCredentialIdValidation.class);

	/**
	 * This method implements the validation of confirmation message credential id.
	 *
	 * @param tenantId                - the tenant identifier.
	 * @param electionEventId         - the election event identifier.
	 * @param votingCardId            - the voting card identifier.
	 * @param confirmationInformation - the confirmation information to be validated.
	 * @param authenticationToken     - the authentication token.
	 * @return true, if successfully validated. Otherwise, false.
	 */
	@Override
	public ValidationError execute(String tenantId, String electionEventId, String votingCardId, ConfirmationInformation confirmationInformation,
			AuthenticationToken authenticationToken) {

		ValidationError result = new ValidationError();
		try {
			// #7213 verify credentialid from authtoken matches credentialid from confirmation info
			if (!confirmationInformation.getCredentialId().equalsIgnoreCase(authenticationToken.getVoterInformation().getCredentialId())) {
				result.setValidationErrorType(ValidationErrorType.FAILED);
				return result;
			}

			String certificateString = confirmationInformation.getCertificate();

			// Obtain certificate
			try (InputStream inputStream = new ByteArrayInputStream(certificateString.getBytes(StandardCharsets.UTF_8))) {
				CertificateFactory cf = CertificateFactory.getInstance("X.509");
				X509Certificate certificate = (X509Certificate) cf.generateCertificate(inputStream);

				CryptoX509Certificate wrappedCertificate = new CryptoX509Certificate(certificate);
				String credentialToVerify = wrappedCertificate.getSubjectDn().getCommonName();

				if (credentialToVerify.contains(confirmationInformation.getCredentialId())) {
					result.setValidationErrorType(ValidationErrorType.SUCCESS);
				}
			}
		} catch (IOException | CertificateException | GeneralCryptoLibException e) {
			LOGGER.error("Failed to execute validation", e);
		}

		return result;
	}
}

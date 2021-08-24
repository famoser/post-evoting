/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.service.confirmation;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.domain.election.model.authentication.AuthenticationToken;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.votingserver.commons.beans.confirmation.ConfirmationInformation;
import ch.post.it.evoting.votingserver.commons.logging.service.I18nLoggerMessages;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.confirmation.ConfirmationMessageValidation;

/**
 * This class implements the signature validation of the confirmation message.
 */
public class ConfirmationMessageSignatureValidation implements ConfirmationMessageValidation {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmationMessageSignatureValidation.class);

	private static final I18nLoggerMessages I18N = I18nLoggerMessages.getInstance();

	@Inject
	private AsymmetricServiceAPI asymmetricService;

	/**
	 * This method implements the validation of signature.
	 *
	 * @param tenantId                - the tenant identifier.
	 * @param electionEventId         - the election event identifier.
	 * @param votingCardId            - the voting card identifier.
	 * @param confirmationInformation - the confirmation information to be validated.
	 * @param authenticationToken     - the authentication token.
	 * @return A ValidationError describing if the rule is satisfied or not.
	 */
	@Override
	public ValidationError execute(String tenantId, String electionEventId, String votingCardId, ConfirmationInformation confirmationInformation,
			AuthenticationToken authenticationToken) {
		ValidationError result = new ValidationError();
		try {
			// get public key
			String certificateString = confirmationInformation.getCertificate();

			// Obtain certificate
			InputStream inputStream = new ByteArrayInputStream(certificateString.getBytes(StandardCharsets.UTF_8));
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			X509Certificate certificate = (X509Certificate) cf.generateCertificate(inputStream);
			PublicKey publicKey = certificate.getPublicKey();

			// verify signature
			byte[] confirmationMessageSignatureBytes = Base64.decode(confirmationInformation.getConfirmationMessage().getSignature());

			String authTokenSignature = authenticationToken.getSignature();

			byte[] confirmationMessageDataToVerify = StringUtils
					.join(confirmationInformation.getConfirmationMessage().getConfirmationKey(), authTokenSignature, votingCardId, electionEventId)
					.getBytes(StandardCharsets.UTF_8);

			if (asymmetricService.verifySignature(confirmationMessageSignatureBytes, publicKey, confirmationMessageDataToVerify)) {
				result.setValidationErrorType(ValidationErrorType.SUCCESS);
			}
		} catch (GeneralCryptoLibException | CertificateException e) {
			LOGGER.error(I18N.getMessage("AuthenticationTokenSignatureValidation.execute.errorValidatingSignature"), tenantId, electionEventId,
					votingCardId, e);
		}
		return result;
	}
}

/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.model.rule;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.factory.CryptoX509Certificate;
import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.votingserver.commons.domain.model.rule.AbstractRule;

/**
 * The Class implementing the rule for validating the credential id in the certificate used for
 * verify the signature.
 */
public class CredentialIdRule implements AbstractRule<Vote> {

	private static final Logger LOGGER = LoggerFactory.getLogger(CredentialIdRule.class);

	/**
	 * return the rule name.
	 *
	 * @return the rule name.
	 */
	// @Override
	@Override
	public String getName() {
		return RuleNames.VOTE_CREDENTIAL_ID.getText();
	}

	/**
	 * Validates if the credential id is contained in the certificate used for verify.
	 *
	 * @param vote The vote to which to apply the rule.
	 * @return true, if successful.
	 */
	@Override
	public ValidationError execute(Vote vote) {
		// validation result. By default is set to false
		ValidationError result = new ValidationError();
		result.setValidationErrorType(ValidationErrorType.FAILED);

		try {
			String certificateString = vote.getCertificate();
			// Obtain certificate
			InputStream inputStream = new ByteArrayInputStream(certificateString.getBytes(StandardCharsets.UTF_8));
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			X509Certificate certificate = (X509Certificate) cf.generateCertificate(inputStream);

			CryptoX509Certificate wrappedCertificate = new CryptoX509Certificate(certificate);
			String credentialToVerify = wrappedCertificate.getSubjectDn().getCommonName();

			if (credentialToVerify.contains(vote.getCredentialId())) {
				result.setValidationErrorType(ValidationErrorType.SUCCESS);
			}
		} catch (CertificateException | GeneralCryptoLibException e) {
			LOGGER.error("Error validating the credential id in the certificate", e);
		}
		return result;
	}
}

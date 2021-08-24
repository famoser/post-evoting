/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.model.rule;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.votingserver.commons.domain.model.rule.AbstractRule;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.service.certificate.X509CertificateService;

/**
 * The Class implementing the rule for verifying signature of the vote.
 */
public class VerifySignatureRule implements AbstractRule<Vote> {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerifySignatureRule.class);

	// The asymmetric service.
	@Inject
	private AsymmetricServiceAPI asymmetricService;

	@Inject
	private X509CertificateService certificateFactory;

	@Override
	public String getName() {
		return RuleNames.VOTE_VERIFY_SIGNATURE.getText();
	}

	/**
	 * This method executes this rule by checking if signature is verified. @param vote - the vote to be verified. @return The
	 * ValidationResultType.SUCCESS if the signature is valid. Otherwise, a ValidationResultType for the corresponding error. @see
	 * AbstractRule#execute(ch.post.it.evoting.votingserver.electioninformation.services .domain.model.vote.Vote
	 */
	@Override
	public ValidationError execute(Vote vote) {
		ValidationError result = new ValidationError();

		try {

			// Obtain certificate
			InputStream inputStream = new ByteArrayInputStream(vote.getCertificate().getBytes(StandardCharsets.UTF_8));
			X509Certificate certificate = certificateFactory.generateCertificate(inputStream);

			PublicKey publicKey = certificate.getPublicKey();

			// verify signature on encrypted vote
			String signature = vote.getSignature();

			byte[] dataToVerify = StringUtils.join(vote.getFieldsAsStringArray()).getBytes(StandardCharsets.UTF_8);
			byte[] signatureBytes = Base64.decode(signature);
			if (asymmetricService.verifySignature(signatureBytes, publicKey, dataToVerify)) {
				result.setValidationErrorType(ValidationErrorType.SUCCESS);
			}
		} catch (CertificateException | GeneralCryptoLibException e) {
			LOGGER.error("", e);
		}

		return result;
	}
}

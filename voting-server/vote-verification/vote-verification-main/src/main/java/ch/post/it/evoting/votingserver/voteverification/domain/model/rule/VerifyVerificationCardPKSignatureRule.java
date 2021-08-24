/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.domain.model.rule;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

import javax.inject.Inject;
import javax.json.JsonException;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.domain.model.rule.AbstractRule;
import ch.post.it.evoting.votingserver.commons.util.JsonUtils;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset.VerificationSetEntity;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset.VerificationSetRepository;

/**
 * Verifies the digital signature of the Verification Card Public Key using the Verification Card Issuer X.509 Certificate.
 */
public class VerifyVerificationCardPKSignatureRule implements AbstractRule<Vote> {

	private static final String DIGITAL_SIGNATURE_VERIFICATION_ERROR_MESSSAGE = "Error verifying the digital signature of the Verification Card Public Key";
	private static final Logger LOGGER = LoggerFactory.getLogger(VerifyVerificationCardPKSignatureRule.class);
	@Inject
	private AsymmetricServiceAPI asymmetricService;
	@Inject
	private VerificationSetRepository verificationSetRepository;

	/**
	 * @see AbstractRule#execute(Object)
	 */
	@Override
	public ValidationError execute(Vote vote) {
		ValidationError result = new ValidationError();
		try {
			// Obtain certificate
			VerificationSetEntity verificationSetData = verificationSetRepository
					.findByTenantIdElectionEventIdVerificationCardSetId(vote.getTenantId(), vote.getElectionEventId(),
							vote.getVerificationCardSetId());
			String verificationSetDataJson = verificationSetData.getJson();
			JsonObject verificationSetDataJsonObject = JsonUtils.getJsonObject(verificationSetDataJson);
			String certificateString = verificationSetDataJsonObject.getString("verificationCardSetIssuerCert");
			InputStream inputStream = new ByteArrayInputStream(certificateString.getBytes(StandardCharsets.UTF_8));
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			X509Certificate certificate = (X509Certificate) cf.generateCertificate(inputStream);
			PublicKey verificationCardSetIssuerPublicKey = certificate.getPublicKey();

			// verify signature on encrypted vote
			// the signature
			byte[] verificationCardPKSignatureBytes = Base64.getDecoder().decode(vote.getVerificationCardPKSignature());

			// data to verify
			final byte[] verificationCardPublicKeyAsBytes = Base64.getDecoder().decode(vote.getVerificationCardPublicKey());
			final byte[] eeidAsBytes = vote.getElectionEventId().getBytes(StandardCharsets.UTF_8);
			final byte[] verificationCardIDAsBytes = vote.getVerificationCardId().getBytes(StandardCharsets.UTF_8);

			// verification
			if (asymmetricService
					.verifySignature(verificationCardPKSignatureBytes, verificationCardSetIssuerPublicKey, verificationCardPublicKeyAsBytes,
							eeidAsBytes, verificationCardIDAsBytes)) {
				result.setValidationErrorType(ValidationErrorType.SUCCESS);
			} else {
				result.setErrorArgs(new String[] { "Invalid digital signature of the Verification Card Public Key" });
			}
		} catch (CertificateException | GeneralCryptoLibException | ResourceNotFoundException | JsonException | IllegalStateException e) {
			LOGGER.error(DIGITAL_SIGNATURE_VERIFICATION_ERROR_MESSSAGE, e);
			result.setErrorArgs(new String[] { DIGITAL_SIGNATURE_VERIFICATION_ERROR_MESSSAGE });
		}

		return result;
	}

	/**
	 * @see AbstractRule#getName()
	 */
	@Override
	public String getName() {
		return RuleNames.VOTE_VERIFICATION_CARD_PUBLIC_KEY_SIGNATURE_VALIDATION.getText();
	}

}

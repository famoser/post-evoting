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
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.bean.X509CertificateType;
import ch.post.it.evoting.cryptolib.certificates.bean.X509DistinguishedName;
import ch.post.it.evoting.cryptolib.certificates.factory.CryptoX509Certificate;
import ch.post.it.evoting.cryptolib.certificates.utils.X509CertificateChainValidator;
import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.domain.model.rule.AbstractRule;
import ch.post.it.evoting.votingserver.commons.util.JsonUtils;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionInformationContent;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionInformationContentRepository;

/**
 * Checks that the Credential ID signing certificate has been issued by the Credentials CA
 * (certificate chain validation) of the same Election Event for which the vote is cast, that it is
 * not expired, and that its usage is set to "sign".
 */
public class VoteCertificateChainValidationRule implements AbstractRule<Vote> {

	private static final String ELECTION_ROOT_CA = "electionRootCA";

	private static final String CREDENTIALS_CA = "credentialsCA";

	private static final Logger LOGGER = LoggerFactory.getLogger(VoteCertificateChainValidationRule.class);

	@Inject
	private ElectionInformationContentRepository electionInformationContentRepository;

	// Create a credential factory to convert string to X509 certificate
	private CertificateFactory cf;

	/**
	 * Validates a certificate chain. CredentialID Auth Certificate vs CredentialsCA.
	 *
	 * @param vote the vote
	 * @return a result of validation.
	 */
	@Override
	public ValidationError execute(Vote vote) {
		ValidationError result = new ValidationError();

		try {
			String certificateString = vote.getCertificate();

			cf = CertificateFactory.getInstance("X.509");

			// Get Certificates from DB
			ElectionInformationContent electionInformationContent = electionInformationContentRepository
					.findByTenantIdElectionEventId(vote.getTenantId(), vote.getElectionEventId());
			String electionInformationContentJson = electionInformationContent.getJson();

			// Trusted certificate -> Election Root CA
			String rootString = JsonUtils.getJsonObject(electionInformationContentJson).getString(ELECTION_ROOT_CA);

			// Get X509 certificate
			X509Certificate electionRootCA = getX509Cert(rootString);
			X509DistinguishedName distinguishedNameElectionRootCA = getDistinguishName(electionRootCA);

			// Root vs Root chain validation
			X509Certificate[] certificateChainRoot = {};
			X509DistinguishedName[] subjectDnsRoot = {};

			// Validate
			boolean validateCertRootResult = validateCert(electionRootCA, distinguishedNameElectionRootCA, X509CertificateType.CERTIFICATE_AUTHORITY,
					certificateChainRoot, subjectDnsRoot, electionRootCA);
			if (validateCertRootResult) {
				// Leaf certificate
				// Get X509 certificate
				X509Certificate credentialIdAuthCert = getX509Cert(certificateString);
				X509DistinguishedName subjectDnCredentialAuthCert = getDistinguishName(credentialIdAuthCert);

				// Intermediate certificate -> Credentials CA
				String credentialsCAString = JsonUtils.getJsonObject(electionInformationContentJson).getString(CREDENTIALS_CA);

				// Get X509 certificate
				X509Certificate credentialsCA = getX509Cert(credentialsCAString);
				X509DistinguishedName distinguishedNameCredentialsCA = getDistinguishName(credentialsCA);

				// CredentialID Auth Certificate vs CredentialsCA chain
				// validation
				X509Certificate[] certificateChainCredential = { credentialsCA };
				X509DistinguishedName[] subjectDnsCredential = { distinguishedNameCredentialsCA };

				// Validate
				boolean validateCertResult = validateCert(credentialIdAuthCert, subjectDnCredentialAuthCert, X509CertificateType.SIGN,
						certificateChainCredential, subjectDnsCredential, electionRootCA);

				if (validateCertResult) {
					result.setValidationErrorType(ValidationErrorType.SUCCESS);
				} else {
					result.setValidationErrorType(ValidationErrorType.FAILED);
					result.setErrorArgs(new String[] { "Certificate not valid" });
				}
			} else {
				result.setErrorArgs(new String[] { "Election root CA is not a valid certificate authority." });
			}
		} catch (CertificateException | GeneralCryptoLibException e1) {
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

	// Validates certificate
	private boolean validateCert(X509Certificate certLeaf, X509DistinguishedName subjectDnsLeaf, X509CertificateType certType,
			X509Certificate[] certChain, X509DistinguishedName[] subjectDns, X509Certificate certTrusted) throws GeneralCryptoLibException {
		X509CertificateChainValidator certificateChainValidator = createCertificateChainValidator(certLeaf, subjectDnsLeaf, certType, certChain,
				subjectDns, certTrusted);
		List<String> validationResult = certificateChainValidator.validate();
		return validationResult.isEmpty();
	}

	// Creates a new instance of a X509CertificateChainValidator.
	private X509CertificateChainValidator createCertificateChainValidator(X509Certificate certLeaf, X509DistinguishedName subjectDnsLeaf,
			X509CertificateType certType, X509Certificate[] certChain, X509DistinguishedName[] subjectDns, X509Certificate certTrusted)
			throws GeneralCryptoLibException {
		return new X509CertificateChainValidator(certLeaf, certType, subjectDnsLeaf, certChain, subjectDns, certTrusted);
	}

	private X509DistinguishedName getDistinguishName(X509Certificate x509Cert) throws GeneralCryptoLibException {
		CryptoX509Certificate wrappedCertificate = new CryptoX509Certificate(x509Cert);
		return wrappedCertificate.getSubjectDn();
	}

	private X509Certificate getX509Cert(String certificateString) throws CertificateException {
		InputStream inputStream = new ByteArrayInputStream(certificateString.getBytes(StandardCharsets.UTF_8));
		return (X509Certificate) cf.generateCertificate(inputStream);
	}

	/**
	 * @see AbstractRule#getName()
	 */
	@Override
	public String getName() {
		return RuleNames.VOTE_VERIFY_CERT_CHAIN.getText();
	}
}

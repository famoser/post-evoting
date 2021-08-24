/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.authentication.services.domain.service.certificate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.bean.X509CertificateType;
import ch.post.it.evoting.cryptolib.certificates.bean.X509DistinguishedName;
import ch.post.it.evoting.cryptolib.certificates.factory.CryptoX509Certificate;
import ch.post.it.evoting.cryptolib.certificates.utils.X509CertificateChainValidator;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.domain.election.validation.ValidationResult;
import ch.post.it.evoting.votingserver.authentication.services.domain.model.authentication.AuthenticationCerts;
import ch.post.it.evoting.votingserver.authentication.services.domain.model.authentication.AuthenticationCertsRepository;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.SemanticErrorException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.SyntaxErrorException;
import ch.post.it.evoting.votingserver.commons.logging.service.I18nLoggerMessages;
import ch.post.it.evoting.votingserver.commons.util.JsonUtils;
import ch.post.it.evoting.votingserver.commons.util.ValidationUtils;

/**
 * Service for certificate chain validation.
 */
@Stateless
public class CertificateChainValidationService {

	private static final String ELECTION_ROOT_CA = "electionRootCA";

	private static final String CREDENTIALS_CA = "credentialsCA";

	private static final Logger LOGGER = LoggerFactory.getLogger(CertificateChainValidationService.class);

	private static final I18nLoggerMessages I18N = I18nLoggerMessages.getInstance();

	@Inject
	private AuthenticationCertsRepository authenticationCertsRepository;

	// Create a credential factory to convert string to X509 certificate
	private CertificateFactory cf;

	/**
	 * Validates a certificate chain. CredentialID Auth Certificate vs CredentialsCA.
	 *
	 * @param tenantId          - the tenant id.
	 * @param electionEventId   - the election event id.
	 * @param certificateString - the certificate to be validated.
	 * @return a result of validation.
	 */
	public ValidationResult validate(String tenantId, String electionEventId, String certificateString) {
		LOGGER.info(I18N.getMessage("CertificateChainValidationService.validate.starting"));

		ValidationResult result = new ValidationResult(true);

		try {
			cf = CertificateFactory.getInstance("X.509");

			// Get Auth Certificates from DB
			AuthenticationCerts authCerts = authenticationCertsRepository.findByTenantIdElectionEventId(tenantId, electionEventId);
			validateCerts(authCerts);
			String authCertsJson = authCerts.getJson();

			// Trusted certificate -> Election Root CA
			String rootString = JsonUtils.getJsonObject(authCertsJson).getString(ELECTION_ROOT_CA);

			// Get X509 certificate
			X509Certificate electionRootCA = getX509Cert(rootString);
			X509DistinguishedName distinguishedNameElectionRootCA = getDistinguishName(electionRootCA);

			// Root vs Root chain validation
			X509Certificate[] certificateChainRoot = {};
			X509DistinguishedName[] subjectDnsRoot = {};

			// Validate
			boolean validateCertRootResult = validateCert(electionRootCA, distinguishedNameElectionRootCA, X509CertificateType.CERTIFICATE_AUTHORITY,
					certificateChainRoot, subjectDnsRoot, electionRootCA);

			LOGGER.info(I18N.getMessage("CertificateChainValidationService.validate.rootValidated"), validateCertRootResult);

			if (validateCertRootResult) {
				// Leaf certificate -> CredentialId Auth Certificate
				// Get X509 certificate
				X509Certificate credentialIdAuthCert = getX509Cert(certificateString);
				X509DistinguishedName subjectDnCredentialAuthCert = getDistinguishName(credentialIdAuthCert);

				// Intermediate certificate -> Credentials CA
				String credentialsCAString = JsonUtils.getJsonObject(authCertsJson).getString(CREDENTIALS_CA);

				// Get X509 certificate
				X509Certificate credentialsCA = getX509Cert(credentialsCAString);
				X509DistinguishedName distinguishedNameCredentialsCA = getDistinguishName(credentialsCA);

				// CredentialID Auth Certificate vs CredentialsCA chain validation
				X509Certificate[] certificateChainCredential = { credentialsCA };
				X509DistinguishedName[] subjectDnsCredential = { distinguishedNameCredentialsCA };

				// Validate
				boolean validateCertResult = validateCert(credentialIdAuthCert, subjectDnCredentialAuthCert, X509CertificateType.SIGN,
						certificateChainCredential, subjectDnsCredential, electionRootCA);

				result.setResult(validateCertResult);

				if (validateCertResult) {
					LOGGER.info(I18N.getMessage("CertificateChainValidationService.validate.certValidated"), validateCertResult);

				} else {
					LOGGER.warn(I18N.getMessage("CertificateChainValidationService.validate.certValidated"), validateCertResult);

					ValidationError validationError = new ValidationError();
					validationError.setValidationErrorType(ValidationErrorType.INVALID_CERTIFICATE_CHAIN);
					result.setValidationError(validationError);
				}

			} else {
				result.setResult(false);
				result.setValidationError(new ValidationError(ValidationErrorType.INVALID_CERTIFICATE_ROOT_CA));
			}

		} catch (CertificateException | GeneralCryptoLibException e1) {
			LOGGER.error(I18N.getMessage("CertificateChainValidationService.validate.cryptoError"), e1);
			result.setResult(false);
			result.setValidationError(new ValidationError(ValidationErrorType.INVALID_CERTIFICATE_CHAIN));
		} catch (ResourceNotFoundException e2) {
			LOGGER.error(I18N.getMessage("CertificateChainValidationService.validate.certNotFound"), e2);
			result.setResult(false);
			result.setValidationError(new ValidationError(ValidationErrorType.INVALID_CERTIFICATE_CHAIN));
		} catch (SyntaxErrorException e3) {
			LOGGER.error(I18N.getMessage("CertificateChainValidationService.validate.syntaxError"), e3);
			result.setResult(false);
			result.setValidationError(new ValidationError(ValidationErrorType.INVALID_CERTIFICATE_CHAIN));
		} catch (SemanticErrorException e4) {
			LOGGER.error(I18N.getMessage("CertificateChainValidationService.validate.semanticError"), e4);
			result.setResult(false);
			result.setValidationError(new ValidationError(ValidationErrorType.INVALID_CERTIFICATE_CHAIN));
		}

		return result;
	}

	/**
	 * Validates a authCerts object from DB.
	 *
	 * @param authCerts - the AuthenticationCerts object from DB.
	 * @throws SyntaxErrorException   when a syntax exception occurs.
	 * @throws SemanticErrorException when a semantic exception occurs.
	 */
	public void validateCerts(AuthenticationCerts authCerts) throws SyntaxErrorException, SemanticErrorException {
		ValidationUtils.validate(authCerts);
	}

	private boolean validateCert(X509Certificate certLeaf, X509DistinguishedName subjectDnsLeaf, X509CertificateType certType,
			X509Certificate[] certChain, X509DistinguishedName[] subjectDns, X509Certificate certTrusted) throws GeneralCryptoLibException {
		X509CertificateChainValidator certificateChainValidator = createCertificateChainValidator(certLeaf, subjectDnsLeaf, certType, certChain,
				subjectDns, certTrusted);
		List<String> validationResult = certificateChainValidator.validate();
		return validationResult == null || validationResult.isEmpty();
	}

	/**
	 * Create a new instance of a X509CertificateChainValidator.
	 *
	 * @param certLeaf       - the leaf certificate.
	 * @param subjectDnsLeaf - the leaf certificate subject distinguished name.
	 * @param certType       - the leaf certificate type.
	 * @param certChain      - the certificate chain of the leaf certificate.
	 * @param subjectDns     - the subject distinguished names of the certificate chain.
	 * @param certTrusted    - the trusted certificate.
	 * @return X509CertificateChainValidator.
	 * @throws GeneralCryptoLibException if fails trying to create a new object.
	 */
	public X509CertificateChainValidator createCertificateChainValidator(X509Certificate certLeaf, X509DistinguishedName subjectDnsLeaf,
			X509CertificateType certType, X509Certificate[] certChain, X509DistinguishedName[] subjectDns, X509Certificate certTrusted)
			throws GeneralCryptoLibException {
		return new X509CertificateChainValidator(certLeaf, certType, subjectDnsLeaf, new Date(), certChain, subjectDns, certTrusted);
	}

	private X509DistinguishedName getDistinguishName(X509Certificate x509Cert) throws GeneralCryptoLibException {
		CryptoX509Certificate wrappedCertificate = new CryptoX509Certificate(x509Cert);
		return wrappedCertificate.getSubjectDn();
	}

	private X509Certificate getX509Cert(String certificateString) throws CertificateException {
		InputStream inputStream = new ByteArrayInputStream(certificateString.getBytes(StandardCharsets.UTF_8));
		return (X509Certificate) cf.generateCertificate(inputStream);
	}
}

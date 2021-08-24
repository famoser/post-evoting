/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import java.io.IOException;
import java.nio.file.Path;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.List;

import org.bouncycastle.cms.CMSException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.VerifiableElGamalEncryptionParameters;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.utils.SignatureVerifier;

/**
 * This implementation reads the prime numbers and encryption parameters' associated files and validates their signature.
 */
@Service
public class SignaturesVerifierServiceImpl implements SignaturesVerifierService {

	@Autowired
	private SignatureVerifier signatureVerifier;

	@Autowired
	private PathResolver pathResolver;

	/**
	 * This method verifies and trusts the file with the encryption parameters used for that electionEvent in the system.
	 *
	 * @param eeId election event identifier
	 * @return The verified encryption parameters
	 * @throws IOException
	 * @throws CertificateException
	 * @throws CMSException
	 * @throws GeneralCryptoLibException
	 */
	@Override
	public VerifiableElGamalEncryptionParameters verifyEncryptionParams(String eeId)
			throws CMSException, GeneralCryptoLibException, IOException, CertificateException {

		Path trustedChainPath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR, eeId, Constants.CONFIG_FILE_NAME_TRUSTED_CHAIN_PEM);

		if (!trustedChainPath.toFile().exists()) {
			signatureVerifier.saveOfflineTrustedChain(verifyPrimeNumbers(eeId), trustedChainPath);
		}

		Path paramsJwtPath = pathResolver
				.resolve(Constants.CONFIG_FILES_BASE_DIR, eeId, Constants.CONFIG_DIR_NAME_CUSTOMER, Constants.CONFIG_DIR_NAME_OUTPUT,
						Constants.CONFIG_FILE_NAME_ENCRYPTION_PARAMETERS_SIGN_JSON);

		// Can trust the public key used for signing the prime files, it is the same used for the
		// encryption params
		PublicKey trustedKey = getOfflineTrustedChain(eeId).get(0).getPublicKey();

		return signatureVerifier.verifyJwt(paramsJwtPath, trustedKey);
	}

	/**
	 * Verifies a P7 signature with the trusted chain of the SDM
	 *
	 * @param filePath
	 * @param signaturePath
	 * @return
	 * @throws IOException
	 * @throws CMSException
	 * @throws GeneralCryptoLibException
	 * @throws CertificateException
	 */
	@Override
	public Certificate[] verifyPkcs7(Path filePath, Path signaturePath)
			throws IOException, CMSException, GeneralCryptoLibException, CertificateException {
		Path trustedCAPath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR, Constants.CONFIG_FILE_NAME_TRUSTED_CA_PEM);

		return signatureVerifier.verifyPkcs7(filePath, signaturePath, trustedCAPath);

	}

	private List<Certificate> getOfflineTrustedChain(String eeId) throws IOException, CertificateException {
		Path trustedChainPath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR, eeId, Constants.CONFIG_FILE_NAME_TRUSTED_CHAIN_PEM);

		return signatureVerifier.readPemCertificates(trustedChainPath);
	}

	/**
	 * This method verifies and trusts the file with the prime numbers list used for that electionEvent in the system.
	 *
	 * @param eeId election event identifier
	 * @return The verified and trusted certificate chain
	 * @throws CMSException
	 * @throws GeneralCryptoLibException
	 * @throws IOException
	 */
	private Certificate[] verifyPrimeNumbers(String eeId) throws CMSException, GeneralCryptoLibException, IOException, CertificateException {
		Path customerOutputPath = pathResolver
				.resolve(Constants.CONFIG_FILES_BASE_DIR, eeId, Constants.CONFIG_DIR_NAME_CUSTOMER, Constants.CONFIG_DIR_NAME_OUTPUT);

		Path trustedCAPath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR, Constants.CONFIG_FILE_NAME_TRUSTED_CA_PEM);

		// Validate prime numbers
		Path primesSignaturePath = customerOutputPath.resolve(Constants.CONFIG_FILE_NAME_REPRESENTATIONS_CSV_SIGN);
		Path primesPath = customerOutputPath.resolve(Constants.CONFIG_FILE_NAME_REPRESENTATIONS_CSV);
		return signatureVerifier.verifyPkcs7(primesPath, primesSignaturePath, trustedCAPath);
	}

}

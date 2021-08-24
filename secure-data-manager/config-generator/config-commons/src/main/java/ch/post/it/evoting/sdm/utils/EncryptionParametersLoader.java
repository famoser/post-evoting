/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import org.bouncycastle.cms.CMSException;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.VerifiableElGamalEncryptionParameters;
import ch.post.it.evoting.domain.election.EncryptionParameters;
import ch.post.it.evoting.sdm.commons.Constants;

/**
 * An adapter of configObjectMapper class to load a set of Encryption parameters from a JSON file.
 */
public class EncryptionParametersLoader {

	private final SignatureVerifier signatureVerifier;

	public EncryptionParametersLoader() throws CertificateException, NoSuchProviderException {
		signatureVerifier = new SignatureVerifier();
	}

	public EncryptionParameters load(final Path electionEventFolder)
			throws IOException, CertificateException, GeneralCryptoLibException, CMSException {

		Path trustedChainPath = Paths.get(electionEventFolder.toString(), Constants.CONFIG_FILE_NAME_TRUSTED_CHAIN_PEM);
		if (!trustedChainPath.toFile().exists()) {
			Path customerOutputPath = Paths.get(electionEventFolder.toString(), Constants.CONFIG_DIR_NAME_CUSTOMER, Constants.CONFIG_DIR_NAME_OUTPUT);

			Path trustedCAPath = electionEventFolder.getParent().resolve(Constants.CONFIG_FILE_NAME_TRUSTED_CA_PEM);

			// Validate prime numbers to get trusted chain
			Path primesSignaturePath = customerOutputPath.resolve(Constants.CONFIG_FILE_NAME_REPRESENTATIONS_CSV_SIGN);
			Path primesPath = customerOutputPath.resolve(Constants.CONFIG_FILE_NAME_REPRESENTATIONS_CSV);
			Certificate[] chain = signatureVerifier.verifyPkcs7(primesPath, primesSignaturePath, trustedCAPath);
			signatureVerifier.saveOfflineTrustedChain(chain, trustedChainPath);
		}

		PublicKey trustedKey = signatureVerifier.readPemCertificates(trustedChainPath).get(0).getPublicKey();

		final Path jwtPath = Paths.get(electionEventFolder.toString(), Constants.CONFIG_DIR_NAME_CUSTOMER, Constants.CONFIG_DIR_NAME_OUTPUT,
				Constants.CONFIG_FILE_NAME_ENCRYPTION_PARAMETERS_SIGN_JSON);
		if (!Files.exists(jwtPath)) {
			throw new IOException("Encryption parameters file not found in path " + jwtPath.toString());
		}

		VerifiableElGamalEncryptionParameters verifiableElGamalEncryptionParameters = signatureVerifier.verifyJwt(jwtPath, trustedKey);

		return new EncryptionParameters(verifiableElGamalEncryptionParameters.getP().toString(),
				verifiableElGamalEncryptionParameters.getQ().toString(), verifiableElGamalEncryptionParameters.getG().toString());
	}
}

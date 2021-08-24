/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import java.io.IOException;
import java.nio.file.Path;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import org.bouncycastle.cms.CMSException;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.VerifiableElGamalEncryptionParameters;

public interface SignaturesVerifierService {

	/**
	 * Requests the verification of the encryption parameters and prime numbers signature
	 *
	 * @param eeId the election event from which the encryption parameters are going to be verified
	 * @return The verified ElGamalEncryptionParameters object
	 * @throws CertificateException
	 * @throws IOException
	 */
	VerifiableElGamalEncryptionParameters verifyEncryptionParams(String eeId)
			throws IOException, CertificateException, CMSException, GeneralCryptoLibException;

	/**
	 * Verify a file and its chain. P7
	 *
	 * @param filePath
	 * @param signaturePath
	 * @return The trusted chain
	 * @throws IOException
	 * @throws CMSException
	 * @throws GeneralCryptoLibException
	 */
	Certificate[] verifyPkcs7(Path filePath, Path signaturePath) throws IOException, CMSException, GeneralCryptoLibException, CertificateException;
}

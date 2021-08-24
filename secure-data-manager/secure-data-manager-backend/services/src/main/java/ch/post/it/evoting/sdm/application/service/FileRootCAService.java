/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;

/**
 * Service responsible for the loading and storing root CA certificates, such as the platform CA or the tenant CA, in files.
 */
public class FileRootCAService implements RootCAService {
	private final PathResolver pathResolver;

	private final String certificateFileName;

	public FileRootCAService(PathResolver pathResolver, String certificateFileName) {
		this.pathResolver = pathResolver;
		this.certificateFileName = certificateFileName;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.post.it.evoting.sdm.application.service.RootCAService#save(java.
	 * security.cert.X509Certificate)
	 */
	@Override
	public void save(X509Certificate certificate) throws CertificateManagementException {
		String pem;
		try {
			pem = PemUtils.certificateToPem(certificate);
		} catch (GeneralCryptoLibException e) {
			throw new IllegalArgumentException("Invalid certificate", e);
		}
		byte[] bytes = pem.getBytes(StandardCharsets.UTF_8);

		Path file = getCertificatePath();
		try {
			Files.createDirectories(file.getParent());
			Files.write(file, bytes);
		} catch (IOException e) {
			throw new CertificateManagementException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.post.it.evoting.sdm.application.service.RootCAService#load()
	 */
	@Override
	public X509Certificate load() throws CertificateManagementException {
		try {
			byte[] pem = Files.readAllBytes(getCertificatePath());
			return (X509Certificate) PemUtils.certificateFromPem(new String(pem, StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new CertificateManagementException(e);
		} catch (GeneralCryptoLibException e) {
			throw new IllegalStateException("Invalid certificate file", e);
		}
	}

	/**
	 * Returns the configuration PEM file storing the PlatformRoot CA certificate. It is not guaranteed that the returned file really exists.
	 *
	 * @return the certificate file.
	 */
	private Path getCertificatePath() {
		return pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR, certificateFileName);
	}
}

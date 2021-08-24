/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.it.utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.factory.CryptoX509Certificate;

/**
 *
 */
public class KeyLoader {

	public static CryptoX509Certificate convertPEMfiletoCryptoX509Certificate(final String fileLocation)
			throws CertificateException, IOException, GeneralCryptoLibException {
		X509Certificate cert = null;
		try {
			File f = new File(fileLocation);
			FileInputStream fis = new FileInputStream(f);
			BufferedInputStream bis = new BufferedInputStream(fis);
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			while (bis.available() > 0) {
				cert = (X509Certificate) cf.generateCertificate(bis);
			}
			return new CryptoX509Certificate(cert);
		} catch (FileNotFoundException ex) {
			throw new CertificateException(String.format("File:%s not found", fileLocation));
		}
	}

	public static CryptoX509Certificate convertPEMStringtoCryptoX509Certificate(final String certString)
			throws CertificateException, GeneralCryptoLibException {

		X509Certificate cert = null;

		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		ByteArrayInputStream bis = new ByteArrayInputStream(certString.getBytes(StandardCharsets.UTF_8));
		while (bis.available() > 0) {
			cert = (X509Certificate) cf.generateCertificate(bis);
		}

		return new CryptoX509Certificate(cert);

	}

	public static CryptoX509Certificate convertCertificateToCryptoCertificate(Certificate certificate)
			throws CertificateException, GeneralCryptoLibException {

		InputStream certificateInputStream = new ByteArrayInputStream(certificate.getEncoded());
		CertificateFactory _certFactory = CertificateFactory.getInstance("X.509");
		X509Certificate cert = (X509Certificate) _certFactory.generateCertificate(certificateInputStream);

		return new CryptoX509Certificate(cert);

	}

	public static CryptoX509Certificate[] fromCertificateArrayToCryptoCertificateArray(Certificate[] certChain)
			throws CertificateException, GeneralCryptoLibException {

		List<CryptoX509Certificate> cryptoX509CertificateList = new ArrayList<>();

		for (Certificate certificate : certChain) {
			CryptoX509Certificate cryptoX509Certificate = convertCertificateToCryptoCertificate(certificate);
			cryptoX509CertificateList.add(cryptoX509Certificate);
		}

		return cryptoX509CertificateList.toArray(new CryptoX509Certificate[cryptoX509CertificateList.size()]);
	}

}

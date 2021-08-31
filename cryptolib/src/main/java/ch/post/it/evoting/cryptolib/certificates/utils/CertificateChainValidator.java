package ch.post.it.evoting.cryptolib.certificates.utils;

import java.security.cert.X509Certificate;
import java.util.stream.Stream;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.bean.X509CertificateType;
import ch.post.it.evoting.cryptolib.certificates.bean.X509DistinguishedName;
import ch.post.it.evoting.cryptolib.certificates.factory.CryptoX509Certificate;

public class CertificateChainValidator {

	private CertificateChainValidator() {
		// Should not be instantiated
	}

	public static boolean isCertificateChainValid(X509Certificate[] certificateChain, X509Certificate platformRootCA)
			throws GeneralCryptoLibException {
		// Shortcut for obviously wrong certificate chains.
		if (certificateChain.length == 0) {
			return false;
		}

		// Measure the intermediate certificate chain (i.e. minus the leaf certificate).
		int intermediateCertificateChainLength = certificateChain.length - 1;
		// Build the intermediate certificate chain.
		X509Certificate[] intermediateCertificateChain = new X509Certificate[intermediateCertificateChainLength];
		System.arraycopy(certificateChain, 1, intermediateCertificateChain, 0, intermediateCertificateChainLength);
		// Get the intermediate certificate subject DNs.
		X509DistinguishedName[] intermediateCertificateSubjectDNs = Stream.of(intermediateCertificateChain).map(c -> {
			try {
				return getDNFromX509Certificate(c);
			} catch (GeneralCryptoLibException e) {
				throw new IllegalArgumentException("Failed to get distinguished name: " + e.getMessage());
			}
		}).toArray(X509DistinguishedName[]::new);
		// Verify the certificate chain against the root certificate.
		X509Certificate leafCertificate = certificateChain[0];

		X509CertificateChainValidator certificateChainValidator = new X509CertificateChainValidator(leafCertificate, X509CertificateType.SIGN,
				getDNFromX509Certificate(leafCertificate), intermediateCertificateChain, intermediateCertificateSubjectDNs, platformRootCA);

		return certificateChainValidator.validate().isEmpty();
	}

	private static X509DistinguishedName getDNFromX509Certificate(X509Certificate certificate) throws GeneralCryptoLibException {
		return new CryptoX509Certificate(certificate).getSubjectDn();
	}
}

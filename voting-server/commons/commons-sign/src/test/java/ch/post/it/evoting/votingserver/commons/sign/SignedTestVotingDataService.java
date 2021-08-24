/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.sign;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import javax.naming.InvalidNameException;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.domain.election.Ballot;

/**
 * Voting data generator with vote signing.
 */
public class SignedTestVotingDataService extends TestVotingDataService {

	private final PublicKey verificationKey;

	private final PrivateKey signingKey;

	private final X509Certificate[] certificateChain;

	public SignedTestVotingDataService(Ballot ballot, AsymmetricServiceAPI asymmetricService, TestCertificateGenerator testCertificateGenerator)
			throws GeneralCryptoLibException, InvalidNameException {
		super(ballot);

		// Create signing data.

		KeyPair signingKeyPair = asymmetricService.getKeyPairForSigning();
		signingKey = signingKeyPair.getPrivate();
		verificationKey = signingKeyPair.getPublic();

		X509Certificate platformCACertificate = testCertificateGenerator.getRootCertificate();
		X509Certificate validationCertificate = testCertificateGenerator
				.createSigningLeafCertificate(signingKeyPair, testCertificateGenerator.getRootKeyPair().getPrivate(), platformCACertificate,
						"Signing certificate");

		certificateChain = new X509Certificate[] { validationCertificate, platformCACertificate };
	}

	public PublicKey getVerificationKey() {
		return verificationKey;
	}

	public PrivateKey getSigningKey() {
		return signingKey;
	}

	public X509Certificate[] getCertificateChain() {
		return certificateChain;
	}
}
